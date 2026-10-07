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
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.GameWindow;
import org.junit.jupiter.api.Test;
import spacetrader.enums.StarSystemId;


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
