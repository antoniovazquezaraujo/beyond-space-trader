/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;


/**
 * Everything a chart needs to draw itself, with no game types involved.
 */
public record ChartViewModel(
    ChartType type,
    List<ChartSystem> systems,
    int currentX,
    int currentY,
    int viewX,
    int viewY,
    int fuel,
    int galaxyWidth,
    int galaxyHeight,
    String trackedRangeText,
    int galacticColumns) {

  /**
   * The galactic chart: the whole galaxy is scaled down to fit the chart area, so it
   * always shows every system and never scrolls.
   */
  public static ChartViewModel galactic(List<ChartSystem> systems, int currentX, int currentY,
      int fuel, int galaxyWidth, int galaxyHeight, int columns) {
    return new ChartViewModel(ChartType.GALACTIC, systems, currentX, currentY, 0, 0,
        fuel, galaxyWidth, galaxyHeight, null, columns);
  }

  /**
   * The short-range chart: a 1:1 map (one sector per character) whose viewport starts
   * at {@code viewX/viewY}.
   */
  public static ChartViewModel shortRange(List<ChartSystem> systems, int currentX, int currentY,
      int viewX, int viewY, int fuel, String trackedRangeText) {
    return new ChartViewModel(ChartType.SHORT_RANGE, systems, currentX, currentY, viewX, viewY,
        fuel, 0, 0, trackedRangeText, 1);
  }
}
