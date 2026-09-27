/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.ArrayList;
import java.util.List;


/**
 * The character art of a ship and the zone of every cell (hull, cockpit, weapon,
 * cargo, engine). The views colour it by zone, and the zones can drive other
 * things later (where a shot hits, the engines going dark when disabled...).
 */
public record ShipArt(List<String> lines, List<String> zones) {
  public static final char HULL = 'H';
  public static final char COCKPIT = 'K';
  public static final char WEAPON = 'W';
  public static final char CARGO = 'C';
  public static final char ENGINE = 'E';

  public int width() {
    return lines.isEmpty() ? 0 : lines.get(0).length();
  }

  public int height() {
    return lines.size();
  }

  public char at(int row, int col) {
    String line = lines.get(row);
    return col < line.length() ? line.charAt(col) : ' ';
  }

  public char zoneAt(int row, int col) {
    String zone = zones.get(row);
    return col < zone.length() ? zone.charAt(col) : ' ';
  }

  /** The same ship looking the other way. */
  public ShipArt mirrored() {
    List<String> mirroredLines = new ArrayList<>(lines.size());
    List<String> mirroredZones = new ArrayList<>(zones.size());
    for(String line : lines) {
      mirroredLines.add(mirror(line));
    }
    for(String zone : zones) {
      mirroredZones.add(mirror(zone));
    }
    return new ShipArt(mirroredLines, mirroredZones);
  }

  private static String mirror(String text) {
    StringBuilder mirrored = new StringBuilder(text.length());
    for(int i = text.length() - 1; i >= 0; i--) {
      mirrored.append(flip(text.charAt(i)));
    }
    return mirrored.toString();
  }

  private static char flip(char character) {
    switch(character) {
      case '\\':
        return '/';
      case '/':
        return '\\';
      case '(':
        return ')';
      case ')':
        return '(';
      case '[':
        return ']';
      case ']':
        return '[';
      case '<':
        return '>';
      case '>':
        return '<';
      case '{':
        return '}';
      case '}':
        return '{';
      default:
        return character;
    }
  }
}
