/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;


/**
 * Shipyard designer screen. The presenter renders the design and mediates the
 * construct and save actions; the view only forwards the commands.
 */
public interface ShipyardView {
  void render(ShipyardDesignerViewModel model);

  void close();

  void showFileError(String fileName, String message);

  /**
   * Asks for the template file to save to; returns {@code null} when cancelled.
   */
  String askSaveTemplateFile();
}
