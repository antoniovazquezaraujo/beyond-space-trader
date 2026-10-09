/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.testing;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.ports.AmbienceKey;
import org.gts.bst.ports.MusicTheme;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.WeaponType;


/**
 * A sound service that records what the game asks for, so the tests can assert
 * what would have sounded without opening any audio device.
 */
public final class TestSoundService implements SoundService {
  private final List<SoundEffect> effects = new ArrayList<>();
  private final List<AmbienceKey> ambiences = new ArrayList<>();
  private final List<WeaponType> weapons = new ArrayList<>();
  private final List<MusicTheme> music = new ArrayList<>();

  @Override
  public void play(SoundEffect effect) {
    effects.add(effect);
  }

  @Override
  public void playWeapon(WeaponType type) {
    weapons.add(type);
  }

  @Override
  public void ambience(AmbienceKey key) {
    ambiences.add(key);
  }

  @Override
  public void music(MusicTheme theme) {
    music.add(theme);
  }

  /** The effects played so far, in order. */
  public List<SoundEffect> played() {
    return effects;
  }

  /** The ambiences asked for so far, in order. */
  public List<AmbienceKey> ambiences() {
    return ambiences;
  }

  /** The weapons fired so far, in order (phase B). */
  public List<WeaponType> weapons() {
    return weapons;
  }

  /** The music moods asked for so far, in order (phase B). */
  public List<MusicTheme> musicThemes() {
    return music;
  }

  /** The last ambience asked for, or null when none was. */
  public AmbienceKey lastAmbience() {
    return ambiences.isEmpty() ? null : ambiences.get(ambiences.size() - 1);
  }

  /** Forgets what was played (a test that checks one event at a time). */
  public void clear() {
    effects.clear();
    ambiences.clear();
    weapons.clear();
    music.clear();
  }

  // The engine loops are not recorded: nothing in phase A asks for them.
  @Override
  public void engine(ShipType type, boolean player) {
  }

  @Override
  public void engineStop(boolean player) {
  }
}
