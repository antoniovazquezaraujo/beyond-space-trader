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
 * Information panel of the equipment screen for the selected item. {@code imageIndex}
 * refers to the application equipment image list (null when nothing is selected).
 */
public record EquipmentInfoViewModel(
    boolean visible,
    String name,
    String type,
    String description,
    String buyPrice,
    String sellPrice,
    String power,
    String charge,
    Integer imageIndex,
    boolean buyVisible,
    boolean sellVisible) {
}

