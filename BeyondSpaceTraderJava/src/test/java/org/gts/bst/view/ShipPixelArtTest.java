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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;


class ShipPixelArtTest {
  @Test
  void encodesThePixelsAsBraille() {
    ShipArt art = ShipPixelArtGenerator.of(ShipType.Wasp, 0).toShipArt();
    assertTrue(art.width() > 0 && art.height() > 0);
    for(String line : art.lines()) {
      for(char character : line.toCharArray()) {
        assertTrue(character == ' ' || (character >= 0x2800 && character <= 0x28FF),
            "not a braille character: " + (int) character);
      }
    }
  }

  @Test
  void mirroringFlipsTheDots() {
    boolean[][] pixels = new boolean[4][2];
    pixels[0][0] = true;
    char[][] zones = {{'H'}};
    ShipPixelArt art = new ShipPixelArt(pixels, zones);

    assertEquals("\u2801", art.toShipArt().lines().get(0).trim(), "dot 1, top left");
    assertEquals("\u2808", art.mirrored().toShipArt().lines().get(0).trim(), "mirrored, dot 4, top right");
  }

  @Test
  void drawsTheSameShipEveryTime() {
    assertEquals(ShipPixelArtGenerator.of(ShipType.Gnat, 3).toShipArt(),
        ShipPixelArtGenerator.of(ShipType.Gnat, 3).toShipArt());
  }
}
