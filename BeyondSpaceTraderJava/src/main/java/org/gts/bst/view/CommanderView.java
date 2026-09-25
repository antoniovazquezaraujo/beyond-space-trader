package org.gts.bst.view;


/**
 * Read-only commander status screen. The presenter fills a {@link CommanderViewModel} and
 * the front-end only renders it.
 */
public interface CommanderView {
  void render(CommanderViewModel model);
}
