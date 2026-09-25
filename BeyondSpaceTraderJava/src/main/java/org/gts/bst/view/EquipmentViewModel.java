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
 * Equipment screen lists: buy entries (for sale at the current system) and sell entries
 * (the ship equipment, with free slots), already formatted.
 */
public record EquipmentViewModel(
    List<String> buyWeapons,
    List<String> buyShields,
    List<String> buyGadgets,
    List<String> sellWeapons,
    List<String> sellShields,
    List<String> sellGadgets) {
}

