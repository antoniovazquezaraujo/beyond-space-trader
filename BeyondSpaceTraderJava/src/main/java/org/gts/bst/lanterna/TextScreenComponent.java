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
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;


/**
 * A window that only shows text lines and forwards the keys: the base of the
 * encounter and cargo transfer screens.
 */
public final class TextScreenComponent extends AbstractInteractableComponent<TextScreenComponent> {
  /**
   * Handles a key; returns whether it was consumed.
   */
  @FunctionalInterface
  public interface KeyHandler {
    boolean handle(KeyStroke keyStroke);
  }

  private final KeyHandler keyHandler;
  private final List<String> lines = new ArrayList<>();

  public TextScreenComponent(KeyHandler keyHandler) {
    this.keyHandler = keyHandler;
  }

  public void lines(List<String> lines) {
    this.lines.clear();
    this.lines.addAll(lines);
    invalidate();
  }

  @Override
  protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
    return keyHandler.handle(keyStroke) ? Interactable.Result.HANDLED : Interactable.Result.UNHANDLED;
  }

  @Override
  protected InteractableRenderer<TextScreenComponent> createDefaultRenderer() {
    return new InteractableRenderer<TextScreenComponent>() {
      @Override
      public TerminalPosition getCursorLocation(TextScreenComponent component) {
        return null;
      }

      @Override
      public TerminalSize getPreferredSize(TextScreenComponent component) {
        return TerminalSize.ZERO;
      }

      @Override
      public void drawComponent(TextGUIGraphics graphics, TextScreenComponent component) {
        component.paint(graphics);
      }
    };
  }

  private void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    int width = size.getColumns();
    int height = size.getRows();
    UiPalette.reset(graphics);
    String blank = " ".repeat(width);
    for(int row = 0; row < height; row++) {
      graphics.putString(0, row, blank);
    }
    for(int i = 0; i < lines.size() && i < height; i++) {
      UiPalette.keys(graphics, 0, i, lines.get(i), width);
    }
  }
}
