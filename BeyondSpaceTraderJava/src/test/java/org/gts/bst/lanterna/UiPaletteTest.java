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

import com.googlecode.lanterna.TextColor;
import org.junit.jupiter.api.Test;


class UiPaletteTest {
  @Test
  void statusColorGoesFromGreenToRed() {
    assertEquals(TextColor.ANSI.GREEN, UiPalette.statusColor(10, 10));
    assertEquals(TextColor.ANSI.YELLOW, UiPalette.statusColor(5, 10));
    assertEquals(TextColor.ANSI.RED, UiPalette.statusColor(2, 10));
    assertEquals(TextColor.ANSI.WHITE, UiPalette.statusColor(0, 0));
  }
}
