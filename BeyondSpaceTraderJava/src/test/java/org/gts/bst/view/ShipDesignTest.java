/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on SpaceTrader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        + "[Firefly]\n"
        + "chasis=small-2\n"
        + "group=A x=3 y=1 n=2 color=red\n"
        + "group=M x=6 y=3\n"
        + "\n"
        + "[Wasp]\n"
        + "chassis=medium-1\n"
        + "group=E x=2 y=2 color=cyan\n"));

    assertEquals(2, designs.size());
    assertEquals("Firefly", designs.get(0).type(), "the header of the section is the type");
    assertEquals("small-2", designs.get(0).chassis());
    assertEquals(2, designs.get(0).groups().size());
    assertEquals(new ShipDesign.LetterGroup('A', 3, 1, 2), designs.get(0).groups().get(0));
    assertEquals(new ShipDesign.LetterGroup('M', 6, 3, 1), designs.get(0).groups().get(1));
    assertEquals("Wasp", designs.get(1).type(), "the matching ignores case");
    assertEquals("medium-1", designs.get(1).chassis(), "the chassis= spelling is accepted too");
    assertEquals(1, designs.get(1).groups().get(0).n(), "n defaults to one");
    assertEquals('E', designs.get(1).groups().get(0).letter(), "an old color= is just ignored");
  }

  /** The old files named their sections and declared the type: both are still read. */
  @Test
  void anOldFileWithANameAndTypeStillLoads() throws IOException {
    List<ShipDesign> designs = ShipDesign.parse(new StringReader(
        "[La niña]\n"
        + "type=Firefly\n"
        + "chasis=small-2\n"
        + "group=A x=1 y=1 n=1\n"));

    assertEquals(1, designs.size());
    assertEquals("Firefly", designs.get(0).type(), "the type= of an old file wins over the header");
    assertEquals("small-2", designs.get(0).chassis());
    assertEquals(List.of(new ShipDesign.LetterGroup('A', 1, 1, 1)), designs.get(0).groups());
  }

  @Test
  void savesHeadingEverySectionWithItsType() throws IOException {
    List<ShipDesign> designs = List.of(
        new ShipDesign("Firefly", "hull-1", List.of(new ShipDesign.LetterGroup('A', 1, 2, 3))),
        new ShipDesign("Wasp", "hull-2", List.of()));
    Path file = Files.createTempFile("naves", ".txt");
    try {
      ShipDesign.save(file.toString(), designs);

      String saved = Files.readString(file);
      assertTrue(saved.contains("[Firefly]"), "the header is the game type: " + saved);
      assertFalse(saved.contains("type="), "the new format has no type= line: " + saved);
      assertEquals(designs, ShipDesign.load(file.toString()), "and it round-trips");
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
