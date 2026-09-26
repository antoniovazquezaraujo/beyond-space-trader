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
import com.googlecode.lanterna.gui2.DefaultWindowDecorationRenderer;


/**
 * The text UI theme, built from {@link UiPalette}: white on black, with the window
 * borders and titles in cyan and the selected rows highlighted. The Lanterna default
 * theme paints the unpainted areas and the dialogs white.
 */
public final class LanternaTheme {
  private LanternaTheme() {
  }

  public static Theme create() {
    SimpleTheme theme = SimpleTheme.makeTheme(false,
        TextColor.ANSI.WHITE, TextColor.ANSI.BLACK,
        TextColor.ANSI.CYAN, TextColor.ANSI.BLACK,
        TextColor.ANSI.YELLOW, TextColor.ANSI.BLACK,
        TextColor.ANSI.BLACK);
    // The window borders and titles are drawn by this renderer, which looks up its
    // own definition in the theme: give it the palette frame colour.
    theme.addOverride(DefaultWindowDecorationRenderer.class, TextColor.ANSI.CYAN, TextColor.ANSI.BLACK);
    return theme;
  }
}
