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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.GameWindow;
import org.junit.jupiter.api.Test;


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
