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
 * A ship drawn as a grid of pixels (2 columns x 4 rows per character cell) plus
 * the zone of every cell. It encodes to braille (U+2800 block), which gives eight
 * dots per character, so the art can be much finer than plain characters.
 */
public final class ShipPixelArt {
  /** Braille dots: bit per (column, row) inside a cell. */
  private static final int[][] BITS = {{0x01, 0x02, 0x04, 0x40}, {0x08, 0x10, 0x20, 0x80}};
  /** Block elements for the 16 quadrant combinations (TL, TR, BL, BR bits). */
  private static final char[] BLOCK_QUADRANTS = {
      ' ', '\u2598', '\u259D', '\u2580', '\u2596', '\u258C', '\u259E', '\u259B',
      '\u2597', '\u259A', '\u2590', '\u259C', '\u2584', '\u2599', '\u259F', '\u2588'};
  private final boolean[][] pixels;
  private final char[][] zones;

  ShipPixelArt(boolean[][] pixels, char[][] zones) {
    this.pixels = pixels;
    this.zones = zones;
  }

  public int cellWidth() {
    return pixels[0].length / 2;
  }

  public int cellHeight() {
    return pixels.length / 4;
  }

  boolean pixel(int row, int column) {
    return pixels[row][column];
  }

  /** The same ship looking the other way (the pixels and the zones flip). */
  public ShipPixelArt mirrored() {
    boolean[][] mirroredPixels = new boolean[pixels.length][pixels[0].length];
    char[][] mirroredZones = new char[zones.length][zones[0].length];
    for(int row = 0; row < pixels.length; row++) {
      for(int column = 0; column < pixels[0].length; column++) {
        mirroredPixels[row][pixels[0].length - 1 - column] = pixels[row][column];
      }
    }
    for(int row = 0; row < zones.length; row++) {
      for(int column = 0; column < zones[0].length; column++) {
        mirroredZones[row][zones[0].length - 1 - column] = zones[row][column];
      }
    }
    return new ShipPixelArt(mirroredPixels, mirroredZones);
  }

  /** Encodes the pixels as block elements: 2x2 quadrants per cell, solid shapes. */
  public ShipArt toBlockArt() {
    List<String> lines = new ArrayList<>(cellHeight());
    List<String> zoneLines = new ArrayList<>(cellHeight());
    for(int cellRow = 0; cellRow < cellHeight(); cellRow++) {
      StringBuilder line = new StringBuilder(cellWidth());
      for(int cellColumn = 0; cellColumn < cellWidth(); cellColumn++) {
        int quadrants = 0;
        for(int quadrant = 0; quadrant < 4; quadrant++) {
          int x = cellColumn * 2 + quadrant % 2;
          int y = cellRow * 4 + quadrant / 2 * 2;
          if(pixels[y][x] && pixels[y + 1][x]) {
            quadrants |= 1 << quadrant;
          }
        }
        line.append(BLOCK_QUADRANTS[quadrants]);
      }
      lines.add(line.toString());
      zoneLines.add(new String(zones[cellRow]));
    }
    return new ShipArt(lines, zoneLines);
  }

  /** Encodes the pixels as braille characters, one per 2x4 block. */
  public ShipArt toShipArt() {
    List<String> lines = new ArrayList<>(cellHeight());
    List<String> zoneLines = new ArrayList<>(cellHeight());
    for(int cellRow = 0; cellRow < cellHeight(); cellRow++) {
      StringBuilder line = new StringBuilder(cellWidth());
      for(int cellColumn = 0; cellColumn < cellWidth(); cellColumn++) {
        int dots = 0;
        for(int x = 0; x < 2; x++) {
          for(int y = 0; y < 4; y++) {
            if(pixels[cellRow * 4 + y][cellColumn * 2 + x]) {
              dots |= BITS[x][y];
            }
          }
        }
        line.append(dots == 0 ? ' ' : (char) (0x2800 + dots));
      }
      lines.add(line.toString());
      zoneLines.add(new String(zones[cellRow]));
    }
    return new ShipArt(lines, zoneLines);
  }
}
