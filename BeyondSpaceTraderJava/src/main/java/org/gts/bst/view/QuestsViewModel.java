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
 * Text of the quests screen, already formatted. {@code hasQuests} tells the front-end
 * whether the text describes open quests (and can therefore contain system links).
 */
public record QuestsViewModel(String text, boolean hasQuests) {
}

