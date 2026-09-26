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
 * The character grid a chart is drawn on. The views implement it over their own
 * surface (Lanterna, a test buffer...), the renderer only writes characters and
 * colours. Coordinates are relative to the chart area.
 */
public interface ChartCanvas {
  int width();

  int height();

  /**
   * Writes a character; out-of-range coordinates are the implementation's problem,
   * the renderer never writes outside the canvas.
   */
  void put(int x, int y, char character, ChartColor color);
}
