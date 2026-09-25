/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import spacetrader.enums.AlertType;


/**
 * Shows modal messages to the player. The model depends on this interface only; each
 * front-end (Swing today, Lanterna later) provides an implementation. The {@link #NONE}
 * constant is a no-op service for contexts without a user interface, such as headless
 * tests or silent startup paths.
 */
@FunctionalInterface
public interface DialogService {
  /**
   * Shows a predefined alert and returns the button the player pressed.
   *
   * @param type the kind of alert (title, text and buttons come from its definition)
   * @param messageArgs values for the {@code ^1}, {@code ^2}... placeholders in the message
   */
  DialogResult alert(AlertType type, String... messageArgs);

  DialogService NONE = new DialogService() {
    @Override
    public DialogResult alert(AlertType type, String... messageArgs) {
      return DialogResult.None;
    }
  };
}

