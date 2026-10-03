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

import org.junit.jupiter.api.Test;


class EditorTextTest {
  @Test
  void measuresPadsAndCutsTextByTerminalCells() {
    // Arrange: a glyph the terminal paints two columns wide (一, U+4E00)
    // Act / Assert: the measure counts the two cells of the wide glyph
    assertEquals(3, EditorText.width("abc"));
    assertEquals(2, EditorText.width("一"));
    assertEquals(4, EditorText.width("a一b"));

    // the padding adds as many spaces as cells are missing, and never cuts
    assertEquals("a  ", EditorText.padRight("a", 3));
    assertEquals("a一", EditorText.padRight("a一", 3));
    assertEquals("  a", EditorText.padLeft("a", 3));
    assertEquals("  一", EditorText.padLeft("一", 4));
    assertEquals("abcd", EditorText.padRight("abcd", 2));
    assertEquals("abcd", EditorText.padLeft("abcd", 2));

    // the cut stops on the cell limit without breaking the wide glyph in half
    assertEquals("a一", EditorText.cut("a一b", 3));
    assertEquals("ab", EditorText.cut("abc", 2));
  }
}
