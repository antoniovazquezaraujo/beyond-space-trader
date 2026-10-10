/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;


/**
 * The channels of the mixer, each with its own volume: the effects, the ambient
 * loop of the screen, the music and the ship engines. The defaults leave
 * headroom; phase B will expose them to the options panel.
 */
public enum SoundChannel {
  EFFECTS,
  AMBIENCE,
  MUSIC,
  ENGINES
}
