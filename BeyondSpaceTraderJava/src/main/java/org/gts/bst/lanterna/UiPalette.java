/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;


/**
 * The ANSI colour palette of the text UI. Every colour has a meaning and is applied
 * through the helpers below, so the whole look can be adjusted in this single file.
 *
 * <p>Text is drawn with {@link #line}, titles with {@link #title}, the keys of a line
 * (written as {@code [C]}) with {@link #keys} and the selected row of a list with
 * {@link #selected}. Amounts use {@link #money} and status values use
 * {@link #statusColor} (green when healthy, yellow when low, red when bad).</p>
 */
public final class UiPalette {
  /** Plain text. */
  public static final TextColor TEXT = TextColor.ANSI.WHITE;
  /** The background of the screen. */
  public static final TextColor BACKGROUND = TextColor.ANSI.BLACK;
  /** Panel titles and section headings. */
  public static final TextColor TITLE = TextColor.ANSI.CYAN;
  /** Keys between brackets, e.g. {@code [C]}. */
  public static final TextColor KEY = TextColor.ANSI.YELLOW;
  /** Emphasised values (day, target, current system). */
  public static final TextColor ACCENT = TextColor.ANSI.CYAN;
  /** Amounts of credits. */
  public static final TextColor MONEY = TextColor.ANSI.YELLOW;
  /** Healthy values: clean record, good hull, ... */
  public static final TextColor GOOD = TextColor.ANSI.GREEN;
  /** Values to watch out for. */
  public static final TextColor WARN = TextColor.ANSI.YELLOW;
  /** Dangerous values: debts, damage, criminal record. */
  public static final TextColor BAD = TextColor.ANSI.RED;
  /** The wormhole marks of the charts and the panels. */
  public static final TextColor WORMHOLE = TextColor.ANSI.MAGENTA;
  public static final TextColor SELECTED_FG = TextColor.ANSI.BLACK;
  public static final TextColor SELECTED_BG = TextColor.ANSI.CYAN;

  private UiPalette() {
  }

  /** Restores the base colours after a styled run of text. */
  public static void reset(TextGraphics graphics) {
    graphics.setForegroundColor(TEXT);
    graphics.setBackgroundColor(BACKGROUND);
  }

  /** Draws plain text clipped to {@code maxX}; returns the x after the text. */
  public static int draw(TextGraphics graphics, int x, int y, String text, TextColor color, int maxX) {
    if(text == null || text.isEmpty() || x >= maxX) {
      return x;
    }
    String clipped = text.length() > maxX - x ? text.substring(0, maxX - x) : text;
    graphics.setForegroundColor(color);
    graphics.setBackgroundColor(BACKGROUND);
    graphics.putString(x, y, clipped);
    reset(graphics);
    return x + clipped.length();
  }

  /** Draws a whole line of plain text, clipped to {@code width}. */
  public static void line(TextGraphics graphics, int x, int y, String text, int width) {
    draw(graphics, x, y, text, TEXT, x + width);
  }

  /** Draws a panel title (cyan). */
  public static void title(TextGraphics graphics, int x, int y, String text, int width) {
    draw(graphics, x, y, text, TITLE, x + width);
  }

  /** Draws an amount (yellow). */
  public static void money(TextGraphics graphics, int x, int y, String text, int width) {
    draw(graphics, x, y, text, MONEY, x + width);
  }

  /** Draws a whole line as the selected row of a list (padded to {@code width}). */
  public static void selected(TextGraphics graphics, int x, int y, String text, int width) {
    String line = text.length() > width ? text.substring(0, width) : text;
    graphics.setForegroundColor(SELECTED_FG);
    graphics.setBackgroundColor(SELECTED_BG);
    graphics.putString(x, y, line);
    if(line.length() < width) {
      graphics.putString(x + line.length(), y, " ".repeat(width - line.length()));
    }
    reset(graphics);
  }

  /** Draws text colouring every bracketed key ({@code [C]}, {@code [ESC]}) in yellow. */
  public static void keys(TextGraphics graphics, int x, int y, String text, int width) {
    String line = text.length() > width ? text.substring(0, width) : text;
    int offset = 0;
    while(offset < line.length()) {
      int open = line.indexOf('[', offset);
      int close = open < 0 ? -1 : line.indexOf(']', open);
      if(open < 0 || close < 0) {
        draw(graphics, x + offset, y, line.substring(offset), TEXT, x + width);
        return;
      }
      if(open > offset) {
        draw(graphics, x + offset, y, line.substring(offset, open), TEXT, x + width);
      }
      draw(graphics, x + open, y, line.substring(open, close + 1), KEY, x + width);
      offset = close + 1;
    }
  }

  /** Green when the value is at two thirds or more, yellow from one third, red below. */
  public static TextColor statusColor(int value, int max) {
    if(max <= 0) {
      return TEXT;
    }
    if(value * 3 >= max * 2) {
      return GOOD;
    }
    return value * 3 >= max ? WARN : BAD;
  }
}
