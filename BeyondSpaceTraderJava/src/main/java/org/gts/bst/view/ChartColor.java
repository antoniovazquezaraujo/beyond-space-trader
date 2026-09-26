/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;


/**
 * The colours of the charts: the ANSI hues plus their dim variants. Star systems use
 * a decorative bright hue and fall back to the dim one once they have been visited;
 * the state decorations (the cross, the parentheses and the wormhole mark) use fixed
 * hues. The canvases map each name to a real terminal colour.
 */
public enum ChartColor {
  DEFAULT,
  WHITE,
  WHITE_DIM,
  CYAN,
  CYAN_DIM,
  GREEN,
  GREEN_DIM,
  YELLOW,
  YELLOW_DIM,
  MAGENTA,
  MAGENTA_DIM,
  RED,
  RED_DIM,
  BLUE,
  BLUE_DIM;

  /** The decorative hues a star system can use. */
  private static final ChartColor[] STAR_COLORS = {
      WHITE, CYAN, GREEN, YELLOW, MAGENTA, RED, BLUE
  };

  /**
   * A stable decorative colour for a system: bright while it has not been visited,
   * dim afterwards.
   */
  public static ChartColor starColor(int systemId, boolean visited) {
    ChartColor color = STAR_COLORS[Math.floorMod(systemId, STAR_COLORS.length)];
    return visited ? dim(color) : color;
  }

  private static ChartColor dim(ChartColor color) {
    switch(color) {
      case WHITE:
        return WHITE_DIM;
      case CYAN:
        return CYAN_DIM;
      case GREEN:
        return GREEN_DIM;
      case YELLOW:
        return YELLOW_DIM;
      case MAGENTA:
        return MAGENTA_DIM;
      case RED:
        return RED_DIM;
      case BLUE:
        return BLUE_DIM;
      default:
        return color;
    }
  }
}
