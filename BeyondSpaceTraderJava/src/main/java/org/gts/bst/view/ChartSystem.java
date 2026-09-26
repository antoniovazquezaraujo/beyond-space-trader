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


/**
 * A star system as the charts need it: its position in the galaxy, its size (which
 * picks the marker glyph), a decorative colour and its state markers. A wormhole
 * system also carries the position of its pair, so the chart can draw the link.
 */
public record ChartSystem(
    int x,
    int y,
    String name,
    boolean visited,
    boolean wormhole,
    boolean warp,
    boolean tracked,
    boolean selected,
    ShipSize size,
    ChartColor color,
    int wormholeToX,
    int wormholeToY) {

  public ChartSystem(int x, int y, String name, boolean visited, boolean wormhole, boolean warp, boolean tracked) {
    this(x, y, name, visited, wormhole, warp, tracked, false, ShipSize.Medium, ChartColor.WHITE, -1, -1);
  }

  public ChartSystem(int x, int y, String name, boolean visited, boolean wormhole, boolean warp, boolean tracked,
      boolean selected, ShipSize size, ChartColor color) {
    this(x, y, name, visited, wormhole, warp, tracked, selected, size, color, -1, -1);
  }

  /** Whether this system has a wormhole and the position of its pair is known. */
  public boolean wormholeLinked() {
    return wormhole && wormholeToX >= 0 && wormholeToY >= 0;
  }
}
