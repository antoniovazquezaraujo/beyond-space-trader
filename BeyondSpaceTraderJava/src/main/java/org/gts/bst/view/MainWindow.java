package org.gts.bst.view;


/**
 * The application main window, as seen by the screens that need to refresh it after a
 * change. Keeps the model out of the navigation glue.
 */
public interface MainWindow {
  void refresh();
}
