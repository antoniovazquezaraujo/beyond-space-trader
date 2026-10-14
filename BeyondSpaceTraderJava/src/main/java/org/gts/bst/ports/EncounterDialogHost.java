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
 * Screens the encounter model asks for while resolving an encounter. The presenter
 * implements it by delegating to the view, so the model never sees a front-end type.
 */
public interface EncounterDialogHost {
  /**
   * The player must free cargo space before scooping: shows the jettison screen.
   */
  void showJettison();

  /**
   * Shows the plunder screen for a boarded ship.
   */
  void showPlunder();

  /**
   * Runs the purchase of cargo from the trader.
   */
  void buyTraderCargo(int tradeItem);

  /**
   * Runs the sale of cargo to the trader.
   */
  void sellTraderCargo(int tradeItem);
}

