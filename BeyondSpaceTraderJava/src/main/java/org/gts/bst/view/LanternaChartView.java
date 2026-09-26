/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;


/**
 * Draws a chart on a Lanterna surface. The graphics must be relative to the chart
 * area (use {@code graphics.newTextGraphics(position, size)}), so the view only
 * cares about its own coordinates.
 */
public final class LanternaChartView implements ChartView {
  private final TextGraphics graphics;
  private final TerminalSize size;

  public LanternaChartView(TextGraphics graphics, TerminalSize size) {
    this.graphics = graphics;
    this.size = size;
  }

  @Override
  public int width() {
    return size.getColumns();
  }

  @Override
  public int height() {
    return size.getRows();
  }

  @Override
  public void put(int x, int y, char character, ChartColor color) {
    graphics.setForegroundColor(foreground(color));
    graphics.setBackgroundColor(background(color));
    graphics.setCharacter(x, y, character);
  }

  private static TextColor foreground(ChartColor color) {
    switch(color) {
      case GREEN:
        return TextColor.ANSI.GREEN;
      case YELLOW:
        return TextColor.ANSI.YELLOW;
      case RED:
        return TextColor.ANSI.RED;
      case CYAN:
        return TextColor.ANSI.CYAN;
      case MAGENTA:
        return TextColor.ANSI.MAGENTA;
      case SELECTED:
        return TextColor.ANSI.BLACK;
      default:
        return TextColor.ANSI.WHITE;
    }
  }

  private static TextColor background(ChartColor color) {
    return color == ChartColor.SELECTED ? TextColor.ANSI.WHITE : TextColor.ANSI.BLACK;
  }
}
