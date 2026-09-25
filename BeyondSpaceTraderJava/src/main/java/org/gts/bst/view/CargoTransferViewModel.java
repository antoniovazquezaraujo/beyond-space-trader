/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;


/**
 * Cargo transfer screen (jettison or plunder): the item quantities to show, already
 * formatted, and the cargo bays line.
 */
public record CargoTransferViewModel(List<String> quantities, String bays) {
}
