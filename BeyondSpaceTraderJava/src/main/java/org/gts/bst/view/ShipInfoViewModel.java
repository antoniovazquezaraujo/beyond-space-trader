package org.gts.bst.view;


/**
 * Information panel of the ship list for the selected ship. {@code imageIndex} refers to
 * the application ship image list.
 */
public record ShipInfoViewModel(
    String name,
    String size,
    String bays,
    String range,
    String hull,
    String weapon,
    String shield,
    String gadget,
    String crew,
    int imageIndex) {
}
