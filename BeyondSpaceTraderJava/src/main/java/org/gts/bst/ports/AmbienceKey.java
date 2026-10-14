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
 * The places of the game that can have their own background ambience: the
 * front-end asks for one when the screen changes and the mixer crossfades to the
 * sample of {@code ambient/<key>.wav} (all the samples are optional, see ADR
 * 0007).
 */
public enum AmbienceKey {
  TITLE,
  NAVIGATION,
  TRADE,
  BANK,
  QUESTS,
  PERSONNEL,
  COMMANDER,
  SHIP,
  SHIPLIST,
  EQUIPMENT,
  OPTIONS,
  HIGHSCORES,
  DESIGNER,
  NEWS,
  ABOUT,
  MENU
}
