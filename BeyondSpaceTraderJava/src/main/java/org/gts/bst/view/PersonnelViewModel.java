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
 * Everything the personnel screen displays, already formatted for the front-end.
 */
public record PersonnelViewModel(
    List<String> crewEntries,
    boolean crewVisible,
    String crewEmptyText,
    List<String> forHireEntries,
    boolean forHireVisible,
    String forHireEmptyText,
    PersonnelInfo info) {
}

