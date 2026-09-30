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
import org.gts.bst.view.ShipPicture;
import org.gts.bst.view.Starfield;


/**
 * The encounter scene: both ships facing each other over a moving starfield,
 * with their hull and shield bars above, the log under them and the fight over
 * the drawing (projectiles, sparks, damage numbers, smoke and explosions).
 *
 * <p>The game is the referee: the presenter fills the model (one round at a
 * time) and this component only plays it. The shot of the player is born when
 * it fires and learns its fate on the next round; the shot of the other ship is
 * born with the part of the round already decided.
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
  private static final char BEAM = '─';
  private static final char SPARK = '✶';
  private static final char BURST = '✱';
  private static final char[] SMOKE = {'░', '▒', '▓'};
  private static final int SPARK_FRAMES = 3;
  private static final int NUMBER_FRAMES = 5;
  private static final int BURST_FRAMES = 8;
  private static final int BEAM_FRAMES = 3;

  /** A beam: the shot of a ship, a line of light from its nose to where the game says. */
  private record Beam(int x1, int y1, int x2, int y2, TextColor color, int frames) {
    Beam aged() {
      return new Beam(x1, y1, x2, y2, color, frames - 1);
    }
  }

  /** A flash of the fight: sparks, bursts, a damage number or a puff of smoke. */
  private record Flash(int x, int y, String text, TextColor color, int frames, boolean rises) {
    Flash aged() {
      return new Flash(x, y + (rises ? -1 : 0), text, color, frames - 1, rises);
    }
  }

  private final KeyHandler keyHandler;
  private final List<String> log = new ArrayList<>();
  private final List<String> alerts = new ArrayList<>();
  private final List<Beam> beams = new ArrayList<>();
  private final List<Flash> flashes = new ArrayList<>();
  private EncounterViewModel model;
  private Starfield starfield;
  private int frame;
  private int youRow;
  private int youColumn;
  /** The geometry of the last paint: where the shots are born and where they land. */
  private int youLeft;
  private int youWidth;
  private int youHeight;
  private int opponentLeft;
  private int opponentWidth;
  private int opponentHeight;
  private int opponentCentre;
  private int areaTop;
  private int areaBottom;
  private int screenWidth;

  public EncounterSceneComponent(KeyHandler keyHandler) {
    this.keyHandler = keyHandler;
  }

  /** The scene model: both ships, their pictures, their bars and the round. */
  public void model(EncounterViewModel model) {
    EncounterViewModel before = this.model;
    this.model = model;
    if(before != null && before.round() != model.round() && screenWidth > 0) {
      play(before, model);
    }
    invalidate();
  }

  /** A quiet alert of the game (an outcome, no question): one more line of the log. */
  public void addAlert(String line) {
    alerts.add(line);
    invalidate();
  }

  /** The log lines of the encounter, already wrapped. */
  public void log(List<String> lines) {
    this.log.clear();
    this.log.addAll(lines);
    invalidate();
  }

  /**
   * Moves the player ship: up and down to look for a gap, right to go through it
   * and left to withdraw. Returns true when the ship has just left the screen to
   * the right (it has gone past the other ship) or to the left (it has fled).
   */
  public boolean move(int dx, int dy) {
    if(model == null || screenWidth <= 0) {
      return false;
    }
    if(dy != 0) {
      youRow = Math.max(0, Math.min(areaBottom - areaTop - youHeight, youRow + dy));
    }
    if(dx != 0 && canMove(dx)) {
      youColumn += dx;
    }
    invalidate();
    return youLeft + youColumn > screenWidth || youLeft + youColumn + youWidth < 0;
  }

  /** Puts the ship back in its half (a failed flee, or the start of the fight). */
  public void resetPosition() {
    youColumn = 0;
    invalidate();
  }

  /** True when moving the ship sideways does not run into the drawing of the other one. */
  private boolean canMove(int dx) {
    ShipPicture mine = model.youPicture().cropped();
    ShipPicture other = model.opponentPicture().cropped();
    int myLeft = youLeft + youColumn + dx;
    int myTop = BARS_ROWS + youRow;
    for(int y = 0; y < mine.height(); y++) {
      for(int x = 0; x < mine.width(); x++) {
        ShipPicture.Cell cell = mine.at(x, y);
        if(cell == null || cell.continuation()) {
          continue;
        }
        int column = myLeft + x - opponentLeft;
        int row = myTop + y - BARS_ROWS;
        if(column >= 0 && row >= 0 && column < other.width() && row < other.height()) {
          ShipPicture.Cell there = other.at(column, row);
          if(there != null && !there.continuation()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /** Moves the stars and the fight: the window timer calls it on every frame. */
  public void tick() {
    frame++;
    if(starfield != null) {
      starfield.advance();
    }
    for(int i = beams.size() - 1; i >= 0; i--) {
      Beam beam = beams.get(i);
      if(beam.frames() <= 1) {
        beams.remove(i);
      } else {
        beams.set(i, beam.aged());
      }
    }
    for(int i = flashes.size() - 1; i >= 0; i--) {
      Flash flash = flashes.get(i);
      if(flash.frames() <= 1) {
        flashes.remove(i);
      } else {
        flashes.set(i, flash.aged());
      }
    }
    invalidate();
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

  /** Plays a round with the game already resolved: beams, smoke and explosions. */
  private void play(EncounterViewModel before, EncounterViewModel after) {
    boolean down = after.opponentHull().value() <= 0;
    if(down && before.opponentHull().value() > 0) {
      for(int i = 0; i < 4; i++) {
        flashes.add(new Flash(opponentLeft + opponentWidth / 2 - 2 + i, opponentCentre - 1 + i % 2,
            String.valueOf(BURST), i % 2 == 0 ? TextColor.ANSI.YELLOW_BRIGHT : TextColor.ANSI.RED_BRIGHT,
            BURST_FRAMES, false));
      }
      return;
    }
    // My beam: aimed at the other ship, where it stops (a hit) or goes on (a miss).
    if(after.youAttacked()) {
      int nose = youLeft + youColumn + youWidth;
      int row = BARS_ROWS + youRow + Math.max(0, youHeight / 2);
      int targetX = opponentLeft + opponentWidth / 2;
      int targetY = opponentCentre;
      int toX = after.youHit() ? targetX : screenWidth - 1;
      int toY = after.youHit() ? targetY : aimedY(nose, row, targetX, targetY, toX);
      beams.add(new Beam(nose, row, toX, toY, TextColor.ANSI.YELLOW_BRIGHT, BEAM_FRAMES));
      if(after.youHit()) {
        impact(opponentLeft, targetY, after.youDamage(), true);
      }
    }
    // Their beam: the same, the other way around.
    if(!down && !after.opponentDisabled()) {
      int nose = opponentLeft;
      int row = BARS_ROWS + Math.max(0, opponentHeight / 2);
      int targetX = youLeft + youColumn + youWidth / 2;
      int targetY = BARS_ROWS + youRow + Math.max(0, youHeight / 2);
      int toX = after.oppHit() ? targetX : 0;
      int toY = after.oppHit() ? targetY : aimedY(nose, row, targetX, targetY, toX);
      beams.add(new Beam(nose, row, toX, toY, TextColor.ANSI.RED_BRIGHT, BEAM_FRAMES));
      if(after.oppHit()) {
        impact(youLeft + youColumn, targetY, after.oppDamage(), false);
      }
    }
  }

  /** The y of the line from (x1,y1) to (x2,y2) when it reaches {@code x}: the aim is kept. */
  private static int aimedY(int x1, int y1, int x2, int y2, int x) {
    if(x2 == x1) {
      return y1;
    }
    return (int)Math.round(y1 + (double)(y2 - y1) * (x - x1) / (x2 - x1));
  }

  /** A beam hit: sparks (or a shield flash) and the damage number. */
  private void impact(int shipLeft, int row, int damage, boolean onOpponent) {
    boolean shield = onOpponent ? model.opponentShield().value() > 0 : model.youShield().value() > 0;
    int x = shipLeft + (onOpponent ? opponentWidth : youWidth) / 2;
    TextColor color = shield ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.YELLOW_BRIGHT;
    for(int i = 0; i < 3; i++) {
      flashes.add(new Flash(x - 1 + i, row + i % 2, String.valueOf(shield ? SPARK : BURST), color, SPARK_FRAMES, false));
    }
    if(damage > 0) {
      flashes.add(new Flash(x, row - 2, "-" + damage, TextColor.ANSI.RED_BRIGHT, NUMBER_FRAMES, true));
    }
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
    int logTop = Math.max(BARS_ROWS + 2, height - LOG_ROWS);
    int half = (width - 1) / 2;
    ShipPicture you = model.youPicture().cropped();
    ShipPicture opponent = model.opponentPicture().cropped();
    screenWidth = width;
    youLeft = shipLeft(0, half, you);
    youWidth = you.width();
    youHeight = you.height();
    opponentLeft = shipLeft(half + 1, width - half - 1, opponent);
    opponentWidth = opponent.width();
    opponentHeight = opponent.height();
    opponentCentre = BARS_ROWS + Math.max(0, opponent.height() / 2);
    areaTop = BARS_ROWS;
    areaBottom = Math.max(BARS_ROWS + 1, logTop - 1);
    boolean down = model.opponentHull().value() <= 0;
    EditorText.picture(graphics, youLeft + youColumn, BARS_ROWS + youRow, you.width(), height, you);
    if(!down) {
      EditorText.picture(graphics, half + 1, BARS_ROWS, width - half - 1, logTop - 1, opponent);
      if(model.opponentDisabled()) {
        drawSmoke(graphics);
      }
    }
    drawFight(graphics);
    drawLog(graphics, width, height, logTop);
  }

  /** The smoke of a ship with its systems disabled. */
  private void drawSmoke(TextGUIGraphics graphics) {
    for(int i = 0; i < 3; i++) {
      graphics.setForegroundColor(i % 2 == 0 ? TextColor.ANSI.WHITE : new TextColor.Indexed(240));
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      graphics.setCharacter(opponentLeft + i * 2, opponentCentre - 3 - i, SMOKE[(frame / 2 + i) % SMOKE.length]);
    }
    UiPalette.reset(graphics);
  }

  /** Draws a beam from its nose to its end, as a line (─, ╱ or ╲). */
  private static void drawBeam(TextGUIGraphics graphics, Beam beam) {
    graphics.setForegroundColor(beam.color());
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    int x = beam.x1();
    int y = beam.y1();
    int dx = Math.abs(beam.x2() - x);
    int dy = Math.abs(beam.y2() - y);
    int sx = x <= beam.x2() ? 1 : -1;
    int sy = y <= beam.y2() ? 1 : -1;
    int error = dx - dy;
    char glyph = beam.y1() == beam.y2() ? BEAM : sy > 0 ? '╲' : '╱';
    while(true) {
      graphics.setCharacter(x, y, glyph);
      if(x == beam.x2() && y == beam.y2()) {
        break;
      }
      int twice = 2 * error;
      int stepY = 0;
      if(twice > -dy) {
        error -= dy;
        x += sx;
      }
      if(twice < dx) {
        error += dx;
        y += sy;
        stepY = sy;
      }
      glyph = stepY == 0 ? BEAM : stepY > 0 ? '╲' : '╱';
    }
  }

  /** The shots of the round and the flashes. */
  private void drawFight(TextGUIGraphics graphics) {
    for(Beam beam : beams) {
      drawBeam(graphics, beam);
    }
    for(Flash flash : flashes) {
      graphics.setForegroundColor(flash.color());
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      graphics.putString(flash.x(), flash.y(), flash.text());
    }
    UiPalette.reset(graphics);
  }

  /** The parallax starfield of the title screen, as the backdrop of the fight. */
  private void drawStars(TextGUIGraphics graphics, int width, int height) {
    int columns = Math.max(1, width - 1);
    int rows = Math.max(1, height - 1);
    if(starfield == null || starfield.dotWidth() != columns * 2 || starfield.dotHeight() != rows * 4) {
      starfield = new Starfield(columns, rows, 0.12, 42);
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
    for(String alert : alerts) {
      if(row >= height - 1) {
        break;
      }
      UiPalette.draw(graphics, 1, row++, alert, UiPalette.ACCENT, width - 1);
    }
  }

  /** The left of a ship picture centred in a column. */
  private static int shipLeft(int left, int width, ShipPicture picture) {
    return left + Math.max(0, (width - picture.width()) / 2);
  }
}
