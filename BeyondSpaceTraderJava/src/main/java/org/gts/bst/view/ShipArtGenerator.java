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
import java.util.Random;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import spacetrader.Consts;
import spacetrader.ShipSpec;


/**
 * Builds the character art of a ship by layers, so it reads as a dense, detailed
 * vessel instead of a clean outline: an irregular hull band, a texture per panel,
 * the functional modules (engines, turrets, cargo pods, cockpit, sensors) and a
 * fringe of greebles. Everything is deterministic per ship type and variant, so
 * the same model always draws the same.
 */
public final class ShipArtGenerator {
  /** Length and height per size class (Tiny..Gargantuan). */
  private static final int[] WIDTHS = {18, 26, 34, 42, 48, 48};
  private static final int[] HEIGHTS = {8, 9, 10, 11, 12, 12};
  /** Textures for the hull panels; the long ones fill, the short ones detail. */
  private static final String[] PANELS = {
      "#=#=#=#=#=", "::::::::::", "-.-.-.-.-.", ";:;:;:;:;:", "=[][][][[]", "..........", "##########",
      "=+ =+ =+ =", "::;::;::;;"};
  private static final String GREEBLE = "'.^|+*<>~:-,v";

  private ShipArtGenerator() {
  }

  /** The art of a ship of the game, first variant. */
  public static ShipArt of(ShipType type) {
    return of(type, 0);
  }

  /** The art of a ship of the game; every variant is a different drawing. */
  public static ShipArt of(ShipType type, int variant) {
    ShipSpec spec = Consts.ShipSpecs.get(type.CastToInt());
    return of(spec.getSize(), spec.CargoBays(), spec.getWeaponSlots(), type.name() + "#" + variant);
  }

  /** The art of a ship with the given size class, cargo bays and weapon mounts. */
  public static ShipArt of(ShipSize size, int cargoBays, int weaponSlots, String seed) {
    int classIndex = Math.max(0, Math.min(WIDTHS.length - 1, size.CastToInt()));
    int width = WIDTHS[classIndex];
    int height = HEIGHTS[classIndex];
    Random random = new Random(seed.hashCode());
    char[][] art = blank(width, height);
    char[][] zones = blank(width, height);
    int middle = height / 2;

    int[] thickness = thickness(random, width, height);
    hull(random, art, zones, thickness, middle);
    texture(random, art, zones);
    details(random, art, zones, height);
    engines(random, art, zones, middle);
    nose(random, art, zones, width);
    cockpit(random, art, zones, width, height);
    cargo(random, art, zones, width, height, cargoBays);
    turrets(random, art, zones, width, height, weaponSlots);
    sensors(random, art, zones, width, height);
    greebles(random, art, zones, width, height);
    return new ShipArt(join(art), join(zones));
  }

  private static char[][] blank(int width, int height) {
    char[][] grid = new char[height][width];
    for(char[] row : grid) {
      java.util.Arrays.fill(row, ' ');
    }
    return grid;
  }

  /** Hull thickness per column: steps and a taper at both ends. */
  private static int[] thickness(Random random, int width, int height) {
    int[] thickness = new int[width];
    int level = 4 + random.nextInt(3);
    for(int column = 0; column < width; column++) {
      if(random.nextDouble() < 0.10) {
        level = Math.max(3, Math.min(height - 3, level + (random.nextBoolean() ? 1 : -1)));
      }
      thickness[column] = level;
    }
    int taper = Math.max(6, width / 5);
    for(int column = 0; column < taper; column++) {
      thickness[column] = Math.max(2, Math.min(thickness[column], 2 + column));
      thickness[width - 1 - column] = Math.max(2, Math.min(thickness[width - 1 - column], 2 + column));
    }
    return thickness;
  }

  private static void hull(Random random, char[][] art, char[][] zones, int[] thickness, int middle) {
    for(int column = 0; column < art[0].length; column++) {
      int top = Math.max(0, middle - thickness[column] / 2);
      int bottom = Math.min(art.length - 1, top + thickness[column] - 1);
      for(int row = top; row <= bottom; row++) {
        art[row][column] = row == top || row == bottom ? (random.nextBoolean() ? '_' : '-') : ':';
        zones[row][column] = ShipArt.HULL;
      }
    }
  }

  /** Panels: runs of a texture per row, with dividers, lights and shadows. */
  private static void texture(Random random, char[][] art, char[][] zones) {
    int width = art[0].length;
    for(int row = 0; row < art.length; row++) {
      int column = 0;
      while(column < width) {
        if(zones[row][column] == ShipArt.HULL && art[row][column] == ':') {
          String panel = PANELS[random.nextInt(PANELS.length)];
          int run = 3 + random.nextInt(6);
          for(int i = 0; i < run && column + i < width; i++) {
            if(art[row][column + i] == ':') {
              art[row][column + i] = panel.charAt((column + i) % panel.length());
            }
          }
          column += run;
        } else {
          column++;
        }
      }
    }
    for(int column = 4; column < width - 6; column += 5 + random.nextInt(4)) {
      for(int row = 0; row < art.length; row++) {
        if(zones[row][column] == ShipArt.HULL && art[row][column] != '_' && art[row][column] != '-') {
          art[row][column] = random.nextBoolean() ? '|' : ':';
        }
      }
    }
    for(int i = 0; i < 5 + random.nextInt(5); i++) {
      int column = 4 + random.nextInt(Math.max(1, width - 10));
      int row = random.nextInt(art.length);
      if(zones[row][column] == ShipArt.HULL && art[row][column] != '_' && art[row][column] != '-') {
        art[row][column] = random.nextBoolean() ? '.' : 'o';
      }
    }
    for(int i = 0; i < 2 + random.nextInt(2); i++) {
      int column = 5 + random.nextInt(Math.max(1, width - 12));
      int row = random.nextInt(art.length);
      if(zones[row][column] == ShipArt.HULL) {
        for(int step = 0; step < 3 + random.nextInt(3) && column + step < width - 2; step++) {
          if(zones[row][column + step] == ShipArt.HULL && art[row][column + step] != '_'
              && art[row][column + step] != '-') {
            art[row][column + step] = random.nextBoolean() ? '.' : ' ';
          }
        }
      }
    }
  }

  private static void details(Random random, char[][] art, char[][] zones, int height) {
    int width = art[0].length;
    int row = 2 + random.nextInt(Math.max(1, height - 4));
    int column = 6 + random.nextInt(Math.max(1, width - 14));
    for(int i = 0; i < 4 + random.nextInt(5) && column + i < width - 3; i++) {
      if(zones[row][column + i] == ShipArt.HULL && art[row][column + i] != '_' && art[row][column + i] != '-') {
        art[row][column + i] = random.nextBoolean() ? '.' : 'o';
      }
    }
  }

  private static void engines(Random random, char[][] art, char[][] zones, int middle) {
    for(int column = 0; column < 2 + random.nextInt(2); column++) {
      for(int row = 0; row < art.length; row++) {
        if(zones[row][column] == ShipArt.HULL) {
          art[row][column] = "{([".charAt(random.nextInt(3));
          zones[row][column] = ShipArt.ENGINE;
        }
      }
      int row = Math.min(art.length - 1, middle + 1);
      art[row][column] = "=~-".charAt(random.nextInt(3));
      zones[row][column] = ShipArt.ENGINE;
    }
  }

  private static void nose(Random random, char[][] art, char[][] zones, int width) {
    for(int row = 0; row < art.length; row++) {
      if(zones[row][width - 1] == ShipArt.HULL) {
        art[row][width - 1] = ">=~".charAt(random.nextInt(3));
      }
      if(zones[row][width - 2] == ShipArt.HULL) {
        art[row][width - 2] = "=~-".charAt(random.nextInt(3));
      }
    }
  }

  private static void cockpit(Random random, char[][] art, char[][] zones, int width, int height) {
    int column = Math.max(4, width - 7 - random.nextInt(4));
    for(int row = 0; row < height; row++) {
      if(zones[row][column] == ShipArt.HULL && (art[row][column] == '_' || art[row][column] == '-')) {
        for(int i = 0; i < 3 && column + i < width; i++) {
          art[row][column + i] = "[o]".charAt(i);
          zones[row][column + i] = ShipArt.COCKPIT;
        }
        return;
      }
    }
  }

  private static void cargo(Random random, char[][] art, char[][] zones, int width, int height, int cargoBays) {
    int pods = Math.min(4, cargoBays / 5);
    for(int pod = 0; pod < pods; pod++) {
      int column = 5 + pod * Math.max(6, (width - 12) / Math.max(1, pods));
      if(column + 3 >= width - 3) {
        continue;
      }
      for(int row = height - 2; row > 1; row--) {
        if(zones[row][column] == ShipArt.HULL && zones[row][column + 1] == ShipArt.HULL
            && art[row][column] != '_' && art[row][column] != '-') {
          String podArt = new String[]{"[##]", "[::]", "{==}"}[random.nextInt(3)];
          for(int i = 0; i < podArt.length(); i++) {
            art[row][column + i] = podArt.charAt(i);
            zones[row][column + i] = ShipArt.CARGO;
          }
          break;
        }
      }
    }
  }

  private static void turrets(Random random, char[][] art, char[][] zones, int width, int height, int weaponSlots) {
    int mounts = Math.min(3, weaponSlots);
    for(int mount = 0; mount < mounts; mount++) {
      int start = 4 + random.nextInt(Math.max(1, width - 12));
      for(int attempt = 0; attempt < width - 8; attempt++) {
        int column = 4 + (start + attempt) % Math.max(1, width - 8);
        for(int row = height - 2; row > 0; row--) {
          if(zones[row][column] == ShipArt.HULL && art[row][column] != ' ') {
            if(row + 1 < height && art[row + 1][column] == ' ') {
              String turret = new String[]{"-^-", "=[]=", "vv", "|^|"}[random.nextInt(4)];
              for(int i = 0; i < turret.length() && column + i < width; i++) {
                art[row + 1][column + i] = turret.charAt(i);
                zones[row + 1][column + i] = ShipArt.WEAPON;
              }
              attempt = width;
            }
            break;
          }
        }
      }
    }
  }

  private static void sensors(Random random, char[][] art, char[][] zones, int width, int height) {
    for(int i = 0; i < 2 + random.nextInt(3); i++) {
      int column = 4 + random.nextInt(Math.max(1, width - 8));
      for(int row = 0; row < height; row++) {
        if(zones[row][column] == ShipArt.HULL && (art[row][column] == '_' || art[row][column] == '-')) {
          if(row > 0 && art[row - 1][column] == ' ') {
            art[row - 1][column] = "'|^".charAt(random.nextInt(3));
            zones[row - 1][column] = ShipArt.HULL;
          }
          break;
        }
      }
    }
  }

  private static void greebles(Random random, char[][] art, char[][] zones, int width, int height) {
    for(int i = 0; i < 8 + random.nextInt(7); i++) {
      int row = random.nextInt(height);
      int column = random.nextInt(width);
      if(art[row][column] != ' ') {
        continue;
      }
      boolean touching = (column > 0 && art[row][column - 1] != ' ') || (column < width - 1 && art[row][column + 1] != ' ')
          || (row > 0 && art[row - 1][column] != ' ') || (row < height - 1 && art[row + 1][column] != ' ');
      if(touching) {
        art[row][column] = GREEBLE.charAt(random.nextInt(GREEBLE.length()));
        zones[row][column] = ShipArt.HULL;
      }
    }
  }

  private static List<String> join(char[][] grid) {
    List<String> lines = new ArrayList<>(grid.length);
    for(char[] row : grid) {
      lines.add(new String(row));
    }
    return lines;
  }
}
