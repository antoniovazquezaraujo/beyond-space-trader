/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.cargo;


/**
 * A quoted cargo purchase: the unit price is fixed when the offer is created (trader
 * prices are randomised), so applying it later cannot change the price.
 */
public record CargoBuyOffer(int tradeItem, CargoBuyOp op, int unitPrice, int maxAmount) {
}

