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
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.AlertButton;
import org.gts.bst.view.AlertDialogHost;
import org.gts.bst.view.DialogResult;


/**
 * Shows the predefined alerts in a Lanterna dialog with the original texts and
 * buttons.
 */
public final class LanternaAlertDialogHost implements AlertDialogHost {
  private final WindowBasedTextGUI gui;

  public LanternaAlertDialogHost(WindowBasedTextGUI gui) {
    this.gui = gui;
  }

  @Override
  public DialogResult show(String title, String message, List<AlertButton> buttons) {
    if(buttons.isEmpty()) {
      return DialogResult.None;
    }
    DialogResult[] chosen = {buttons.get(0).result()};
    List<Button> row = new ArrayList<>();
    BasicWindow dialog = new BasicWindow(title) {
      @Override
      public boolean handleInput(KeyStroke key) {
        if(moveButtonFocus(row, this, key)) {
          return true;
        }
        if(key.getKeyType() == KeyType.Escape) {
          chosen[0] = cancelResult(buttons);
          close();
          return true;
        }
        if(key.getKeyType() == KeyType.Enter && !(getFocusedInteractable() instanceof Button)) {
          chosen[0] = buttons.get(0).result();
          close();
          return true;
        }
        return super.handleInput(key);
      }
    };
    Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
    panel.addComponent(new Label(wrapped(message)));
    panel.addComponent(new EmptySpace(TerminalSize.ONE));
    Panel buttonRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
    Button first = null;
    for(AlertButton button : buttons) {
      Button component = new Button(button.text(), () -> {
        chosen[0] = button.result();
        dialog.close();
      });
      buttonRow.addComponent(component);
      row.add(component);
      if(first == null) {
        first = component;
      }
    }
    panel.addComponent(buttonRow);
    dialog.setComponent(panel);
    if(first != null) {
      dialog.setFocusedInteractable(first);
    }
    gui.addWindow(dialog);
    gui.waitForWindowToClose(dialog);
    return chosen[0];
  }

  /**
   * The n/p keys move the focus along the row of buttons (down/up), wrapping
   * around; any other key is left to the dialog.
   */
  private static boolean moveButtonFocus(List<Button> row, BasicWindow dialog, KeyStroke key) {
    if(row.isEmpty() || key.getKeyType() != KeyType.Character) {
      return false;
    }
    char character = Character.toLowerCase(key.getCharacter());
    if(character != 'n' && character != 'p') {
      return false;
    }
    int current = row.indexOf(dialog.getFocusedInteractable());
    if(current < 0) {
      // No button focused: n picks the first and p the last.
      current = 0;
    }
    int next = Math.floorMod(current + (character == 'n' ? 1 : -1), row.size());
    dialog.setFocusedInteractable(row.get(next));
    return true;
  }

  /** The message wrapped to a width that fits in the terminal. */
  private String wrapped(String message) {
    int columns = gui.getScreen() == null ? 70 : gui.getScreen().getTerminalSize().getColumns();
    List<String> lines = new ArrayList<>();
    MainTextComponent.wrap(lines, message, Math.max(20, Math.min(72, columns - 8)));
    return String.join("\n", lines);
  }

  /**
   * The result of Escape: the Cancel or No button when there is one, the default
   * button otherwise (a dialog that only says "Ok" closes with it).
   */
  private static DialogResult cancelResult(List<AlertButton> buttons) {
    for(AlertButton button : buttons) {
      if(button.result() == DialogResult.Cancel || button.result() == DialogResult.No) {
        return button.result();
      }
    }
    return buttons.get(0).result();
  }
}
