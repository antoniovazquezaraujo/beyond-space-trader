/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

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
import org.junit.jupiter.api.Test;


class HullEditorViewTest {
  @Test
  void addsAColourLetterPaintsItAndSaves() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=white\nxxxxx\nxxxxx\n"));
    Path file = Files.createTempFile("chassis", ".txt");
    Files.writeString(file, "[uno]\ncolor=white\nxxxxx\nxxxxx\n");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      HullEditorView view = new HullEditorView(hulls);
      view.hullsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains("uno"), screenText(screen));

      view.handleKey(new KeyStroke('+', false, false));
      view.handleKey(new KeyStroke('t', false, false));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Cyan (A)"), "the panel of elements: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      String saved = Files.readString(file);
      assertTrue(saved.contains("key=A color=cyan"), saved);
      assertTrue(saved.contains("zone=A x=2 y=2 w=1 h=1"), saved);
      assertTrue(saved.contains("xxxxx"), "the drawing stays: " + saved);
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void removesTheSelectedElementAndItsLetters() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=white\nxxxxx\nxxxxx\n"));
    Path file = Files.createTempFile("chassis", ".txt");
    Files.writeString(file, "[uno]\ncolor=white\nxxxxx\nxxxxx\n");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      HullEditorView view = new HullEditorView(hulls);
      view.hullsPath(file.toString());
      gui.addWindow(view);
      gui.updateScreen();

      view.handleKey(new KeyStroke('+', false, false));
      view.handleKey(new KeyStroke('t', false, false));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("1. Cyan (A)"), screenText(screen));

      // - removes the element and the letters it had painted
      view.handleKey(new KeyStroke('-', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("removed A"), screenText(screen));
      assertFalse(screenText(screen).contains("Cyan"), "the panel is empty: " + screenText(screen));
      assertTrue(screenText(screen).contains("no elements"), screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      String saved = Files.readString(file);
      assertFalse(saved.contains("key="), saved);
      assertFalse(saved.contains("zone="), saved);
      assertTrue(saved.contains("xxxxx"), "the drawing stays: " + saved);
    } finally {
      screen.stopScreen();
      screen.close();
      Files.deleteIfExists(file);
    }
  }

  @Test
  void paintsTheBackgroundsOfTheColourElements() throws IOException {
    List<ShipArtFile> hulls = ShipArtFile.parse(new StringReader("[uno]\ncolor=white\nxxxxx\nxxxxx\n"));
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      HullEditorView view = new HullEditorView(hulls);
      gui.addWindow(view);
      gui.updateScreen();
      assertFalse(hasBackground(screen, TextColor.ANSI.BLUE), screenText(screen));

      // + adds the element, f cycles its background to blue and space paints it
      view.handleKey(new KeyStroke('+', false, false));
      view.handleKey(new KeyStroke('f', false, false));
      view.handleKey(new KeyStroke('f', false, false));
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(hasBackground(screen, TextColor.ANSI.BLUE),
          "the blue background of the element is painted: " + screenText(screen));
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
