/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.GameWindow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacetrader.enums.Activity;
import spacetrader.enums.AlertType;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;
import spacetrader.util.Hashtable;


/**
 * The voyage component behind hand-written doubles: the departure checks, the
 * travel loop, the timespace rip and the consequences run without the game
 * facade. The seeded test runs the loop over a real game as the encounter
 * context, with a window double that fixes the path of the shared random.
 */
class VoyageTest {
  private TestDialogService dialogs;
  private Game game;
  private Commander cmdr;
  private Universe universe;
  private Market market;
  private Newspaper newspaper;
  private Quests quests;
  private Encounter encounter;
  private int daysAdvanced;
  private RecordingConsequences consequences;

  @BeforeEach
  void setUp() {
    dialogs = new TestDialogService();
    // The universe generation and the commander need a game behind them; the
    // voyage only sees it through the narrow context double and the real
    // objects handed over explicitly.
    game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    cmdr = game.Commander();
    universe = Universe.from(game.Universe(), game.Wormholes());
    market = new Market();
    newspaper = new Newspaper();
    quests = new Quests(cmdr, universe, market, newspaper, game.Mercenaries(), game.SpaceMonster(),
        game.Difficulty(), dialogs, () -> { });
    encounter = new Encounter(game);
    consequences = new RecordingConsequences();
  }

  @Test
  void aDebtOverTheLimitBlocksTheWarp() {
    cmdr.setDebt(Consts.DebtTooLarge + 1);
    int cash = cmdr.getCash();
    int fuel = cmdr.getShip().getFuel();
    StarSystem quiet = quietSystem();

    voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()), new ResultWindow(EncounterResult.Continue),
        new FakeNavigation(quiet)).warp(false);

    assertEquals(List.of(AlertType.DebtTooLargeGrounded), dialogs.alerts());
    assertEquals(cash, cmdr.getCash(), "a blocked warp must not charge anything");
    assertEquals(fuel, cmdr.getShip().getFuel(), "a blocked warp must not spend fuel");
    assertEquals(0, daysAdvanced, "a blocked warp must not spend a day");
    assertFalse(consequences.arrived, "a blocked warp must not arrive");
  }

  @Test
  void wildWithoutABeamLaserBlocksTheWarpWhenThePlayerCancels() {
    boardWild();
    quests.questStatusWild(SpecialEvent.StatusWildStarted);
    dialogs.setResult(DialogResult.Cancel);
    StarSystem quiet = quietSystem();

    voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()), new ResultWindow(EncounterResult.Continue),
        new FakeNavigation(quiet)).warp(false);

    assertEquals(List.of(AlertType.WildWontStayAboardLaser), dialogs.alerts());
    assertEquals(SpecialEvent.StatusWildStarted, quests.questStatusWild(),
        "a cancelled warp must keep Wild on the ship");
    assertEquals(0, daysAdvanced, "a cancelled warp must not spend a day");
    assertFalse(consequences.arrived, "a cancelled warp must not arrive");
  }

  @Test
  void wildWithoutABeamLaserLeavesTheShipAndTheWarpContinues() {
    boardWild();
    quests.questStatusWild(SpecialEvent.StatusWildStarted);
    dialogs.setResult(DialogResult.Yes);
    StarSystem quiet = quietSystem();

    voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()), new ResultWindow(EncounterResult.Continue),
        new FakeNavigation(quiet)).warp(false);

    assertEquals(List.of(AlertType.WildWontStayAboardLaser, AlertType.WildLeavesShip, AlertType.TravelUneventfulTrip),
        dialogs.alerts());
    assertEquals(SpecialEvent.StatusWildNotStarted, quests.questStatusWild(),
        "the leaving of Wild must be written through the quest status");
    assertEquals(1, daysAdvanced, "the departure must spend one day");
    assertTrue(consequences.arrived, "the warp without Wild must arrive");
  }

  @Test
  void anUneventfulTripWarnsAndArrives() {
    StarSystem quiet = quietSystem();
    int cash = cmdr.getCash();
    int fuel = cmdr.getShip().getFuel();
    Voyage voyage = voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()),
        new ResultWindow(EncounterResult.Continue), new FakeNavigation(quiet));

    voyage.warp(false);

    assertEquals(List.of(AlertType.TravelUneventfulTrip), dialogs.alerts());
    assertEquals(cash - 60, cmdr.getCash(), "the departure must charge the three costs");
    assertEquals(fuel, cmdr.getShip().getFuel(), "a destination at the same coordinates spends no fuel");
    assertEquals(1, daysAdvanced, "the departure must spend one day");
    assertEquals(0, voyage.getClicks(), "the loop must walk the twenty clicks");
    assertTrue(consequences.arrived, "the arrival must run after the trip");
  }

  @Test
  void theSingularityAddsTheArrivalNewsBeforeTheReset() {
    StarSystem quiet = quietSystem();
    CountDownSpy current = new CountDownSpy(cmdr.CurrentSystem().Serialize(), newspaper);
    game.Universe()[cmdr.getCurrentSystemId().CastToInt()] = current;
    int cash = cmdr.getCash();
    Voyage voyage = voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()),
        new ResultWindow(EncounterResult.Continue), new FakeNavigation(quiet));

    voyage.warp(true);

    assertEquals(List.of(NewsEvent.ExperimentArrival.CastToInt()), current.newsAtCountDown,
        "the singularity must add the arrival news before the countdown");
    assertTrue(newspaper.events().isEmpty(), "the news reset follows immediately in the same departure");
    assertEquals(cash, cmdr.getCash(), "the singularity jump must charge no departure costs");
    assertEquals(0, daysAdvanced, "the singularity jump must spend no day");
    assertEquals(0, voyage.getClicks(), "the loop must walk the twenty clicks");
    assertTrue(consequences.arrived, "the singularity jump must arrive");
  }

  @Test
  void theTimespaceRipWarnsAndSwitchesTheDestination() {
    quests.questStatusExperiment(SpecialEvent.StatusExperimentPerformed);
    quests.fabricRipProbability(Consts.FabricRipInitialProbability);
    StarSystem quiet = quietSystem();
    FakeNavigation navigation = new FakeNavigation(quiet);
    Voyage voyage = voyage(new QuietContext(cmdr, encounter, quiet, game.Difficulty()),
        new ResultWindow(EncounterResult.Continue), navigation);

    assertFalse(voyage.travel(), "the quiet context must walk the trip without encounters");

    assertTrue(dialogs.alerts().contains(AlertType.SpecialTimespaceFabricRip));
    assertNotNull(navigation.selectedSystemId, "the rip must switch the destination through the navigation");
  }

  @Test
  void theArrestRunsInTheLoopAndTheArrivalRunsAfterIt() {
    StarSystem acamar = system(StarSystemId.Acamar);
    StarSystem quiet = quietSystem();
    Voyage voyage = voyage(new EncounteringContext(cmdr, encounter, game.SpaceMonster(), acamar),
        new ResultWindow(EncounterResult.Arrested), new FakeNavigation(quiet));

    voyage.warp(false);

    assertEquals(List.of("arrested", "arrival"), consequences.order,
        "the arrest must stop the trip in the loop and the arrival must run after");
    assertEquals(-1, voyage.getClicks(), "the arrest must leave the clicks at -1");
    assertEquals(1, daysAdvanced, "the departure still spends its day before the trip");
  }

  @Test
  void theDestructionStopsTheTripBeforeTheArrival() {
    StarSystem acamar = system(StarSystemId.Acamar);
    StarSystem quiet = quietSystem();
    Voyage voyage = voyage(new EncounteringContext(cmdr, encounter, game.SpaceMonster(), acamar),
        new ResultWindow(EncounterResult.Killed), new FakeNavigation(quiet));

    assertThrows(GameOver.class, () -> voyage.warp(false));

    assertEquals(List.of("destroyed"), consequences.order, "destruction must stop the flow before the arrival");
    assertFalse(consequences.arrived);
  }

  @Test
  void theSeededTravelLoopShowsTheQuestEncounterOnTheLastClick() throws ReflectiveOperationException {
    Random previous = replaceRandom(new Random(2L));
    try {
      ClicksWindow window = new ClicksWindow(EncounterResult.Continue);
      Game seeded = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      window.game = seeded;
      seeded.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      seeded.SelectedSystemId(StarSystemId.Acamar);

      boolean encounter = seeded.Travel();

      assertTrue(encounter, "the quest monster must interrupt the trip");
      assertEquals(List.of(1), window.encounterClicks, "the monster waits for the last click");
      assertEquals(0, seeded.getClicks(), "the loop must walk the twenty clicks");
    } finally {
      replaceRandom(previous);
    }
  }

  @Test
  void theSeededTravelLoopEndsTheTripWhenTheEncounterArrests() throws ReflectiveOperationException {
    Random previous = replaceRandom(new Random(2L));
    try {
      ClicksWindow window = new ClicksWindow(EncounterResult.Arrested);
      Game seeded = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      window.game = seeded;
      seeded.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      seeded.SelectedSystemId(StarSystemId.Acamar);

      boolean encounter = seeded.Travel();

      assertTrue(encounter, "the quest monster must interrupt the trip");
      assertEquals(List.of(1), window.encounterClicks, "the monster waits for the last click");
      assertEquals(-1, seeded.getClicks(), "the arrest must leave the clicks at -1");
      assertTrue(dialogs.alerts().contains(AlertType.EncounterArrested), "the real arrest must run");
    } finally {
      replaceRandom(previous);
    }
  }

  @Test
  void theTripStateRoundTripsThroughTheOldSaveKeys() {
    game.setClicks(7);
    game.setArrivedViaWormhole(true);

    Hashtable hash = game.Serialize();

    assertInstanceOf(Integer.class, hash.get("_clicks"));
    assertEquals(7, hash.get("_clicks"));
    assertInstanceOf(Boolean.class, hash.get("_arrivedViaWormhole"));
    assertEquals(Boolean.TRUE, hash.get("_arrivedViaWormhole"));

    Game loaded = new Game(hash, null, dialogs);

    assertEquals(7, loaded.getClicks());
    assertTrue(loaded.getArrivedViaWormhole());
  }

  @Test
  void anOldSaveWithoutTheTripKeysKeepsTheLegacyDefaults() {
    Hashtable save = game.Serialize();
    save.remove("_clicks");
    save.remove("_arrivedViaWormhole");

    Game loaded = new Game(save, null, dialogs);

    assertEquals(0, loaded.getClicks());
    assertFalse(loaded.getArrivedViaWormhole());
  }

  @Test
  void theFacadeDelegatesTheTripStateAndTheCountdown() {
    game.setClicks(3);
    game.setArrivedViaWormhole(true);

    assertEquals(3, game.getClicks());
    assertTrue(game.getArrivedViaWormhole());
    assertEquals(game.Difficulty().CastToInt() + 3, game.CountDownStart());
  }

  private Voyage voyage(EncounterContext context, GameWindow window, Voyage.Navigation navigation) {
    return new Voyage(cmdr, universe, market, newspaper, quests, encounter, context, game.Difficulty(), window,
        dialogs, navigation, new FakeCosts(), days -> daysAdvanced += days, consequences);
  }

  /** A destination whose political system has no activity, so no encounter fires. */
  private QuietSystem quietSystem() {
    return new QuietSystem(cmdr.CurrentSystem());
  }

  /** Builds a system without randomizing its trade items (which reads the game). */
  private static StarSystem system(StarSystemId id) {
    Hashtable hash = new Hashtable();
    hash.add("_id", id.CastToInt());
    return new StarSystem(hash);
  }

  private void boardWild() {
    CrewMember[] crew = cmdr.getShip().Crew();
    crew[crew.length - 1] = game.Mercenaries()[CrewMemberId.Wild.CastToInt()];
  }

  /** Seeds the shared random source of {@link Functions}; returns the source to restore. */
  private static Random replaceRandom(Random random) throws ReflectiveOperationException {
    Field rand = Functions.class.getDeclaredField("rand");
    rand.setAccessible(true);
    Random previous = (Random)rand.get(null);
    rand.set(null, random);
    return previous;
  }

  /** The destination of the trip double: records the system the rip switched to. */
  private static final class FakeNavigation implements Voyage.Navigation {
    private final StarSystem target;
    private StarSystemId selectedSystemId;

    private FakeNavigation(StarSystem target) {
      this.target = target;
    }

    @Override
    public StarSystem warpSystem() {
      return target;
    }

    @Override
    public void selectSystemForRip(StarSystemId systemId) {
      selectedSystemId = systemId;
    }
  }

  /** Departure costs double: fixed amounts so the arithmetic stays visible. */
  private static final class FakeCosts implements Voyage.DepartureCosts {
    @Override
    public int mercenaryCosts() {
      return 10;
    }

    @Override
    public int insuranceCosts() {
      return 20;
    }

    @Override
    public int wormholeCosts() {
      return 30;
    }
  }

  /** Consequences double: records the callbacks in order instead of touching the game. */
  private static final class RecordingConsequences implements Voyage.Consequences {
    private final List<String> order = new ArrayList<>();
    private boolean arrived;

    @Override
    public void arrested() {
      order.add("arrested");
    }

    @Override
    public void escapeWithPod() {
      order.add("escapeWithPod");
    }

    @Override
    public void destroyed() {
      order.add("destroyed");
      throw new GameOver();
    }

    @Override
    public void arrival() {
      arrived = true;
      order.add("arrival");
    }
  }

  /** Stops the flow the way the end-of-game exception does in the real game. */
  private static final class GameOver extends RuntimeException {
  }

  /** Window double: always continues the trip. */
  private static final class ResultWindow implements GameWindow {
    private final EncounterResult result;

    private ResultWindow(EncounterResult result) {
      this.result = result;
    }

    @Override
    public EncounterResult showEncounter() {
      return result;
    }

    @Override
    public void showNewspaper() {
    }

    @Override
    public void UpdateStatusBar() {
    }

    @Override
    public void UpdateAll() {
    }
  }

  /** Window double: records the clicks left when the encounter shows. */
  private static final class ClicksWindow implements GameWindow {
    private final EncounterResult result;
    private final List<Integer> encounterClicks = new ArrayList<>();
    private Game game;

    private ClicksWindow(EncounterResult result) {
      this.result = result;
    }

    @Override
    public EncounterResult showEncounter() {
      encounterClicks.add(game.getClicks());
      return result;
    }

    @Override
    public void showNewspaper() {
    }

    @Override
    public void UpdateStatusBar() {
    }

    @Override
    public void UpdateAll() {
    }
  }

  /**
   * Context double for the quiet trips: it only answers the rolls with a system
   * whose activities are all absent, so the generator never shows an encounter.
   */
  private static final class QuietContext implements EncounterContext {
    private final Commander commander;
    private final Encounter encounter;
    private final StarSystem warpSystem;
    private final Difficulty difficulty;

    private QuietContext(Commander commander, Encounter encounter, StarSystem warpSystem, Difficulty difficulty) {
      this.commander = commander;
      this.encounter = encounter;
      this.warpSystem = warpSystem;
      this.difficulty = difficulty;
    }

    @Override
    public Encounter encounter() {
      return encounter;
    }

    @Override
    public Commander Commander() {
      return commander;
    }

    @Override
    public StarSystem WarpSystem() {
      return warpSystem;
    }

    @Override
    public int getClicks() {
      return 0;
    }

    @Override
    public boolean getArrivedViaWormhole() {
      return false;
    }

    @Override
    public Difficulty Difficulty() {
      return difficulty;
    }

    @Override
    public GameOptions Options() {
      throw notOnThePath("Options()");
    }

    @Override
    public ArrayList<VeryRareEncounter> VeryRareEncounters() {
      throw notOnThePath("VeryRareEncounters()");
    }

    @Override
    public int getChanceOfVeryRareEncounter() {
      throw notOnThePath("getChanceOfVeryRareEncounter()");
    }

    @Override
    public int getChanceOfTradeInOrbit() {
      throw notOnThePath("getChanceOfTradeInOrbit()");
    }

    @Override
    public Ship SpaceMonster() {
      throw notOnThePath("SpaceMonster()");
    }

    @Override
    public Ship Scorpion() {
      throw notOnThePath("Scorpion()");
    }

    @Override
    public Ship Scarab() {
      throw notOnThePath("Scarab()");
    }

    @Override
    public Ship Dragonfly() {
      throw notOnThePath("Dragonfly()");
    }

    @Override
    public int getQuestStatusSpaceMonster() {
      throw notOnThePath("getQuestStatusSpaceMonster()");
    }

    @Override
    public int getQuestStatusScarab() {
      throw notOnThePath("getQuestStatusScarab()");
    }

    @Override
    public int getQuestStatusPrincess() {
      throw notOnThePath("getQuestStatusPrincess()");
    }

    @Override
    public int getQuestStatusGemulon() {
      throw notOnThePath("getQuestStatusGemulon()");
    }

    @Override
    public int getQuestStatusDragonfly() {
      throw notOnThePath("getQuestStatusDragonfly()");
    }

    private static UnsupportedOperationException notOnThePath(String method) {
      return new UnsupportedOperationException(method + " is not part of the quiet path");
    }
  }

  /**
   * Context double for the encounter paths: it always answers with the space
   * monster of the quest, which the generator only needs the clicks, the
   * destination and the quest state for.
   */
  private static final class EncounteringContext implements EncounterContext {
    private final Commander commander;
    private final Encounter encounter;
    private final Ship spaceMonster;
    private final StarSystem warpSystem;

    private EncounteringContext(Commander commander, Encounter encounter, Ship spaceMonster, StarSystem warpSystem) {
      this.commander = commander;
      this.encounter = encounter;
      this.spaceMonster = spaceMonster;
      this.warpSystem = warpSystem;
    }

    @Override
    public Encounter encounter() {
      return encounter;
    }

    @Override
    public Commander Commander() {
      return commander;
    }

    @Override
    public StarSystem WarpSystem() {
      return warpSystem;
    }

    @Override
    public int getClicks() {
      return 1;
    }

    @Override
    public Ship SpaceMonster() {
      return spaceMonster;
    }

    @Override
    public int getQuestStatusSpaceMonster() {
      return SpecialEvent.StatusSpaceMonsterAtAcamar;
    }

    @Override
    public boolean getArrivedViaWormhole() {
      throw notOnThePath("getArrivedViaWormhole()");
    }

    @Override
    public GameOptions Options() {
      throw notOnThePath("Options()");
    }

    @Override
    public Difficulty Difficulty() {
      throw notOnThePath("Difficulty()");
    }

    @Override
    public ArrayList<VeryRareEncounter> VeryRareEncounters() {
      throw notOnThePath("VeryRareEncounters()");
    }

    @Override
    public int getChanceOfVeryRareEncounter() {
      throw notOnThePath("getChanceOfVeryRareEncounter()");
    }

    @Override
    public int getChanceOfTradeInOrbit() {
      throw notOnThePath("getChanceOfTradeInOrbit()");
    }

    @Override
    public Ship Scorpion() {
      throw notOnThePath("Scorpion()");
    }

    @Override
    public Ship Scarab() {
      throw notOnThePath("Scarab()");
    }

    @Override
    public Ship Dragonfly() {
      throw notOnThePath("Dragonfly()");
    }

    @Override
    public int getQuestStatusScarab() {
      throw notOnThePath("getQuestStatusScarab()");
    }

    @Override
    public int getQuestStatusPrincess() {
      throw notOnThePath("getQuestStatusPrincess()");
    }

    @Override
    public int getQuestStatusGemulon() {
      throw notOnThePath("getQuestStatusGemulon()");
    }

    @Override
    public int getQuestStatusDragonfly() {
      throw notOnThePath("getQuestStatusDragonfly()");
    }

    private static UnsupportedOperationException notOnThePath(String method) {
      return new UnsupportedOperationException(method + " is not part of the encounter path");
    }
  }

  /**
   * Star system double without any activity: the encounter rolls of the
   * generator always miss it.
   */
  private static final class QuietSystem extends StarSystem {
    private static final PoliticalSystem QUIET = new PoliticalSystem(PoliticalSystemType.Anarchy, 0,
        Activity.Absent, Activity.Absent, Activity.Absent, TechLevel.t0, TechLevel.t7, 0, true, true,
        TradeItemType.Water);

    private QuietSystem(StarSystem around) {
      super(StarSystemId.Sol, around.X(), around.Y(), around.Size(), around.TechLevel(), around.PoliticalSystemType(),
          around.SystemPressure(), around.SpecialResource());
    }

    @Override
    public void InitializeTradeItems() {
      // The quiet destination only answers the encounter rolls.
    }

    @Override
    public PoliticalSystem PoliticalSystem() {
      return QUIET;
    }
  }

  /** Star system double that reports the news the singularity adds before the countdown. */
  private static final class CountDownSpy extends StarSystem {
    private final Newspaper newspaper;
    private final List<Integer> newsAtCountDown = new ArrayList<>();

    private CountDownSpy(Hashtable hash, Newspaper newspaper) {
      super(hash);
      this.newspaper = newspaper;
    }

    @Override
    public void CountDown(int countDown) {
      newsAtCountDown.addAll(newspaper.events());
      super.CountDown(countDown);
    }
  }
}
