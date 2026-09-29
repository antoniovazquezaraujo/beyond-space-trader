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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;


class ShipRendererTest {
  private static final String CHASSIS = "[uno]\ncolor=white\nkey=A color=red bgcolor=blue\n"
      + "zone=A x=1 y=0 w=2 h=1\n"
      + "xxxxx\nxx  x\n";

  @Test
  void drawsTheChassisWithTheColoursOfItsZones() throws IOException {
    ShipPicture picture = ShipRenderer.draw(design(), chassis(CHASSIS), List.of(), List.of(), 0);

    assertEquals(5, picture.width());
    assertEquals(2, picture.height());
    assertEquals('x', picture.at(0, 0).codePoint());
    assertEquals("white", picture.at(0, 0).color(), "the hull's own colour");
    assertEquals("red", picture.at(1, 0).color(), "the colour of the zone");
    assertEquals("blue", picture.at(2, 0).background(), "and its background");
    assertEquals("white", picture.at(3, 0).color(), "the zone ends at its second cell");
    assertNull(picture.at(2, 1), "a space in the drawing is empty");
    assertEquals('x', picture.at(4, 1).codePoint());
  }

  @Test
  void stampsThePiecesOverTheirSites() throws IOException {
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Beam Laser]\nkey=A\ncolor=cyan\nB\n"
        + "[Engine]\nkey=M\ncolor=white\nM\n"));
    ShipDesign design = new ShipDesign("nave", "Wasp", "uno",
        List.of(new ShipDesign.LetterGroup('A', 0, 0, 3), new ShipDesign.LetterGroup('M', 4, 0, 1)));
    ShipPicture picture = ShipRenderer.draw(design, chassis(CHASSIS), pieces,
        List.of("Beam Laser", "Pulse Laser", "Engine"), 0);

    assertEquals('B', picture.at(0, 0).codePoint(), "the first weapon");
    assertEquals("cyan", picture.at(0, 0).color());
    assertEquals('A', picture.at(1, 0).codePoint(), "and the second");
    assertEquals('x', picture.at(2, 0).codePoint(), "a site with no item keeps the chassis");
    assertEquals('M', picture.at(4, 0).codePoint(), "the engine goes on its own site");
    assertEquals("white", picture.at(4, 0).color());
  }

  @Test
  void drawsTheCargoGaugeWithoutAPiece() throws IOException {
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Cargo Gauge]\nkey=B\ncolor=green\n.\n"));
    ShipDesign design = new ShipDesign("nave", "Wasp", "uno", List.of(new ShipDesign.LetterGroup('B', 0, 0, 9)));
    ShipPicture picture = ShipRenderer.draw(design, chassis(CHASSIS), pieces, List.of(), 10);

    assertEquals("⣿⣀", text(picture, 0, 0, 2), "braille: one dot per bay (ten)");
    assertEquals('x', picture.at(2, 0).codePoint(), "and the rest of the run keeps the chassis");
  }

  @Test
  void leavesTheChassisWhenAnItemHasNoArt() throws IOException {
    ShipDesign design = new ShipDesign("nave", "Wasp", "uno", List.of(new ShipDesign.LetterGroup('A', 0, 0, 1)));
    ShipPicture picture = ShipRenderer.draw(design, chassis(CHASSIS), List.of(), List.of("Pulse Laser"), 0);

    assertEquals('x', picture.at(0, 0).codePoint(), "nothing to draw");
    assertEquals("white", picture.at(0, 0).color(), "the hull keeps its colour");
  }

  private static ShipArtFile chassis(String art) throws IOException {
    return ShipArtFile.parse(new StringReader(art)).get(0);
  }

  private static ShipDesign design() {
    return new ShipDesign("nave", "Wasp", "uno", List.of());
  }

  /** The glyphs of a run of the picture, left to right. */
  private static String text(ShipPicture picture, int x, int y, int n) {
    StringBuilder text = new StringBuilder();
    for(int i = 0; i < n; i++) {
      ShipPicture.Cell cell = picture.at(x + i, y);
      if(cell != null) {
        text.appendCodePoint(cell.codePoint());
      }
    }
    return text.toString();
  }
}
