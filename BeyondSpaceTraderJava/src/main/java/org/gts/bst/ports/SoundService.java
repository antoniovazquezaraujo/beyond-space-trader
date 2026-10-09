/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.ports;

import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.WeaponType;


/**
 * Plays the sounds of the game. The front-ends provide an implementation (the
 * sample engine of {@code org.gts.bst.audio}, or a recorder in the tests); the
 * {@link #NONE} constant is the no-op service for headless runs and for the
 * player who turned the sound off.
 *
 * <p>Only {@link #play} is abstract: the rest are optional capabilities with a
 * silent default, so {@code NONE} and the test fakes keep compiling as the port
 * grows (phase A adds the weapons, the engines, the ambience and the music, see
 * ADR 0007).
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

  /**
   * Fires a weapon: the sample of {@code combat/laser-<weapon>}. Phase B will
   * call it from the combat; for now it only has to exist and stay silent by
   * default.
   *
   * @param type the weapon that fired
   */
  default void playWeapon(WeaponType type) {
  }

  /**
   * Starts the engine loop of a ship ({@code ships/<shiptype>}); calling it again
   * replaces the loop of the same side, and the previous one fades out.
   *
   * @param type the ship whose engine starts
   * @param player true for the player ship, false for the rival
   */
  default void engine(ShipType type, boolean player) {
  }

  /** Stops the engine loop of a ship (fades it out). */
  default void engineStop(boolean player) {
  }

  /**
   * Changes the background ambience of the screen ({@code ambient/<key>}); the
   * same key is ignored, so calling it on every key press is cheap.
   *
   * @param key the ambience of the screen that is opening
   */
  default void ambience(AmbienceKey key) {
  }

  /**
   * Changes the music mood ({@code music/<theme>}); {@link MusicTheme#NONE}
   * stops it. Phase A only leaves the port ready.
   *
   * @param theme the mood to play, or none to stop
   */
  default void music(MusicTheme theme) {
  }

  /** A service that plays nothing (tests, headless runs and the mute option). */
  SoundService NONE = effect -> {
  };
}
