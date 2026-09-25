package org.gts.bst.view;


/**
 * Read-only quests screen. The presenter fills a {@link QuestsViewModel}; the front-end
 * only renders it (and turns system names into links).
 */
public interface QuestsView {
  void render(QuestsViewModel model);
}
