/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.VeryRareEncounter;


/**
 * The narrow view of the game the {@link EncounterGenerator} asks while it
 * decides the encounters of a trip: the trip state, the commander, the
 * destination, the options, the difficulty, the quest ships and the quest
 * states. It is read-only except for the encounter being generated, which the
 * generator fills in, and the list of pending very rare encounters, from which
 * it removes the ones it uses. Game implements it, so the generator never
 * holds a reference back to the game (ADR 0004/0005).
 */
public interface EncounterContext {
  /** The encounter of the current trip: its state and its rules. */
  Encounter encounter();
  Commander Commander();
  StarSystem WarpSystem();
  int getClicks();
  boolean getArrivedViaWormhole();
  GameOptions Options();
  Difficulty Difficulty();
  ArrayList<VeryRareEncounter> VeryRareEncounters();
  int getChanceOfVeryRareEncounter();
  int getChanceOfTradeInOrbit();
  Ship SpaceMonster();
  Ship Scorpion();
  Ship Scarab();
  Ship Dragonfly();
  int getQuestStatusSpaceMonster();
  int getQuestStatusScarab();
  int getQuestStatusPrincess();
  int getQuestStatusGemulon();
  int getQuestStatusDragonfly();
}
