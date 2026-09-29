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
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\nkey=M\ncolor=red\nM\n"));
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
      assertTrue(screenText(screen).contains("motor(M)"), "the pieces list: " + screenText(screen));

      view.handleKey(new KeyStroke('M', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("motor 1/1"), "the sites summary: " + screenText(screen));

      view.handleKey(new KeyStroke('s', false, false));
      List<ShipDesign> saved = ShipDesign.load(file.toString());
      assertEquals(1, saved.size());
      assertEquals("Firefly", saved.get(0).type());
      assertEquals("uno", saved.get(0).chassis());
      assertEquals(1, saved.get(0).groups().size());
      assertEquals(new ShipDesign.LetterGroup('M', 2, 2, 1, "white"), saved.get(0).groups().get(0));

      // el espacio borra el grupo entero
      view.handleKey(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("motor 0/1"), "the group is gone: " + screenText(screen));
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
