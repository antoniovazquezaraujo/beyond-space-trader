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
 * A ship assembly: a chassis and the pieces placed on it, each with its position
 * and its colour. It is what the composer saves to ships/naves.txt and reloads.
 */
public record ShipAssembly(String name, String chassis, List<ShipPlacement> pieces) {
  /** One piece of the assembly: which one, where and its colour. */
  public record ShipPlacement(String piece, int x, int y, String color) {
  }

  public static ShipAssembly empty(String chassis) {
    return new ShipAssembly("borrador", chassis, List.of());
  }

  public ShipAssembly with(ShipPlacement placement) {
    List<ShipPlacement> all = new ArrayList<>(pieces);
    all.add(placement);
    return new ShipAssembly(name, chassis, List.copyOf(all));
  }

  public ShipAssembly withoutLast() {
    if(pieces.isEmpty()) {
      return this;
    }
    return new ShipAssembly(name, chassis, List.copyOf(pieces.subList(0, pieces.size() - 1)));
  }

  public ShipAssembly withChassis(String newChassis) {
    return new ShipAssembly(name, newChassis, pieces);
  }

  /** Loads an assembly, or null when the file does not exist yet. */
  public static ShipAssembly load(String fileName) throws IOException {
    Path path = Path.of(fileName);
    if(!Files.isRegularFile(path)) {
      return null;
    }
    try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  public static void save(String fileName, ShipAssembly assembly) throws IOException {
    try(PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of(fileName), StandardCharsets.UTF_8))) {
      writer.println("[" + assembly.name() + "]");
      writer.println("chasis=" + assembly.chassis());
      for(ShipPlacement placement : assembly.pieces()) {
        writer.println("pieza=" + placement.piece() + " x=" + placement.x() + " y=" + placement.y()
            + " color=" + placement.color());
      }
    }
  }

  /** Parses the first assembly of a reader. */
  static ShipAssembly parse(Reader reader) throws IOException {
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    String name = "borrador";
    String chassis = "";
    List<ShipPlacement> pieces = new ArrayList<>();
    for(String line = buffered.readLine(); line != null; line = buffered.readLine()) {
      String trimmed = line.strip();
      if(trimmed.isEmpty() || trimmed.startsWith(";")) {
        continue;
      }
      if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
        name = trimmed.substring(1, trimmed.length() - 1).strip();
      } else if(trimmed.startsWith("chasis=")) {
        chassis = trimmed.substring("chasis=".length()).strip();
      } else if(trimmed.startsWith("pieza=")) {
        String[] parts = trimmed.substring("pieza=".length()).split("\\s+");
        if(parts.length >= 4) {
          pieces.add(new ShipPlacement(parts[0], Integer.parseInt(parts[1].substring(2)),
              Integer.parseInt(parts[2].substring(2)), parts[3].substring("color=".length())));
        }
      }
    }
    return new ShipAssembly(name, chassis, List.copyOf(pieces));
  }
}
