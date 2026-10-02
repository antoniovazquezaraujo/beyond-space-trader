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
      assertTrue(screenText(screen).contains("1  M  Engine  0/1  ⚠"),
          "the panel of elements: " + screenText(screen));

      // the pen starts on the only element (M, the engine): space paints
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1  M  Engine  1/1  ✓"), "the count: " + screenText(screen));

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
      assertTrue(screenText(screen).contains("1  M  Engine  0/1  ⚠"), "the letter is gone: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void picksTheChassisFromTheListAndCyclesTheType() throws IOException {
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
      assertTrue(screenText(screen).contains("─ Ships "), screenText(screen));
      assertTrue(screenText(screen).contains("─ Elements "), screenText(screen));
      assertTrue(panelRowOf(screen, "prueba") >= 0, screenText(screen));

      // f opens the chassis list
      view.handleKey(new KeyStroke('f', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chassis:"), screenText(screen));
      assertTrue(screenText(screen).contains("uno"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.Enter));

      // y cycles the type: it is written next to the name, in brackets
      for(int i = 0; i < 3; i++) {
        view.handleKey(new KeyStroke('y', false, false));
      }
      gui.updateScreen();
      assertTrue(panelRowOf(screen, "prueba [Firefly]") >= 0, screenText(screen));
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
      assertTrue(screenText(screen).contains("1  A  Weapon  1/1  ✓"),
          "the Firefly admits one weapon: " + screenText(screen));

      // another weapon, apart: it counts all the same and does not fit
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("takes 1"), "the warning: " + screenText(screen));
      assertTrue(screenText(screen).contains("1  A  Weapon  1/1  ✓"), "it is not written: " + screenText(screen));
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
      assertTrue(screenText(screen).contains("1  M  Engine  ?  ?"),
          "without a type there is no maximum: " + screenText(screen));
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
      assertTrue(screenText(screen).contains("1  M  Engine  3/3  ✓"), screenText(screen));

      assertTrue(screenText(screen).contains("⚠ hull size: tiny (type: huge)"),
          "the size of the chassis is checked: " + screenText(screen));

      wasp.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("takes 3"), screenText(screen));
      assertTrue(screenText(screen).contains("1  M  Engine  3/3  ✓"), "it is not written: " + screenText(screen));
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
      // the title bar does not repeat the open ship: it is in the list
      assertFalse(screenText(screen).contains("ships: uno"), screenText(screen));
      // the open ship is the one with a background of its own in the ships list
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowOf(screen, "uno")).getBackgroundColor(),
          screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, panelRowOf(screen, "dos")).getBackgroundColor(),
          screenText(screen));

      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowOf(screen, "dos")).getBackgroundColor(),
          screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, panelRowOf(screen, "uno")).getBackgroundColor(),
          screenText(screen));

      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowOf(screen, "uno")).getBackgroundColor(),
          "from the last one it goes back to the first: " + screenText(screen));
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
      assertTrue(screenText(screen).contains("1  A  Weapon  1/1  ✓"), screenText(screen));

      // n moves the pen to M (the engine); space over the A erases it anyway
      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Engine (M)"), screenText(screen));
      view.handleKey(new KeyStroke(KeyType.ArrowLeft));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1  A  Weapon  0/1  ⚠"), "the A is erased: " + screenText(screen));
      assertTrue(screenText(screen).contains("2  M  Engine  0/1  ⚠"), "and no M was painted: " + screenText(screen));

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

      int weapon = panelRowOf(screen, "1  A  Weapon");
      int engine = panelRowOf(screen, "2  M  Engine");
      assertTrue(weapon >= 0 && engine >= 0, screenText(screen));

      // the first element is selected: its row has a background of its own
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, weapon).getBackgroundColor(),
          "the selected element: " + screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, engine).getBackgroundColor(),
          "the others: " + screenText(screen));

      // n moves the highlight to the engine
      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowOf(screen, "2  M  Engine")).getBackgroundColor(),
          "the engine is now selected: " + screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, panelRowOf(screen, "1  A  Weapon")).getBackgroundColor(),
          "and the weapon is not: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void linesUpTheElementColumnsAndLeavesTheKeyWithoutParentheses() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Energy Shield]\nkey=E\ncolor=cyan\nE\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();

      String[] lines = screenText(screen).split("\n", -1);
      String weapon = lines[panelRowOf(screen, "1  A  Weapon")];
      String shield = lines[panelRowOf(screen, "2  E  Shield")];
      assertTrue(weapon.contains("1  A  Weapon  0/1  ⚠"), screenText(screen));
      assertTrue(shield.contains("2  E  Shield  0/1  ⚠"), screenText(screen));
      assertFalse(screenText(screen).contains("Weapon (A)") || screenText(screen).contains("Shield (E)"),
          "the key is not in parentheses: " + screenText(screen));
      assertEquals(weapon.indexOf("A"), shield.indexOf("E"), "the key column: " + screenText(screen));
      assertEquals(weapon.indexOf("Weapon"), shield.indexOf("Shield"), "the name column: " + screenText(screen));
      assertEquals(weapon.indexOf("0/1"), shield.indexOf("0/1"), "the have/max column: " + screenText(screen));
      assertEquals(weapon.indexOf("⚠"), shield.indexOf("⚠"), "the mark column: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void linesUpTheShipsAndTheirTypes() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[uno]\ntype=Firefly\nchasis=h1\n[una nave larga]\ntype=Gnat\nchasis=h2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[h1]\ncolor=cyan\nxxxxx\n[h2]\ncolor=red\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      gui.addWindow(view);
      gui.updateScreen();

      String[] lines = screenText(screen).split("\n", -1);
      int first = panelRowWith(screen, "[Firefly]");
      int second = panelRowWith(screen, "[Gnat]");
      assertTrue(first >= 0 && second >= 0, screenText(screen));
      assertEquals(lines[first].indexOf("[Firefly]"), lines[second].indexOf("[Gnat]"),
          "the type column: " + screenText(screen));

      // the highlight of the open ship covers the whole panel row
      int divider = lines[first].indexOf('│', 1);
      assertTrue(divider > 1, screenText(screen));
      for(int x = 1; x < divider; x++) {
        assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(x, first).getBackgroundColor(),
            "the open ship fills its row: " + screenText(screen));
        assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(x, second).getBackgroundColor(),
            "the closed ship does not: " + screenText(screen));
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void warnsOnlyWhenTheHullSizeDoesNotMatchTheType() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());

      // Firefly is Small: the size of the chassis fits, so there is no warning
      ShipEditorView fits = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n")), hulls, List.of());
      gui.addWindow(fits);
      gui.updateScreen();
      assertFalse(screenText(screen).contains("hull size"), screenText(screen));
      fits.handleKey(new KeyStroke(KeyType.Escape));

      // Wasp is Huge: the warning says which size each one has, inside the panel
      ShipEditorView mismatched = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Wasp\nchasis=uno\n")), hulls, List.of());
      gui.addWindow(mismatched);
      gui.updateScreen();
      String warning = "⚠ hull size: small (type: huge)";
      assertTrue(screenText(screen).contains(warning), screenText(screen));
      int row = panelRowWith(screen, warning);
      String[] lines = screenText(screen).split("\n", -1);
      assertTrue(lines[row].indexOf(warning) + warning.length() < lines[row].indexOf('│', 1),
          "the warning fits in the panel: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void paintsTheBackgroundsOfThePieces() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nbgcolor=blue\nA\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      gui.updateScreen();
      assertFalse(hasBackground(screen, TextColor.ANSI.BLUE), screenText(screen));

      // space paints the weapon: its preview keeps the blue background
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(hasBackground(screen, TextColor.ANSI.BLUE),
          "the blue background of the piece is painted: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static boolean hasBackground(Screen screen, TextColor color) {
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(color.equals(screen.getBackCharacter(column, row).getBackgroundColor())) {
          return true;
        }
      }
    }
    return false;
  }

  private static int panelRowOf(Screen screen, String text) {
    String[] lines = screenText(screen).split("\n", -1);
    for(int row = 0; row < lines.length; row++) {
      if(lines[row].startsWith("│" + text)) {
        return row;
      }
    }
    return -1;
  }

  private static int panelRowWith(Screen screen, String text) {
    String[] lines = screenText(screen).split("\n", -1);
    for(int row = 0; row < lines.length; row++) {
      if(lines[row].startsWith("│") && lines[row].contains(text)) {
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
