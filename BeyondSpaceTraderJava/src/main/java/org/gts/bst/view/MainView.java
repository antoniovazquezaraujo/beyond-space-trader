package org.gts.bst.view;

import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;


/**
 * Main window view. The presenter renders each area of the window through these methods;
 * the front-end only maps them to controls.
 */
public interface MainView {
  void renderStatusBar(MainStatusViewModel model);

  void renderSystemInfo(SystemInfoViewModel model);

  void renderCharts(ChartsViewModel model);

  void renderCargo(CargoViewModel model);

  void renderDock(DockViewModel model);

  void renderShipyard(ShipyardViewModel model);

  void renderTargetSystem(TargetSystemViewModel model);

  /**
   * Asks the player how much to spend on fuel; returns {@code null} when cancelled.
   */
  Integer askFuelAmount(int maxAmount);

  /**
   * Asks the player how much to spend on repairs; returns {@code null} when cancelled.
   */
  Integer askRepairsAmount(int maxAmount);

  /**
   * Asks the player how much cargo to buy; returns {@code null} when cancelled.
   */
  Integer askCargoBuyQuantity(CargoBuyOffer offer);

  /**
   * Asks the player how much cargo to sell; returns {@code null} when cancelled.
   */
  Integer askCargoSellQuantity(CargoSellOffer offer);
}
