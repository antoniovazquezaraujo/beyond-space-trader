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
import org.gts.bst.ports.SoundService;


/**
 * Small dialogs shared by the text UI screens. They accept the sound service of
 * the screen that opens them, so the quantity dialogs sound when accepted.
 */
public final class LanternaDialogs {
  private LanternaDialogs() {
  }

  /**
   * Asks for an amount between 0 and {@code maxAmount}; returns {@code null} when the
   * player cancels or types something invalid.
   */
  public static Integer askAmount(WindowBasedTextGUI gui, String title, String prompt, int maxAmount) {
    return askAmount(gui, title, prompt, maxAmount, SoundService.NONE);
  }

  /** The same, with the sound of the screen that asks. */
  public static Integer askAmount(WindowBasedTextGUI gui, String title, String prompt, int maxAmount,
      SoundService sound) {
    String input = InputDialog.show(gui, title, prompt, "0", sound);
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

  /** The same question, asked in a dialog at the bottom of the screen (the fight). */
  public static Integer askAmountAtBottom(WindowBasedTextGUI gui, String title, String prompt, int maxAmount) {
    return askAmountAtBottom(gui, title, prompt, maxAmount, SoundService.NONE);
  }

  /** The same, with the sound of the screen that asks. */
  public static Integer askAmountAtBottom(WindowBasedTextGUI gui, String title, String prompt, int maxAmount,
      SoundService sound) {
    String input = InputDialog.showAtBottom(gui, title, prompt, "0", sound);
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
