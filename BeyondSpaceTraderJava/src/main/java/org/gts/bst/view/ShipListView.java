package org.gts.bst.view;


/**
 * Ship list screen. The presenter renders the rows and the information panel and mediates
 * the purchase; the view only forwards the selected index.
 */
public interface ShipListView {
  void render(ShipListViewModel model);

  void renderInfo(ShipInfoViewModel info);
}
