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
 * Information panel of the ship list for the selected ship. {@code imageIndex} refers to
 * the application ship image list.
 */
public record ShipInfoViewModel(
    String name,
    String size,
    String bays,
    String range,
    String hull,
    String weapon,
    String shield,
    String gadget,
    String crew,
    int imageIndex,
    ShipType type,
    ShipPicture picture) {
}

