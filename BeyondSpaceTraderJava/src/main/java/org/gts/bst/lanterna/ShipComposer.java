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
import java.util.List;
import org.gts.bst.view.ShipArtFile;


/**
 * The ship composer (a design tool, not the game): it reads the chassis and the
 * pieces from the text files next to the repository (ships/chassis.txt and
 * ships/pieces.txt), so they can be edited with any editor and reloaded.
 *
 * <pre>
 *   ./run-composer.sh              -> starts the composer
 *   ./run-composer.sh --lang es    -> Spanish texts
 * </pre>
 */
public final class ShipComposer {
  private ShipComposer() {
  }

  public static void main(String[] args) throws IOException {
    java.util.Locale locale = LanternaApp.languageFrom(args);
    if(locale != null) {
      java.util.Locale.setDefault(locale);
    }
    final List<ShipArtFile> chassis;
    try {
      chassis = ShipArtFile.load("chassis.txt");
    } catch(IOException e) {
      System.err.println(e.getMessage());
      return;
    }
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      gui.addWindowAndWait(new ShipComposerView(chassis));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }
}
