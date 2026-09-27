/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import spacetrader.Consts;
import spacetrader.ShipSpec;


/**
 * Draws a ship as pixels (to encode as braille): a symmetric hull profile, panel
 * gaps, windows, and the modules on top of it (engines, turrets, cargo pods,
 * cockpit, sensors) plus a fringe of greebles. Deterministic per type and variant.
 */
public final class ShipPixelArtGenerator {
  /** Cells (2x4 pixels each) per size class and hull heights. */
  private static final int[] WIDTH_CELLS = {11, 14, 18, 22, 25, 25};
  private static final int[] HEIGHT_CELLS = {4, 5, 5, 6, 6, 7};

  private ShipPixelArtGenerator() {
  }

  /** Tiny deterministic generator (xorshift), so the art never depends on a shared RNG. */
  private static final class Rng {
    private long state;

    private Rng(String seed) {
      state = seed.hashCode() * 0x9E3779B97F4A7C15L + 0x1234567L;
      if(state == 0) {
        state = 1;
      }
    }

    private long next() {
      state ^= state << 13;
      state ^= state >>> 7;
      state ^= state << 17;
      return state;
    }

    int nextInt(int bound) {
      return (int) Math.floorMod(next(), (long) Math.max(1, bound));
    }

    boolean nextBoolean() {
      return (next() & 1) == 0;
    }

    double nextDouble() {
      return (next() >>> 11) / (double) (1L << 53);
    }
  }

  public static ShipPixelArt of(ShipType type, int variant) {
    ShipSpec spec = Consts.ShipSpecs.get(type.CastToInt());
    return of(spec.getSize(), spec.CargoBays(), spec.getWeaponSlots(), type.name() + "#" + variant);
  }

  public static ShipPixelArt of(ShipSize size, int cargoBays, int weaponSlots, String seed) {
    int classIndex = Math.max(0, Math.min(WIDTH_CELLS.length - 1, size.CastToInt()));
    int cells = WIDTH_CELLS[classIndex];
    int rows = HEIGHT_CELLS[classIndex];
    int width = cells * 2;
    int height = rows * 4;
    boolean[][] pixels = new boolean[height][width];
    char[][] zones = new char[rows][cells];
    for(int row = 0; row < rows; row++) {
      for(int column = 0; column < cells; column++) {
        zones[row][column] = ' ';
      }
    }
    Rng random = new Rng(seed);
    int middle = height / 2;

    // --- silueta: media altura por columna (extremos afilados) y simetría vertical
    int[] half = new int[width];
    int level = Math.max(2, height / 4);
    for(int x = 0; x < width; x++) {
      double t = (double) x / (width - 1);
      double shape = Math.sin(Math.PI * Math.pow(t, 0.9)) * height * 0.30 + height * 0.06;
      if(random.nextDouble() < 0.08) {
        level = Math.max(height / 8, Math.min(height / 3, level + (random.nextBoolean() ? 1 : -1)));
      }
      int value = (int) Math.round(Math.min(shape, level));
      if(x < 5) {
        value = Math.max(1, x - 1);
      }
      if(x > width - 7) {
        value = Math.max(1, (width - 2 - x) / 2);
      }
      half[x] = Math.max(1, value);
    }
    for(int x = 0; x < width; x++) {
      for(int y = middle - half[x]; y <= middle + half[x]; y++) {
        if(y >= 0 && y < height) {
          pixels[y][x] = true;
          zones[y / 4][x / 2] = ShipArt.HULL;
        }
      }
    }

    // --- paneles: cortes verticales dentro del casco
    for(int x = 5; x < width - 5; x += 4 + random.nextInt(3)) {
      if(random.nextDouble() < 0.75) {
        for(int y = 1; y < height - 1; y++) {
          if(pixels[y][x] && pixels[y][x - 1] && pixels[y][x + 1]) {
            pixels[y][x] = false;
          }
        }
      }
    }
    // --- ventanas
    int windows = 3 + random.nextInt(4);
    for(int i = 0; i < windows; i++) {
      int x = 6 + random.nextInt(Math.max(1, width - 12));
      for(int y = 0; y < height; y++) {
        if(pixels[y][x] && y > 0 && pixels[y - 1][x] && pixels[y + 1 < height ? y + 1 : y][x]) {
          pixels[y][x] = false;
          break;
        }
      }
    }

    // --- motores: bloque de toberas en la cola
    int engineRows = Math.max(2, half[1]);
    for(int x = 0; x < Math.min(4, width / 4); x++) {
      for(int y = middle - engineRows; y <= middle + engineRows; y++) {
        if(y >= 0 && y < height) {
          pixels[y][x] = true;
          zones[y / 4][x / 2] = ShipArt.ENGINE;
        }
      }
    }

    // --- cabina: sobre el borde superior, cerca del morro
    int cockpit = width - 8 - random.nextInt(4);
    int cockpitTop = middle - half[cockpit];
    for(int dx = 0; dx < 4 && cockpit + dx < width; dx++) {
      for(int dy = 0; dy < 3; dy++) {
        int y = cockpitTop - 2 + dy;
        if(y >= 0) {
          pixels[y][cockpit + dx] = true;
          zones[y / 4][(cockpit + dx) / 2] = ShipArt.COCKPIT;
        }
      }
    }

    // --- bodegas: bloques dentro del casco
    int pods = Math.min(4, cargoBays / 5);
    for(int pod = 0; pod < pods; pod++) {
      int x = 8 + pod * Math.max(6, (width - 16) / Math.max(1, pods));
      if(x + 5 >= width - 3) {
        continue;
      }
      int y = middle + Math.max(0, half[x] - 4);
      for(int dx = 0; dx < 5; dx++) {
        for(int dy = 0; dy < 4; dy++) {
          if(y + dy < height) {
            pixels[y + dy][x + dx] = true;
            zones[(y + dy) / 4][(x + dx) / 2] = ShipArt.CARGO;
          }
        }
      }
    }

    // --- torretas: colgando del borde inferior
    int mounts = Math.min(3, weaponSlots);
    for(int mount = 0; mount < mounts; mount++) {
      int x = 6 + random.nextInt(Math.max(1, width - 14));
      int bottom = middle + half[x];
      if(bottom + 3 >= height) {
        continue;
      }
      for(int dx = 0; dx < 3; dx++) {
        for(int dy = 0; dy < 3; dy++) {
          pixels[bottom + dy][x + dx] = true;
          zones[(bottom + dy) / 4][(x + dx) / 2] = ShipArt.WEAPON;
        }
      }
      pixels[bottom + 3][x + 1] = true;
    }

    // --- antenas y fleco
    for(int i = 0; i < 2 + random.nextInt(3); i++) {
      int x = 5 + random.nextInt(Math.max(1, width - 10));
      int top = middle - half[x] - 1;
      if(top >= 2) {
        pixels[top][x] = true;
        pixels[top - 1][x] = true;
        zones[top / 4][x / 2] = ShipArt.HULL;
      }
    }
    for(int i = 0; i < 10 + random.nextInt(10); i++) {
      int x = random.nextInt(width);
      int y = random.nextInt(height);
      if(pixels[y][x]) {
        continue;
      }
      boolean touching = (x > 0 && pixels[y][x - 1]) || (x < width - 1 && pixels[y][x + 1])
          || (y > 0 && pixels[y - 1][x]) || (y < height - 1 && pixels[y + 1][x]);
      if(touching) {
        pixels[y][x] = true;
        zones[y / 4][x / 2] = ShipArt.HULL;
      }
    }
    return new ShipPixelArt(pixels, zones);
  }
}
