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
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    graphics.setCharacter(x, y, character);
  }

  @Override
  public void putInverted(int x, int y, char character, ChartColor color) {
    graphics.setForegroundColor(TextColor.ANSI.BLACK);
    graphics.setBackgroundColor(foreground(color));
    graphics.setCharacter(x, y, character);
  }

  private static TextColor foreground(ChartColor color) {
    switch(color) {
      case WHITE:
        return TextColor.ANSI.WHITE_BRIGHT;
      case WHITE_DIM:
        return TextColor.ANSI.WHITE;
      case CYAN:
        return TextColor.ANSI.CYAN_BRIGHT;
      case CYAN_DIM:
        return TextColor.ANSI.CYAN;
      case GREEN:
        return TextColor.ANSI.GREEN_BRIGHT;
      case GREEN_DIM:
        return TextColor.ANSI.GREEN;
      case YELLOW:
        return TextColor.ANSI.YELLOW_BRIGHT;
      case YELLOW_DIM:
        return TextColor.ANSI.YELLOW;
      case MAGENTA:
        return TextColor.ANSI.MAGENTA_BRIGHT;
      case MAGENTA_DIM:
        return TextColor.ANSI.MAGENTA;
      case RED:
        return TextColor.ANSI.RED_BRIGHT;
      case RED_DIM:
        return TextColor.ANSI.RED;
      case BLUE:
        return TextColor.ANSI.BLUE_BRIGHT;
      case BLUE_DIM:
        return TextColor.ANSI.BLUE;
      default:
        return TextColor.ANSI.WHITE;
    }
  }
}
