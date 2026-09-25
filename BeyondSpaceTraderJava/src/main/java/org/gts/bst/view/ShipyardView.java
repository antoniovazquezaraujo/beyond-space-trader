package org.gts.bst.view;

import spacetrader.ShipTemplate;


/**
 * Shipyard designer screen. The presenter renders the model and mediates the commands;
 * the view only applies it to the widgets and performs the file dialogs and bitmap
 * handling, which are inherently front-end specific.
 */
public interface ShipyardView {
  void render(ShipyardDesignerViewModel model);

  void close();

  void showFileError(String fileName, String message);

  /**
   * Asks for the template file to save to; returns {@code null} when cancelled.
   */
  String askSaveTemplateFile();

  /**
   * Adopts the images of the selected template as the current custom images.
   */
  void adoptTemplateImages(ShipTemplate template);

  /**
   * Attaches the current custom images to a template that is about to be saved.
   */
  void applyCustomImages(ShipTemplate template);

  /**
   * Copies the current custom images to the application after building a custom ship.
   */
  void applyCustomShipImages();
}
