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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\n..##\n#..#\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[total]\ncolor=red\n##\n##\n"));
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

      view.handleKey(new KeyStroke(KeyType.Enter));
      assertEquals(1, view.placedCount());
      view.handleKey(new KeyStroke('x', false, false));
      assertEquals(0, view.placedCount(), "x clears the assembly");
      view.handleKey(new KeyStroke('h', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[h] pieza (no)"), screenText(screen));

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

  @Test
  void drawsGlyphsOutsideTheBasicPlane() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\n🁣x\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[total]\ncolor=red\n##\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 12)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      gui.addWindow(new ShipComposerView(chassis, pieces, null));
      gui.updateScreen();

      assertTrue(screenText(screen).contains("🁣"), "the domino tile is drawn");
      assertTrue(ShipArtFile.isWide(0x1F063), "Lanterna paints the domino in two columns");
      assertFalse(ShipArtFile.isWide('x'), "a letter is one column");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheGlyphStrip() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\n##\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[total]\ncolor=red\n##\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(120, 12)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipComposerView view = new ShipComposerView(chassis, pieces, null);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('g', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("glifos:"), screenText(screen));
      assertTrue(screenText(screen).contains("🁣"), "the strip shows the domino tile");

      view.handleKey(new KeyStroke('g', false, false));
      gui.updateScreen();
      assertFalse(screenText(screen).contains("glifos:"), "g hides the strip again");
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
        line.append(screen.getBackCharacter(column, row).getCharacterString());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }
}
