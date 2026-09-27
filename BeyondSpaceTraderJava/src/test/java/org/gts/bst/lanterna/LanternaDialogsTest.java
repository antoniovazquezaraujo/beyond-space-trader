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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.api.Test;


class LanternaDialogsTest {
  @Test
  void enterAcceptsTheAmountTyped() throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Integer[] answer = new Integer[1];
      Thread worker = new Thread(() -> answer[0] = LanternaDialogs.askAmount(gui, "Buy", "How many?", 20));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui);
      // The initial text is zero and the caret is at its end, so 0 + "12" is 12.
      dialog.handleInput(new KeyStroke('1', false, false));
      dialog.handleInput(new KeyStroke('2', false, false));
      dialog.handleInput(new KeyStroke(KeyType.Enter));
      worker.join(5000);
      assertFalse(worker.isAlive(), "the dialog should close with Enter");
      assertEquals(Integer.valueOf(12), answer[0]);
      assertTrue(gui.getWindows().isEmpty(), "the dialog should be gone");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void escapeCancelsTheAmount() throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Integer[] answer = new Integer[1];
      Thread worker = new Thread(() -> answer[0] = LanternaDialogs.askAmount(gui, "Buy", "How many?", 20));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui);
      dialog.handleInput(new KeyStroke(KeyType.Escape));
      worker.join(5000);
      assertFalse(worker.isAlive(), "the dialog should close with Escape");
      assertNull(answer[0], "Escape is a cancel");
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
