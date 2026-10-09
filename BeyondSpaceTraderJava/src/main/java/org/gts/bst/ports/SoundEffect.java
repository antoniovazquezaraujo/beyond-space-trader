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
 * The sound effects the game asks for. The name says what happened in the game,
 * not how it sounds: the front-end chooses the timbre (see ADR 0006).
 */
public enum SoundEffect {
  /** A step in a list or the menu. */
  MENU_MOVE,
  /** An entry accepted (a menu, a toggle). */
  MENU_SELECT,
  /** A message to read (a dialog with a single button). */
  ALERT,
  /** A question waiting for an answer (a dialog with two buttons). */
  WARNING,
  /** A shot is born. */
  LASER,
  /** A shot lands on a ship. */
  HIT,
  /** A ship is destroyed. */
  EXPLOSION,
  /** The ship warps away. */
  WARP,
  /** The ship gets away from an encounter (the map warp keeps {@link #WARP}). */
  ESCAPE
}
