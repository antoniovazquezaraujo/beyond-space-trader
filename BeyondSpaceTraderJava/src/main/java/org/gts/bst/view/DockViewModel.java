package org.gts.bst.view;


/**
 * Dock panel of the main window (fuel and repairs), already formatted.
 */
public record DockViewModel(
    String fuelStatus,
    String fuelCost,
    boolean fuelButtonVisible,
    String hullStatus,
    String repairCost,
    boolean repairButtonVisible) {
}
