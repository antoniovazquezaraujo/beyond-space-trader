package org.gts.bst.view;


/**
 * Current system panel of the main window, already formatted.
 */
public record SystemInfoViewModel(
    String name,
    String size,
    String tech,
    String polSys,
    String resource,
    String police,
    String pirates,
    String pressure,
    boolean pressurePreVisible,
    boolean newsVisible,
    boolean mercVisible,
    boolean specialVisible,
    String mercTooltip,
    String specialTooltip) {
}
