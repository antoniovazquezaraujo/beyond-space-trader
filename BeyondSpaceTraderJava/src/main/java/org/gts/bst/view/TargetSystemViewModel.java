package org.gts.bst.view;


/**
 * Target system panel of the main window, already formatted.
 */
public record TargetSystemViewModel(
    boolean navigationVisible,
    String name,
    String size,
    String tech,
    String polSys,
    String resource,
    String police,
    String pirates,
    String distance,
    boolean outOfRangeVisible,
    boolean warpVisible,
    boolean trackVisible) {
}
