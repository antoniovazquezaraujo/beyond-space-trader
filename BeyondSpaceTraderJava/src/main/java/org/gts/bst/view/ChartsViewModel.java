package org.gts.bst.view;


/**
 * Chart controls of the main window (wormhole labels and jump/find buttons).
 */
public record ChartsViewModel(
    boolean wormholeVisible,
    String wormholeName,
    boolean jumpVisible,
    boolean findVisible) {
}
