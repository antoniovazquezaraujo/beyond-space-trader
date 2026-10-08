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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;


class QuestsTest {
  @Test
  void startsAtZero() {
    Quests quests = newQuests();

    assertEquals(0, quests.questStatusArtifact());
    assertEquals(0, quests.questStatusDragonfly());
    assertEquals(0, quests.questStatusExperiment());
    assertEquals(0, quests.questStatusGemulon());
    assertEquals(0, quests.questStatusJapori());
    assertEquals(0, quests.questStatusJarek());
    assertEquals(0, quests.questStatusMoon());
    assertEquals(0, quests.questStatusPrincess());
    assertEquals(0, quests.questStatusScarab());
    assertEquals(0, quests.questStatusSculpture());
    assertEquals(0, quests.questStatusSpaceMonster());
    assertEquals(0, quests.questStatusWild());
    assertEquals(0, quests.reactorStatus(), "the reactor mission is another of the counters");
    assertEquals(0, quests.fabricRipProbability());
    assertFalse(quests.canSuperWarp());
  }

  @Test
  void writesAndReadsEveryQuestStatus() {
    Quests quests = newQuests();

    quests.questStatusArtifact(1);
    quests.questStatusDragonfly(2);
    quests.questStatusExperiment(3);
    quests.questStatusGemulon(4);
    quests.questStatusJapori(5);
    quests.questStatusJarek(6);
    quests.questStatusMoon(7);
    quests.questStatusPrincess(8);
    quests.reactorStatus(9);
    quests.questStatusScarab(10);
    quests.questStatusSculpture(11);
    quests.questStatusSpaceMonster(12);
    quests.questStatusWild(13);

    assertEquals(1, quests.questStatusArtifact());
    assertEquals(2, quests.questStatusDragonfly());
    assertEquals(3, quests.questStatusExperiment());
    assertEquals(4, quests.questStatusGemulon());
    assertEquals(5, quests.questStatusJapori());
    assertEquals(6, quests.questStatusJarek());
    assertEquals(7, quests.questStatusMoon());
    assertEquals(8, quests.questStatusPrincess());
    assertEquals(9, quests.reactorStatus());
    assertEquals(10, quests.questStatusScarab());
    assertEquals(11, quests.questStatusSculpture());
    assertEquals(12, quests.questStatusSpaceMonster());
    assertEquals(13, quests.questStatusWild());
  }

  @Test
  void writesAndReadsTheFabricRipAndTheSingularity() {
    Quests quests = newQuests();

    quests.fabricRipProbability(Consts.FabricRipInitialProbability);
    quests.canSuperWarp(true);

    assertEquals(Consts.FabricRipInitialProbability, quests.fabricRipProbability());
    assertTrue(quests.canSuperWarp());

    quests.fabricRipProbability(0);
    quests.canSuperWarp(false);

    assertEquals(0, quests.fabricRipProbability());
    assertFalse(quests.canSuperWarp());
  }

  @Test
  void isTheReactorStatusTheArrivalReadsAndWrites() {
    Quests quests = newQuests();
    assertInstanceOf(Arrival.ReactorStatus.class, quests);

    Arrival.ReactorStatus reactor = quests;
    reactor.reactorStatus(SpecialEvent.StatusReactorFuelOk);

    assertEquals(SpecialEvent.StatusReactorFuelOk, reactor.reactorStatus());
  }

  @Test
  void savedQuestStateSurvivesARoundTrip() {
    Game game = newGame();
    game.setQuestStatusArtifact(SpecialEvent.StatusArtifactDone);
    game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyDone);
    game.setQuestStatusExperiment(SpecialEvent.StatusExperimentCancelled);
    game.setQuestStatusGemulon(SpecialEvent.StatusGemulonTooLate);
    game.setQuestStatusJapori(SpecialEvent.StatusJaporiDone);
    game.setQuestStatusJarek(SpecialEvent.StatusJarekDone);
    game.setQuestStatusMoon(SpecialEvent.StatusMoonDone);
    game.setQuestStatusPrincess(SpecialEvent.StatusPrincessDone);
    game.setQuestStatusReactor(SpecialEvent.StatusReactorDone);
    game.setQuestStatusScarab(SpecialEvent.StatusScarabDone);
    game.setQuestStatusSculpture(SpecialEvent.StatusSculptureDone);
    game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterDone);
    game.setQuestStatusWild(SpecialEvent.StatusWildDone);
    game.setFabricRipProbability(3);
    game.setCanSuperWarp(true);

    Game loaded = new Game(game.Serialize(), null, new TestDialogService());

    assertEquals(SpecialEvent.StatusArtifactDone, loaded.getQuestStatusArtifact());
    assertEquals(SpecialEvent.StatusDragonflyDone, loaded.getQuestStatusDragonfly());
    assertEquals(SpecialEvent.StatusExperimentCancelled, loaded.getQuestStatusExperiment());
    assertEquals(SpecialEvent.StatusGemulonTooLate, loaded.getQuestStatusGemulon());
    assertEquals(SpecialEvent.StatusJaporiDone, loaded.getQuestStatusJapori());
    assertEquals(SpecialEvent.StatusJarekDone, loaded.getQuestStatusJarek());
    assertEquals(SpecialEvent.StatusMoonDone, loaded.getQuestStatusMoon());
    assertEquals(SpecialEvent.StatusPrincessDone, loaded.getQuestStatusPrincess());
    assertEquals(SpecialEvent.StatusReactorDone, loaded.getQuestStatusReactor());
    assertEquals(SpecialEvent.StatusScarabDone, loaded.getQuestStatusScarab());
    assertEquals(SpecialEvent.StatusSculptureDone, loaded.getQuestStatusSculpture());
    assertEquals(SpecialEvent.StatusSpaceMonsterDone, loaded.getQuestStatusSpaceMonster());
    assertEquals(SpecialEvent.StatusWildDone, loaded.getQuestStatusWild());
    assertEquals(3, loaded.getFabricRipProbability());
    assertTrue(loaded.getCanSuperWarp());
  }

  private static Game newGame() {
    return new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }

  /**
   * A quest component over the objects of a real game: the missions need a
   * generated universe and a commander behind them, but the component itself
   * never sees the game.
   */
  private static Quests newQuests() {
    Game game = newGame();
    return new Quests(game.Commander(), Universe.from(game.Universe(), game.Wormholes()), new Market(), new Newspaper(),
        game.Mercenaries(), game.SpaceMonster(), game.Difficulty(), game.Dialogs(), () -> { });
  }
}
