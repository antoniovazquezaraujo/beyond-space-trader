/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.DialogService;
import spacetrader.enums.AlertType;


/**
 * Test double for {@link DialogService} that records every alert, message and
 * confirmation shown and returns a configurable result for each kind. The three
 * results default to {@link DialogResult#None}, so a test that wants an action
 * applied must say so explicitly.
 */
public class TestDialogService implements DialogService {
  private final List<AlertType> alerts = new ArrayList<>();
  private final List<String> messageTitles = new ArrayList<>();
  private final List<String> messages = new ArrayList<>();
  private final List<String> confirmTitles = new ArrayList<>();
  private final List<String> confirms = new ArrayList<>();
  private DialogResult result = DialogResult.None;
  private DialogResult messageResult = DialogResult.None;
  private DialogResult confirmResult = DialogResult.None;

  /** The result of every {@link #alert}. */
  public void setResult(DialogResult result) {
    this.result = result;
  }

  /** The result of every {@link #message}. */
  public void setMessageResult(DialogResult result) {
    this.messageResult = result;
  }

  /** The result of every {@link #confirm}. */
  public void setConfirmResult(DialogResult result) {
    this.confirmResult = result;
  }

  @Override
  public DialogResult alert(AlertType type, String... messageArgs) {
    alerts.add(type);
    return result;
  }

  @Override
  public DialogResult message(String title, String message) {
    messageTitles.add(title);
    messages.add(message);
    return messageResult;
  }

  @Override
  public DialogResult confirm(String title, String message) {
    confirmTitles.add(title);
    confirms.add(message);
    return confirmResult;
  }

  public List<AlertType> alerts() {
    return alerts;
  }

  public List<String> messageTitles() {
    return messageTitles;
  }

  public List<String> messages() {
    return messages;
  }

  public List<String> confirmTitles() {
    return confirmTitles;
  }

  public List<String> confirms() {
    return confirms;
  }
}
