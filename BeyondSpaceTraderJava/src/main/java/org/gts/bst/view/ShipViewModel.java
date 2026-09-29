/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import org.gts.bst.ship.ShipType;


/**
 * Everything the current-ship screen displays, already formatted for the front-end. The
 * equipment is rendered as two aligned text columns (labels and values).
 */
public record ShipViewModel(
    String type,
    String equipmentLabels,
    String equipmentValues,
    String specialCargo,
    ShipType typeId,
    ShipPicture picture) {
}

