/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;
import java.util.Set;
import org.gts.bst.ship.ShipType;


/**
 * Everything the encounter screen displays, already formatted: the available actions,
 * the continuous-action indicator, the encounter image index, both ships and the texts.
 */
public record EncounterViewModel(
    Set<EncounterAction> actions,
    boolean continueVisible,
    int imageIndex,
    String youShip,
    Bar youHull,
    Bar youShield,
    String opponentShip,
    Bar opponentHull,
    Bar opponentShield,
    String encounterText,
    String actionText,
    ShipType youType,
    ShipType opponentType,
    boolean youHit,
    boolean oppHit,
    int youDamage,
    int oppDamage,
    ShipPicture youPicture,
    ShipPicture opponentPicture,
    boolean opponentDisabled,
    boolean youAttacked,
    boolean opponentIgnores,
    int opponentPilot,
    boolean commanderFleeing,
    int round,
    String speech,
    List<String> youPieces,
    List<String> opponentPieces,
    int youCargoBays,
    int opponentCargoBays) {
  /** A model with no legend of pieces (small scenes and tests). */
  public EncounterViewModel(
      Set<EncounterAction> actions,
      boolean continueVisible,
      int imageIndex,
      String youShip,
      Bar youHull,
      Bar youShield,
      String opponentShip,
      Bar opponentHull,
      Bar opponentShield,
      String encounterText,
      String actionText,
      ShipType youType,
      ShipType opponentType,
      boolean youHit,
      boolean oppHit,
      int youDamage,
      int oppDamage,
      ShipPicture youPicture,
      ShipPicture opponentPicture,
      boolean opponentDisabled,
      boolean youAttacked,
      boolean opponentIgnores,
      int opponentPilot,
      boolean commanderFleeing,
      int round,
      String speech) {
    this(actions, continueVisible, imageIndex, youShip, youHull, youShield, opponentShip, opponentHull,
        opponentShield, encounterText, actionText, youType, opponentType, youHit, oppHit, youDamage, oppDamage,
        youPicture, opponentPicture, opponentDisabled, youAttacked, opponentIgnores, opponentPilot,
        commanderFleeing, round, speech, List.of(), List.of(), 0, 0);
  }

  /** A bar of the scene: the value and its maximum. */
  public record Bar(int value, int max) {
  }
}

