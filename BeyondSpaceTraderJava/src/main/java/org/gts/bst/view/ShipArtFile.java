/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


/**
 * One definition of a ship part (a chassis or a piece) read from a text file:
 * a name, a colour name and the drawing, where '.' is empty and everything else
 * (unicode included) is part of the art.
 */
public record ShipArtFile(String name, String color, List<String> lines) {
  /** Loads the parts of a file, looking for it in the usual places. */
  public static List<ShipArtFile> load(String fileName) throws IOException {
    File file = find(fileName);
    if(file == null) {
      throw new IOException("no encuentro " + fileName + " (¿ejecutas desde la raíz del repositorio?)");
    }
    try(BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  private static File find(String fileName) {
    for(String candidate : new String[] {"ships/" + fileName, "../ships/" + fileName,
        "BeyondSpaceTraderJava/ships/" + fileName}) {
      File file = new File(candidate);
      if(file.isFile()) {
        return file;
      }
    }
    return null;
  }

  /** Parses the parts of a reader: sections, optional colours and art lines. */
  static List<ShipArtFile> parse(Reader reader) throws IOException {
    List<ShipArtFile> parts = new ArrayList<>();
    String name = null;
    String color = "blanco";
    List<String> lines = new ArrayList<>();
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    for(String line = buffered.readLine(); line != null; line = buffered.readLine()) {
      String trimmed = line.strip();
      if(trimmed.isEmpty() || trimmed.startsWith(";")) {
        continue;
      }
      if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
        if(name != null) {
          parts.add(new ShipArtFile(name, color, List.copyOf(lines)));
        }
        name = trimmed.substring(1, trimmed.length() - 1).strip();
        color = "blanco";
        lines.clear();
      } else if(name != null && trimmed.startsWith("color=")) {
        color = trimmed.substring("color=".length()).strip();
      } else if(name != null) {
        lines.add(line.replace('.', ' '));
      }
    }
    if(name != null) {
      parts.add(new ShipArtFile(name, color, List.copyOf(lines)));
    }
    return parts;
  }

  public int width() {
    int width = 0;
    for(String line : lines) {
      width = Math.max(width, line.length());
    }
    return width;
  }

  public int height() {
    return lines.size();
  }

  public char at(int row, int column) {
    String line = lines.get(row);
    return column < line.length() ? line.charAt(column) : ' ';
  }
}
