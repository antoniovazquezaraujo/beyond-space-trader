/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.DialogWindow;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import spacetrader.Strings;


/**
 * Input dialog whose Enter accepts and whose Escape cancels, without having to move
 * the focus to the buttons (the Lanterna dialog needs the Ok button to be focused).
 */
public final class InputDialog extends DialogWindow {
  private final TextBox textBox;
  private String result;

  private InputDialog(String title, String prompt, String initial) {
    super(title);
    textBox = new TextBox(initial);
    Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
    panel.addComponent(new Label(prompt));
    panel.addComponent(new EmptySpace(TerminalSize.ONE));
    panel.addComponent(textBox);
    panel.addComponent(new EmptySpace(TerminalSize.ONE));
    Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
    buttons.addComponent(new Button(Strings.ButtonOk, this::accept));
    buttons.addComponent(new Button(Strings.ButtonCancel, this::cancel));
    buttons.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.End));
    panel.addComponent(buttons);
    setComponent(panel);
    setFocusedInteractable(textBox);
  }

  /**
   * The same dialog, but sitting at the bottom of the screen, so it does not cover
   * the ships of the encounter scene.
   */
  public static String showAtBottom(WindowBasedTextGUI gui, String title, String prompt, String initial) {
    InputDialog dialog = new InputDialog(title, prompt, initial);
    TerminalSize size = gui.getScreen().getTerminalSize();
    dialog.setHints(java.util.Set.of(com.googlecode.lanterna.gui2.Window.Hint.MODAL));
    dialog.setPosition(new com.googlecode.lanterna.TerminalPosition(2, Math.max(0, size.getRows() - 9)));
    return dialog.showDialog(gui);
  }

  @Override
  public boolean handleInput(KeyStroke key) {
    if(key.getKeyType() == KeyType.Enter && !(getFocusedInteractable() instanceof Button)) {
      accept();
      return true;
    }
    if(key.getKeyType() == KeyType.Escape) {
      cancel();
      return true;
    }
    return super.handleInput(key);
  }

  private void accept() {
    result = textBox.getText();
    close();
  }

  private void cancel() {
    result = null;
    close();
  }

  @Override
  public String showDialog(WindowBasedTextGUI gui) {
    result = null;
    super.showDialog(gui);
    return result;
  }

  /**
   * Asks for a text; returns {@code null} when the player cancels with Escape.
   */
  public static String show(WindowBasedTextGUI gui, String title, String prompt, String initial) {
    return new InputDialog(title, prompt, initial).showDialog(gui);
  }
}
