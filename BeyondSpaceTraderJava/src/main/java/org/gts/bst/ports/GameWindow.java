/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.ports;

import org.gts.bst.events.EncounterResult;


/**
 * What the model needs from the application window: the encounter screen and the
 * refreshes. The Lanterna main window implements it.
 */
public interface GameWindow {
  EncounterResult showEncounter();

  void showNewspaper();

  void UpdateStatusBar();

  void UpdateAll();
}
