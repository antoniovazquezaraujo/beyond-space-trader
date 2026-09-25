package org.gts.bst.view;


/**
 * Screens the encounter model asks for while resolving an encounter. The presenter
 * implements it by delegating to the view, so the model never sees a front-end type.
 */
public interface EncounterDialogHost {
  /**
   * The player must free cargo space before scooping: shows the jettison screen.
   */
  void showJettison();

  /**
   * Shows the plunder screen for a boarded ship.
   */
  void showPlunder();

  /**
   * Runs the purchase of cargo from the trader.
   */
  void buyTraderCargo(int tradeItem);

  /**
   * Runs the sale of cargo to the trader.
   */
  void sellTraderCargo(int tradeItem);
}
