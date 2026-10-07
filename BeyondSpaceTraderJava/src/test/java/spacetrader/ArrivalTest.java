/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.ShieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;


class ArrivalTest {
  private static final int NO_REACTOR = SpecialEvent.StatusReactorNotStarted;

  private TestDialogService dialogs;
  private Game game;
  private Commander cmdr;
  private Universe universe;
  private Market market;
  private Newspaper newspaper;
  private GameOptions options;

  @BeforeEach
  void setUp() {
    dialogs = new TestDialogService();
    // The universe generation and the commander's current system need a game behind them;
    // the arrival component itself never sees it.
    game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    cmdr = game.Commander();
    universe = Universe.from(game.Universe(), game.Wormholes());
    market = new Market();
    newspaper = new Newspaper();
    options = new GameOptions(false);
  }

  @Test
  void reactorMeltdownWithEscapePodAlertsAndEscapes() {
    cmdr.getShip().setEscapePod(true);
    FakeReactor reactor = new FakeReactor(SpecialEvent.StatusReactorDate);
    RecordingConsequences consequences = new RecordingConsequences();

    arrival(new FakeNavigation(null), reactor, () -> { }, consequences).arrive(system(StarSystemId.Sol));

    assertEquals(List.of(AlertType.ReactorMeltdown), dialogs.alerts());
    assertEquals(SpecialEvent.StatusReactorNotStarted, reactor.reactorStatus());
    assertTrue(consequences.escapedWithPod);
    assertFalse(consequences.destroyed);
  }

  @Test
  void reactorMeltdownWithoutEscapePodIsDestroyed() {
    cmdr.getShip().setEscapePod(false);
    FakeReactor reactor = new FakeReactor(SpecialEvent.StatusReactorDate);
    RecordingConsequences consequences = new RecordingConsequences();

    arrival(new FakeNavigation(null), reactor, () -> { }, consequences).arrive(system(StarSystemId.Sol));

    assertEquals(List.of(AlertType.ReactorMeltdown, AlertType.ReactorDestroyed), dialogs.alerts());
    assertEquals(SpecialEvent.StatusReactorNotStarted, reactor.reactorStatus());
    assertTrue(consequences.destroyed);
    assertFalse(consequences.escapedWithPod);
  }

  @Test
  void reactorWarnsWhenTheFuelRunsOutAndBeforeTheMeltdown() {
    assertReactorWarning(SpecialEvent.StatusReactorFuelOk + 1, AlertType.ReactorWarningFuel);
    assertReactorWarning(SpecialEvent.StatusReactorDate - 4, AlertType.ReactorWarningFuelGone);
    assertReactorWarning(SpecialEvent.StatusReactorDate - 2, AlertType.ReactorWarningTemp);
  }

  @Test
  void tribblesEatFoodAndGrow() {
    Ship ship = cmdr.getShip();
    ship.setTribbles(100);
    int food = TradeItemType.Food.CastToInt();
    ship.Cargo()[food] = 10;
    boolean[] messageCleared = {false};

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> messageCleared[0] = true,
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertTrue(ship.getTribbles() > 100, "the tribbles must grow with food: " + ship.getTribbles());
    assertTrue(ship.Cargo()[food] < 10, "the food must be eaten");
    assertTrue(dialogs.alerts().contains(AlertType.TribblesAteFood));
    assertTrue(messageCleared[0], "the tribble message must be cleared");
  }

  @Test
  void tribblesDieWithTheReactorOnBoard() {
    Ship ship = cmdr.getShip();
    ship.setTribbles(10);
    boolean[] messageCleared = {false};

    arrival(new FakeNavigation(null), new FakeReactor(SpecialEvent.StatusReactorFuelOk), () -> messageCleared[0] = true,
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertEquals(0, ship.getTribbles());
    assertTrue(dialogs.alerts().contains(AlertType.TribblesAllDied));
    assertTrue(messageCleared[0], "the tribble message must be cleared");
  }

  @Test
  void halfTheTribblesDieWithTheReactorOnBoard() {
    Ship ship = cmdr.getShip();
    ship.setTribbles(100);

    arrival(new FakeNavigation(null), new FakeReactor(SpecialEvent.StatusReactorFuelOk), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertEquals(50, ship.getTribbles());
    assertTrue(dialogs.alerts().contains(AlertType.TribblesHalfDied));
  }

  @Test
  void tribblesDieWithNarcoticsOnBoard() {
    Ship ship = cmdr.getShip();
    ship.setTribbles(500);
    int narc = TradeItemType.Narcotics.CastToInt();
    int furs = TradeItemType.Furs.CastToInt();
    ship.Cargo()[narc] = 10;
    int fursBefore = ship.Cargo()[furs];

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertTrue(ship.Cargo()[narc] < 10, "the narcotics must be eaten");
    assertTrue(ship.Cargo()[furs] > fursBefore, "the eaten narcotics become furs");
    assertTrue(ship.getTribbles() < 500, "the tribbles must die: " + ship.getTribbles());
    assertTrue(dialogs.alerts().contains(AlertType.TribblesMostDied));
  }

  @Test
  void aLargeDebtWarnsOnArrival() {
    cmdr.setDebt(Consts.DebtWarning);

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertTrue(dialogs.alerts().contains(AlertType.DebtWarning));
  }

  @Test
  void theLoanReminderRunsEveryFiveDaysWhenTheOptionIsOn() {
    cmdr.setDebt(1000);
    cmdr.setDays(5);
    options.setRemindLoans(true);

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertEquals(List.of(AlertType.DebtReminder), dialogs.alerts());
  }

  @Test
  void noLoanReminderOffTheFiveDayClockOrWithTheOptionOff() {
    cmdr.setDebt(1000);
    cmdr.setDays(4);
    options.setRemindLoans(true);
    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));
    assertFalse(dialogs.alerts().contains(AlertType.DebtReminder));

    cmdr.setDays(5);
    options.setRemindLoans(false);
    dialogs.alerts().clear();
    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Sol));
    assertFalse(dialogs.alerts().contains(AlertType.DebtReminder));
  }

  @Test
  void theEasterEggAtOgGivesALightningShieldForOneOfEachTradeItem() {
    // The starting Gnat has no shield slots; the egg needs a free one.
    Ship ship = new Ship(ShipType.Firefly);
    cmdr.setShip(ship);
    for(int i = 0; i < ship.Cargo().length; i++) {
      ship.Cargo()[i] = 1;
      cmdr.PriceCargo()[i] = 5;
    }
    assertTrue(ship.FreeSlotsShield() > 0);

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(system(StarSystemId.Og));

    assertTrue(dialogs.alerts().contains(AlertType.Egg));
    assertTrue(ship.HasShield(ShieldType.Lightning));
    for(int i = 0; i < ship.Cargo().length; i++) {
      assertEquals(0, ship.Cargo()[i]);
      assertEquals(0, cmdr.PriceCargo()[i]);
    }
  }

  @Test
  void arrivalMovesTheCommanderMarksTheSystemVisitedAndClearsTheNewspaper() {
    StarSystem destination = system(StarSystemId.Sol);
    destination.SpecialEventType(SpecialEventType.Dragonfly);
    newspaper.paid(true);
    FakeNavigation navigation = new FakeNavigation(destination);
    RecordingConsequences consequences = new RecordingConsequences();

    arrival(navigation, new FakeReactor(NO_REACTOR), () -> { }, consequences).arrive(destination);

    assertEquals(destination.Id(), cmdr.getCurrentSystemId());
    assertTrue(destination.Visited());
    assertFalse(newspaper.paid());
    assertEquals(1, navigation.cleared, "the tracked system must be switched off on arrival");
    assertEquals(List.of(NewsEvent.Dragonfly.CastToInt()), newspaper.events(), "the arrival rumours must be added");
    assertFalse(consequences.newspaperShown);
  }

  @Test
  void theTrackingIsOnlyClearedWhenAutoOffIsOn() {
    StarSystem destination = system(StarSystemId.Sol);
    options.setTrackAutoOff(false);
    FakeNavigation navigation = new FakeNavigation(destination);

    arrival(navigation, new FakeReactor(NO_REACTOR), () -> { }, new RecordingConsequences()).arrive(destination);

    assertEquals(0, navigation.cleared);
  }

  @Test
  void arrivalRecalculatesThePricesOfTheSystem() {
    StarSystem destination = system(StarSystemId.Sol);
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      market.buy()[i] = -1;
      market.sell()[i] = -1;
    }

    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { },
        new RecordingConsequences()).arrive(destination);

    int traded = 0;
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      assertTrue(market.sell()[i] >= 0, "the sell prices must be recalculated");
      if(destination.ItemTraded(Consts.TradeItems.get(i))) {
        traded++;
        assertTrue(market.buy()[i] > market.sell()[i], "buy " + market.buy()[i] + " <= sell " + market.sell()[i]);
      } else {
        assertEquals(0, market.buy()[i]);
      }
    }
    assertTrue(traded > 0, "the destination must trade something");
    // The buy prices are derived from the sell prices without randomness.
    assertArrayEquals(
        TradeCalculator.CalculateBuyPrices(destination, market.sell(), cmdr.getPoliceRecordScore(), cmdr.getShip().Trader()),
        market.buy());
  }

  @Test
  void theNewspaperIsShownOnlyWhenTheOptionSaysSo() {
    StarSystem destination = system(StarSystemId.Sol);
    RecordingConsequences hidden = new RecordingConsequences();

    options.setNewsAutoShow(false);
    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { }, hidden).arrive(destination);
    assertFalse(hidden.newspaperShown);

    options.setNewsAutoShow(true);
    RecordingConsequences shown = new RecordingConsequences();
    arrival(new FakeNavigation(null), new FakeReactor(NO_REACTOR), () -> { }, shown).arrive(destination);
    assertTrue(shown.newspaperShown);
  }

  private void assertReactorWarning(int status, AlertType expected) {
    dialogs.alerts().clear();
    FakeReactor reactor = new FakeReactor(status);

    arrival(new FakeNavigation(null), reactor, () -> { }, new RecordingConsequences()).arrive(system(StarSystemId.Sol));

    assertEquals(List.of(expected), dialogs.alerts());
    assertEquals(status, reactor.reactorStatus(), "a warning must not change the mission state");
  }

  private Arrival arrival(Arrival.Navigation navigation, Arrival.ReactorStatus reactor, Runnable clearTribbleMessage,
      Arrival.Consequences consequences) {
    return new Arrival(cmdr, universe, market, newspaper, options, game.Difficulty(), dialogs, game,
        navigation, reactor, clearTribbleMessage, consequences);
  }

  private StarSystem system(StarSystemId id) {
    return universe.systems()[id.CastToInt()];
  }

  /** Navigation double: records whether the tracked system was switched off. */
  private static final class FakeNavigation implements Arrival.Navigation {
    private StarSystem tracked;
    private int cleared;

    FakeNavigation(StarSystem tracked) {
      this.tracked = tracked;
    }

    @Override
    public StarSystem trackedSystem() {
      return tracked;
    }

    @Override
    public void clearTracked() {
      tracked = null;
      cleared++;
    }
  }

  /** Reactor mission state double: keeps the status the arrival reads and writes. */
  private static final class FakeReactor implements Arrival.ReactorStatus {
    private int status;

    FakeReactor(int status) {
      this.status = status;
    }

    @Override
    public int reactorStatus() {
      return status;
    }

    @Override
    public void reactorStatus(int status) {
      this.status = status;
    }
  }

  /** Consequences double: records which callback ran instead of touching the game. */
  private static final class RecordingConsequences implements Arrival.Consequences {
    private boolean escapedWithPod;
    private boolean destroyed;
    private boolean newspaperShown;

    @Override
    public void escapeWithPod() {
      escapedWithPod = true;
    }

    @Override
    public void destroyed() {
      destroyed = true;
    }

    @Override
    public void showNewspaper() {
      newspaperShown = true;
    }
  }
}
