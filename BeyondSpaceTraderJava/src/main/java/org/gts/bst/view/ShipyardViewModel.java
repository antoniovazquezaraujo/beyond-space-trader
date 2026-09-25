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
 * Shipyard panel of the main window (ships, equipment and escape pod), already formatted.
 */
public record ShipyardViewModel(
    String shipsForSale,
    boolean buyShipVisible,
    boolean designVisible,
    String equipForSale,
    boolean equipVisible,
    String escapePod,
    boolean podVisible) {
}

