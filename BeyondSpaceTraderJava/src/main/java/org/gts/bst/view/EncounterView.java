package org.gts.bst.view;


import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;


/**
 * Encounter screen. The presenter renders it and drives the flow; the view forwards the
 * button commands, performs the screens the model asks for and controls its own timer
 * and closing.
 */
public interface EncounterView {
  void render(EncounterViewModel model);

  void close();

  void startTimer();

  void stopTimer();

  void showJettison();

  void showPlunder();

  Integer askCargoBuyQuantity(CargoBuyOffer offer);

  Integer askCargoSellQuantity(CargoSellOffer offer);
}
