/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.util.List;
import org.gts.bst.view.AlertButton;
import org.gts.bst.view.DialogResult;
import org.junit.jupiter.api.Test;


class LanternaAlertDialogHostTest {
  @Test
  void enterChoosesTheFirstButton() throws IOException, InterruptedException {
    DialogResult answer = answerWith(new KeyStroke(KeyType.Enter),
        List.of(new AlertButton("Yes", DialogResult.Yes), new AlertButton("No", DialogResult.No)));
    assertEquals(DialogResult.Yes, answer);
  }

  @Test
  void escapeChoosesTheCancelButton() throws IOException, InterruptedException {
    DialogResult answer = answerWith(new KeyStroke(KeyType.Escape),
        List.of(new AlertButton("Yes", DialogResult.Yes), new AlertButton("No", DialogResult.No)));
    assertEquals(DialogResult.No, answer);
  }

  @Test
  void escapeClosesAnAlertWithAnOkButtonOnly() throws IOException, InterruptedException {
    DialogResult answer = answerWith(new KeyStroke(KeyType.Escape),
        List.of(new AlertButton("Ok", DialogResult.OK)));
    assertEquals(DialogResult.OK, answer);
  }

  @Test
  void longMessagesAreWrappedInsteadOfCut() throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaAlertDialogHost host = new LanternaAlertDialogHost(gui);
      String message = "This is a long alert message that would not fit in the dialog without "
          + "wrapping, so the host has to wrap it into several lines that fit in the terminal. "
          + "The end of the message.";
      DialogResult[] answer = new DialogResult[1];
      Thread worker = new Thread(() -> answer[0] = host.show("Title", message,
          List.of(new AlertButton("Ok", DialogResult.OK))));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui);
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains("The end of the message."), text);
      assertTrue(text.contains("long alert"), text);
      dialog.handleInput(new KeyStroke(KeyType.Escape));
      worker.join(5000);
      assertEquals(DialogResult.OK, answer[0]);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      StringBuilder line = new StringBuilder();
      for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
        line.append(screen.getBackCharacter(x, y).getCharacter());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }

  private static DialogResult answerWith(KeyStroke key, List<AlertButton> buttons)
      throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaAlertDialogHost host = new LanternaAlertDialogHost(gui);
      DialogResult[] answer = new DialogResult[1];
      Thread worker = new Thread(() -> answer[0] = host.show("Title", "Message", buttons));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui);
      dialog.handleInput(key);
      worker.join(5000);
      assertFalse(worker.isAlive(), "the dialog should close with the key");
      return answer[0];
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static Window waitForDialog(MultiWindowTextGUI gui) throws InterruptedException {
    for(int i = 0; i < 500; i++) {
      for(Window window : gui.getWindows()) {
        return window;
      }
      Thread.sleep(10);
    }
    fail("the dialog was not shown");
    return null;
  }
}
