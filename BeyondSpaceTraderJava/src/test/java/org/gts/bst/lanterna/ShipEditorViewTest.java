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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipDesign;
import org.junit.jupiter.api.Test;


class ShipEditorViewTest {
  @Test
  void writesLettersAndSavesThem() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Engine]\nkey=M\ncolor=red\nM\n"));
    Path file = Files.createTempFile("naves", ".txt");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      view.shipsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("prueba"), screenText(screen));
      assertTrue(screenText(screen).contains("Engine"), "the tree of pieces: " + screenText(screen));

      view.handleKey(new KeyStroke('M', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("M: Engine"), "the tree of sites: " + screenText(screen));
      assertTrue(screenText(screen).contains("1/1"), "the count: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(1, saved.size());
      assertEquals("Firefly", saved.get(0).type());
      assertEquals("uno", saved.get(0).chassis());
      assertEquals(1, saved.get(0).groups().size());
      assertEquals(new ShipDesign.LetterGroup('M', 2, 2, 1), saved.get(0).groups().get(0));

      // el espacio borra el grupo entero
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("M: Engine  0/1"), "the group is gone: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void picksTheChassisAndTheTypeFromLists() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\nchasis=nope\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\n"));
    List<ShipArtFile> pieces = List.of();
    Path file = java.nio.file.Files.createTempFile("naves", ".txt");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      view.shipsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("no encuentro el chasis"), screenText(screen));

      view.handleKey(new KeyStroke('h', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chasis de la nave:"), screenText(screen));
      assertTrue(screenText(screen).contains("uno"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.Enter));

      view.handleKey(new KeyStroke('y', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("type de la nave:"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.ArrowDown));
      view.handleKey(new KeyStroke(KeyType.ArrowDown));
      view.handleKey(new KeyStroke(KeyType.Enter));
      view.handleKey(new KeyStroke('s', false, false));

      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals("uno", saved.get(0).chassis(), "the chassis is picked from the list");
      assertEquals("Firefly", saved.get(0).type(), "the third type in the enum");
    } finally {
      screen.stopScreen();
      screen.close();
      java.nio.file.Files.deleteIfExists(file);
    }
  }

  @Test
  void refusesMorePiecesThanTheShipAdmits() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Pulse Laser]\nkey=A\ncolor=red\nA\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('A', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("A: Weapon"), "the Firefly admits one weapon: " + screenText(screen));

      // otra arma, separada: se cuenta igual y no cabe
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke('A', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("admite 1"), "the warning: " + screenText(screen));
      assertTrue(screenText(screen).contains("A: Weapon  1/1"), "it is not written: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void needsTheTypeAndRefusesTheEnginesOverTheLimit() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[uno]\nsize=tiny\ncolor=cyan\nxxxxxxx\nxxxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Engine]\nkey=M\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());

      // sin type= no deja colocar sitios
      ShipEditorView noType = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(noType);
      gui.updateScreen();
      noType.handleKey(new KeyStroke('M', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("elige el type"), screenText(screen));
      noType.handleKey(new KeyStroke(KeyType.Escape));

      // con type=Wasp (Huge) caben tres motores y el cuarto no
      ShipEditorView wasp = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Wasp\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(wasp);
      for(int i = 0; i < 3; i++) {
        wasp.handleKey(new KeyStroke('M', false, false));
        wasp.handleKey(new KeyStroke(KeyType.ArrowRight));
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("M: Engine  3/3"), screenText(screen));

      assertTrue(screenText(screen).contains("size tiny vs Huge"),
          "the size of the chassis is checked: " + screenText(screen));

      wasp.handleKey(new KeyStroke('M', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("admite 3"), screenText(screen));
      assertTrue(screenText(screen).contains("M: Engine  3/3"), "it is not written: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void drawsOnePiecePerLetterAndCyclesTheVariants() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Wasp\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxxxx\nxxxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nL\n[Beam Laser]\nkey=A\ncolor=cyan\nB\n"
        + "[Cargo Gauge]\nkey=B\ncolor=green\n.\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      // dos armas seguidas: la vista dibuja una pieza en cada letra
      view.handleKey(new KeyStroke('A', false, false));
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke('A', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("LL"), "one piece per letter: " + screenText(screen));

      // v cambia la variante que se ve
      view.handleKey(new KeyStroke('v', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("preview: Beam Laser"), screenText(screen));
      assertTrue(screenText(screen).contains("BB"), "the other variant: " + screenText(screen));
      assertTrue(screenText(screen).contains("⣿⣿⣿⣿⣄"),
          "the cargo gauge of the Wasp (35 bays) is drawn by itself: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheGlyphStrip() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\n"));
    List<ShipArtFile> pieces = List.of();
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(140, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKeyWithStrip(new KeyStroke('g', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("glifos:"), screenText(screen));
      view.handleKeyWithStrip(new KeyStroke('g', false, false));
      gui.updateScreen();
      assertTrue(!screenText(screen).contains("glifos:"), "g hides it again: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void addsANewShipAndSavesIt() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\n"));
    List<ShipArtFile> pieces = List.of();
    Path file = Files.createTempFile("naves", ".txt");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      view.shipsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("nueva"), "the new ship: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(2, saved.size());
      assertEquals("nueva", saved.get(1).name());
      assertEquals("uno", saved.get(1).chassis(), "the new ship starts with the same chassis");
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
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
