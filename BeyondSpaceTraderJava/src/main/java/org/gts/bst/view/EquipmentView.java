package org.gts.bst.view;


/**
 * Equipment screen. The presenter renders the lists and the information panel and
 * mediates the buy/sell actions; the view only forwards selections and the buttons.
 */
public interface EquipmentView {
  void render(EquipmentViewModel model);

  void renderInfo(EquipmentInfoViewModel info);
}
