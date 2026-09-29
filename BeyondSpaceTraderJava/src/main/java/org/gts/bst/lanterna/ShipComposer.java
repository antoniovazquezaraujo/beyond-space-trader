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
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      while(true) {
        final List<ShipArtFile> chassis;
        final List<ShipArtFile> pieces;
        final List<org.gts.bst.view.ShipDesign> designs;
        try {
          chassis = ShipArtFile.load("chassis.txt");
          pieces = ShipArtFile.load("pieces.txt");
          designs = org.gts.bst.view.ShipDesign.load(ShipArtFile.resolve("ships.txt").toString());
        } catch(IOException e) {
          System.err.println(e.getMessage());
          return;
        }
        int chosen = chooseMode(gui);
        if(chosen == 0) {
          gui.addWindowAndWait(new ShipEditorView(designs, chassis, pieces));
        } else if(chosen == 1) {
          gui.addWindowAndWait(new HullEditorView(chassis));
        } else {
          break;
        }
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The little menu: the ship editor or the hull editor (Esc goes back here). */
  private static int chooseMode(MultiWindowTextGUI gui) {
    com.googlecode.lanterna.gui2.ActionListBox menu = new com.googlecode.lanterna.gui2.ActionListBox();
    com.googlecode.lanterna.gui2.BasicWindow window = new com.googlecode.lanterna.gui2.BasicWindow(
        "compositor de naves");
    final int[] chosen = {-1};
    menu.addItem("Naves: disenar las naves (letras y piezas)", () -> {
      chosen[0] = 0;
      window.close();
    });
    menu.addItem("Fuselajes: colorear los cascos", () -> {
      chosen[0] = 1;
      window.close();
    });
    window.setComponent(menu);
    window.setCloseWindowWithEscape(true);
    gui.addWindowAndWait(window);
    return chosen[0];
  }
}
