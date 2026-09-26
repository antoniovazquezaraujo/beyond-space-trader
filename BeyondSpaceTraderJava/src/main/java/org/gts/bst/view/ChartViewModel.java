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
    int maxRange,
    int galaxyWidth,
    int galaxyHeight,
    String trackedRangeText) {

  /**
   * The galactic chart: the viewport is centred on {@code viewX/viewY} (the cursor
   * or the current system) and the fuel range is measured from the current system.
   */
  public static ChartViewModel galactic(List<ChartSystem> systems, int currentX, int currentY,
      int viewX, int viewY, int fuel, int galaxyWidth, int galaxyHeight) {
    return new ChartViewModel(ChartType.GALACTIC, systems, currentX, currentY, viewX, viewY,
        fuel, 0, galaxyWidth, galaxyHeight, null);
  }

  /**
   * The short-range chart: it is always centred on the current system.
   */
  public static ChartViewModel shortRange(List<ChartSystem> systems, int currentX, int currentY,
      int fuel, int maxRange, String trackedRangeText) {
    return new ChartViewModel(ChartType.SHORT_RANGE, systems, currentX, currentY, currentX, currentY,
        fuel, maxRange, 0, 0, trackedRangeText);
  }
}
