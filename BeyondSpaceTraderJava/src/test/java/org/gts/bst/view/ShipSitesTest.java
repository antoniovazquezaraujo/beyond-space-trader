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
    assertNull(ShipSites.budgetOf("insecto-diminuto"), "a chassis that is not a ship type");
  }

  @Test
  void drawsTheCargoGauge() {
    assertEquals("⣿⣀", ShipSites.gauge(10), "the owner's table: 10 bays");
    assertEquals("⣿", ShipSites.gauge(8));
    assertEquals("", ShipSites.gauge(0));
    assertEquals("⣿⣿⣿⣿⣿⣿⣿⣤", ShipSites.gauge(60), "the owner's table: 60 bays");
  }

  @Test
  void countsTheSitesOfAChassis() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader("[Wasp]\nMMA\n"));
    List<ShipSites.Site> sites = ShipSites.sitesOf(parts.get(0));

    assertEquals(2, sites.size());
    assertEquals(new ShipSites.Site('M', 0, 0, 2), sites.get(0));
    assertEquals(new ShipSites.Site('A', 2, 0, 1), sites.get(1));
    assertEquals(2, ShipSites.count(sites).get('M'));
    assertEquals(2, ShipSites.longestRun(sites).get('M'));
    assertEquals(1, ShipSites.longestRun(sites).get('A'));
  }

  @Test
  void warnsAboutTheSitesTheTypeNeeds() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader("[Wasp]\nMMA\n"));
    Map<Character, Integer> placed = ShipSites.count(ShipSites.sitesOf(parts.get(0)));
    Map<Character, Integer> runs = ShipSites.longestRun(ShipSites.sitesOf(parts.get(0)));
    List<String> warnings = ShipSites.warnings("Wasp", placed, runs);

    assertTrue(warnings.contains("faltan sitios de arma (2)"), warnings.toString());
    assertTrue(warnings.contains("faltan celdas en la bodega (6)"), warnings.toString());
    assertTrue(warnings.contains("faltan sitios de cabina (1)"), warnings.toString());
    assertEquals(7, warnings.size(), warnings.toString());

    List<String> panel = ShipSites.panel("Wasp", placed, runs);
    assertTrue(panel.get(0).contains("Wasp (Huge)"), panel.toString());
    assertTrue(panel.toString().contains("arma"), panel.toString());
    assertTrue(panel.toString().contains("1/3"), panel.toString());
  }

  @Test
  void flagsTheSitesTheTypeDoesNotAdmit() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader("[Flea]\nAAA\n"));
    List<String> warnings = ShipSites.warnings("Flea", ShipSites.count(ShipSites.sitesOf(parts.get(0))),
        ShipSites.longestRun(ShipSites.sitesOf(parts.get(0))));

    assertTrue(warnings.contains("sobran sitios de arma (3)"), warnings.toString());
  }

  @Test
  void theCustomShipAdmitsExtraWeapons() throws IOException {
    List<ShipArtFile> parts = ShipArtFile.parse(new StringReader("[Custom]\nAAAAAAAA\n"));
    List<String> warnings = ShipSites.warnings("Custom", ShipSites.count(ShipSites.sitesOf(parts.get(0))),
        ShipSites.longestRun(ShipSites.sitesOf(parts.get(0))));

    assertTrue(warnings.stream().noneMatch(w -> w.contains("sobran") && w.contains("arma")), warnings.toString());
  }
}
