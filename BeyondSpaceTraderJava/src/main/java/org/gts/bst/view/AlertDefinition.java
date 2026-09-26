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
 * Title, message, buttons and results of a predefined alert, with no front-end
 * types: each front-end decides how to show it.
 */
public record AlertDefinition(
    String title,
    String message,
    String button1,
    DialogResult result1,
    String button2,
    DialogResult result2) {
}
