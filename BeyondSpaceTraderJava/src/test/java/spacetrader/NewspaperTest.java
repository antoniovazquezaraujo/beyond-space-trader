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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ports.DialogService;
import org.gts.bst.ports.GameWindow;
import org.junit.jupiter.api.Test;
import spacetrader.enums.StarSystemId;
import spacetrader.util.Hashtable;


class NewspaperTest {
  @Test
  void showsTheNewspaperThroughTheWindowWhenItIsPaid() {
    WindowDouble window = new WindowDouble();
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
    game.Options().setNewsAutoPay(true);
    game.setPaidForNewspaper(false);

    game.ShowNewspaper();

    assertEquals(1, window.newspapers);
    assertTrue(game.getPaidForNewspaper());
  }

  @Test
  void asksBeforeShowingIt() {
    WindowDouble window = new WindowDouble();
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
    game.Options().setNewsAutoPay(false);
    game.setPaidForNewspaper(false);

    game.ShowNewspaper();

    assertEquals(0, window.newspapers);
    assertFalse(game.getPaidForNewspaper());
  }

  @Test
  void startsEmptyAndUnpaid() {
    Newspaper newspaper = new Newspaper();

    assertTrue(newspaper.events().isEmpty());
    assertFalse(newspaper.paid());
  }

  @Test
  void addsEventsAndReadsTheLatest() {
    Newspaper newspaper = new Newspaper();

    newspaper.add(NewsEvent.Dragonfly);
    newspaper.add(NewsEvent.CaughtLittering);

    assertEquals(List.of(NewsEvent.Dragonfly.CastToInt(), NewsEvent.CaughtLittering.CastToInt()), newspaper.events());
    assertEquals(NewsEvent.CaughtLittering.CastToInt(), newspaper.latest());
  }

  @Test
  void eventsIsTheLiveList() {
    Newspaper newspaper = new Newspaper();

    newspaper.events().add(7);

    assertEquals(7, newspaper.latest());
  }

  @Test
  void latestFailsWhenThereAreNoEvents() {
    Newspaper newspaper = new Newspaper();

    assertThrows(IndexOutOfBoundsException.class, newspaper::latest);
  }

  @Test
  void replacesAnEventWithTheNewOne() {
    Newspaper newspaper = new Newspaper();
    newspaper.add(NewsEvent.Dragonfly);
    newspaper.add(NewsEvent.CaughtLittering);

    newspaper.replace(NewsEvent.CaughtLittering.CastToInt(), NewsEvent.Japori.CastToInt());

    assertEquals(List.of(NewsEvent.Dragonfly.CastToInt(), NewsEvent.Japori.CastToInt()), newspaper.events());
  }

  @Test
  void replaceAddsTheEventWhenTheOldOneIsNotThere() {
    Newspaper newspaper = new Newspaper();

    newspaper.replace(NewsEvent.Scarab.CastToInt(), NewsEvent.ScarabHarass.CastToInt());

    assertEquals(List.of(NewsEvent.ScarabHarass.CastToInt()), newspaper.events());
  }

  @Test
  void resetsTheEvents() {
    Newspaper newspaper = new Newspaper();
    newspaper.add(NewsEvent.Scarab);

    newspaper.reset();

    assertTrue(newspaper.events().isEmpty());
  }

  @Test
  void paidFlagCanBeSet() {
    Newspaper newspaper = new Newspaper();

    newspaper.paid(true);
    assertTrue(newspaper.paid());

    newspaper.paid(false);
    assertFalse(newspaper.paid());
  }

  @Test
  void headMatchesTheFacade() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();

    String head = newspaper.head(game.Commander());

    assertFalse(head.isEmpty());
    assertEquals(game.NewspaperHead(), head);
  }

  @Test
  void textMatchesTheFacadeAndIncludesTheCommanderName() {
    Game game = newGame();
    game.NewsAddEvent(NewsEvent.CaughtLittering);
    Newspaper newspaper = new Newspaper();
    newspaper.add(NewsEvent.CaughtLittering);

    String text = newspaper.text(game.Commander(), game.Universe(), game.Difficulty());

    assertEquals(game.NewspaperText(), text);
    assertTrue(text.contains(game.Commander().Name()));
  }

  @Test
  void savedStateSurvivesAHashRoundTrip() {
    Newspaper newspaper = new Newspaper();
    newspaper.add(NewsEvent.WildArrested);
    newspaper.add(NewsEvent.Japori);
    newspaper.paid(true);
    Hashtable save = new Hashtable();

    newspaper.saveTo(save);
    Newspaper loaded = new Newspaper();
    loaded.loadFrom(save);

    assertEquals(newspaper.events(), loaded.events());
    assertTrue(loaded.paid());
  }

  @Test
  void savedNewspaperStateSurvivesARoundTrip() {
    Game game = newGame();
    game.NewsAddEvent(NewsEvent.WildArrested);
    game.setPaidForNewspaper(true);

    Game loaded = new Game(game.Serialize(), null, new TestDialogService());

    assertEquals(game.NewsEvents(), loaded.NewsEvents());
    assertTrue(loaded.getPaidForNewspaper());
  }

  @Test
  void addsTheRumorOfTheSystemAlways() {
    Game game = newGame();
    StarSystem system = game.Universe()[StarSystemId.Baratas.CastToInt()];
    system.SpecialEventType(SpecialEventType.Dragonfly);
    Newspaper newspaper = new Newspaper();

    newspaper.addEventsOnArrival(system, new FakeQuests());

    assertEquals(List.of(NewsEvent.Dragonfly.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheDragonflyRumorOnlyWhenTheQuestIsThere() {
    Game game = newGame();
    StarSystem system = game.Universe()[StarSystemId.Baratas.CastToInt()];
    system.SpecialEventType(SpecialEventType.DragonflyBaratas);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.dragonfly = SpecialEvent.StatusDragonflyFlyBaratas;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.DragonflyBaratas.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheDragonflyDestroyedRumors() {
    Game game = newGame();
    StarSystem system = game.Universe()[StarSystemId.Zalkon.CastToInt()];
    system.SpecialEventType(SpecialEventType.DragonflyDestroyed);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    quests.dragonfly = SpecialEvent.StatusDragonflyFlyZalkon;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.DragonflyZalkon.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.dragonfly = SpecialEvent.StatusDragonflyDestroyed;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.DragonflyDestroyed.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.dragonfly = SpecialEvent.StatusDragonflyNotStarted;
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());
  }

  @Test
  void addsTheArtifactRumorOnlyWhenItIsOnBoard() {
    Game game = newGame();
    StarSystem system = game.Universe()[0];
    system.SpecialEventType(SpecialEventType.ArtifactDelivery);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.artifactOnBoard = true;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.ArtifactDelivery.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheJarekAndWildRumorsOnlyWhenTheyAreOnBoard() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    StarSystem jarekSystem = game.Universe()[StarSystemId.Devidia.CastToInt()];
    jarekSystem.SpecialEventType(SpecialEventType.JarekGetsOut);

    newspaper.addEventsOnArrival(jarekSystem, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.jarekOnBoard = true;
    newspaper.addEventsOnArrival(jarekSystem, quests);
    assertEquals(List.of(NewsEvent.JarekGetsOut.CastToInt()), newspaper.events());

    StarSystem wildSystem = game.Universe()[StarSystemId.Kravat.CastToInt()];
    wildSystem.SpecialEventType(SpecialEventType.WildGetsOut);
    newspaper.reset();

    newspaper.addEventsOnArrival(wildSystem, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.wildOnBoard = true;
    newspaper.addEventsOnArrival(wildSystem, quests);
    assertEquals(List.of(NewsEvent.WildGetsOut.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheExperimentRumorOnlyWhileTheExperimentRuns() {
    Game game = newGame();
    StarSystem system = game.Universe()[0];
    system.SpecialEventType(SpecialEventType.ExperimentStopped);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.experiment = SpecialEvent.StatusExperimentPerformed;
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.experiment = SpecialEvent.StatusExperimentStarted;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.ExperimentStopped.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheGemulonRumors() {
    Game game = newGame();
    StarSystem system = game.Universe()[StarSystemId.Gemulon.CastToInt()];
    system.SpecialEventType(SpecialEventType.GemulonRescued);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.gemulon = SpecialEvent.StatusGemulonStarted;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.GemulonRescued.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.gemulon = SpecialEvent.StatusGemulonTooLate;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.GemulonInvaded.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheSpaceMonsterRumors() {
    Game game = newGame();
    StarSystem system = game.Universe()[StarSystemId.Acamar.CastToInt()];
    system.SpecialEventType(SpecialEventType.SpaceMonsterKilled);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.spaceMonster = SpecialEvent.StatusSpaceMonsterAtAcamar;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.SpaceMonster.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.spaceMonster = SpecialEvent.StatusSpaceMonsterDestroyed;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.SpaceMonsterKilled.CastToInt()), newspaper.events());
  }

  @Test
  void addsNothingWithoutASpecialEvent() {
    Game game = newGame();
    StarSystem system = game.Universe()[0];
    system.SpecialEventType(SpecialEventType.NA);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    quests.dragonfly = SpecialEvent.StatusDragonflyFlyBaratas;
    quests.artifactOnBoard = true;
    quests.jarekOnBoard = true;
    quests.wildOnBoard = true;

    newspaper.addEventsOnArrival(system, quests);

    assertTrue(newspaper.events().isEmpty());
  }

  @Test
  void theFacadeAddsTheRumorForAQuestState() {
    Game game = newGame();
    game.Commander().CurrentSystem().SpecialEventType(SpecialEventType.ArtifactDelivery);
    game.setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);

    game.NewsAddEventsOnArrival();

    assertEquals(List.of(NewsEvent.ArtifactDelivery.CastToInt()), game.NewsEvents());
  }

  @Test
  void theFacadeAddsTheRumorWhenTheQuestStateMatches() {
    Game game = newGame();
    game.Commander().CurrentSystem().SpecialEventType(SpecialEventType.DragonflyBaratas);
    game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyBaratas);

    game.NewsAddEventsOnArrival();

    assertEquals(List.of(NewsEvent.DragonflyBaratas.CastToInt()), game.NewsEvents());
  }

  @Test
  void theFacadeReplacesAndResetsTheEvents() {
    Game game = newGame();
    game.NewsAddEvent(NewsEvent.Dragonfly);
    game.NewsAddEvent(NewsEvent.CaughtLittering);

    game.NewsReplaceEvent(NewsEvent.CaughtLittering.CastToInt(), NewsEvent.Japori.CastToInt());
    assertEquals(List.of(NewsEvent.Dragonfly.CastToInt(), NewsEvent.Japori.CastToInt()), game.NewsEvents());

    game.NewsResetEvents();
    assertTrue(game.NewsEvents().isEmpty());
  }

  @Test
  void theFacadeReadsTheLatestEventFromTheLiveList() {
    Game game = newGame();
    game.NewsAddEvent(NewsEvent.Dragonfly);
    game.NewsAddEvent(NewsEvent.CaughtLittering);

    assertEquals(NewsEvent.CaughtLittering.CastToInt(), game.NewsLatestEvent());

    game.NewsEvents().add(NewsEvent.Scarab.CastToInt());
    assertEquals(NewsEvent.Scarab.CastToInt(), game.NewsLatestEvent());
  }

  @Test
  void theFacadeAddsTheJarekAndWildRumorsOnlyWhenTheyAreOnBoard() {
    Game game = newGame();
    StarSystem system = game.Commander().CurrentSystem();
    system.SpecialEventType(SpecialEventType.JarekGetsOut);

    game.NewsAddEventsOnArrival();
    assertTrue(game.NewsEvents().isEmpty());

    board(game, CrewMemberId.Jarek);
    game.NewsAddEventsOnArrival();
    assertEquals(List.of(NewsEvent.JarekGetsOut.CastToInt()), game.NewsEvents());

    game.NewsResetEvents();
    system.SpecialEventType(SpecialEventType.WildGetsOut);
    game.NewsAddEventsOnArrival();
    assertTrue(game.NewsEvents().isEmpty());

    board(game, CrewMemberId.Wild);
    game.NewsAddEventsOnArrival();
    assertEquals(List.of(NewsEvent.WildGetsOut.CastToInt()), game.NewsEvents());
  }

  @Test
  void addsTheDragonflyMelinaAndRegulasRumors() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    StarSystem system = systemWith(game, SpecialEventType.DragonflyMelina);

    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.dragonfly = SpecialEvent.StatusDragonflyFlyMelina;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.DragonflyMelina.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.DragonflyRegulas);
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.dragonfly = SpecialEvent.StatusDragonflyFlyRegulas;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.DragonflyRegulas.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheExperimentFailedRumorAlways() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();

    newspaper.addEventsOnArrival(systemWith(game, SpecialEventType.ExperimentFailed), new FakeQuests());

    assertEquals(List.of(NewsEvent.ExperimentFailed.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheGemulonRumorAlways() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();

    newspaper.addEventsOnArrival(systemWith(game, SpecialEventType.Gemulon), new FakeQuests());

    assertEquals(List.of(NewsEvent.Gemulon.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheGemulonInvadedRumorWhenTheQuestIsDone() {
    Game game = newGame();
    StarSystem system = systemWith(game, SpecialEventType.GemulonRescued);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    quests.gemulon = SpecialEvent.StatusGemulonDone;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.GemulonInvaded.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.gemulon = SpecialEvent.StatusGemulonNotStarted;
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());
  }

  @Test
  void addsTheJaporiRumorsOnlyWhileTheErrandIsPending() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    StarSystem japoriSystem = systemWith(game, SpecialEventType.Japori);

    quests.japori = SpecialEvent.StatusJaporiInTransit;
    newspaper.addEventsOnArrival(japoriSystem, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.japori = SpecialEvent.StatusJaporiNotStarted;
    newspaper.addEventsOnArrival(japoriSystem, quests);
    assertEquals(List.of(NewsEvent.Japori.CastToInt()), newspaper.events());

    newspaper.reset();
    StarSystem deliverySystem = systemWith(game, SpecialEventType.JaporiDelivery);
    quests.japori = SpecialEvent.StatusJaporiNotStarted;
    newspaper.addEventsOnArrival(deliverySystem, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.japori = SpecialEvent.StatusJaporiInTransit;
    newspaper.addEventsOnArrival(deliverySystem, quests);
    assertEquals(List.of(NewsEvent.JaporiDelivery.CastToInt()), newspaper.events());
  }

  @Test
  void addsThePrincessRumors() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    StarSystem system = systemWith(game, SpecialEventType.Princess);

    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.Princess.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.PrincessCentauri);
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.princess = SpecialEvent.StatusPrincessFlyCentauri;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.PrincessCentauri.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.PrincessInthara);
    quests.princess = SpecialEvent.StatusPrincessFlyInthara;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.PrincessInthara.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.PrincessQonos);
    quests.princess = SpecialEvent.StatusPrincessFlyQonos;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.PrincessQonos.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.princess = SpecialEvent.StatusPrincessRescued;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.PrincessRescued.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.PrincessReturned);
    quests.princess = SpecialEvent.StatusPrincessReturned;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.PrincessReturned.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheScarabRumors() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();
    StarSystem system = systemWith(game, SpecialEventType.Scarab);

    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.Scarab.CastToInt()), newspaper.events());

    newspaper.reset();
    system.SpecialEventType(SpecialEventType.ScarabDestroyed);
    newspaper.addEventsOnArrival(system, quests);
    assertTrue(newspaper.events().isEmpty());

    quests.scarab = SpecialEvent.StatusScarabHunting;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.ScarabHarass.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.scarab = SpecialEvent.StatusScarabDestroyed;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.ScarabDestroyed.CastToInt()), newspaper.events());

    newspaper.reset();
    quests.scarab = SpecialEvent.StatusScarabDone;
    newspaper.addEventsOnArrival(system, quests);
    assertEquals(List.of(NewsEvent.ScarabDestroyed.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheSculptureRumors() {
    Game game = newGame();
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    newspaper.addEventsOnArrival(systemWith(game, SpecialEventType.Sculpture), quests);
    assertEquals(List.of(NewsEvent.SculptureStolen.CastToInt()), newspaper.events());

    newspaper.reset();
    newspaper.addEventsOnArrival(systemWith(game, SpecialEventType.SculptureDelivered), quests);
    assertEquals(List.of(NewsEvent.SculptureTracked.CastToInt()), newspaper.events());
  }

  @Test
  void addsTheSpaceMonsterKilledRumorWhenTheMonsterIsGone() {
    Game game = newGame();
    StarSystem system = systemWith(game, SpecialEventType.SpaceMonsterKilled);
    Newspaper newspaper = new Newspaper();
    FakeQuests quests = new FakeQuests();

    quests.spaceMonster = SpecialEvent.StatusSpaceMonsterDone;
    newspaper.addEventsOnArrival(system, quests);

    assertEquals(List.of(NewsEvent.SpaceMonsterKilled.CastToInt()), newspaper.events());
  }

  private StarSystem systemWith(Game game, SpecialEventType type) {
    StarSystem system = game.Universe()[0];
    system.SpecialEventType(type);
    return system;
  }

  private void board(Game game, CrewMemberId id) {
    CrewMember[] crew = game.Commander().getShip().Crew();
    crew[crew.length - 1] = game.Mercenaries()[id.CastToInt()];
  }

  private Game newGame() {
    return new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
  }

  private static final class FakeQuests implements QuestStates {
    private int dragonfly;
    private int experiment;
    private int gemulon;
    private int japori;
    private int princess;
    private int scarab;
    private int spaceMonster;
    private boolean artifactOnBoard;
    private boolean jarekOnBoard;
    private boolean wildOnBoard;

    @Override
    public int getQuestStatusDragonfly() {
      return dragonfly;
    }

    @Override
    public int getQuestStatusExperiment() {
      return experiment;
    }

    @Override
    public int getQuestStatusGemulon() {
      return gemulon;
    }

    @Override
    public int getQuestStatusJapori() {
      return japori;
    }

    @Override
    public int getQuestStatusPrincess() {
      return princess;
    }

    @Override
    public int getQuestStatusScarab() {
      return scarab;
    }

    @Override
    public int getQuestStatusSpaceMonster() {
      return spaceMonster;
    }

    @Override
    public boolean artifactOnBoard() {
      return artifactOnBoard;
    }

    @Override
    public boolean jarekOnBoard() {
      return jarekOnBoard;
    }

    @Override
    public boolean wildOnBoard() {
      return wildOnBoard;
    }
  }

  private static class WindowDouble implements GameWindow {
    private int newspapers;

    @Override
    public EncounterResult showEncounter() {
      return EncounterResult.Continue;
    }

    @Override
    public void showNewspaper() {
      newspapers++;
    }

    @Override
    public void UpdateStatusBar() {
    }

    @Override
    public void UpdateAll() {
    }
  }
}
