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
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.EncounterViewModel;
import org.gts.bst.view.Starfield;


/**
 * The encounter scene: both ships facing each other over a starfield, with
 * their hull and shield bars above, the log under them and the actions with
 * their keys at the bottom. The presenter fills the model; this component only
 * paints it and forwards the keys.
 */
public final class EncounterSceneComponent extends AbstractInteractableComponent<EncounterSceneComponent> {
  /**
   * Handles a key; returns whether it was consumed.
   */
  @FunctionalInterface
  public interface KeyHandler {
    boolean handle(KeyStroke keyStroke);
  }

  /** The bars take the first rows; the log these under the ships. */
  private static final int BARS_ROWS = 2;
  private static final int LOG_ROWS = 5;
  private static final int BAR_CELLS = 8;
  private static final char BAR_FULL = '█';
  private static final char BAR_EMPTY = '░';

  private final KeyHandler keyHandler;
  private final List<String> log = new ArrayList<>();
  private EncounterViewModel model;
  private Starfield starfield;

  public EncounterSceneComponent(KeyHandler keyHandler) {
    this.keyHandler = keyHandler;
  }

  /** The scene model: both ships, their pictures and their bars. */
  public void model(EncounterViewModel model) {
    this.model = model;
    invalidate();
  }

  /** The log lines of the encounter, already wrapped. */
  public void log(List<String> lines) {
    this.log.clear();
    this.log.addAll(lines);
    invalidate();
  }

  /** Moves the stars of the background (the window timer calls it). */
  public void tick() {
    if(starfield != null) {
      starfield.advance();
      invalidate();
    }
  }

  @Override
  protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
    return keyHandler.handle(keyStroke) ? Interactable.Result.HANDLED : Interactable.Result.UNHANDLED;
  }

  @Override
  protected InteractableRenderer<EncounterSceneComponent> createDefaultRenderer() {
    return new InteractableRenderer<EncounterSceneComponent>() {
      @Override
      public TerminalPosition getCursorLocation(EncounterSceneComponent component) {
        return null;
      }

      @Override
      public TerminalSize getPreferredSize(EncounterSceneComponent component) {
        return TerminalSize.ZERO;
      }

      @Override
      public void drawComponent(TextGUIGraphics graphics, EncounterSceneComponent component) {
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
    drawStars(graphics, width, height);
    if(model == null) {
      return;
    }
    drawBars(graphics, width);
    int shipsTop = BARS_ROWS;
    int logTop = Math.max(shipsTop + 2, height - LOG_ROWS);
    int half = (width - 1) / 2;
    EditorText.picture(graphics, 0, shipsTop, half, logTop - 1, model.youPicture().cropped());
    EditorText.picture(graphics, half + 1, shipsTop, width - half - 1, logTop - 1,
        model.opponentPicture().cropped());
    drawLog(graphics, width, height, logTop);
  }

  /** The parallax starfield of the title screen, as the backdrop of the fight. */
  private void drawStars(TextGUIGraphics graphics, int width, int height) {
    int columns = Math.max(1, width - 1);
    int rows = Math.max(1, height - 1);
    if(starfield == null || starfield.dotWidth() != columns * 2 || starfield.dotHeight() != rows * 4) {
      starfield = new Starfield(columns, rows, 0.10, 42);
    }
    Starfield.Frame frame = starfield.frame(columns, rows);
    for(int row = 0; row < rows; row++) {
      for(int column = 0; column < columns; column++) {
        int shade = frame.shades()[row][column];
        if(shade >= 0) {
          graphics.setForegroundColor(new TextColor.Indexed(shade));
          graphics.setCharacter(column, row, frame.lines().get(row).charAt(column));
        }
      }
    }
    UiPalette.reset(graphics);
  }

  /** The name and the two bars of each ship: yours on the left, the other on the right. */
  private void drawBars(TextGUIGraphics graphics, int width) {
    drawShipBars(graphics, 1, width / 2, model.youShip(), model.youHull(), model.youShield());
    int reserved = model.opponentShip().length() + 8 + BAR_CELLS + 9 + BAR_CELLS;
    drawShipBars(graphics, Math.max(width / 2 + 1, width - reserved - 1), width - 1, model.opponentShip(),
        model.opponentHull(), model.opponentShield());
  }

  private void drawShipBars(TextGUIGraphics graphics, int x, int maxX, String name, EncounterViewModel.Bar hull,
      EncounterViewModel.Bar shield) {
    int at = UiPalette.draw(graphics, x, 0, name, UiPalette.ACCENT, maxX);
    at = UiPalette.draw(graphics, at, 0, "  casco ", UiPalette.TEXT, maxX);
    at = UiPalette.draw(graphics, at, 0, bar(hull), UiPalette.statusColor(hull.value(), hull.max()), maxX);
    at = UiPalette.draw(graphics, at, 0, "  escudo ", UiPalette.TEXT, maxX);
    UiPalette.draw(graphics, at, 0, bar(shield), UiPalette.statusColor(shield.value(), shield.max()), maxX);
  }

  /** A bar of {@code BAR_CELLS} cells: the filled part stands for the value. */
  private static String bar(EncounterViewModel.Bar bar) {
    int filled = bar.max() <= 0 ? 0 : Math.max(0, Math.min(BAR_CELLS, bar.value() * BAR_CELLS / bar.max()));
    return String.valueOf(BAR_FULL).repeat(filled) + String.valueOf(BAR_EMPTY).repeat(BAR_CELLS - filled);
  }

  /** The log of the encounter, over the stars. */
  private void drawLog(TextGUIGraphics graphics, int width, int height, int logTop) {
    int row = logTop;
    for(String line : log) {
      if(row >= height - 1) {
        break;
      }
      UiPalette.draw(graphics, 1, row++, line, UiPalette.TEXT, width - 1);
    }
  }
}
