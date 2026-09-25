package org.gts.bst.view;


/**
 * Information panel of the personnel screen for the selected crew member.
 */
public record PersonnelInfo(
    boolean visible,
    String name,
    boolean rateVisible,
    String rate,
    String pilot,
    String fighter,
    String trader,
    String engineer,
    boolean hireFireVisible,
    String hireFireText) {
}
