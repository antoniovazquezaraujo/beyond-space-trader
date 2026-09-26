/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.Arrays;


/**
 * In-memory canvas for the chart tests.
 */
class TestChartCanvas implements ChartCanvas {
  private final int width;
  private final int height;
  private final char[][] characters;
  private final ChartColor[][] colors;
  private final boolean[][] inverted;

  TestChartCanvas(int width, int height) {
    this.width = width;
    this.height = height;
    this.characters = new char[height][width];
    this.colors = new ChartColor[height][width];
    this.inverted = new boolean[height][width];
    for(char[] row : characters) {
      Arrays.fill(row, ' ');
    }
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void put(int x, int y, char character, ChartColor color) {
    characters[y][x] = character;
    colors[y][x] = color;
    inverted[y][x] = false;
  }

  @Override
  public void putInverted(int x, int y, char character, ChartColor color) {
    characters[y][x] = character;
    colors[y][x] = color;
    inverted[y][x] = true;
  }

  char at(int x, int y) {
    return characters[y][x];
  }

  ChartColor colorAt(int x, int y) {
    return colors[y][x];
  }

  boolean invertedAt(int x, int y) {
    return inverted[y][x];
  }

  String line(int y) {
    return new String(characters[y]);
  }
}
