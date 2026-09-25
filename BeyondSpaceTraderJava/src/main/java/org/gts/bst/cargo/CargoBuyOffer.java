package org.gts.bst.cargo;


/**
 * A quoted cargo purchase: the unit price is fixed when the offer is created (trader
 * prices are randomised), so applying it later cannot change the price.
 */
public record CargoBuyOffer(int tradeItem, CargoBuyOp op, int unitPrice, int maxAmount) {
}
