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
 * A braille starfield with horizontal parallax: every star is a single dot of a
 * cell (2x4 per character), so it moves in halves of a cell; the far ones are
 * slow and dim, the near ones fast and bright. Deterministic with a seed.
 */
public final class Starfield {
  /** The braille bit of a dot inside the cell, by (column, row). */
  private static final int[][] DOT_BITS = {{0, 1, 2, 6}, {3, 4, 5, 7}};
  /** One star: its position in dots, its depth (0 far .. 1 near) and its speed. */
  public record Star(double x, double y, double depth, double speed) {
  }

  /** One frame: the characters and the grey shade (232-255) of every cell, or -1. */
  public record Frame(List<String> lines, int[][] shades) {
  }

  private final int dotWidth;
  private final int dotHeight;
  private final List<Star> stars = new ArrayList<>();
  private long state;

  public Starfield(int columns, int rows, double density, long seed) {
    dotWidth = columns * 2;
    dotHeight = rows * 4;
    state = seed * 0x9E3779B97F4A7C15L + 0x1234567L;
    int count = (int) (columns * rows * density);
    for(int i = 0; i < count; i++) {
      stars.add(newStar());
    }
  }

  public List<Star> stars() {
    return stars;
  }

  public int dotWidth() {
    return dotWidth;
  }

  public int dotHeight() {
    return dotHeight;
  }

  /** Moves every star to the left; the ones leaving come back from the right. */
  public void advance() {
    advance(false);
  }

  /**
   * Moves the sky: to the left when flying on, to the right when the ship goes
   * backwards (turning away or being chased), so the stars sell the retreat.
   */
  public void advance(boolean backwards) {
    for(int i = 0; i < stars.size(); i++) {
      Star star = stars.get(i);
      double x = star.x() + (backwards ? star.speed() : -star.speed());
      if(x < -1) {
        x += dotWidth + nextDouble() * dotWidth * 0.1;
        stars.set(i, new Star(x, nextInt(dotHeight), star.depth(), star.speed()));
      } else if(x > dotWidth + 1) {
        x -= dotWidth + nextDouble() * dotWidth * 0.1;
        stars.set(i, new Star(x, nextInt(dotHeight), star.depth(), star.speed()));
      } else {
        stars.set(i, new Star(x, star.y(), star.depth(), star.speed()));
      }
    }
  }

  /** Paints the stars as braille cells plus their grey shade. */
  public Frame frame(int columns, int rows) {
    int[][] masks = new int[rows][columns];
    double[][] depths = new double[rows][columns];
    for(int row = 0; row < rows; row++) {
      for(int column = 0; column < columns; column++) {
        depths[row][column] = -1;
      }
    }
    for(Star star : stars) {
      int x = (int) star.x();
      if(x < 0 || x >= columns * 2) {
        continue;
      }
      int cellColumn = x / 2;
      int cellRow = (int) star.y() / 4;
      if(cellRow < 0 || cellRow >= rows) {
        continue;
      }
      masks[cellRow][cellColumn] |= 1 << DOT_BITS[x % 2][(int) star.y() % 4];
      depths[cellRow][cellColumn] = Math.max(depths[cellRow][cellColumn], star.depth());
    }
    List<String> lines = new ArrayList<>(rows);
    for(int row = 0; row < rows; row++) {
      StringBuilder line = new StringBuilder(columns);
      for(int column = 0; column < columns; column++) {
        line.append(masks[row][column] == 0 ? ' ' : (char) (0x2800 + masks[row][column]));
      }
      lines.add(line.toString());
    }
    return new Frame(lines, shades(depths, masks));
  }

  private static int[][] shades(double[][] depths, int[][] masks) {
    int[][] shades = new int[depths.length][depths[0].length];
    for(int row = 0; row < depths.length; row++) {
      for(int column = 0; column < depths[0].length; column++) {
        shades[row][column] = masks[row][column] == 0 ? -1 : 232 + (int) (depths[row][column] * 23);
      }
    }
    return shades;
  }

  private Star newStar() {
    double depth = nextDouble();
    return new Star(nextDouble() * dotWidth, nextInt(dotHeight), depth, 0.35 + 3.4 * Math.pow(depth, 1.5));
  }

  private long next() {
    state ^= state << 13;
    state ^= state >>> 7;
    state ^= state << 17;
    return state;
  }

  private int nextInt(int bound) {
    return (int) Math.floorMod(next(), (long) Math.max(1, bound));
  }

  private double nextDouble() {
    return (next() >>> 11) / (double) (1L << 53);
  }
}
