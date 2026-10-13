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
 * The conditional alarms of an encounter: the loops that sound while the fight
 * is on (see ADR 0007). They can play together: the attack alarm starts with
 * the first shot of the rival and the danger alarm with the first hit on the
 * player ship.
 */
public enum Alarm {
  /** The rival fired for the first time. */
  ATTACK,
  /** The player ship took its first hit. */
  DANGER
}
