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

import com.googlecode.lanterna.TextColor;
import org.junit.jupiter.api.Test;


class ShipColorsTest {
  @Test
  void takesEnglishNames() {
    assertEquals(TextColor.ANSI.RED, ShipColors.color("red"));
    assertEquals(TextColor.ANSI.CYAN, ShipColors.color("cyan"));
    assertEquals(TextColor.ANSI.WHITE_BRIGHT, ShipColors.color("brightwhite"));
    assertEquals(new TextColor.Indexed(208), ShipColors.color("orange"));
    assertEquals(new TextColor.Indexed(238), ShipColors.color("darkgrey"));
  }

  @Test
  void ignoresSpanishNames() {
    assertEquals(TextColor.ANSI.WHITE, ShipColors.color("rojo"), "names are English only");
    assertEquals(TextColor.ANSI.WHITE, ShipColors.color("cian"), "names are English only");
  }

  @Test
  void takesHexAndPaletteNumbers() {
    assertEquals(new TextColor.RGB(255, 136, 0), ShipColors.color("#ff8800"));
    assertEquals(new TextColor.Indexed(45), ShipColors.color("45"));
    assertEquals(TextColor.ANSI.WHITE, ShipColors.color("no-such-colour"));
  }
}
