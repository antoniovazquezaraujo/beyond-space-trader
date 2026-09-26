/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;


class ShipSpritesTest {
  @Test
  void everyShipTypeHasASpriteThatFitsTheGrid() {
    for(ShipType type : ShipType.values()) {
      List<String> art = ShipSprites.of(type);
      assertFalse(art.isEmpty(), type.name());
      assertTrue(art.size() <= ShipSprites.HEIGHT, type.name() + " has too many rows");
      for(String line : art) {
        assertTrue(line.length() <= ShipSprites.WIDTH, type.name() + ": " + line);
      }
    }
  }

  @Test
  void theResourceCoversEveryShipType() throws IOException {
    try(InputStream stream = ShipSprites.class.getResourceAsStream("/spacetrader/ships.txt")) {
      assertNotNull(stream, "ships.txt must be packaged");
      String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      for(ShipType type : ShipType.values()) {
        assertTrue(text.contains("[" + type.name() + "]"), type.name());
      }
    }
  }

  @Test
  void shipsHaveTheirOwnSprite() {
    assertNotEquals(ShipSprites.of(ShipType.Flea), ShipSprites.of(ShipType.Wasp));
  }
}
