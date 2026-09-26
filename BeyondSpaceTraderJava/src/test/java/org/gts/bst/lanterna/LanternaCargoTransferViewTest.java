/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.presenter.CargoTransferPresenter;
import org.gts.bst.view.DialogService;
import org.junit.jupiter.api.Test;
import spacetrader.Game;


class LanternaCargoTransferViewTest {
  @Test
  void showsTheOpponentCargoAndTakesItWithShiftDigit() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      game.getOpponent().Cargo()[0] = 7;
      LanternaCargoTransferView view = new LanternaCargoTransferView(gui, game, CargoTransferPresenter.Mode.Plunder);
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      assertTrue(screenText(screen).contains("Water"), screenText(screen));

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('1', false, false, true));

      assertEquals(7, game.Commander().getShip().Cargo()[0]);
      assertEquals(0, game.getOpponent().Cargo()[0]);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
        text.append(screen.getBackCharacter(x, y).getCharacter());
      }
      text.append('\n');
    }
    return text.toString();
  }
}
