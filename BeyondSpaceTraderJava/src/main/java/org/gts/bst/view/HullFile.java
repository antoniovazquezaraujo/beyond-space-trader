/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Writes the colour letters and the zones of some hulls back to the chassis file.
 * Only the key= and zone= lines of those hulls are rewritten, right after their
 * header: the drawing, the comments and the other hulls stay exactly as they are.
 */
public final class HullFile {
  private HullFile() {
  }

  public static void save(String fileName, List<ShipArtFile> hulls) throws IOException {
    Path path = Path.of(fileName);
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    List<String> out = new ArrayList<>();
    ShipArtFile current = null;
    for(String line : lines) {
      String trimmed = line.strip();
      if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
        current = find(hulls, trimmed.substring(1, trimmed.length() - 1).strip());
        out.add(line);
        if(current != null) {
          if(!current.size().isEmpty()) {
            out.add("size=" + current.size());
          }
          for(ShipArtFile.ColorLetter letter : current.letters()) {
            out.add("key=" + letter.letter() + " color=" + letter.color()
                + (letter.bgColor().isEmpty() ? "" : " bgcolor=" + letter.bgColor())
                + (letter.blink() ? " blink=true" : ""));
          }
          for(ShipArtFile.Zone zone : current.zones()) {
            out.add("zone=" + zone.letter() + " x=" + zone.x() + " y=" + zone.y() + " w=" + zone.w() + " h="
                + zone.h());
          }
        }
        continue;
      }
      if(current != null && (trimmed.startsWith("key=") || trimmed.startsWith("zone=")
          || trimmed.startsWith("size="))) {
        continue; // las viejas se tiran: las nuevas van arriba, tras la cabecera
      }
      out.add(line);
    }
    Files.write(path, out, StandardCharsets.UTF_8);
  }

  private static ShipArtFile find(List<ShipArtFile> hulls, String name) {
    for(ShipArtFile hull : hulls) {
      if(hull.name().equals(name)) {
        return hull;
      }
    }
    return null;
  }
}
