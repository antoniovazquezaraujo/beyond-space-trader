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
 * picks the marker glyph), a decorative colour and its state markers.
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
    ChartColor color) {

  public ChartSystem(int x, int y, String name, boolean visited, boolean wormhole, boolean warp, boolean tracked) {
    this(x, y, name, visited, wormhole, warp, tracked, false, ShipSize.Medium, ChartColor.WHITE);
  }
}
