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
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.Set;


/**
 * A full screen window with a canvas: the editors just paint and handle keys, and
 * they share the blink clock (for the pieces and the zones that flash).
 */
abstract class ArtEditorWindow extends BasicWindow {
  private final Canvas canvas = new Canvas();
  private boolean blinkOn = true;
  private boolean glyphStrip;
  /** Sample glyphs to check in the terminal: a wide one leaves a hole after the marker. */
  private static final String GLYPH_SAMPLE = "🁣 🁩 🂓 ┃ ⚀ ⚅ ┃ ⣿ ⠿ ┃ ⧯ ⎅ ⏌ ⎚ ⛁ ┃ ↠ ⇉ ⦖ ⧎ ◒ ◈ ⍉ ⏚ ┃ 😀 中 ┃ ┌─┐";

  ArtEditorWindow(String title) {
    super(title);
    setHints(Set.of(Window.Hint.FULL_SCREEN));
    setComponent(canvas);
    setFocusedInteractable(canvas);
    // Red de seguridad: si el gestor de ventanas se come una tecla (TAB cambia el
    // foco), la recogemos aqui y se la pasamos al editor.
    addWindowListener(new com.googlecode.lanterna.gui2.WindowListenerAdapter() {
      @Override
      public void onUnhandledInput(com.googlecode.lanterna.gui2.Window window, KeyStroke key,
          java.util.concurrent.atomic.AtomicBoolean delivered) {
        if(handleKey(key)) {
          delivered.set(true);
        }
      }
    });
    java.util.Timer timer = new java.util.Timer("editor-blink", true);
    timer.scheduleAtFixedRate(new java.util.TimerTask() {
      @Override
      public void run() {
        blinkOn = !blinkOn;
        canvas.invalidate();
      }
    }, 500, 500);
  }

  protected boolean blinkOn() {
    return blinkOn;
  }

  protected void redraw() {
    canvas.invalidate();
  }

  protected abstract void paint(TextGUIGraphics graphics);

  protected abstract boolean handleKey(KeyStroke key);

  /** The glyph strip first, then the editor's keys. */
  boolean handleKeyWithStrip(KeyStroke key) {
    if(key.getKeyType() == com.googlecode.lanterna.input.KeyType.Character
        && Character.toLowerCase(key.getCharacter()) == 'g') {
      glyphStrip = !glyphStrip;
      redraw();
      return true;
    }
    return handleKey(key);
  }

  private final class Canvas extends AbstractInteractableComponent<Canvas> {
    @Override
    protected InteractableRenderer<Canvas> createDefaultRenderer() {
      return new InteractableRenderer<Canvas>() {
        @Override
        public TerminalPosition getCursorLocation(Canvas component) {
          return null;
        }

        @Override
        public TerminalSize getPreferredSize(Canvas component) {
          return TerminalSize.ZERO;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, Canvas component) {
          paint(graphics);
          if(glyphStrip) {
            graphics.setForegroundColor(com.googlecode.lanterna.TextColor.ANSI.WHITE);
            graphics.setBackgroundColor(com.googlecode.lanterna.TextColor.ANSI.BLACK);
            graphics.putString(0, Math.max(0, component.getSize().getRows() - 1), EditorText.cut("glifos: "
                + GLYPH_SAMPLE + "   (cada ┃: pegado al glifo = 2 columnas; con hueco = 1 columna)"
                + "   · [g] quitar", component.getSize().getColumns()));
          }
        }
      };
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke key) {
      return handleKeyWithStrip(key) ? Result.HANDLED : Result.UNHANDLED;
    }
  }
}
