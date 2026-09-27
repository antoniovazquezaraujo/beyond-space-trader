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
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import spacetrader.Consts;
import spacetrader.ShipSpec;


/**
 * Builds the character art of a ship from its size class and its real fit (cargo
 * bays and weapon mounts), with a zone per piece. It is deterministic: the same
 * ship type always draws the same, and hand-made art can override it later.
 */
public final class ShipArtGenerator {
  /** Hull outlines; '.' is empty space. The nose points right. */
  private static final String[][] HULLS = {
      {
          "...._____",
          ".._/.....\\__",
          "./..........>",
          ".\\_..___.._/",
          "...\\/...\\/",
      },
      {
          "......______",
          "...__/......\\___",
          "../............\\__",
          "./..............._>",
          ".\\_..______..__/",
          "....\\/......\\/",
      },
      {
          "........________",
          "....___/........\\____",
          ".../.................\\___",
          "../...................._>",
          "..|.......................\\",
          "..|_________...._________|",
          "............\\__/",
      },
  };
  private static final int[] COCKPIT_COLUMN = {5, 7, 9};
  private static final int CARGO_BAYS_PER_POD = 5;
  private static final int MAX_WEAPON_MOUNTS = 3;

  private ShipArtGenerator() {
  }

  /** The art of a ship of the game, from its spec. */
  public static ShipArt of(ShipType type) {
    ShipSpec spec = Consts.ShipSpecs.get(type.CastToInt());
    return of(spec.getSize(), spec.CargoBays(), spec.getWeaponSlots(), type.name());
  }

  /** The art of a ship with the given size class, cargo bays and weapon mounts. */
  public static ShipArt of(ShipSize size, int cargoBays, int weaponSlots, String seed) {
    int hullIndex = Math.min(HULLS.length - 1, Math.max(0, size.CastToInt() / 2));
    String[] hull = HULLS[hullIndex];
    int width = 0;
    for(String row : hull) {
      width = Math.max(width, row.length());
    }
    List<StringBuilder> art = new ArrayList<>(hull.length);
    List<StringBuilder> zones = new ArrayList<>(hull.length);
    for(String row : hull) {
      String padded = (row + " ".repeat(width)).substring(0, width);
      art.add(new StringBuilder(padded.replace('.', ' ')));
      StringBuilder zoneRow = new StringBuilder(width);
      for(int col = 0; col < width; col++) {
        zoneRow.append(padded.charAt(col) == '.' || padded.charAt(col) == ' ' ? ' ' : ShipArt.HULL);
      }
      zones.add(zoneRow);
    }
    int interior = art.size() - 3;
    stamp(art, zones, 0, COCKPIT_COLUMN[hullIndex], "[o]", ShipArt.COCKPIT);
    stamp(art, zones, interior, 2, "<<", ShipArt.ENGINE);
    int pods = Math.min(4, cargoBays / CARGO_BAYS_PER_POD);
    if(pods > 0) {
      List<Integer> room = new ArrayList<>();
      for(int col = 5; col < width - 4; col++) {
        if(zones.get(interior).charAt(col) == ' ') {
          room.add(col);
        }
      }
      for(int pod = 0; pod < pods && pod * 5 < room.size(); pod++) {
        stamp(art, zones, interior, room.get(pod * 5), "[##]", ShipArt.CARGO);
      }
    }
    int mounts = Math.min(MAX_WEAPON_MOUNTS, weaponSlots);
    if(mounts > 0) {
      art.add(new StringBuilder(" ".repeat(width)));
      zones.add(new StringBuilder(" ".repeat(width)));
      int row = art.size() - 1;
      int step = Math.max(5, width / mounts);
      for(int mount = 0; mount < mounts; mount++) {
        String battery = (seed.hashCode() + mount) % 2 == 0 ? "=>-=" : "=[]=";
        stamp(art, zones, row, 2 + mount * step, battery, ShipArt.WEAPON);
      }
    }
    List<String> lines = new ArrayList<>(art.size());
    List<String> zoneLines = new ArrayList<>(zones.size());
    for(int row = 0; row < art.size(); row++) {
      lines.add(art.get(row).toString());
      zoneLines.add(zones.get(row).toString());
    }
    return new ShipArt(lines, zoneLines);
  }

  private static void stamp(List<StringBuilder> art, List<StringBuilder> zones, int row, int col, String text,
      char zone) {
    for(int i = 0; i < text.length() && col + i < art.get(row).length(); i++) {
      art.get(row).setCharAt(col + i, text.charAt(i));
      zones.get(row).setCharAt(col + i, zone);
    }
  }
}
