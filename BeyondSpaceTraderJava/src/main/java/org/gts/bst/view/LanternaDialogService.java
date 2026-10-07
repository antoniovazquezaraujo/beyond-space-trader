/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.DialogService;
import spacetrader.Functions;
import spacetrader.enums.AlertType;


/**
 * Dialog service over an {@link AlertDialogHost}: resolves the predefined alert and
 * its placeholders, then lets the host present it. The image alerts (AppStart and
 * the game-end ones) are not shown in the text UI yet.
 */
public final class LanternaDialogService implements DialogService {
  private final AlertDialogHost host;
  private BiConsumer<AlertType, String> quiet;

  public LanternaDialogService(AlertDialogHost host) {
    this.host = host;
  }

  /**
   * Sends the informative alerts (the ones with a single button) to a sink instead of
   * opening a window; the questions (two buttons) keep their dialog. The sink gets the
   * type and the resolved text, so the screen can tell which alerts it says aloud. A
   * screen sets it while it is open and clears it (with null) when it closes.
   */
  public void quietTo(BiConsumer<AlertType, String> sink) {
    this.quiet = sink;
  }

  @Override
  public DialogResult alert(AlertType type, String... messageArgs) {
    AlertDefinition definition = Alerts.get(type);
    if(definition == null) {
      // Image alerts only have a title; show it with a single button.
      host.show(Alerts.title(type), "",
          List.of(new AlertButton(spacetrader.Strings.AlertButtonOk, DialogResult.OK)));
      return DialogResult.OK;
    }
    String[] args = messageArgs == null ? new String[0] : messageArgs;
    if(quiet != null && definition.button2() == null) {
      // An outcome, not a question: one more line of the log, with no dialog.
      String text = Functions.StringVars(definition.message(), args);
      quiet.accept(type, text == null || text.isBlank() ? Functions.StringVars(definition.title(), args) : text);
      return DialogResult.OK;
    }
    List<AlertButton> buttons = new ArrayList<>(2);
    buttons.add(new AlertButton(definition.button1(), definition.result1()));
    if(definition.button2() != null) {
      buttons.add(new AlertButton(definition.button2(), definition.result2()));
    }
    return host.show(Functions.StringVars(definition.title(), args),
        Functions.StringVars(definition.message(), args), buttons);
  }

  /**
   * A free-text message with one OK button. It never goes through the quiet sink:
   * the offer must be seen even when the informative alerts are being logged.
   */
  @Override
  public DialogResult message(String title, String message) {
    return host.show(title, message,
        List.of(new AlertButton(spacetrader.Strings.AlertButtonOk, DialogResult.OK)));
  }

  /**
   * A free-text yes/no question. It never goes through the quiet sink either: the
   * player must answer it.
   */
  @Override
  public DialogResult confirm(String title, String message) {
    return host.show(title, message, List.of(
        new AlertButton(spacetrader.Strings.AlertButtonYes, DialogResult.Yes),
        new AlertButton(spacetrader.Strings.AlertButtonNo, DialogResult.No)));
  }
}
