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
        + "color=cyan\n"
        + "..##\n"
        + "#..#\n"
        + "\n"
        + "[dos]\n"
        + ";arte\n"
        + "\n"
        + " ∫\n"));

    assertEquals(2, parts.size());
    assertEquals("uno", parts.get(0).name());
    assertEquals("cyan", parts.get(0).color());
    assertEquals(3, parts.get(0).height(), "the blank row before [dos] is part of [uno]");
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

  @Test
  void blankRowsAtTheEdgesArePartOfThePart() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "[pieza]\n"
        + "\n"
        + " x \n"
        + "\n"));

    assertEquals(3, parts.get(0).height(), "the blank rows count");
    assertEquals(' ', parts.get(0).at(0, 1), "the top row is empty");
    assertEquals('x', parts.get(0).at(1, 1));
    assertEquals(' ', parts.get(0).at(2, 1), "the bottom row is empty");
  }

  @Test
  void readsTheLetterOfAPieceAndTheColourLettersOfAChassis() throws IOException {
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[motor]\nletra=M\ncolor=red\n⧯\n"));
    assertEquals("M", pieces.get(0).letter(), "the site the piece fills");

    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader(
        "[uno]\n"
        + "letra=X color=red bgcolor=blue blink=true\n"
        + "letra=Y color=green\n"
        + "zona=X x=1 y=2 w=3 h=4\n"
        + "zona=Y x=0 y=0 w=1 h=1\n"
        + "..\n"));

    assertEquals(2, chassis.get(0).letters().size());
    assertEquals(new ShipArtFile.ColorLetter('X', "red", "blue", true), chassis.get(0).letters().get(0));
    assertEquals(new ShipArtFile.ColorLetter('Y', "green", "", false), chassis.get(0).letters().get(1));
    assertEquals(2, chassis.get(0).zones().size());
    assertEquals(new ShipArtFile.Zone('X', 1, 2, 3, 4), chassis.get(0).zones().get(0));
    assertEquals(new ShipArtFile.Zone('Y', 0, 0, 1, 1), chassis.get(0).zones().get(1));
    assertEquals("", chassis.get(0).letter(), "a chassis has no piece letter");
  }

  @Test
  void wideGlyphsTakeTwoCells() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "[fichas]\n"
        + "🁣x\n"));

    assertEquals(3, parts.get(0).width(), "the domino tile takes two cells");
    assertEquals(0x1F063, parts.get(0).at(0, 0), "the whole code point comes back");
    assertEquals(ShipArtFile.CONTINUATION, parts.get(0).at(0, 1), "the second cell is its continuation");
    assertEquals('x', parts.get(0).at(0, 2), "the letter goes after the two cells");
  }

  @Test
  void wideListReservesASecondCell() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "wide=2190 U+26A1\n"
        + "[pieza]\n"
        + "←x\n"));

    assertEquals(3, parts.get(0).width(), "the arrow takes two cells now");
    assertEquals('←', parts.get(0).at(0, 0));
    assertEquals(ShipArtFile.CONTINUATION, parts.get(0).at(0, 1), "the second cell is reserved");
    assertEquals('x', parts.get(0).at(0, 2));
  }

  @Test
  void narrowListRemovesTheSecondCell() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader(
        "narrow=1F063\n"
        + "[pieza]\n"
        + "🁣x\n"));

    assertEquals(2, parts.get(0).width(), "the domino takes one cell now");
    assertEquals('x', parts.get(0).at(0, 1), "the letter comes next");
  }
}
