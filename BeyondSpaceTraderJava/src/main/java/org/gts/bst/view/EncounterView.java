/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;


import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;


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

  /** The police inspecting the ship: the scanner, and the catwalk if they take cargo. */
  default void inspection(boolean confiscated) {
  }

  /** A catwalk between the ships: the cargo goes over it. */
  default void catwalk() {
  }
}

