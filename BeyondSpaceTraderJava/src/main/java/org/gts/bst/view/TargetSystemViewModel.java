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
 * Target system panel of the main window, already formatted.
 */
public record TargetSystemViewModel(
    boolean navigationVisible,
    String name,
    String size,
    String tech,
    String polSys,
    String resource,
    String police,
    String pirates,
    String distance,
    boolean outOfRangeVisible,
    boolean warpVisible,
    boolean trackVisible) {
}

