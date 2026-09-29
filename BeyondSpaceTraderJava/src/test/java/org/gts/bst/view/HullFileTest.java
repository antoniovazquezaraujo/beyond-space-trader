/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;


class HullFileTest {
  @Test
  void rewritesOnlyTheLettersAndZonesOfTheEditedHulls() throws IOException {
    String content = ";; cabecera\n[uno]\ncolor=white\nkey=Z color=olive\nzone=Z x=0 y=0 w=1 h=1\nxxxxx\n\n"
        + "[dos]\ncolor=red\nzzz\n";
    Path file = Files.createTempFile("chassis", ".txt");
    Files.writeString(file, content);
    try {
      ShipArtFile uno = new ShipArtFile("uno", "white", List.of(), false, "", "",
          List.of(new ShipArtFile.ColorLetter('X', "red", "blue", true)),
          List.of(new ShipArtFile.Zone('X', 1, 2, 3, 4)));
      HullFile.save(file.toString(), List.of(uno));

      String saved = Files.readString(file);
      assertTrue(saved.contains(";; cabecera"), saved);
      assertTrue(saved.contains("key=X color=red bgcolor=blue blink=true"), saved);
      assertTrue(saved.contains("zone=X x=1 y=2 w=3 h=4"), saved);
      assertFalse(saved.contains("key=Z"), "the old letters are gone: " + saved);
      assertTrue(saved.contains("xxxxx"), "the drawing stays: " + saved);
      assertTrue(saved.contains("[dos]") && saved.contains("zzz"), "the other hull stays: " + saved);
      assertTrue(saved.indexOf("key=X") < saved.indexOf("xxxxx"), "the letters go after the header: " + saved);
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
