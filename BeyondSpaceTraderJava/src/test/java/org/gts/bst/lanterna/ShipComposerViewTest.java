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
      view.handleKey(new KeyStroke('v', false, false));
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

  @Test
  void showsTheSitePanelAndItsWarnings() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[Wasp]\ncolor=cyan\nMA\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      gui.addWindow(new ShipComposerView(chassis, pieces, null));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains("Wasp (Huge)"), text);
      assertTrue(text.contains("arma"), text);
      assertTrue(text.contains("1/3"), "the panel counts the sites against the budget: " + text);
      assertTrue(text.contains("faltan"), "the warnings are shown: " + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void previewsTheCargoGauge() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[Flea]\ncolor=cyan\nBB\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipComposerView view = new ShipComposerView(chassis, pieces, null);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('v', false, false));
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains("⣿"), "ten bays are a full cell and two dots: " + text);
      assertTrue(text.contains("vista: vacia"), "the panel shows the preview: " + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheShipTypeList() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[Wasp]\ncolor=cyan\nMA\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipComposerView view = new ShipComposerView(chassis, pieces, null);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('t', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("tipo de nave:"), screenText(screen));
      assertTrue(screenText(screen).contains("Flea"), screenText(screen));

      view.handleKey(new KeyStroke(KeyType.ArrowDown));
      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("SpaceMonster (Huge)"),
          "the panel follows the chosen type: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void writesSitesWithTheCursor() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[Wasp]\ncolor=cyan\nx   x\nx   x\nx   x\nx   x\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipComposerView view = new ShipComposerView(chassis, pieces, null);
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("0/3"), "no sites yet: " + screenText(screen));

      view.handleKey(new KeyStroke('e', false, false));
      view.handleKey(new KeyStroke('A', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1/3"), "one weapon site: " + screenText(screen));
      assertTrue(screenText(screen).contains("SITIOS"), "the site mode line: " + screenText(screen));

      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke('A', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("2/3"), "another weapon site: " + screenText(screen));

      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1/3"), "the space erases: " + screenText(screen));

      // el espacio sobre el dibujo no lo borra
      for(int i = 0; i < 3; i++) {
        view.handleKey(new KeyStroke(KeyType.ArrowLeft));
      }
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("celda: x"), "the drawing is still there: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void savesTheSitesInTheChassisFile() throws IOException {
    String content = ";; un comentario\n[Wasp]\ncolor=cyan\nx   x\nx   x\n";
    java.nio.file.Path file = java.nio.file.Files.createTempFile("chassis", ".txt");
    java.nio.file.Files.writeString(file, content);
    try {
      List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader(content));
      List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\ncolor=red\nM\n"));
      Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
      screen.startScreen();
      try {
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        gui.setTheme(LanternaTheme.create());
        ShipComposerView view = new ShipComposerView(chassis, pieces, null);
        view.chassisPath(file.toString());
        gui.addWindow(view);

        view.handleKey(new KeyStroke('e', false, false));
        view.handleKey(new KeyStroke(KeyType.ArrowLeft));
        view.handleKey(new KeyStroke(KeyType.ArrowUp));
        view.handleKey(new KeyStroke('A', false, false));
        view.handleKey(new KeyStroke('s', false, false));

        String saved = java.nio.file.Files.readString(file);
        assertTrue(saved.contains("xA  x"), "the letter is in the file: " + saved);
        assertTrue(saved.contains(";; un comentario"), "the comments survive: " + saved);
        assertTrue(saved.contains("color=cyan"), "the keys survive: " + saved);
      } finally {
        screen.stopScreen();
        screen.close();
      }
    } finally {
      java.nio.file.Files.deleteIfExists(file);
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
