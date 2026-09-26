/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.LanternaDialogService;
import spacetrader.Game;


/**
 * Starts the game with the text UI. The model and the presenters are the same as in
 * the Swing front-end; only the views change.
 */
public final class LanternaApp {
  private LanternaApp() {
  }

  public static void main(String[] args) throws IOException {
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaDialogService dialogs = new LanternaDialogService(new LanternaAlertDialogHost(gui));
      Game[] game = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> game[0], gui);
      game[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      MainPresenter presenter = new MainPresenter(() -> game[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindowAndWait(window.asWindow());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }
}
