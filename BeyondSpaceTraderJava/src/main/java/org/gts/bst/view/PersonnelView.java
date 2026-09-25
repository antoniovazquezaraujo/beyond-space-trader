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
 * Personnel screen. The presenter renders the lists and the information panel and
 * mediates the hire/fire action; the view only forwards selections and the button.
 */
public interface PersonnelView {
  void render(PersonnelViewModel model);

  void renderInfo(PersonnelInfo info);
}

