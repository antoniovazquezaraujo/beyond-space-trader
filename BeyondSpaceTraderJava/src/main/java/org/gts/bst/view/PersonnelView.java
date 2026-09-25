package org.gts.bst.view;


/**
 * Personnel screen. The presenter renders the lists and the information panel and
 * mediates the hire/fire action; the view only forwards selections and the button.
 */
public interface PersonnelView {
  void render(PersonnelViewModel model);

  void renderInfo(PersonnelInfo info);
}
