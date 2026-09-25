package org.gts.bst.view;

import java.util.Set;


/**
 * Everything the encounter screen displays, already formatted: the available actions,
 * the continuous-action indicator, the encounter image index, both ships and the texts.
 */
public record EncounterViewModel(
    Set<EncounterAction> actions,
    boolean continueVisible,
    int imageIndex,
    String youShip,
    String youHull,
    String youShields,
    String opponentShip,
    String opponentHull,
    String opponentShields,
    String encounterText,
    String actionText) {
}
