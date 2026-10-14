/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on SpaceTrader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.gts.bst.view.ShipPicture;
import spacetrader.Strings;


/**
 * The repair scene of the text UI: the ship being hammered back into shape.
 *
 * <p>The component only plays the repair: it takes the drawing of the ship and
 * the hull values, raises the bar from {@code from} to {@code to} while the
 * sparks fly over the hull, and calls back when it is over (or when the player
 * skips it with any key). The workshop loop sounds the whole time (see
 * {@link SoundService#repairLoop(boolean)}) and the done effect closes the
 * scene. The model is never touched here: the presenter applies the repair when
 * the scene returns.
 */
public final class RepairSceneComponent extends AbstractInteractableComponent<RepairSceneComponent> {
  /** The clock of the scene: the host timer calls {@link #tick()} at this pace. */
  static final int FRAME_MILLIS = 110;
  /** The repair lasts two frames per hull point, clamped to this range (4 s to 10 s). */
  private static final int FRAMES_PER_POINT = 2;
  private static final int MIN_FRAMES = 36;
  private static final int MAX_FRAMES = 91;
  /** The frames between two hammer blows: five, six or seven, in turn. */
  private static final int HIT_MIN = 5;
  private static final int HIT_SPREAD = 3;
  /** The sparks live a few frames, like the flashes of an encounter. */
  private static final int SPARK_FRAMES = 4;
  /** The full bar and the done sound stay a moment before the window goes. */
  private static final int DONE_FRAMES = 5;
  private static final int BAR_CELLS = 32;
  private static final char BAR_FULL = '█';
  private static final char BAR_EMPTY = '░';
  private static final String SPARK = "✶";
  private static final String BURST = "✱";
  private static final String DOT = "·";

  /** A spark of the hammer, at a cell of the drawing (top-left = 0,0). */
  private record Flash(int x, int y, String text, TextColor color, int frames) {
    Flash aged() {
      return new Flash(x, y, text, color, frames - 1);
    }
  }

  private final ShipPicture picture;
  private final int from;
  private final int to;
  private final int max;
  private final SoundService sound;
  private final int totalFrames;
  private final List<Flash> flashes = new ArrayList<>();
  private Runnable onDone;
  private int frame;
  private int hull;
  private int hits;
  private int nextHit = 1;
  private int doneFrames;
  private boolean finished;
  private boolean closed;

  public RepairSceneComponent(ShipPicture picture, int from, int to, int max, SoundService sound) {
    this.picture = picture;
    this.from = from;
    this.to = to;
    this.max = Math.max(max, to);
    this.sound = sound;
    this.totalFrames = Math.max(MIN_FRAMES, Math.min(MAX_FRAMES, (to - from) * FRAMES_PER_POINT));
    this.hull = from;
    // The workshop sounds the whole time the scene is open.
    sound.repairLoop(true);
  }

  /** The callback of the end: the host takes the window away. */
  public void onDone(Runnable done) {
    this.onDone = done;
  }

  /** The hull the bar shows now. */
  int hull() {
    return hull;
  }

  /** True when the repair is complete (the scene only waits to be taken away). */
  boolean finished() {
    return finished;
  }

  /** Advances the repair one frame: the bar rises and the hammer hits. */
  public void tick() {
    ageFlashes();
    if(finished) {
      if(doneFrames > 0 && --doneFrames == 0) {
        close();
        return;
      }
      invalidate();
      return;
    }
    frame++;
    if(frame >= totalFrames) {
      complete();
      return;
    }
    hull = from + (to - from) * frame / totalFrames;
    if(frame >= nextHit) {
      hits++;
      nextHit = frame + HIT_MIN + hits % HIT_SPREAD;
      sparks();
    }
    invalidate();
  }

  /** The sparks of a hammer blow on the hull (the workshop loop carries the sound). */
  private void sparks() {
    int x = 1 + hits * 3 % Math.max(1, picture.width() - 2);
    int y = 1 + hits * 2 % Math.max(1, picture.height() - 2);
    flashes.add(new Flash(x, y, SPARK, TextColor.ANSI.YELLOW_BRIGHT, SPARK_FRAMES));
    flashes.add(new Flash(x - 1, y + 1, DOT, TextColor.ANSI.WHITE, SPARK_FRAMES));
    flashes.add(new Flash(x + 2, y - 1, DOT, TextColor.ANSI.YELLOW, SPARK_FRAMES));
  }

  /** The repair is done: the full bar, a last burst and the done sound. */
  private void complete() {
    finished = true;
    hull = to;
    sound.play(SoundEffect.REPAIR_DONE);
    doneFrames = DONE_FRAMES;
    for(int i = 0; i < 3; i++) {
      flashes.add(new Flash(1 + picture.width() * (i + 1) / 4, picture.height() / 2, BURST,
          TextColor.ANSI.GREEN_BRIGHT, DONE_FRAMES));
    }
    invalidate();
  }

  /** Takes the window away, once: the workshop loop fades out with it. */
  private void close() {
    if(closed) {
      return;
    }
    closed = true;
    invalidate();
    sound.repairLoop(false);
    if(onDone != null) {
      onDone.run();
    }
  }

  private void ageFlashes() {
    for(int i = flashes.size() - 1; i >= 0; i--) {
      Flash flash = flashes.get(i);
      if(flash.frames() <= 1) {
        flashes.remove(i);
      } else {
        flashes.set(i, flash.aged());
      }
    }
  }

  @Override
  protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
    // Any key skips to the end: the repair completes and the scene takes its bow.
    if(finished) {
      close();
    } else {
      complete();
    }
    return Interactable.Result.HANDLED;
  }

  @Override
  protected InteractableRenderer<RepairSceneComponent> createDefaultRenderer() {
    return new InteractableRenderer<RepairSceneComponent>() {
      @Override
      public TerminalPosition getCursorLocation(RepairSceneComponent component) {
        return null;
      }

      @Override
      public TerminalSize getPreferredSize(RepairSceneComponent component) {
        return TerminalSize.ZERO;
      }

      @Override
      public void drawComponent(TextGUIGraphics graphics, RepairSceneComponent component) {
        paint(graphics);
      }
    };
  }

  private void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    int width = size.getColumns();
    int height = size.getRows();
    UiPalette.reset(graphics);
    for(int row = 0; row < height; row++) {
      graphics.putString(0, row, " ".repeat(width));
    }
    if(width <= 0 || height <= 0) {
      return;
    }
    boolean blinkOn = frame / 3 % 2 == 0;
    centered(graphics, 1, Strings.RepairSceneTitle, UiPalette.TITLE, width);
    int left = Math.max(0, (width - picture.width()) / 2);
    int top = Math.max(3, (height - picture.height()) / 2);
    EditorText.picture(graphics, 0, top, width, Math.max(top, height - 2), picture, blinkOn);
    for(Flash flash : flashes) {
      int x = left + flash.x();
      int y = top + flash.y();
      if(x >= 0 && x + flash.text().length() <= width && y >= 0 && y < height) {
        graphics.setForegroundColor(flash.color());
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.putString(x, y, flash.text());
      }
    }
    UiPalette.reset(graphics);
    drawBar(graphics, width, height);
    centered(graphics, height - 1, Strings.RepairSceneSkip, UiPalette.WARN, width);
  }

  /** The hull bar at the foot, with the numbers of the repair. */
  private void drawBar(TextGUIGraphics graphics, int width, int height) {
    String bar = bar();
    String numbers = from + " -> " + to + " / " + max;
    int x = Math.max(0, (width - bar.length() - numbers.length() - 3) / 2);
    x = UiPalette.draw(graphics, x, Math.max(0, height - 2), bar, UiPalette.statusColor(hull, max), width);
    UiPalette.draw(graphics, x + 2, Math.max(0, height - 2), numbers, UiPalette.TEXT, width);
  }

  /** The bar: a cell per hull point, capped to its width. */
  private String bar() {
    int filled = max <= 0 ? 0 : Math.max(0, Math.min(BAR_CELLS, hull * BAR_CELLS / max));
    return String.valueOf(BAR_FULL).repeat(filled) + String.valueOf(BAR_EMPTY).repeat(BAR_CELLS - filled);
  }

  private static void centered(TextGUIGraphics graphics, int row, String text, TextColor color, int width) {
    UiPalette.draw(graphics, Math.max(0, (width - EditorText.width(text)) / 2), row, text, color, width);
  }
}
