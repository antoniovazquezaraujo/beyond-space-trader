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
import java.util.Map;
import java.util.TreeMap;


/**
 * The letters painted over a hull while editing: a grid where every cell holds a
 * letter or nothing. The runs (letter, x, y, length) are derived from it, so
 * painting, erasing and saving stay simple and the files stay compact.
 */
public final class LetterGrid {
  /** A run of the same letter in a row. */
  public record Run(char letter, int x, int y, int n) {
  }

  private final Map<Integer, Map<Integer, Character>> cells = new TreeMap<>();

  /** The grid of a ship design, from its letter groups. */
  public static LetterGrid ofDesign(ShipDesign design) {
    LetterGrid grid = new LetterGrid();
    for(ShipDesign.LetterGroup group : design.groups()) {
      for(int i = 0; i < Math.max(1, group.n()); i++) {
        grid.set(group.x() + i, group.y(), group.letter());
      }
    }
    return grid;
  }

  /** The grid of a hull, from its colour zones. */
  public static LetterGrid ofZones(ShipArtFile hull) {
    LetterGrid grid = new LetterGrid();
    for(ShipArtFile.Zone zone : hull.zones()) {
      for(int row = 0; row < Math.max(1, zone.h()); row++) {
        for(int column = 0; column < Math.max(1, zone.w()); column++) {
          grid.set(zone.x() + column, zone.y() + row, zone.letter());
        }
      }
    }
    return grid;
  }

  public void set(int x, int y, char letter) {
    if(x < 0 || y < 0) {
      return;
    }
    cells.computeIfAbsent(y, row -> new TreeMap<>()).put(x, letter);
  }

  public void clear(int x, int y) {
    Map<Integer, Character> row = cells.get(y);
    if(row == null) {
      return;
    }
    row.remove(x);
    if(row.isEmpty()) {
      cells.remove(y);
    }
  }

  /** Removes every cell with a letter (when the element that paints it is deleted). */
  public int clearLetter(char letter) {
    int cleared = 0;
    for(Map<Integer, Character> row : cells.values()) {
      for(int x : new ArrayList<>(row.keySet())) {
        if(row.get(x) == letter) {
          row.remove(x);
          cleared++;
        }
      }
    }
    cells.values().removeIf(Map::isEmpty);
    return cleared;
  }

  /** The letter of a cell, or ' ' when it is empty. */
  public char at(int x, int y) {
    Map<Integer, Character> row = cells.get(y);
    Character letter = row == null ? null : row.get(x);
    return letter == null ? ' ' : letter;
  }

  /** The run that covers a cell, or null when the cell is empty. */
  public Run runAt(int x, int y) {
    char letter = at(x, y);
    if(letter == ' ') {
      return null;
    }
    int from = x;
    while(at(from - 1, y) == letter) {
      from--;
    }
    int to = x;
    while(at(to + 1, y) == letter) {
      to++;
    }
    return new Run(letter, from, y, to - from + 1);
  }

  /** The runs of every row, in reading order: what the files save. */
  public List<Run> runs() {
    List<Run> runs = new ArrayList<>();
    for(Map.Entry<Integer, Map<Integer, Character>> row : cells.entrySet()) {
      int y = row.getKey();
      Map<Integer, Character> columns = row.getValue();
      for(int x : new ArrayList<>(columns.keySet())) {
        char letter = columns.get(x);
        if(letter == ' ' || at(x - 1, y) == letter) {
          continue;
        }
        int n = 1;
        while(at(x + n, y) == letter) {
          n++;
        }
        runs.add(new Run(letter, x, y, n));
      }
    }
    return runs;
  }

  /** The colour zones of a hull, from the grid (one run per row). */
  public List<ShipArtFile.Zone> zones() {
    List<ShipArtFile.Zone> zones = new ArrayList<>();
    for(Run run : runs()) {
      zones.add(new ShipArtFile.Zone(run.letter(), run.x(), run.y(), run.n(), 1));
    }
    return zones;
  }
}
