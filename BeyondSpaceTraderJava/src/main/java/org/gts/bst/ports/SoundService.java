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
 * Plays the sound effects of the game. The front-ends provide an implementation
 * (the synthesiser of {@code org.gts.bst.audio}, or a recorder in the tests); the
 * {@link #NONE} constant is the no-op service for headless runs and for the player
 * who turned the sound off.
 *
 * <p>The service is a port in the sense of ADR 0005: it lives in a neutral package
 * and nobody in {@code spacetrader.*} needs to depend on an audio technology. The
 * calls are fire-and-forget and never block the caller.
 */
@FunctionalInterface
public interface SoundService {
  /**
   * Plays an effect. Implementations may queue it and return at once; with a
   * disabled service (see {@link #NONE}) this does nothing.
   *
   * @param effect the effect to play, never null
   */
  void play(SoundEffect effect);

  /**
   * The laser of the rival ship, with a lower tone when its pilot is weak (an
   * easy opponent). The default ignores the variant and plays {@link
   * SoundEffect#LASER}, so implementations that do not model tones keep working.
   *
   * @param easy true when the rival is an easy opponent
   */
  default void playRivalLaser(boolean easy) {
    play(SoundEffect.LASER);
  }

  /** A service that plays nothing (tests, headless runs and the mute option). */
  SoundService NONE = effect -> {
  };
}
