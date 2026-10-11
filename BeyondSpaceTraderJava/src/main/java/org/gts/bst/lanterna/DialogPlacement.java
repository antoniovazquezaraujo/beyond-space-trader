/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;


/**
 * Where the floating dialogs of the app land: centred at the bottom of the
 * screen, one row over the edge, instead of the top-left corner the window
 * manager would pick. Quantities, names, alerts and warnings all use the same
 * placement; a window that does not fit clamps to 0.
 */
final class DialogPlacement {
  private DialogPlacement() {
  }

  /** The bottom-centre position of a window over a screen. */
  static TerminalPosition bottomCenter(TerminalSize screen, TerminalSize window) {
    int column = Math.max(0, (screen.getColumns() - window.getColumns()) / 2);
    int row = Math.max(0, screen.getRows() - window.getRows() - 1);
    return new TerminalPosition(column, row);
  }

  /** Adds a window, pins it at the bottom and waits until it closes. */
  static void showAtBottom(WindowBasedTextGUI gui, Window window) {
    gui.addWindow(window);
    TerminalSize screen = gui.getScreen().getTerminalSize();
    window.setPosition(bottomCenter(screen, window.getDecoratedSize()));
    window.invalidate();
    gui.waitForWindowToClose(window);
  }
}
