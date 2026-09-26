/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;


/**
 * The text UI theme: white on black everywhere, with the selection inverted. The
 * Lanterna default theme paints the unpainted areas and the dialogs white.
 */
public final class LanternaTheme {
  private LanternaTheme() {
  }

  public static Theme create() {
    return SimpleTheme.makeTheme(false,
        TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
        TextColor.ANSI.BLACK, TextColor.ANSI.WHITE,
        TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
        TextColor.ANSI.BLACK);
  }
}
