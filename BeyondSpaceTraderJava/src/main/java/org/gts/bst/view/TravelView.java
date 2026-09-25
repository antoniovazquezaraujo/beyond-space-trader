package org.gts.bst.view;

import org.gts.bst.events.EncounterResult;


/**
 * The screen the travel flow needs: shows the encounter and returns its result. The
 * model asks for it through this port instead of opening the form itself.
 */
public interface TravelView {
  EncounterResult showEncounter();
}
