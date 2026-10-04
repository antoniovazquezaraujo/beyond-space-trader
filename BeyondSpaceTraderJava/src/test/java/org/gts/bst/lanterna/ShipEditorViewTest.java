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
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipDesign;
import org.junit.jupiter.api.Test;


class ShipEditorViewTest {
  @Test
  void writesLettersAndSavesThem() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Flea\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=tiny\ncolor=cyan\nxxxxx\nxxxxx\nxxxxx\n"));
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
      int row = panelRowWith(screen, "[Flea]");
      assertTrue(row >= 0 && screenText(screen).split("\n", -1)[row].contains("prueba"),
          "the loaded design is at the place of its type: " + screenText(screen));
      assertTrue(screenText(screen).contains("1  M  Engine  0/1  ⚠"),
          "the panel of elements: " + screenText(screen));

      // the pen starts on the only element (M, the engine): space paints
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1  M  Engine  1/1  ✓"), "the count: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(ShipType.values().length, saved.size(), "one design per game type");
      assertEquals("prueba", saved.get(0).name());
      assertEquals("Flea", saved.get(0).type());
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
  void listsTheSeventeenTypesInEnumOrder() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(List.of(), List.of(), List.of());
      gui.addWindow(view);
      gui.updateScreen();

      ShipType[] types = ShipType.values();
      assertEquals(17, types.length, "the game has 17 ship types");
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowWith(screen, "[Flea]")).getBackgroundColor(),
          "the first type is open: " + screenText(screen));
      for(int i = 1; i < types.length; i++) {
        view.handleKey(new KeyStroke(KeyType.Tab));
        gui.updateScreen();
        assertTrue(screenText(screen).contains("ship " + (i + 1) + "/17: " + types[i].name()),
            "TAB walks the enum in order: " + screenText(screen));
      }

      // from the last one it goes back to the first
      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("ship 1/17: " + types[0].name()),
          "the list is a cycle: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void picksTheChassisOfItsSizeAndKeepsTheType() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[big]\ntype=Wasp\nchasis=ocupado\n[prueba]\ntype=Firefly\nchasis=nope\n"));
    String chassisText = "[trini]\nsize=tiny\ncolor=cyan\nxxxxx\n"
        + "[chico]\nsize=small\ncolor=cyan\nxxxxx\n"
        + "[medio]\nsize=medium\ncolor=cyan\nxxxxx\n"
        + "[largo]\nsize=large\ncolor=cyan\nxxxxx\n"
        + "[grande]\nsize=huge\ncolor=cyan\nxxxxx\n"
        + "[libre]\ncolor=cyan\nxxxxx\n"
        + "[ocupado]\ncolor=cyan\nxxxxx\n";
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(chassisText));
    Path file = Files.createTempFile("naves", ".txt");
    Path chassisFile = Files.createTempFile("chassis", ".txt");
    Files.writeString(chassisFile, chassisText);
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      view.shipsPath(file.toString());
      view.hullsPath(chassisFile.toString());
      gui.addWindow(view);
      gui.updateScreen();

      // the design of Firefly is the third one of the list
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertTrue(panelRowWith(screen, "[Firefly]") >= 0, screenText(screen));

      // f only offers the hulls of the size of the type and the ones without size
      view.handleKey(new KeyStroke('f', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chassis:"), screenText(screen));
      assertTrue(screenText(screen).contains("chico"), "the hull of the size: " + screenText(screen));
      assertTrue(screenText(screen).contains("libre (no size)"),
          "a hull without size is offered, marked: " + screenText(screen));
      assertFalse(screenText(screen).contains("grande"), "another size is out: " + screenText(screen));
      assertFalse(screenText(screen).contains("ocupado"),
          "a hull used by another size is out: " + screenText(screen));

      // pick the hull without size: it adopts the size of the type
      view.handleKey(new KeyStroke(KeyType.ArrowDown));
      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chasis=libre (size=small)"), screenText(screen));

      // y no longer cycles the type
      assertEquals(-1, panelRowWith(screen, "[Wasp]"));
      view.handleKey(new KeyStroke('y', false, false));
      view.handleKey(new KeyStroke('y', false, false));
      gui.updateScreen();
      assertTrue(panelRowWith(screen, "[Firefly]") >= 0, screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(17, saved.size());
      assertEquals("prueba", saved.get(ShipType.Firefly.ordinal()).name());
      assertEquals("Firefly", saved.get(ShipType.Firefly.ordinal()).type(), "the type never changes");
      assertEquals("libre", saved.get(ShipType.Firefly.ordinal()).chassis());
      assertTrue(Files.readString(chassisFile).contains("[libre]\nsize=small"),
          "the adopted size lands in chassis.txt");
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
      Files.deleteIfExists(chassisFile);
    }
  }

  @Test
  void nAndPMoveInsideTheChassisList() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=nope\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[trini]\nsize=tiny\ncolor=cyan\nxxxxx\n"
        + "[chico]\nsize=small\ncolor=cyan\nxxxxx\n"
        + "[medio]\nsize=medium\ncolor=cyan\nxxxxx\n"
        + "[largo]\nsize=large\ncolor=cyan\nxxxxx\n"
        + "[grande]\nsize=huge\ncolor=cyan\nxxxxx\n"
        + "[libre]\ncolor=cyan\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      gui.addWindow(view);
      gui.updateScreen();
      // The open type is the Firefly: chico (small) and libre (no size) are offered.
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));

      // f opens the list on the first offered hull (chico): n goes down to libre
      view.handleKey(new KeyStroke('f', false, false));
      view.handleKey(new KeyStroke('n', false, false));
      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chasis=libre"), screenText(screen));

      // now the list opens on libre: n wraps to the first one
      view.handleKey(new KeyStroke('f', false, false));
      view.handleKey(new KeyStroke('n', false, false));
      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chasis=chico"), "n wraps around: " + screenText(screen));

      // and p goes back up (chico is the first: p wraps to the last one, libre)
      view.handleKey(new KeyStroke('f', false, false));
      view.handleKey(new KeyStroke('p', false, false));
      view.handleKey(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("chasis=libre"), "p wraps to the last one: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void nAndPCycleThePenWithoutAListAndWrap() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Gnat\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();

      // the pen starts on A: p wraps to the last element and n back to the first
      view.handleKey(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Engine (M)"), "p wraps to the last: " + screenText(screen));
      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Weapon (A)"), "n wraps to the first: " + screenText(screen));
      view.handleKey(new KeyStroke('N', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("element: Engine (M)"), "uppercase N too: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void refusesMorePiecesThanTheShipAdmits() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Gnat\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Pulse Laser]\nkey=A\ncolor=red\nA\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();

      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1  A  Weapon  1/1  ✓"),
          "the Gnat admits one weapon: " + screenText(screen));

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
  void refusesTheEnginesOverTheLimitAndWarnsOfTheSize() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[uno]\nsize=tiny\ncolor=cyan\nxxxxxxx\nxxxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Engine]\nkey=M\ncolor=red\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());

      // with type=Wasp (Huge) three engines fit and the fourth does not
      ShipEditorView wasp = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Wasp\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(wasp);
      for(int i = 0; i < 9; i++) {
        wasp.handleKey(new KeyStroke(KeyType.Tab));
      }
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
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=huge\ncolor=cyan\nxxxxxxx\nxxxxxxx\n"));
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
      for(int i = 0; i < 9; i++) {
        view.handleKey(new KeyStroke(KeyType.Tab));
      }
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
  void createsTheMissingDesignAndSavesIt() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=scout\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[trini]\nsize=tiny\ncolor=cyan\nxxxxx\n[scout]\nsize=small\ncolor=cyan\nxxxxx\n"));
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

      // the file has no design for Flea: the editor makes one of the type and its size
      int row = panelRowWith(screen, "[Flea]");
      assertTrue(row >= 0 && screenText(screen).split("\n", -1)[row].contains("Flea"),
          "the missing design is created in memory: " + screenText(screen));

      // and it is editable like any other
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1  M  Engine  1/1  ✓"), screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(17, saved.size());
      assertEquals("Flea", saved.get(ShipType.Flea.ordinal()).name());
      assertEquals("Flea", saved.get(ShipType.Flea.ordinal()).type());
      assertEquals("trini", saved.get(ShipType.Flea.ordinal()).chassis(), "the first hull of its size");
      assertEquals(1, saved.get(ShipType.Flea.ordinal()).groups().size(), "the painted site is saved");
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void dropsUnknownAndRepeatedDesignsAndSavesTheSeventeen() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[uno]\ntype=Nope\nchasis=h1\n"
        + "[dos]\ntype=Firefly\nchasis=h1\n"
        + "[tres]\ntype=Firefly\nchasis=h1\n"
        + "[cuatro]\ntype=Flea\nchasis=h1\n"));
    String chassisText = "[h1]\ncolor=cyan\nxxxxx\n";
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(chassisText));
    Path file = Files.createTempFile("naves", ".txt");
    Path chassisFile = Files.createTempFile("chassis", ".txt");
    Files.writeString(chassisFile, chassisText);
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      view.shipsPath(file.toString());
      view.hullsPath(chassisFile.toString());
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("ignored 2 designs"),
          "the editor says what it dropped: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      assertTrue(Files.readString(chassisFile).contains("[h1]\nsize=small"),
          "the adopted size lands in the temp chassis, never in the real one: " + Files.readString(chassisFile));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(17, saved.size());
      ShipType[] types = ShipType.values();
      for(int i = 0; i < types.length; i++) {
        assertEquals(types[i].name(), saved.get(i).type(), "one design per type, in enum order");
      }
      assertEquals("cuatro", saved.get(ShipType.Flea.ordinal()).name());
      assertEquals("dos", saved.get(ShipType.Firefly.ordinal()).name());
      for(ShipDesign design : saved) {
        assertFalse(design.name().equals("uno") || design.name().equals("tres"),
            "the unknown and the repeated one are gone: " + design.name());
      }
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
      Files.deleteIfExists(chassisFile);
    }
  }

  @Test
  void noLongerAddsShipsWithPlusNorCyclesTheTypeWithY() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\n"));
    Path file = Files.createTempFile("naves", ".txt");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      view.shipsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();
      assertFalse(screenText(screen).contains("[+]"), "the keys line has no new ship: " + screenText(screen));
      assertFalse(screenText(screen).contains("[y]"), "the keys line has no type cycle: " + screenText(screen));
      assertTrue(screenText(screen).contains("[t] rename"), "renaming stays: " + screenText(screen));

      // + does nothing and y does not touch the type: the 17 are already there
      view.handleKey(new KeyStroke('+', false, false));
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke('y', false, false));
      view.handleKey(new KeyStroke('y', false, false));
      gui.updateScreen();
      assertFalse(screenText(screen).contains("new"), "no ship was added: " + screenText(screen));
      assertTrue(panelRowWith(screen, "[Firefly]") >= 0, screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(17, saved.size());
      assertEquals("prueba", saved.get(ShipType.Firefly.ordinal()).name());
      assertEquals("Firefly", saved.get(ShipType.Firefly.ordinal()).type());
      for(ShipDesign design : saved) {
        assertFalse(design.name().equals("new"), "no ship called `new`");
      }
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void renamingKeepsTheType() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\n"));
    Path file = Files.createTempFile("naves", ".txt");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      view.shipsPath(file.toString());
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();

      // the [t] dialog lands in this method: rename and the type is untouched
      view.rename("otra nave");
      gui.updateScreen();
      int row = panelRowWith(screen, "[Firefly]");
      assertTrue(row >= 0 && screenText(screen).split("\n", -1)[row].contains("otra nave"),
          "the renamed design: " + screenText(screen));
      assertTrue(screenText(screen).contains("renamed: otra nave"), screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals("otra nave", saved.get(ShipType.Firefly.ordinal()).name());
      assertEquals("Firefly", saved.get(ShipType.Firefly.ordinal()).type());
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void tabCyclesTheTypes() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[uno]\ntype=Flea\nchasis=h1\n[dos]\ntype=Gnat\nchasis=h2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[h1]\nsize=tiny\ncolor=cyan\nxxxxx\n[h2]\nsize=small\ncolor=red\nxxxxx\n"));
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
      // the open type is the one with a background of its own in the ships list
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowWith(screen, "[Flea]")).getBackgroundColor(),
          screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, panelRowWith(screen, "[Gnat]")).getBackgroundColor(),
          screenText(screen));

      view.handleKey(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowWith(screen, "[Gnat]")).getBackgroundColor(),
          screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, panelRowWith(screen, "[Flea]")).getBackgroundColor(),
          screenText(screen));
      assertTrue(screenText(screen).contains("ship 2/17: dos"), screenText(screen));

      // ⇧TAB goes back
      view.handleKey(new KeyStroke(KeyType.ReverseTab));
      gui.updateScreen();
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, panelRowWith(screen, "[Flea]")).getBackgroundColor(),
          "from the second one it goes back: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void spaceErasesAnyLetterEvenIfItIsNotThePen() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Gnat\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
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
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Gnat\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
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
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Energy Shield]\nkey=E\ncolor=cyan\nE\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));
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
        "[uno]\ntype=Flea\nchasis=h1\n[una nave larga]\ntype=Gnat\nchasis=h2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[h1]\nsize=tiny\ncolor=cyan\nxxxxx\n[h2]\nsize=small\ncolor=red\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      gui.addWindow(view);
      gui.updateScreen();

      String[] lines = screenText(screen).split("\n", -1);
      int first = panelRowWith(screen, "[Flea]");
      int second = panelRowWith(screen, "[Gnat]");
      assertTrue(first >= 0 && second >= 0, screenText(screen));
      assertEquals(lines[first].indexOf("[Flea]"), lines[second].indexOf("[Gnat]"),
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
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=tiny\ncolor=cyan\nxxxxx\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());

      // Flea is Tiny: the size of the chassis fits, so there is no warning
      ShipEditorView fits = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Flea\nchasis=uno\n")), hulls, List.of());
      gui.addWindow(fits);
      gui.updateScreen();
      assertFalse(screenText(screen).contains("hull size"), screenText(screen));
      fits.handleKey(new KeyStroke(KeyType.Escape));

      // the same Flea with a Small chassis: the warning says which size each one has
      ShipEditorView mismatched = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Flea\nchasis=uno\n")),
          ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n")), List.of());
      gui.addWindow(mismatched);
      gui.updateScreen();
      String warning = "⚠ hull size: small (type: tiny)";
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
  void alignsTheColumnsWhenTheNumberingGrowsAndTheWidthsTie() throws IOException {
    // Arrange: ten elements, so the number column is two cells wide, with names
    // and counts of the same width (a tie) and one kind without a known limit
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n"
        + "[Energy Shield]\nkey=E\ncolor=cyan\nE\n"
        + "[Extra Cargo Bays]\nkey=G\ncolor=green\nG\n"
        + "[Cockpit]\nkey=C\ncolor=white\nC\n"
        + "[Engine]\nkey=M\ncolor=red\nM\n"
        + "[Fuel Tank]\nkey=F\ncolor=green\nD\n"
        + "[Cargo Gauge]\nkey=B\ncolor=green\nB\n"
        + "[Role Trader]\nkey=R\ncolor=white\nR\n"
        + "[Escape Pod]\nkey=P\ncolor=white\nP\n"
        + "[Rare Piece]\nkey=X\ncolor=white\nX\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));

      // Act
      gui.updateScreen();

      // Assert: the rows of keys 1..9 keep the columns of the row 10
      String[] lines = screenText(screen).split("\n", -1);
      int ten = panelRowWith(screen, "X  Part");
      int weapon = panelRowWith(screen, "A  Weapon");
      int shield = panelRowWith(screen, "E  Shield");
      int cargo = panelRowWith(screen, "B  Cargo");
      assertTrue(ten >= 0 && weapon >= 0 && shield >= 0 && cargo >= 0, screenText(screen));
      assertTrue(lines[ten].contains("10  X  Part       ?  ?"), screenText(screen));
      for(char key : List.of('A', 'E', 'G', 'C', 'M', 'F', 'B', 'R', 'P')) {
        int row = panelRowWith(screen, key + "  ");
        assertTrue(row >= 0, "the row of " + key + ": " + screenText(screen));
        assertEquals(lines[ten].indexOf('X'), lines[row].indexOf(key),
            "the key column of " + key + ": " + screenText(screen));
      }
      assertEquals(lines[weapon].indexOf("0/1"), lines[shield].indexOf("0/1"),
          "two counts of the same width: " + screenText(screen));
      assertEquals(lines[weapon].indexOf("0/1"), lines[cargo].indexOf("0/4"),
          "the have/max column: " + screenText(screen));
      assertEquals(lines[weapon].indexOf("0/1") + 2, lines[ten].indexOf("?"),
          "the `?` of a missing limit is padded like a count: " + screenText(screen));
      assertEquals(lines[weapon].indexOf("⚠"), lines[shield].indexOf("⚠"),
          "the mark column: " + screenText(screen));
      assertEquals(lines[weapon].indexOf("⚠"), lines[ten].lastIndexOf("?"),
          "the `?` of a missing limit sits in the mark column: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void cutsLongNamesWithoutMovingTheTypeColumn() throws IOException {
    // Arrange: a name longer than the name column, with two other types to line up
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[una nave extremadamente larga]\ntype=Firefly\nchasis=h1\n"
        + "[nave alfa]\ntype=Gnat\nchasis=h2\n"
        + "[nave beta]\ntype=Mosquito\nchasis=h3\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader(
        "[h1]\nsize=small\ncolor=cyan\nxxxxx\n[h2]\nsize=small\ncolor=red\nxxxxx\n[h3]\nsize=small\ncolor=red\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, List.of());
      gui.addWindow(view);

      // Act
      gui.updateScreen();

      // Assert
      String[] lines = screenText(screen).split("\n", -1);
      int first = panelRowWith(screen, "[Firefly]");
      int second = panelRowWith(screen, "[Gnat]");
      assertTrue(first >= 0 && second >= 0, screenText(screen));
      assertEquals(lines[first].indexOf("[Firefly]"), lines[second].indexOf("[Gnat]"),
          "the type column does not move: " + screenText(screen));
      assertFalse(screenText(screen).contains("una nave extremadamente larga"),
          "the long name is cut: " + screenText(screen));
      assertTrue(screenText(screen).contains("una nave extremadam"),
          "the head of the name is kept: " + screenText(screen));
      int divider = lines[first].indexOf('│', 1);
      assertTrue(lines[first].indexOf("[Firefly]") + "[Firefly]".length() <= divider,
          "the type fits in the panel: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void marksAnOverQuotaRowInItsColumn() throws IOException {
    // Arrange: a design with two shields where the Firefly takes one
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[prueba]\ntype=Firefly\nchasis=uno\ngroup=E x=2 y=2 n=2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Energy Shield]\nkey=E\ncolor=cyan\nE\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));

      // Act
      gui.updateScreen();

      // Assert: the first element (the weapon) is selected; the shield is not
      String[] lines = screenText(screen).split("\n", -1);
      int weapon = panelRowWith(screen, "A  Weapon");
      int shield = panelRowWith(screen, "E  Shield");
      assertTrue(weapon >= 0 && shield >= 0, screenText(screen));
      assertTrue(lines[weapon].contains("1  A  Weapon  0/1  ⚠"), screenText(screen));
      assertTrue(lines[shield].contains("2  E  Shield  2/1  ✗"), screenText(screen));
      assertEquals(lines[weapon].indexOf("0/1"), lines[shield].indexOf("2/1"),
          "the have/max column: " + screenText(screen));
      assertEquals(lines[weapon].indexOf("⚠"), lines[shield].indexOf("✗"),
          "the mark column: " + screenText(screen));
      assertEquals(TextColor.ANSI.RED,
          screen.getFrontCharacter(lines[shield].indexOf('✗'), shield).getForegroundColor(),
          "the over-quota mark warns in red: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void coloursTheWholeElementRowByTheSignOfItsCount() throws IOException {
    // Arrange: one element of each sign: a weapon missing, two shields over the
    // quota, the engine just right and a piece whose kind fixes no limit
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[prueba]\ntype=Firefly\nchasis=uno\n"
        + "group=E x=2 y=2 n=2\ngroup=M x=4 y=2\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n"
        + "[Energy Shield]\nkey=E\ncolor=cyan\nE\n"
        + "[Engine]\nkey=M\ncolor=white\nM\n"
        + "[Rare Piece]\nkey=X\ncolor=white\nX\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));

      // Act: the pen starts on the weapon, which is thus selected
      gui.updateScreen();

      // Assert: the three other rows take the colour of their mark
      String[] lines = screenText(screen).split("\n", -1);
      int weapon = panelRowWith(screen, "1  A  Weapon  0/1  ⚠");
      int shield = panelRowWith(screen, "2  E  Shield  2/1  ✗");
      int engine = panelRowWith(screen, "3  M  Engine  1/1  ✓");
      int part = panelRowWith(screen, "4  X  Part");
      assertTrue(weapon >= 0 && shield >= 0 && engine >= 0 && part >= 0, screenText(screen));
      assertTrue(lines[part].contains("4  X  Part      ?  ?"), "the piece without a limit: " + screenText(screen));
      assertRowColour(screen, lines, weapon, TextColor.ANSI.BLACK, "the selected weapon");
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, weapon).getBackgroundColor(),
          "the selected weapon stays inverted: " + screenText(screen));
      assertRowColour(screen, lines, shield, TextColor.ANSI.RED, "the over-quota shield");
      assertRowColour(screen, lines, engine, TextColor.ANSI.GREEN_BRIGHT, "the engine just right");
      assertRowColour(screen, lines, part, TextColor.ANSI.WHITE, "the piece without a limit");

      // Act: pick the shield; the weapon row now shows its own warning colour
      view.handleKey(new KeyStroke('2', false, false));
      gui.updateScreen();

      // Assert: the yellow fills the weapon row; the selected shield stays inverted
      lines = screenText(screen).split("\n", -1);
      weapon = panelRowWith(screen, "1  A  Weapon  0/1  ⚠");
      shield = panelRowWith(screen, "2  E  Shield  2/1  ✗");
      assertRowColour(screen, lines, weapon, TextColor.ANSI.YELLOW, "the missing weapon");
      assertRowColour(screen, lines, shield, TextColor.ANSI.BLACK, "the selected shield");
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(1, shield).getBackgroundColor(),
          "the selected shield stays inverted: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void paintsTheWholeSelectedRowOfAnElement() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Gnat\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));

      // Act
      gui.updateScreen();

      // Assert
      String[] lines = screenText(screen).split("\n", -1);
      int weapon = panelRowOf(screen, "1  A  Weapon");
      int engine = panelRowOf(screen, "2  M  Engine");
      assertTrue(weapon >= 0 && engine >= 0, screenText(screen));
      int divider = lines[weapon].indexOf('│', 1);
      assertTrue(divider > 1, screenText(screen));
      for(int x = 1; x < divider; x++) {
        assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(x, weapon).getBackgroundColor(),
            "the open element fills its whole row: " + screenText(screen));
        assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(x, engine).getBackgroundColor(),
            "the closed element does not: " + screenText(screen));
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void clearsTheYellowAfterTheHullSizeWarning() throws IOException {
    // Arrange: Beetle is Medium and takes no weapon; the small hull warns and
    // its yellow must not drip into the first element row, right below
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Energy Shield]\nkey=E\ncolor=cyan\nE\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(
          ShipDesign.parse(new StringReader("[prueba]\ntype=Beetle\nchasis=uno\n")), hulls, pieces);
      gui.addWindow(view);
      for(int i = 0; i < 5; i++) {
        view.handleKey(new KeyStroke(KeyType.Tab));
      }
      gui.updateScreen();

      // Act: pick the shield, so the weapon row (a check mark) stays unselected
      view.handleKey(new KeyStroke('2', false, false));
      gui.updateScreen();

      // Assert
      String warning = "⚠ hull size: small (type: medium)";
      int warningRow = panelRowWith(screen, warning);
      int weapon = panelRowWith(screen, "✓");
      assertTrue(warningRow >= 0 && weapon >= 0, screenText(screen));
      assertTrue(screenText(screen).contains("1  A  Weapon  0/0  ✓"), screenText(screen));
      assertEquals(TextColor.ANSI.YELLOW, screen.getFrontCharacter(1, warningRow).getForegroundColor(),
          "the warning is yellow: " + screenText(screen));
      assertEquals(TextColor.ANSI.GREEN_BRIGHT, screen.getFrontCharacter(1, weapon).getForegroundColor(),
          "the row under the warning takes the colour of its own mark: " + screenText(screen));
      String[] lines = screenText(screen).split("\n", -1);
      int mark = lines[weapon].indexOf('✓');
      assertEquals(TextColor.ANSI.GREEN_BRIGHT, screen.getFrontCharacter(mark, weapon).getForegroundColor(),
          "the mark is not yellow either: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void keepsThePanelAndTheColumnsAtEightyAndHundredColumns() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Energy Shield]\nkey=E\ncolor=cyan\nE\n"));
    for(int columns : List.of(80, 100)) {
      Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(columns, 30)));
      screen.startScreen();
      try {
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        gui.setTheme(LanternaTheme.create());
        ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
        gui.addWindow(view);
        view.handleKey(new KeyStroke(KeyType.Tab));
        view.handleKey(new KeyStroke(KeyType.Tab));

        // Act
        gui.updateScreen();

        // Assert: the panel keeps its 34 cells and the columns their places
        String[] lines = screenText(screen).split("\n", -1);
        int weapon = panelRowWith(screen, "A  Weapon");
        int shield = panelRowWith(screen, "E  Shield");
        assertTrue(weapon >= 0 && shield >= 0, screenText(screen));
        assertEquals(35, lines[weapon].indexOf('│', 1),
            "the panel of " + columns + " columns: " + screenText(screen));
        assertEquals(columns == 80 ? 57 : 67, lines[weapon].indexOf('│', 36),
            "the canvas of " + columns + " columns: " + screenText(screen));
        assertEquals(lines[weapon].indexOf("A"), lines[shield].indexOf("E"),
            "the key column at " + columns + ": " + screenText(screen));
        assertEquals(lines[weapon].indexOf("0/1"), lines[shield].indexOf("0/1"),
            "the have/max column at " + columns + ": " + screenText(screen));
        assertEquals(lines[weapon].indexOf("⚠"), lines[shield].indexOf("⚠"),
            "the mark column at " + columns + ": " + screenText(screen));
      } finally {
        screen.stopScreen();
        screen.close();
      }
    }
  }

  @Test
  void paintsTheBackgroundsOfThePieces() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader("[prueba]\ntype=Firefly\nchasis=uno\n"));
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\nsize=small\ncolor=cyan\nxxxxx\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nbgcolor=blue\nA\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(110, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipEditorView view = new ShipEditorView(designs, hulls, pieces);
      gui.addWindow(view);
      view.handleKey(new KeyStroke(KeyType.Tab));
      view.handleKey(new KeyStroke(KeyType.Tab));
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

  /** Checks that every cell of a panel row, from the left border to the divider, keeps a colour. */
  private static void assertRowColour(Screen screen, String[] lines, int row, TextColor color, String what) {
    int divider = lines[row].indexOf('│', 1);
    assertTrue(divider > 1, screenText(screen));
    for(int x = 1; x < divider; x++) {
      assertEquals(color, screen.getFrontCharacter(x, row).getForegroundColor(),
          what + " at x=" + x + ": " + screenText(screen));
    }
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
