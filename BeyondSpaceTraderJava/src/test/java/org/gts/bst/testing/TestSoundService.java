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
import org.gts.bst.ports.Alarm;
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
  /** A ship engine starting: its type and the side. */
  public record Engine(ShipType type, boolean player) {
  }

  /** An alarm loop starting or stopping. */
  public record AlarmCall(Alarm alarm, boolean on) {
  }

  private final List<SoundEffect> effects = new ArrayList<>();
  private final List<AmbienceKey> ambiences = new ArrayList<>();
  private final List<WeaponType> weapons = new ArrayList<>();
  private final List<MusicTheme> music = new ArrayList<>();
  private final List<Engine> engines = new ArrayList<>();
  private final List<Boolean> engineStops = new ArrayList<>();
  private final List<AlarmCall> alarms = new ArrayList<>();
  private final List<Boolean> repairLoops = new ArrayList<>();

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

  /** The engines started so far, in order. */
  public List<Engine> engines() {
    return engines;
  }

  /** The sides whose engines were stopped, in order. */
  public List<Boolean> engineStops() {
    return engineStops;
  }

  /** The alarms started or stopped so far, in order. */
  public List<AlarmCall> alarms() {
    return alarms;
  }

  /** The workshop loop of the repair scene started or stopped, in order. */
  public List<Boolean> repairLoops() {
    return repairLoops;
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
    engines.clear();
    engineStops.clear();
    alarms.clear();
    repairLoops.clear();
  }

  @Override
  public void engine(ShipType type, boolean player) {
    engines.add(new Engine(type, player));
  }

  @Override
  public void engineStop(boolean player) {
    engineStops.add(player);
  }

  @Override
  public void alarm(Alarm alarm, boolean on) {
    alarms.add(new AlarmCall(alarm, on));
  }

  @Override
  public void repairLoop(boolean on) {
    repairLoops.add(on);
  }
}
