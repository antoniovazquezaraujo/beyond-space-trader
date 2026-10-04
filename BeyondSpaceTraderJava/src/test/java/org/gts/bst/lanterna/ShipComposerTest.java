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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import spacetrader.Strings;


class ShipComposerTest {
  @Test
  void theCoverIsPaintedCentredBehindTheMenu() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      gui.addWindow(ShipComposer.menu());
      gui.updateScreen();

      // The 98-column cover lands centred, from column 1: the logo starts at 37.
      TextCharacter logo = screen.getBackCharacter(37, 0);
      assertEquals('⣿', logo.getCharacter(), "the logo of the cover is drawn: " + screenText(screen));
      assertEquals(TextColor.ANSI.WHITE, logo.getForegroundColor());
      assertEquals(' ', screen.getBackCharacter(0, 0).getCharacter(), "the cover is centred, not glued to the corner");

      TextCharacter star = screen.getBackCharacter(5, 1);
      assertEquals('✦', star.getCharacter(), "the star of the logo: " + screenText(screen));
      assertEquals(TextColor.ANSI.CYAN, star.getForegroundColor());

      // The centred menu rides in the free band: logo and mark stay whole behind it.
      assertTrue(screenText(screen).contains("Ships: design the ships"), screenText(screen));
      assertEquals('⣿', screen.getBackCharacter(37, 0).getCharacter(), "the menu starts below the logo");
      assertEquals('_', screen.getBackCharacter(31, 24).getCharacter(), "the menu ends above the shuttle");
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(62, 25).getForegroundColor(),
          "the mark below the menu is intact");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aSmallScreenLeavesTheCoverOutAndTheMenuWorks() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      ShipComposer.MenuWindow menu = ShipComposer.menu();
      gui.addWindow(menu);
      gui.updateScreen();

      assertEquals(' ', screen.getBackCharacter(5, 1).getCharacter(), "the cover does not fit: it is not painted");
      assertTrue(screenText(screen).contains("Ships:"), "the menu is still drawn: " + screenText(screen));

      // And it still responds: n moves to the hull editor and ENTER chooses it.
      menu.handleInput(new KeyStroke('n', false, false));
      menu.handleInput(new KeyStroke(KeyType.Enter));
      assertEquals(1, menu.chosen(), "n then ENTER chooses the hull editor");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void arrowsAndNpMoveTheMenuSelectionAndEnterChooses() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      ShipComposer.MenuWindow menu = ShipComposer.menu();
      gui.addWindow(menu);
      gui.updateScreen();
      ActionListBox options = menu.menu();

      assertEquals(0, options.getSelectedIndex(), "the first option is selected");
      menu.handleInput(new KeyStroke(KeyType.ArrowDown));
      assertEquals(1, options.getSelectedIndex(), "the arrows move the selection");
      menu.handleInput(new KeyStroke('n', false, false));
      assertEquals(ShipComposer.MenuWindow.QUIT, options.getSelectedIndex(), "n moves down");
      menu.handleInput(new KeyStroke('N', false, false));
      assertEquals(0, options.getSelectedIndex(), "n wraps around (uppercase too)");
      menu.handleInput(new KeyStroke('p', false, false));
      assertEquals(ShipComposer.MenuWindow.QUIT, options.getSelectedIndex(), "p moves up, wrapping");
      menu.handleInput(new KeyStroke('P', false, false));
      assertEquals(1, options.getSelectedIndex());
      menu.handleInput(new KeyStroke(KeyType.ArrowUp));
      assertEquals(0, options.getSelectedIndex());

      menu.handleInput(new KeyStroke(KeyType.Enter));
      assertEquals(0, menu.chosen(), "ENTER chooses the selected option");
      assertFalse(gui.getWindows().contains(menu), "the menu closes with its choice");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void escapeReturnsTheQuitOption() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      ShipComposer.MenuWindow menu = ShipComposer.menu();
      gui.addWindow(menu);
      gui.updateScreen();

      menu.handleInput(new KeyStroke(KeyType.Escape));
      assertEquals(ShipComposer.MenuWindow.QUIT, menu.chosen(), "ESC quits the composer from the menu");
      assertFalse(gui.getWindows().contains(menu));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theBrandRowsOfTheCoverAreExact() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      gui.addWindow(ShipComposer.menu());
      gui.updateScreen();

      // The mark under the shuttle, exactly as the cover writes it. On a 100-column
      // screen the 98-column cover starts at column 1, so the brand starts at column 58.
      assertEquals("   ·                                  _.-\"\"-.             ___ ___ _____",
          row(screen, 21).stripTrailing(), "the first row of the mark");
      assertEquals("                                  .-'  _--_  '-.         | _ ) __|_   _|",
          row(screen, 22).stripTrailing(), "the second row of the mark");
      assertEquals("                               .'   .'/  /'.   '.        | _ \\__ \\ | |",
          row(screen, 23).stripTrailing(), "the third row of the mark");
      assertEquals("                             /___/  ______  /___|        |___/___/ |_|",
          row(screen, 24).stripTrailing(), "the fourth row of the mark");
      assertEquals("                             [___] [( )( )] [___]            Beyond",
          row(screen, 25).stripTrailing(), "the Beyond row");
      assertEquals("                 ·               \\_|______|_/             Space Trader                          ·",
          row(screen, 26).stripTrailing(), "the Space Trader row");

      // The colours of the cover: the brand white, the shuttle cyan.
      assertEquals(TextColor.ANSI.WHITE, screen.getBackCharacter(58, 21).getForegroundColor(),
          "the mark is white: " + screenText(screen));
      assertEquals(TextColor.ANSI.CYAN, screen.getBackCharacter(38, 21).getForegroundColor(),
          "the shuttle is cyan: " + screenText(screen));

      // The logo is braille, never a row of B's: the BBBBBB of the report was a
      // transcription artefact of the terminal font, not what the screen holds.
      assertEquals('⣿', screen.getBackCharacter(37, 0).getCharacter(),
          "the SHIP EDITOR logo is braille: " + screenText(screen));
      assertFalse(screenText(screen).contains("BBBBBB"),
          "the screen never spells BBBBBB (the logo is braille): " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCoverFillsAScreenOfItsExactSize() throws IOException {
    TitleSplash cover = ComposerCover.load().splash();
    assertNotNull(cover, "the composer cover resource is packaged");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(cover.width(), cover.height())));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = ShipComposer.createGui(screen);
      gui.addWindow(ShipComposer.menu());
      gui.updateScreen();

      // The cover fills the screen from the corner: every ink cell lands exactly on
      // its cell of the art and nothing is shifted or painted outside the screen.
      for(int y = 0; y < cover.height(); y++) {
        for(int x = 0; x < cover.width(); x++) {
          int codePoint = cover.codePointAt(x, y);
          if(codePoint == ' ' || codePoint == TitleSplash.CONTINUATION) {
            continue;
          }
          assertEquals(new String(Character.toChars(codePoint)), screen.getBackCharacter(x, y).getCharacterString(),
              "cell " + x + "," + y + " of the cover: " + screenText(screen));
        }
      }
      // And the mark is whole, without the left margin of the wider screen.
      assertEquals("  ·                                  _.-\"\"-.             ___ ___ _____",
          row(screen, 21).stripTrailing(), "the mark at the exact-fit screen");
      assertFalse(screenText(screen).contains("BBBBBB"), "no B row on the cover: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aCoverOneCellTooSmallIsLeftOutEntirely() throws IOException {
    TitleSplash cover = ComposerCover.load().splash();
    assertNotNull(cover, "the composer cover resource is packaged");
    // One column short and one row short: the cover would not fit, so it is not
    // painted at all (not even clipped at the edges) and the menu keeps working.
    for(TerminalSize size : List.of(
        new TerminalSize(cover.width() - 1, cover.height()),
        new TerminalSize(cover.width(), cover.height() - 1))) {
      Screen screen = new TerminalScreen(new DefaultVirtualTerminal(size));
      screen.startScreen();
      try {
        MultiWindowTextGUI gui = ShipComposer.createGui(screen);
        ShipComposer.MenuWindow menu = ShipComposer.menu();
        gui.addWindow(menu);
        gui.updateScreen();

        assertFalse(screenText(screen).contains("⣿"), "no cover on " + size + ": " + screenText(screen));
        assertFalse(screenText(screen).contains("___ ___ _____"),
            "no mark on " + size + ": " + screenText(screen));
        assertTrue(screenText(screen).contains(Strings.ComposerShips), "the menu is drawn on " + size);

        menu.handleInput(new KeyStroke('n', false, false));
        menu.handleInput(new KeyStroke(KeyType.Enter));
        assertEquals(1, menu.chosen(), "the menu works on " + size);
      } finally {
        screen.stopScreen();
        screen.close();
      }
    }
  }

  /** A row of the screen, keeping the trailing spaces. */
  private static String row(Screen screen, int y) {
    StringBuilder line = new StringBuilder();
    for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
      line.append(screen.getBackCharacter(x, y).getCharacter());
    }
    return line.toString();
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      StringBuilder line = new StringBuilder();
      for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
        line.append(screen.getBackCharacter(x, y).getCharacter());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }
}
