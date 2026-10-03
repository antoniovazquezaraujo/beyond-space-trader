/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.QuestsView;
import org.gts.bst.view.QuestsViewModel;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Game;
import spacetrader.SpecialEvent;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.enums.StarSystemId;


class QuestsPresenterTest {

  @Test
  void showsNoQuestsWhenNothingIsActive() {
    RecordingView view = new RecordingView();

    new QuestsPresenter(newGame(), view).update();

    assertTrue(view.model.quests().isEmpty());
    assertFalse(view.model.hasDestinations());
  }

  @Test
  void listsActiveQuestsWithTheirDestination() {
    Game game = newGame();
    game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(List.of(entry(Strings.QuestMoon, game, StarSystemId.Utopia)), view.model.quests());
    assertTrue(view.model.hasDestinations());
  }

  @Test
  void listsTribblesWithoutDestination() {
    Game game = newGame();
    game.Commander().getShip().setTribbles(3);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(1, view.model.quests().size());
    assertEquals(Strings.QuestTribbles, view.model.quests().get(0).text());
    assertNull(view.model.quests().get(0).systemName());
    assertFalse(view.model.hasDestinations());
  }

  @Test
  void theHiddenBaysQuestFollowsTheSculpture() {
    Game game = newGame();
    game.setQuestStatusSculpture(SpecialEvent.StatusSculptureDelivered);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(List.of(entry(Strings.QuestSculptureHiddenBays, game, StarSystemId.Endor)),
        view.model.quests());
  }

  @Test
  void aDeliveredReactorDoesNotShowTheHiddenBaysQuest() {
    Game game = newGame();
    game.setQuestStatusReactor(SpecialEvent.StatusReactorDelivered);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(List.of(entry(Strings.QuestReactorLaser, game, StarSystemId.Nix)), view.model.quests());
  }

  @Test
  void mapsTheQuestsToTheirDestinationSystems() {
    Game game = newGame();
    game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
    game.setQuestStatusJapori(SpecialEvent.StatusJaporiInTransit);
    game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyRegulas);
    game.setQuestStatusPrincess(SpecialEvent.StatusPrincessFlyInthara);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(List.of(
        entry(Strings.QuestSpaceMonsterKill, game, StarSystemId.Acamar),
        entry(Strings.QuestJaporiDeliver, game, StarSystemId.Japori),
        entry(Strings.QuestDragonflyRegulas, game, StarSystemId.Regulas),
        entry(Strings.QuestPrincessInthara, game, StarSystemId.Inthara)),
        view.model.quests());
  }

  @Test
  void theScarabHuntPointsAtTheWormholeSystem() {
    Game game = newGame();
    game.setQuestStatusScarab(SpecialEvent.StatusScarabHunting);
    StarSystem hiding = Consts.SpecialEvents.get(SpecialEventType.ScarabDestroyed.CastToInt())
        .Location(game.Universe());
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(List.of(new QuestsViewModel.Entry(Strings.QuestScarabFind, hiding.Name())), view.model.quests());
  }

  @Test
  void theArtifactWithoutADeliverySystemHasNoDestination() {
    Game game = newGame();
    game.setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
    Consts.SpecialEvents.get(SpecialEventType.ArtifactDelivery.CastToInt())
        .Location(game.Universe()).SpecialEventType(SpecialEventType.NA);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(1, view.model.quests().size());
    assertEquals(Strings.QuestArtifact, view.model.quests().get(0).text());
    assertNull(view.model.quests().get(0).systemName());
  }

  private static QuestsViewModel.Entry entry(String text, Game game, StarSystemId id) {
    return new QuestsViewModel.Entry(text, game.Universe()[id.CastToInt()].Name());
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
  }

  private static class RecordingView implements QuestsView {
    private QuestsViewModel model;

    @Override
    public void render(QuestsViewModel model) {
      this.model = model;
    }
  }
}
