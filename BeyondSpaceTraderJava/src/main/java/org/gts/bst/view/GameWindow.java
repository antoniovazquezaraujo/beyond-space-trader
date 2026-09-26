/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import jwinforms.ImageList;
import jwinforms.WfImage;
import org.gts.bst.events.EncounterResult;


/**
 * What the model needs from the application window: the encounter screen, the
 * refreshes and the sprite images. The Swing front-end implements it today; the
 * Lanterna one returns no images because the text UI does not draw sprites.
 */
public interface GameWindow {
  EncounterResult showEncounter();

  void UpdateStatusBar();

  void UpdateAll();

  ImageList ShipImages();

  ImageList EquipmentImages();

  ImageList DirectionImages();

  WfImage[] CustomShipImages();

  void setCustomShipImages(WfImage[] images);
}
