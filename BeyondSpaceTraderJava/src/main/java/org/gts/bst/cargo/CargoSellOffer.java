package org.gts.bst.cargo;


/**
 * A quoted cargo sale. {@code price} is the unit price (negative when dumping costs
 * money), fixed when the offer is created.
 */
public record CargoSellOffer(int tradeItem, CargoSellOp op, int price, int maxAmount) {
}
