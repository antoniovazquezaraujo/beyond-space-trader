package org.gts.bst.view;


/**
 * Information panel of the equipment screen for the selected item. {@code imageIndex}
 * refers to the application equipment image list (null when nothing is selected).
 */
public record EquipmentInfoViewModel(
    boolean visible,
    String name,
    String type,
    String description,
    String buyPrice,
    String sellPrice,
    String power,
    String charge,
    Integer imageIndex,
    boolean buyVisible,
    boolean sellVisible) {
}
