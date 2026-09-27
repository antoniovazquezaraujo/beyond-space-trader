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
import java.util.Locale;


/**
 * Standalone viewer for the generated ship art (a design tool, not the game):
 * pick a ship, move it around the screen and mirror it.
 *
 * <pre>
 *   ./run-ships.sh              -> starts the viewer
 *   ./run-ships.sh --lang es    -> the ship names in Spanish texts
 * </pre>
 */
public final class ShipArtViewer {
  private ShipArtViewer() {
  }

  public static void main(String[] args) throws IOException {
    Locale locale = LanternaApp.languageFrom(args);
    if(locale != null) {
      Locale.setDefault(locale);
    }
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipArtView view = new ShipArtView();
      gui.addWindowAndWait(view);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }
}
