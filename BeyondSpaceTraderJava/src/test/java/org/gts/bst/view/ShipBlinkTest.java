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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;


class ShipBlinkTest {
  @Test
  void piecesCanBlink() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "[sirena]\n"
        + "color=brightred\n"
        + "blink=yes\n"
        + "##\n"
        + "\n"
        + "[motor]\n"
        + "color=orange\n"
        + "##\n"));

    assertTrue(parts.get(0).blink(), "the siren blinks");
    assertFalse(parts.get(1).blink(), "the engine does not");
    assertTrue(parts.get(0).color().equals("brightred"));
  }
}
