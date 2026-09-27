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


class ShipArtGeneratorTest {
  @Test
  void drawsTheSameShipEveryTime() {
    assertEquals(ShipArtGenerator.of(ShipType.Gnat), ShipArtGenerator.of(ShipType.Gnat));
    assertTrue(ShipArtGenerator.of(ShipType.Gnat).width() > 0);
  }

  @Test
  void biggerShipsDrawBigger() {
    assertTrue(ShipArtGenerator.of(ShipType.Termite).width() > ShipArtGenerator.of(ShipType.Flea).width());
  }

  @Test
  void theFitShowsInTheZones() {
    ShipArt gnat = ShipArtGenerator.of(ShipType.Gnat);
    assertTrue(zoneCount(gnat, ShipArt.COCKPIT) > 0);
    assertTrue(zoneCount(gnat, ShipArt.ENGINE) > 0);
    assertTrue(zoneCount(gnat, ShipArt.CARGO) > 0);
    assertTrue(zoneCount(gnat, ShipArt.WEAPON) > 0);
    assertEquals(0, zoneCount(ShipArtGenerator.of(ShipType.Flea), ShipArt.WEAPON),
        "the Flea has no weapon mounts");
  }

  @Test
  void mirroringTwiceIsTheSameShip() {
    ShipArt art = ShipArtGenerator.of(ShipType.Beetle);
    assertEquals(art, art.mirrored().mirrored());
  }

  private static int zoneCount(ShipArt art, char zone) {
    int count = 0;
    for(int row = 0; row < art.height(); row++) {
      for(int col = 0; col < art.width(); col++) {
        if(art.zoneAt(row, col) == zone) {
          count++;
        }
      }
    }
    return count;
  }
}
