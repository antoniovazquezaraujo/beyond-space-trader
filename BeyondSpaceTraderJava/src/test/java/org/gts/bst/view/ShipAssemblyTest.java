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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;


class ShipAssemblyTest {
  @Test
  void savesAndLoadsAnAssembly() throws IOException {
    ShipAssembly assembly = new ShipAssembly("prueba", "insecto-diminuto", List.of(
        new ShipAssembly.ShipPlacement("motor", 0, 2, "orange"),
        new ShipAssembly.ShipPlacement("antena", 6, 0, "cyan")));
    Path file = Files.createTempFile("naves", ".txt");
    try {
      ShipAssembly.save(file.toString(), assembly);
      assertEquals(assembly, ShipAssembly.load(file.toString()));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void keepsNamesWithSpacesAndSkipsBrokenLines() throws IOException {
    ShipAssembly assembly = ShipAssembly.parse(new java.io.StringReader(
        "[prueba]\n"
        + "chasis=uno\n"
        + "pieza=torreta laser x=9 y=4 color=red\n"
        + "pieza=vaina x=1 y=1\n"
        + "pieza=motor x=0 y=2 color=orange\n"));

    assertEquals(2, assembly.pieces().size(), "the broken line is skipped");
    assertEquals("torreta laser", assembly.pieces().get(0).piece());
    assertEquals(9, assembly.pieces().get(0).x());
    assertEquals("orange", assembly.pieces().get(1).color());
  }

  @Test
  void addsAndUndoesPieces() {
    ShipAssembly assembly = ShipAssembly.empty("bloque-mediano");
    assembly = assembly.with(new ShipAssembly.ShipPlacement("motor", 1, 1, "red"));
    assertEquals(1, assembly.pieces().size());
    assertEquals(0, assembly.withoutLast().pieces().size());
  }
}
