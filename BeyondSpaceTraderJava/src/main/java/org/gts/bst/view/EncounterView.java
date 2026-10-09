/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;


import java.util.function.Supplier;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.ports.MusicTheme;
import org.gts.bst.ship.equip.WeaponType;


/**
 * Encounter screen. The presenter renders it and drives the flow; the view forwards the
 * button commands, performs the screens the model asks for and controls its own timer
 * and closing.
 */
public interface EncounterView {
  void render(EncounterViewModel model);

  void close();

  void startTimer();

  void stopTimer();

  void showJettison();

  void showPlunder();

  Integer askCargoBuyQuantity(CargoBuyOffer offer);

  Integer askCargoSellQuantity(CargoSellOffer offer);

  /** One more line of the log of the scene, to read while the scene waits. */
  default void log(String line) {
  }

  /**
   * A quiet alert of the game that the other ship says aloud, as a bubble under it.
   * It is feedback of an action: the next part, round or action takes it away.
   */
  default void speech(String line) {
  }

  /**
   * Like {@link #speech(String)}, but the scene stays waiting for the player to read
   * it (the encounter closes when they leave). For the alerts that end the encounter.
   */
  default void speechAndWait(String line) {
  }

  /**
   * You got away while fleeing: the camera follows you, so you stay in the
   * scene and the other ship (behind you) is the one that leaves it.
   */
  default void escaped() {
  }

  /** The police inspecting the ship: the scanner, and the catwalk if they take cargo. */
  default void inspection(boolean confiscated) {
  }

  /** The pirates looting the ship: the catwalk and the boxes if they take cargo. */
  default void looted(boolean cargo) {
  }

  /**
   * The tension of the encounter, re-evaluated on every part: the view crossfades
   * the music when it changes (see ADR 0007).
   */
  default void music(MusicTheme theme) {
  }

  /**
   * The strongest weapon of each ship, so the scene can sound the shot with the
   * right sample. The suppliers are read when a beam is born, not now.
   */
  default void weapons(Supplier<WeaponType> you, Supplier<WeaponType> opponent) {
  }

}

