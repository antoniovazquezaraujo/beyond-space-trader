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
        new ShipAssembly.ShipPlacement("motor", 0, 2, "naranja"),
        new ShipAssembly.ShipPlacement("antena", 6, 0, "cian")));
    Path file = Files.createTempFile("naves", ".txt");
    try {
      ShipAssembly.save(file.toString(), assembly);
      assertEquals(assembly, ShipAssembly.load(file.toString()));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void addsAndUndoesPieces() {
    ShipAssembly assembly = ShipAssembly.empty("bloque-mediano");
    assembly = assembly.with(new ShipAssembly.ShipPlacement("motor", 1, 1, "rojo"));
    assertEquals(1, assembly.pieces().size());
    assertEquals(0, assembly.withoutLast().pieces().size());
  }
}
