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

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;


class ShipArtFileTest {
  @Test
  void parsesSectionsColorsAndArt() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "; comentario\n"
        + "\n"
        + "[uno]\n"
        + "color=cian\n"
        + "..##\n"
        + "#..#\n"
        + "\n"
        + "[dos]\n"
        + ";arte\n"
        + "\n"
        + " ∫\n"));

    assertEquals(2, parts.size());
    assertEquals("uno", parts.get(0).name());
    assertEquals("cian", parts.get(0).color());
    assertEquals(2, parts.get(0).height());
    assertEquals(4, parts.get(0).width());
    assertEquals('.', parts.get(0).at(0, 0), "a dot is drawn");
    assertEquals('.', parts.get(0).at(0, 1), "a dot is drawn");
    assertEquals('#', parts.get(0).at(0, 2));
    assertEquals('#', parts.get(0).at(1, 0));
    assertEquals("white", parts.get(1).color(), "white is the default");
    assertEquals(';', parts.get(1).at(0, 0), "an art line can start with a semicolon");
    assertEquals(' ', parts.get(1).at(1, 0), "a blank line is an empty row");
    assertEquals('∫', parts.get(1).at(2, 1), "unicode is allowed");
  }

  @Test
  void keepsSpacesLiterallyAtBothEnds() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "[pieza]\n"
        + "  .  \n"
        + "  x  \n"));

    assertEquals(5, parts.get(0).width(), "the trailing spaces count");
    assertEquals(' ', parts.get(0).at(0, 0), "the leading space is empty");
    assertEquals('.', parts.get(0).at(0, 2), "the dot is drawn");
    assertEquals(' ', parts.get(0).at(0, 4), "the trailing space is kept");
    assertEquals('x', parts.get(0).at(1, 2));
  }
}
