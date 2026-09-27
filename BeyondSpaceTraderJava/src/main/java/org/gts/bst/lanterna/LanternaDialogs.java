/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;


/**
 * Small dialogs shared by the text UI screens.
 */
public final class LanternaDialogs {
  private LanternaDialogs() {
  }

  /**
   * Asks for an amount between 0 and {@code maxAmount}; returns {@code null} when the
   * player cancels or types something invalid.
   */
  public static Integer askAmount(WindowBasedTextGUI gui, String title, String prompt, int maxAmount) {
    String input = InputDialog.show(gui, title, prompt, "0");
    if(input == null) {
      return null;
    }
    try {
      int value = Integer.parseInt(input.trim());
      return value >= 0 && value <= maxAmount ? value : null;
    } catch(NumberFormatException e) {
      return null;
    }
  }
}
