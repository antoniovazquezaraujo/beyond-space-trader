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
 * Information panel of the personnel screen for the selected crew member.
 */
public record PersonnelInfo(
    boolean visible,
    String name,
    boolean rateVisible,
    String rate,
    String pilot,
    String fighter,
    String trader,
    String engineer,
    boolean hireFireVisible,
    String hireFireText) {
}

