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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.gts.bst.ship.ShipSize;
import org.junit.jupiter.api.Test;


class ShipSitesTest {
  @Test
  void readsTheBudgetOfTheShipType() {
    ShipSites.Budget wasp = ShipSites.budgetOf("Wasp");
    assertEquals(ShipSize.Huge, wasp.size());
    assertEquals(35, wasp.cargoBays());
    assertEquals(6, wasp.cargoCells(), "35 bays plus two cargo gadgets need six cells");
    assertEquals(3, wasp.weapons());
    assertEquals(2, wasp.shields());
    assertEquals(2, wasp.gadgets());
    assertEquals(3, wasp.engines());
    assertEquals(2, wasp.fuel());
    assertEquals(1, wasp.pod(), "the Wasp can be bought");

    assertEquals(9, ShipSites.budgetOf("Termite").cargoCells(), "60 bays plus two gadgets need nine cells");
    assertEquals(2, ShipSites.budgetOf("Flea").cargoCells());
    assertEquals(0, ShipSites.budgetOf("Flea").weapons(), "the Flea carries no weapons");
    assertEquals(0, ShipSites.budgetOf("Bottle").pod(), "the Bottle is not for sale");
    assertNull(ShipSites.budgetOf("scout"), "a chassis that is not a ship type");
  }

  @Test
  void drawsTheCargoGauge() {
    assertEquals("⣿⣀", ShipSites.gauge(10), "the owner's table: 10 bays");
    assertEquals("⣿", ShipSites.gauge(8));
    assertEquals("", ShipSites.gauge(0));
    assertEquals("⣿⣿⣿⣿⣿⣿⣿⣤", ShipSites.gauge(60), "the owner's table: 60 bays");
  }




  @Test
  void checksTheSizeOfAChassis() {
    assertTrue(ShipSites.sizeFits("", ShipSize.Huge), "no size declared always fits");
    assertTrue(ShipSites.sizeFits("any", ShipSize.Tiny), "any fits everything");
    assertTrue(ShipSites.sizeFits("Huge", ShipSize.Huge), "the case does not matter");
    assertFalse(ShipSites.sizeFits("tiny", ShipSize.Huge), "a rowing boat is not a cargo ship");
  }

  @Test
  void countsTheSitesByKindUsingThePieces() throws IOException {
    List<ShipArtFile> pieces = ShipArtFile.parse(new StringReader(
        "[Pulse Laser]\nkey=A\ncolor=red\nA\n[Engine]\nkey=M\ncolor=white\nM\n"));
    List<ShipDesign.LetterGroup> groups = List.of(
        new ShipDesign.LetterGroup('A', 1, 1, 2),
        new ShipDesign.LetterGroup('M', 4, 1, 1),
        new ShipDesign.LetterGroup('Z', 6, 1, 3));

    assertEquals(ShipSites.Kind.WEAPON, ShipSites.kindOf("Pulse Laser"));
    assertEquals(ShipSites.Kind.SHIELD, ShipSites.kindOf("Energy Shield"));
    assertEquals(ShipSites.Kind.GADGET, ShipSites.kindOf("Cloaking Device"));
    assertEquals(ShipSites.Kind.CARGO, ShipSites.kindOf("Cargo Gauge"));
    assertEquals(ShipSites.Kind.PART, ShipSites.kindOf("Antenna"));
    assertEquals(ShipSites.Kind.WEAPON, ShipSites.kindOfLetter('A', pieces));
    assertEquals(ShipSites.Kind.ENGINES, ShipSites.kindOfLetter('M', pieces));
    assertEquals(ShipSites.Kind.PART, ShipSites.kindOfLetter('Z', pieces), "a letter with no piece");
    List<ShipArtFile> roles = ShipArtFile.parse(new StringReader(
        "[Role Trader]\nkey=R\ncolor=gold\n$\n[Role Pirate]\nkey=R\ncolor=white\n\u2620\n"));
    assertEquals(ShipSites.Kind.ROLE, ShipSites.kindOfLetter('R', roles),
        "the three markers share the site letter: the game picks the glyph");

    Map<ShipSites.Kind, Integer> counts = ShipSites.countsByKind(groups, pieces);
    assertEquals(2, counts.get(ShipSites.Kind.WEAPON));
    assertEquals(1, counts.get(ShipSites.Kind.ENGINES));
    assertEquals(3, counts.get(ShipSites.Kind.PART));
  }

}
