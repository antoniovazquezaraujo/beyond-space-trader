/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.ports;


/**
 * The music moods of the game: a calm loop for navigation and trade, a tense
 * one for danger. Phase A only leaves the port and the mixer ready; the
 * encounters and the travel will ask for them in phase B.
 */
public enum MusicTheme {
  NONE,
  CALM,
  TENSE
}
