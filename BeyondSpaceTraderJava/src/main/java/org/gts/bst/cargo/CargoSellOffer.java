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
 * A quoted cargo sale. {@code price} is the unit price (negative when dumping costs
 * money), fixed when the offer is created.
 */
public record CargoSellOffer(int tradeItem, CargoSellOp op, int price, int maxAmount) {
}

