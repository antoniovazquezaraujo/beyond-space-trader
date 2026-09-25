package org.gts.bst.view;


/**
 * High scores screen. The presenter fills a {@link HighScoresViewModel}; the front-end
 * only renders it.
 */
public interface HighScoresView {
  void render(HighScoresViewModel model);
}
