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
import org.junit.jupiter.api.Test;
import spacetrader.Strings;


class ShipArtViewTest {
  @Test
  void drawsAShipAndLetsItBeMovedAndChanged() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      ShipArtView view = new ShipArtView();
      gui.addWindow(view);
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.ShipNames.get(0)), screenText(screen));

      int x = view.shipX();
      view.handleKey(new KeyStroke(KeyType.ArrowRight));
      gui.updateScreen();
      assertEquals(x + 1, view.shipX());

      view.handleKey(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertEquals(1, view.shipIndex());
      assertTrue(screenText(screen).contains(Strings.ShipNames.get(1)), screenText(screen));

      view.handleKey(new KeyStroke('b', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[b] braille (si)"), screenText(screen));
      view.handleKey(new KeyStroke('b', false, false));
      view.handleKey(new KeyStroke('z', false, false));
      view.handleKey(new KeyStroke('c', false, false));
      view.handleKey(new KeyStroke('d', false, false));
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains("[z] zonas (si)"), text);
      assertTrue(text.contains("paleta: pirata"), text);
      assertTrue(text.contains("[d] motores (apagados)"), text);
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
        line.append(screen.getBackCharacter(column, row).getCharacter());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }
}
