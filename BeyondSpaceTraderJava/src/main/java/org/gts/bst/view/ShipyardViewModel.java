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
