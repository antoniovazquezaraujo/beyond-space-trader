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
 * A chart view: a canvas that knows how to draw a {@link ChartViewModel}.
 */
public interface ChartView extends ChartCanvas {
  default void render(ChartViewModel model) {
    ChartRenderer.render(this, model);
  }
}
