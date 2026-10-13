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
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;


/**
 * A full screen window with a canvas: the editors just paint and handle keys, and
 * they share the blink clock (for the pieces and the zones that flash). The sound
 * service of the tool that opens the editor feeds the shared menu sounds; the
 * default is silence, so the editors still open without a device.
 */
abstract class ArtEditorWindow extends BasicWindow {
  private final Canvas canvas = new Canvas();
  private volatile boolean blinkOn = true;
  private final SoundService sound;

  ArtEditorWindow(String title) {
    this(title, SoundService.NONE);
  }

  /** The same, with the sound of the tool that opens the editor. */
  ArtEditorWindow(String title, SoundService sound) {
    super(title);
    this.sound = sound;
    setHints(Set.of(Window.Hint.FULL_SCREEN));
    setComponent(canvas);
    setFocusedInteractable(canvas);
    // Safety net: if the window manager swallows a key (TAB changes the focus),
    // we catch it here and pass it to the editor.
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

  /** A step of the cursor or the selection: the shared sound of the whole UI. */
  protected final void menuMove() {
    sound.play(SoundEffect.MENU_MOVE);
  }

  /** An acceptance of the editor (an edit, a command): the shared select sound. */
  protected final void menuSelect() {
    sound.play(SoundEffect.MENU_SELECT);
  }

  /** The sound of the tool, for the dialogs the editors open (the name). */
  protected final SoundService sound() {
    return sound;
  }

  protected boolean blinkOn() {
    return blinkOn;
  }

  protected void redraw() {
    canvas.invalidate();
  }

  protected abstract void paint(TextGUIGraphics graphics);

  protected abstract boolean handleKey(KeyStroke key);

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
        }
      };
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke key) {
      return handleKey(key) ? Result.HANDLED : Result.UNHANDLED;
    }
  }
}
