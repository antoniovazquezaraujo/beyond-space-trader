package org.gts.bst.view;


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
}
