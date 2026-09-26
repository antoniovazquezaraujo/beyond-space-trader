/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;


class LanternaChartViewTest {
  @Test
  void drawsTheChartOnALanternaSurface() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(21, 9)));
    screen.startScreen();
    try {
      LanternaChartView view = new LanternaChartView(screen.newTextGraphics(), screen.getTerminalSize());
      List<ChartSystem> systems = List.of(
          new ChartSystem(0, 0, "Here", true, false, false, false),
          new ChartSystem(3, 0, "Sol", false, false, false, false));

      view.render(ChartViewModel.shortRange(systems, 0, 0, 5, 20, null));

      TextCharacter current = screen.getBackCharacter(10, 4);
      assertEquals('+', current.getCharacter());
      assertEquals(TextColor.ANSI.CYAN, current.getForegroundColor());
      TextCharacter sol = screen.getBackCharacter(13, 4);
      assertEquals('o', sol.getCharacter());
      assertEquals(TextColor.ANSI.GREEN, sol.getForegroundColor());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }
}
