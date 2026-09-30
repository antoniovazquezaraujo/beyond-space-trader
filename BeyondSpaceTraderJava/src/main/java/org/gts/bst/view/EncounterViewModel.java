/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

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
    int round) {
  /** A bar of the scene: the value and its maximum. */
  public record Bar(int value, int max) {
  }
}

