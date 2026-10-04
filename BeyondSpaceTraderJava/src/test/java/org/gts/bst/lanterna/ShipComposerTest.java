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
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import org.junit.jupiter.api.Test;


class ShipComposerTest {
  @Test
  void theCoverIsPaintedCentredBehindTheMenu() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = composerGui(screen);
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
      MultiWindowTextGUI gui = composerGui(screen);
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
      MultiWindowTextGUI gui = composerGui(screen);
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
      MultiWindowTextGUI gui = composerGui(screen);
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

  /** The composer GUI: the cover behind the windows, with the project theme. */
  private static MultiWindowTextGUI composerGui(Screen screen) {
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), ComposerCover.load());
    gui.setTheme(LanternaTheme.create());
    return gui;
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
