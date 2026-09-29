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


/** Small helpers shared by the editors: painting glyphs and cutting text. */
final class EditorText {
  private EditorText() {
  }

  /** Draws one glyph (a code point) in its colour, optionally reversed. */
  static void glyph(TextGUIGraphics graphics, int column, int row, int codePoint, TextColor color, boolean reverse) {
    TextCharacter character = TextCharacter.fromString(new String(Character.toChars(codePoint)), color,
        TextColor.ANSI.BLACK)[0];
    graphics.setCharacter(column, row, reverse ? character.withModifier(SGR.REVERSE) : character);
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
