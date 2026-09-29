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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;


class ShipDesignTest {
  @Test
  void parsesSeveralDesignsWithGroups() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        ";; las naves\n"
        + "[firefly pirata]\n"
        + "tipo=Firefly\n"
        + "fuselaje=insecto-diminuto\n"
        + "grupo=A x=3 y=1 n=2 color=red\n"
        + "grupo=M x=6 y=3\n"
        + "\n"
        + "[wasp]\n"
        + "tipo=Wasp\n"
        + "chasis=insecto-pequeno\n"
        + "grupo=E x=2 y=2 color=cyan\n"));

    assertEquals(2, designs.size());
    assertEquals("firefly pirata", designs.get(0).name());
    assertEquals("Firefly", designs.get(0).type());
    assertEquals("insecto-diminuto", designs.get(0).chassis());
    assertEquals(2, designs.get(0).groups().size());
    assertEquals(new ShipDesign.LetterGroup('A', 3, 1, 2, "red"), designs.get(0).groups().get(0));
    assertEquals(new ShipDesign.LetterGroup('M', 6, 3, 1, ""), designs.get(0).groups().get(1));
    assertEquals("Wasp", designs.get(1).type());
    assertEquals("insecto-pequeno", designs.get(1).chassis(), "chasis= is accepted too");
    assertEquals(1, designs.get(1).groups().get(0).n(), "n defaults to one");
    assertEquals("cyan", designs.get(1).groups().get(0).color());
  }

  @Test
  void savesAndReloadsEveryDesign() throws IOException {
    List<ShipDesign> designs = List.of(
        new ShipDesign("uno", "Firefly", "hull-1", List.of(new ShipDesign.LetterGroup('A', 1, 2, 3, "red"))),
        new ShipDesign("dos", "Wasp", "hull-2", List.of()));
    Path file = Files.createTempFile("naves", ".txt");
    try {
      ShipDesign.save(file.toString(), designs);
      assertEquals(designs, ShipDesign.load(file.toString()));
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
