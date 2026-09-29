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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * A ship design, one section of ships.txt: its name, its game type (the spec that
 * sets the site budget), the chassis it uses and the letter groups (the sites the
 * game fills, with where they start, how many there are and their colour).
 */
public record ShipDesign(String name, String type, String chassis, List<LetterGroup> groups) {
  /** A group of the same site letter: where it starts, how many and its colour. */
  public record LetterGroup(char letter, int x, int y, int n) {
  }

  /** Parses every design of a reader. */
  public static List<ShipDesign> parse(Reader reader) throws IOException {
    List<ShipDesign> designs = new ArrayList<>();
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    String name = null;
    String type = "";
    String chassis = "";
    List<LetterGroup> groups = new ArrayList<>();
    for(String line = buffered.readLine(); line != null; line = buffered.readLine()) {
      String trimmed = line.strip();
      if(trimmed.isEmpty() || trimmed.startsWith(";;")) {
        continue;
      }
      if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
        if(name != null) {
          designs.add(new ShipDesign(name, type, chassis, List.copyOf(groups)));
        }
        name = trimmed.substring(1, trimmed.length() - 1).strip();
        type = "";
        chassis = "";
        groups.clear();
        continue;
      }
      if(name == null) {
        continue;
      }
      if(trimmed.startsWith("type=")) {
        type = trimmed.substring("type=".length()).strip();
      } else if(trimmed.startsWith("chasis=") || trimmed.startsWith("chassis=")) {
        chassis = trimmed.substring(trimmed.indexOf('=') + 1).strip();
      } else if(trimmed.startsWith("group=")) {
        LetterGroup group = group(trimmed.substring("group=".length()));
        if(group != null) {
          groups.add(group);
        }
      }
    }
    if(name != null) {
      designs.add(new ShipDesign(name, type, chassis, List.copyOf(groups)));
    }
    return designs;
  }

  /** Parses "A x=1 y=2 n=3 color=red"; the numbers and the colour are optional. */
  private static LetterGroup group(String text) {
    String[] tokens = text.strip().split("\\s+");
    if(tokens.length == 0 || tokens[0].isEmpty()) {
      return null;
    }
    char letter = tokens[0].charAt(0);
    int x = 0;
    int y = 0;
    int n = 1;
    for(int i = 1; i < tokens.length; i++) {
      String[] pair = tokens[i].split("=", 2);
      if(pair.length != 2) {
        continue;
      }
      try {
        switch(pair[0]) {
          case "x":
            x = Integer.parseInt(pair[1]);
            break;
          case "y":
            y = Integer.parseInt(pair[1]);
            break;
          case "n":
            n = Integer.parseInt(pair[1]);
            break;
          default:
            break;
        }
      } catch(NumberFormatException e) {
        // un numero raro se ignora
      }
    }
    return new LetterGroup(letter, x, y, n);
  }

  /** Loads the designs of a file, or an empty list when the file does not exist. */
  public static List<ShipDesign> load(String fileName) throws IOException {
    Path path = Path.of(fileName);
    if(!Files.isRegularFile(path)) {
      return List.of();
    }
    try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  /** Saves every design of the list, one section each. */
  public static void save(String fileName, List<ShipDesign> designs) throws IOException {
    try(PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(fileName), StandardCharsets.UTF_8))) {
      for(ShipDesign design : designs) {
        writer.println("[" + design.name() + "]");
        writer.println("type=" + design.type());
        writer.println("chasis=" + design.chassis());
        for(LetterGroup group : design.groups()) {
          writer.println("group=" + group.letter() + " x=" + group.x() + " y=" + group.y() + " n=" + group.n());
        }
      }
    }
  }
}
