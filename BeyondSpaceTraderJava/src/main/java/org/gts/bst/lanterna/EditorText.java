/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipPicture;
import org.gts.bst.view.ShipColors;


/** Small helpers shared by the editors: painting glyphs and cutting text. */
final class EditorText {
  private EditorText() {
  }

  /** The two colours of a glyph: what is drawn and what is behind it. */
  record Brush(TextColor color, TextColor background) {
  }

  /**
   * The brush of a drawing: the colour over its background. With the blink in its
   * dark half the two swap, so the shape shows in the background colour over a
   * block of its own colour.
   */
  static Brush brush(String color, String background, boolean blink, boolean blinkOn) {
    TextColor foreground = ShipColors.color(color);
    TextColor behind = background == null || background.isEmpty() ? TextColor.ANSI.BLACK
        : ShipColors.color(background);
    return blink && !blinkOn ? new Brush(behind, foreground) : new Brush(foreground, behind);
  }

  /** Draws one glyph (a code point) with a brush, optionally reversed. */
  static void glyph(TextGUIGraphics graphics, int column, int row, int codePoint, Brush brush, boolean reverse) {
    TextCharacter character = TextCharacter.fromString(new String(Character.toChars(codePoint)), brush.color(),
        brush.background())[0];
    graphics.setCharacter(column, row, reverse ? character.withModifier(SGR.REVERSE) : character);
  }

  /** Draws one glyph (a code point) in its colour on black, optionally reversed. */
  static void glyph(TextGUIGraphics graphics, int column, int row, int codePoint, TextColor color, boolean reverse) {
    glyph(graphics, column, row, codePoint, new Brush(color, TextColor.ANSI.BLACK), reverse);
  }

  /** Draws one cell of a ship picture (its colours come as names). */
  static void glyph(TextGUIGraphics graphics, int column, int row, ShipPicture.Cell cell) {
    glyph(graphics, column, row, cell, true);
  }

  /** Draws one cell of a ship picture: with the blink off, it hides in its background. */
  static void glyph(TextGUIGraphics graphics, int column, int row, ShipPicture.Cell cell, boolean blinkOn) {
    glyph(graphics, column, row, cell.codePoint(), brush(cell.color(), cell.background(), cell.blink(), blinkOn), false);
  }

  /**
   * Paints a ship picture centred in a column, from a row on and down to another:
   * returns how many rows it used, so the caller can keep laying out below it.
   */
  static int picture(TextGUIGraphics graphics, int left, int row, int width, int maxRow, ShipPicture picture) {
    return picture(graphics, left, row, width, maxRow, picture, true);
  }

  /** The same painting, with the phase of the blink of the drawing. */
  static int picture(TextGUIGraphics graphics, int left, int row, int width, int maxRow, ShipPicture picture,
      boolean blinkOn) {
    int from = left + Math.max(0, (width - picture.width()) / 2);
    int rows = 0;
    for(int y = 0; y < picture.height() && row + y < maxRow; y++) {
      for(int x = 0; x < picture.width(); x++) {
        ShipPicture.Cell cell = picture.at(x, y);
        if(cell != null && !cell.continuation()) {
          glyph(graphics, from + x, row + y, cell, blinkOn);
        }
      }
      rows++;
    }
    return rows;
  }

  /** A panel section: the title inside a line, like `─ Ships ─────`. */
  static String section(String title, int cells) {
    String head = "─ " + title + " ";
    if(head.length() >= cells) {
      return cut(head, cells);
    }
    return head + "─".repeat(cells - head.length());
  }

  /** Trims a text to a number of cells, without breaking a glyph in half. */
  static String cut(String text, int cells) {
    StringBuilder cut = new StringBuilder();
    int width = 0;
    for(int i = 0; i < text.length() && width < cells; ) {
      int codePoint = text.codePointAt(i);
      i += Character.charCount(codePoint);
      width += ShipArtFile.isWide(codePoint) ? 2 : 1;
      if(width > cells) {
        break;
      }
      cut.appendCodePoint(codePoint);
    }
    return cut.toString();
  }
}
