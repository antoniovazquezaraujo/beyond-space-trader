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
 * Everything the commander status screen displays, already formatted for the front-end.
 */
public record CommanderViewModel(
    String name,
    String difficulty,
    String time,
    String pilot,
    String fighter,
    String trader,
    String engineer,
    String cash,
    String debt,
    String netWorth,
    String kills,
    String record,
    String reputation,
    Bounty bounty) {

  public record Bounty(boolean visible, String label, String amount) {
  }
}

