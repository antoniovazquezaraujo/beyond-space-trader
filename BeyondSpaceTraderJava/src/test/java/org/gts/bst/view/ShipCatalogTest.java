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
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;


class ShipCatalogTest {
  @Test
  void drawsTheAssemblyOfTheType() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader("[uno]\nsize=small\nxxxxx\n"));
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader("[Engine]\nkey=M\ncolor=red\nM\n"));
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[nave]\ntype=Firefly\nchasis=uno\ngroup=M x=1 y=0 n=1\n"));
    ShipCatalog catalog = new ShipCatalog(designs, chassis, pieces);

    ShipPicture picture = catalog.picture(ShipType.Firefly, List.of("Engine"), 0);

    assertEquals('x', picture.at(0, 0).codePoint());
    assertEquals('M', picture.at(1, 0).codePoint(), "the engine goes on the site of the assembly");
    assertEquals("red", picture.at(1, 0).color());
  }

  @Test
  void fallsBackToAChassisOfTheSize() throws IOException {
    List<ShipArtFile> chassis = ShipArtFile.parse(new StringReader(
        "[pequeno]\nsize=small\nxxx\n[grande]\nsize=huge\nxxxx\n"));
    ShipCatalog catalog = new ShipCatalog(List.of(), chassis, List.of());

    ShipPicture picture = catalog.picture(ShipType.Firefly, List.of(), 0);

    assertEquals(3, picture.width(), "the chassis of its size, not the huge one");
    assertEquals('x', picture.at(0, 0).codePoint());
  }

  @Test
  void withoutArtThePictureIsEmpty() {
    ShipPicture picture = new ShipCatalog(List.of(), List.of(), List.of())
        .picture(ShipType.Firefly, List.of(), 0);

    assertEquals(0, picture.width());
    assertEquals(0, picture.height());
  }
}
