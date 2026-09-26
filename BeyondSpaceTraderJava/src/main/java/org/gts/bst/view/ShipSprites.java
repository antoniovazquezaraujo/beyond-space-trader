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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.gts.bst.ship.ShipType;


/**
 * The ASCII sprites of the ships, loaded from the {@code spacetrader/ships.txt}
 * resource (one {@code [ShipType]} section per ship). They are small schematic
 * drawings in a fixed grid; any ship without art uses a generic sprite.
 */
public final class ShipSprites {
  public static final int WIDTH = 12;
  public static final int HEIGHT = 5;
  private static final List<String> GENERIC = List.of(
      "     /\\",
      "    /  \\_",
      "   | o o >",
      "    \\___/");
  private static final Map<ShipType, List<String>> SPRITES = load();

  private ShipSprites() {
  }

  /** The sprite lines of the given ship type (never empty). */
  public static List<String> of(ShipType type) {
    List<String> sprite = SPRITES.get(type);
    return sprite != null ? sprite : GENERIC;
  }

  private static Map<ShipType, List<String>> load() {
    Map<ShipType, List<String>> sprites = new EnumMap<>(ShipType.class);
    try(InputStream stream = ShipSprites.class.getResourceAsStream("/spacetrader/ships.txt")) {
      if(stream == null) {
        return sprites;
      }
      ShipType current = null;
      List<String> lines = new ArrayList<>();
      for(String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n", -1)) {
        if(line.startsWith("[") && line.endsWith("]")) {
          add(sprites, current, lines);
          current = typeOf(line.substring(1, line.length() - 1));
          lines = new ArrayList<>();
        } else if(current != null) {
          String art = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
          lines.add(art.length() > WIDTH ? art.substring(0, WIDTH) : art);
        }
      }
      add(sprites, current, lines);
    } catch(IOException e) {
      // Without the resource every ship falls back to the generic sprite.
    }
    return sprites;
  }

  private static void add(Map<ShipType, List<String>> sprites, ShipType type, List<String> lines) {
    if(type == null) {
      return;
    }
    while(!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
      lines.remove(lines.size() - 1);
    }
    if(!lines.isEmpty()) {
      sprites.put(type, List.copyOf(lines));
    }
  }

  private static ShipType typeOf(String name) {
    for(ShipType type : ShipType.values()) {
      if(type.name().equals(name)) {
        return type;
      }
    }
    return null;
  }
}
