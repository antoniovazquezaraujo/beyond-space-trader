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
 * Equipment screen. The presenter renders the lists and the information panel and
 * mediates the buy/sell actions; the view only forwards selections and the buttons.
 */
public interface EquipmentView {
  void render(EquipmentViewModel model);

  void renderInfo(EquipmentInfoViewModel info);
}

