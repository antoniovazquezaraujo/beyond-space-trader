package org.gts.bst.view;

import java.util.List;


/**
 * Equipment screen lists: buy entries (for sale at the current system) and sell entries
 * (the ship equipment, with free slots), already formatted.
 */
public record EquipmentViewModel(
    List<String> buyWeapons,
    List<String> buyShields,
    List<String> buyGadgets,
    List<String> sellWeapons,
    List<String> sellShields,
    List<String> sellGadgets) {
}
