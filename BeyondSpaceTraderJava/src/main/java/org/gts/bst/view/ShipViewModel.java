package org.gts.bst.view;


/**
 * Everything the current-ship screen displays, already formatted for the front-end. The
 * equipment is rendered as two aligned text columns (labels and values).
 */
public record ShipViewModel(String type, String equipmentLabels, String equipmentValues, String specialCargo) {
}
