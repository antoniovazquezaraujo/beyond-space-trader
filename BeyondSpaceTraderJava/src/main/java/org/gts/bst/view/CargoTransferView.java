/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;


/**
 * Cargo transfer screen. The presenter renders the quantities and mediates the
 * transfer; the view only forwards the row commands and asks for the amounts.
 */
public interface CargoTransferView {
  void render(CargoTransferViewModel model);

  /**
   * Asks how much of the jettison offer to discard; returns {@code null} when cancelled.
   */
  Integer askSellQuantity(CargoSellOffer offer);

  /**
   * Asks how much of the plunder offer to take; returns {@code null} when cancelled.
   */
  Integer askBuyQuantity(CargoBuyOffer offer);
}
