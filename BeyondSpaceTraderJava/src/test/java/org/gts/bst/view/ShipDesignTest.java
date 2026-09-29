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
        + "type=Firefly\n"
        + "chasis=scout\n"
        + "group=A x=3 y=1 n=2 color=red\n"
        + "group=M x=6 y=3\n"
        + "\n"
        + "[wasp]\n"
        + "type=Wasp\n"
        + "chassis=corvette\n"
        + "group=E x=2 y=2 color=cyan\n"));

    assertEquals(2, designs.size());
    assertEquals("firefly pirata", designs.get(0).name());
    assertEquals("Firefly", designs.get(0).type());
    assertEquals("scout", designs.get(0).chassis());
    assertEquals(2, designs.get(0).groups().size());
    assertEquals(new ShipDesign.LetterGroup('A', 3, 1, 2), designs.get(0).groups().get(0));
    assertEquals(new ShipDesign.LetterGroup('M', 6, 3, 1), designs.get(0).groups().get(1));
    assertEquals("Wasp", designs.get(1).type());
    assertEquals("corvette", designs.get(1).chassis(), "the chassis= spelling is accepted too");
    assertEquals(1, designs.get(1).groups().get(0).n(), "n defaults to one");
    assertEquals('E', designs.get(1).groups().get(0).letter(), "an old color= is just ignored");
  }

  @Test
  void savesAndReloadsEveryDesign() throws IOException {
    List<ShipDesign> designs = List.of(
        new ShipDesign("uno", "Firefly", "hull-1", List.of(new ShipDesign.LetterGroup('A', 1, 2, 3))),
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
