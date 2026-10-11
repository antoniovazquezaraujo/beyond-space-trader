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
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import spacetrader.Strings;


/**
 * Input dialog whose Enter accepts and whose Escape cancels, without having to move
 * the focus to the buttons (the Lanterna dialog needs the Ok button to be focused).
 * Accepting (Enter or the Ok button) plays the select sound; a cancel stays silent.
 */
public final class InputDialog extends DialogWindow {
  private final TextBox textBox;
  private final SoundService sound;
  private String result;

  private InputDialog(String title, String prompt, String initial, SoundService sound) {
    super(title);
    this.sound = sound;
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
   * Opens the dialog at the bottom of the screen (the shared placement of every
   * floating dialog) and waits until it closes.
   */
  @Override
  public String showDialog(WindowBasedTextGUI gui) {
    result = null;
    DialogPlacement.showAtBottom(gui, this);
    return result;
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
    sound.play(SoundEffect.MENU_SELECT);
    result = textBox.getText();
    close();
  }

  private void cancel() {
    result = null;
    close();
  }

  /**
   * Asks for a text; returns {@code null} when the player cancels with Escape.
   */
  public static String show(WindowBasedTextGUI gui, String title, String prompt, String initial) {
    return show(gui, title, prompt, initial, SoundService.NONE);
  }

  /** The same, with the sound of the screen that opens it. */
  public static String show(WindowBasedTextGUI gui, String title, String prompt, String initial,
      SoundService sound) {
    return new InputDialog(title, prompt, initial, sound).showDialog(gui);
  }
}
