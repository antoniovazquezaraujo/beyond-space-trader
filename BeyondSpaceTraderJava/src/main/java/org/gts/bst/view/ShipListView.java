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
 * Ship list screen. The presenter renders the rows and the information panel and mediates
 * the purchase; the view only forwards the selected index.
 */
public interface ShipListView {
  void render(ShipListViewModel model);

  void renderInfo(ShipInfoViewModel info);
}

