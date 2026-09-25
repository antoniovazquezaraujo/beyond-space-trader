package org.gts.bst.view;


/**
 * Read-only current-ship screen. The presenter fills a {@link ShipViewModel} and the
 * front-end only renders it.
 */
public interface ShipView {
  void render(ShipViewModel model);
}
