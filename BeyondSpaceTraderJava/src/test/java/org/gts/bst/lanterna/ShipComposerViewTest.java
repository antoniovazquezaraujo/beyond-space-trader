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
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.gts.bst.view.ShipArtFile;
import org.junit.jupiter.api.Test;


class ShipComposerViewTest {
  @Test
  void placesAndUndoesAPieceOverTheChassis() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[uno]\ncolor=cian\n..##\n#..#\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[total]\ncolor=rojo\n##\n##\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 12)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipComposerView view = new ShipComposerView(chassis, pieces, null);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertEquals(1, view.placedCount());
      assertTrue(screenText(screen).contains("pieza: total"), screenText(screen));

      view.handleKey(new KeyStroke('u', false, false));
      assertEquals(0, view.placedCount());

      // moverse mas alla del borde no debe reventar el dibujo
      for(int i = 0; i < 30; i++) {
        view.handleKey(new KeyStroke(KeyType.ArrowLeft));
        view.handleKey(new KeyStroke(KeyType.ArrowUp));
      }
      gui.updateScreen();
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      StringBuilder line = new StringBuilder();
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        line.append(screen.getBackCharacter(column, row).getCharacter());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }
}
