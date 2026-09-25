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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.QuestsView;
import org.gts.bst.view.QuestsViewModel;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.SpecialEvent;
import spacetrader.Strings;


class QuestsPresenterTest {

  @Test
  void showsNoQuestsWhenNothingIsActive() {
    RecordingView view = new RecordingView();

    new QuestsPresenter(newGame(), view).update();

    assertEquals(Strings.QuestNone, view.model.text());
    assertFalse(view.model.hasQuests());
  }

  @Test
  void listsActiveQuests() {
    Game game = newGame();
    game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(Strings.QuestMoon, view.model.text());
    assertTrue(view.model.hasQuests());
  }

  @Test
  void listsTribblesAsAQuest() {
    Game game = newGame();
    game.Commander().getShip().setTribbles(3);
    RecordingView view = new RecordingView();

    new QuestsPresenter(game, view).update();

    assertEquals(Strings.QuestTribbles, view.model.text());
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

