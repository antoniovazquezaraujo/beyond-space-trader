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
import com.googlecode.lanterna.TextColor;
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
      assertTrue(screenText(screen).contains("1. Engine (M)"), "the panel of elements: " + screenText(screen));

      // the pen starts on the only element (M, the engine): space paints
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1/1"), "the count: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(1, saved.size());
      assertEquals("Firefly", saved.get(0).type());
      assertEquals("uno", saved.get(0).chassis());
      assertEquals(1, saved.get(0).groups().size());
      assertEquals(new ShipDesign.LetterGroup('M', 2, 2, 1), saved.get(0).groups().get(0));

      // one cell further: it would go over the quota, so it is not painted
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("takes 1"), "the quota: " + screenText(screen));

      // and on the M already there, space erases it
      view.handleKey(new KeyStroke(KeyType.ArrowLeft));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Engine (M)  0/1"), "the letter is gone: " + screenText(screen));
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
      assertTrue(screenText(screen).contains("cannot find the chassis"), screenText(screen));

      view.handleKey(new KeyStroke('f', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chassis:"), screenText(screen));
      assertTrue(screenText(screen).contains("uno"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.Enter));

      view.handleKey(new KeyStroke('y', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("ship type:"), screenText(screen));
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

      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Weapon (A)"), "the Firefly admits one weapon: " + screenText(screen));

      // another weapon, apart: it counts all the same and does not fit
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("takes 1"), "the warning: " + screenText(screen));
      assertTrue(screenText(screen).contains("1. Weapon (A)  1/1"), "it is not written: " + screenText(screen));
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

      // without type= no site can be placed
      ShipEditorView noType = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(noType);
      gui.updateScreen();
      noType.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("pick the ship type"), screenText(screen));
      noType.handleKey(new KeyStroke(KeyType.Escape));

      // with type=Wasp (Huge) three engines fit and the fourth does not
      ShipEditorView wasp = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Wasp\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(wasp);
      for(int i = 0; i < 3; i++) {
        wasp.handleKey(new KeyStroke(' ', false, false));
        wasp.handleKey(new KeyStroke(KeyType.ArrowRight));
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Engine (M)  3/3"), screenText(screen));

      assertTrue(screenText(screen).contains("size tiny vs Huge"),
          "the size of the chassis is checked: " + screenText(screen));

      wasp.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("takes 3"), screenText(screen));
      assertTrue(screenText(screen).contains("1. Engine (M)  3/3"), "it is not written: " + screenText(screen));
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

      // two weapons in a row: the preview draws a piece at every letter
      // the space moves right, so the two letters end up together
      view.handleKey(new KeyStroke(' ', false, false));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("LL"), "one piece per letter: " + screenText(screen));

      // v cycles the variant shown
      view.handleKey(new KeyStroke('v', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("preview: Beam Laser"), screenText(screen));
      assertTrue(screenText(screen).contains("BB"), "the other variant: " + screenText(screen));

      // the cargo gauge needs no piece: pick the cargo with 2 and paint five cells
      view.handleKey(new KeyStroke('2', false, false));
      for(int i = 0; i < 5; i++) {
        view.handleKey(new KeyStroke(' ', false, false));
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("⣿⣿⣿⣿⣄"),
          "the cargo gauge of the Wasp (35 bays) is drawn by itself: " + screenText(screen));
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

      view.handleKey(new KeyStroke('+', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("new"), "the new ship: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(2, saved.size());
      assertEquals("new", saved.get(1).name());
      assertEquals("uno", saved.get(1).chassis(), "the new ship starts with the same chassis");
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void tabCyclesTheShips() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[uno]\ntype=Firefly\nchasis=h1\n[dos]\ntype=Gnat\nchasis=h2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[h1]\ncolor=cyan\nxxxxx\n[h2]\ncolor=red\nxxxxx\n"));
    List<ShipArtFile> pieces = List.of();
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[uno]"), screenText(screen));

      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[dos]"), screenText(screen));

      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[uno]"), "from the last one it goes back to the first: "
          + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void spaceErasesAnyLetterEvenIfItIsNotThePen() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      // the pen starts on A (the weapon): space paints it
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Weapon (A)  1/1"), screenText(screen));

      // n moves the pen to M (the engine); space over the A erases it anyway
      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Engine (M)"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.ArrowLeft));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Weapon (A)  0/1"), "the A is erased: " + screenText(screen));
      assertTrue(screenText(screen).contains("2. Engine (M)  0/1"), "and no M was painted: " + screenText(screen));

      // a number picks an element directly
      view.handleKey(new KeyStroke('1', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Weapon (A)"), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void marksTheSelectedElementWithItsOwnBackground() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      int weapon = rowOf(screen, "1. Weapon (A)");
      int engine = rowOf(screen, "2. Engine (M)");
      assertTrue(weapon >= 0 && engine >= 0, screenText(screen));

      // the first element is selected: its row has a background of its own
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, weapon).getBackgroundColor(),
          "the selected element: " + screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, engine).getBackgroundColor(),
          "the others: " + screenText(screen));

      // n moves the highlight to the engine
      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, rowOf(screen, "2. Engine (M)")).getBackgroundColor(),
          "the engine is now selected: " + screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, rowOf(screen, "1. Weapon (A)")).getBackgroundColor(),
          "and the weapon is not: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static int rowOf(Screen screen, String text) {
    String[] lines = screenText(screen).split("\n", -1);
    for(int row = 0; row < lines.length; row++) {
      if(lines[row].contains(text)) {
        return row;
      }
    }
    return -1;
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
