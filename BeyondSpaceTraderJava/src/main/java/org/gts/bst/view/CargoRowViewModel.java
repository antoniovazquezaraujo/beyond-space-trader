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
 * One row of the cargo table, already formatted. {@code sellBold} and {@code buyBold}
 * tell the front-end whether to highlight those prices (profitable trade).
 */
public record CargoRowViewModel(
    String sellPrice,
    String sellQty,
    String sellButtonText,
    boolean sellVisible,
    String buyPrice,
    String buyQty,
    boolean buyVisible,
    String targetPrice,
    String targetDiff,
    String targetPct,
    boolean sellBold,
    boolean buyBold,
    String targetSellPrice,
    String targetBuyPrice) {

  public static CargoRowViewModel empty() {
    return new CargoRowViewModel("", "", "", false, "", "", false, "", "", "", false, false, "", "");
  }
}

