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
import com.googlecode.lanterna.gui2.AbstractComponent;
import com.googlecode.lanterna.gui2.ComponentRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;


/**
 * The cover of the composer, drawn behind the menu window: the ship art read once
 * from {@code /org/gts/bst/lanterna/composer.txt}, centred on the screen. When the
 * terminal is smaller than the drawing, nothing is painted (the black screen shows
 * through) and the menu keeps working.
 */
final class ComposerCover extends AbstractComponent<ComposerCover> {
  /** The resource of the composer cover. */
  static final String RESOURCE = "/org/gts/bst/lanterna/composer.txt";

  private final TitleSplash splash;

  ComposerCover(TitleSplash splash) {
    this.splash = splash;
  }

  /** The cover read from its resource (or empty when the resource is missing). */
  static ComposerCover load() {
    return new ComposerCover(TitleSplash.fromResource(RESOURCE));
  }

  /** The drawing of the cover; null when the resource was missing. */
  TitleSplash splash() {
    return splash;
  }

  @Override
  protected ComponentRenderer<ComposerCover> createDefaultRenderer() {
    return new ComponentRenderer<ComposerCover>() {
      @Override
      public TerminalSize getPreferredSize(ComposerCover component) {
        return TerminalSize.ZERO;
      }

      @Override
      public void drawComponent(TextGUIGraphics graphics, ComposerCover component) {
        component.paint(graphics);
      }
    };
  }

  /** Centres the drawing; a drawing that does not fit the terminal is left out. */
  private void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    if(splash == null || splash.width() <= 0 || splash.height() <= 0
        || splash.width() > size.getColumns() || splash.height() > size.getRows()) {
      return;
    }
    splash.draw(graphics, (size.getColumns() - splash.width()) / 2, (size.getRows() - splash.height()) / 2);
  }
}
