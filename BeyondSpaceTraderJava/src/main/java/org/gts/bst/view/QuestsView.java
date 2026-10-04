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
 * Quests screen. The presenter fills a {@link QuestsViewModel} with one entry per open
 * quest; the front-end lists them, moves the selection through them and, on ENTER, sets
 * the destination of the selected entry as the map target.
 */
public interface QuestsView {
  void render(QuestsViewModel model);
}

