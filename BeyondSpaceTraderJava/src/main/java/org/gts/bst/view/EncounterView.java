package org.gts.bst.view;


/**
 * Encounter screen. The presenter renders it and drives the flow; the view forwards the
 * button commands and controls its own timer and closing.
 */
public interface EncounterView {
  void render(EncounterViewModel model);

  void close();

  void startTimer();

  void stopTimer();
}
