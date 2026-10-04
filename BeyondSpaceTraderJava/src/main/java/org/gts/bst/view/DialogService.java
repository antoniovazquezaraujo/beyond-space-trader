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

  /**
   * Shows a plain message with a single OK button. The title and the message arrive
   * already resolved: this is the free-text companion of {@link #alert}, for the
   * texts that live outside the predefined alerts.
   *
   * <p>The default implementation returns {@link DialogResult#None}: with no user
   * interface there is nobody to acknowledge the message, so the caller must not
   * apply the action behind it. Front-ends override it to really ask.
   *
   * @param title the window title
   * @param message the body text
   * @return the button the player pressed, never null
   */
  default DialogResult message(String title, String message) {
    return DialogResult.None;
  }

  /**
   * Asks a yes/no question over an already resolved text.
   *
   * <p>The default implementation returns {@link DialogResult#None}, not a Yes: a
   * service without a user interface cannot consent, so the caller must not apply
   * the action behind the question. Front-ends override it to really ask.
   *
   * @param title the window title
   * @param message the question
   * @return Yes, No or None when nobody could answer, never null
   */
  default DialogResult confirm(String title, String message) {
    return DialogResult.None;
  }

  DialogService NONE = new DialogService() {
    @Override
    public DialogResult alert(AlertType type, String... messageArgs) {
      return DialogResult.None;
    }
  };
}

