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

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.CommanderView;
import org.gts.bst.view.CommanderViewModel;
import org.gts.bst.view.DialogService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Game;


class CommanderPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheCommanderState() {
    RecordingView view = new RecordingView();

    new CommanderPresenter(newGame(), view).update();

    assertEquals("Antonio", view.model.name());
    assertEquals("Normal", view.model.difficulty());
    assertEquals("1,000 cr.", view.model.cash());
    assertEquals("0 cr.", view.model.debt());
    assertEquals("Clean", view.model.record());
    assertEquals("Harmless", view.model.reputation());
    assertTrue(view.model.pilot().matches("\\d+ \\(\\d+\\)"));
    assertFalse(view.model.bounty().visible());
  }

  @Test
  void showsTheBountyForWantedCommanders() {
    Game game = newGame();
    game.Commander().setPoliceRecordScore(Consts.PoliceRecordScorePsychopath);
    RecordingView view = new RecordingView();

    new CommanderPresenter(game, view).update();

    assertTrue(view.model.bounty().visible());
    assertEquals("Bounty offered:", view.model.bounty().label());
    assertEquals("100,000 cr.", view.model.bounty().amount());
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
  }

  private static class RecordingView implements CommanderView {
    private CommanderViewModel model;

    @Override
    public void render(CommanderViewModel model) {
      this.model = model;
    }
  }
}

