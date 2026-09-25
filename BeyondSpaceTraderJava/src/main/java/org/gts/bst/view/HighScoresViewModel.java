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
 * High scores screen: three rows, possibly empty. Rows that are not {@code filled} are
 * left with the front-end defaults.
 */
public record HighScoresViewModel(List<Row> rows) {
  public record Row(boolean filled, String name, String score, String status) {
  }
}

