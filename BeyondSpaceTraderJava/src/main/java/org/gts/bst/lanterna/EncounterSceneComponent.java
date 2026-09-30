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
  private static final char SHOT = '•';
  private static final char SPARK = '✶';
  private static final char BURST = '✱';
  private static final char[] SMOKE = {'░', '▒', '▓'};
  private static final int SHOT_SPEED = 6;
  private static final int SPARK_FRAMES = 3;
  private static final int NUMBER_FRAMES = 5;
  private static final int BURST_FRAMES = 8;

  /** A projectile: the game says if it lands on the ship or goes past it. */
  private record Shot(int x, int y, boolean mine, boolean fated, boolean hit, int damage) {
    Shot moved(int next) {
      return new Shot(next, y, mine, fated, hit, damage);
    }

    Shot withFate(boolean hit, int damage) {
      return new Shot(x, y, mine, true, hit, damage);
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
  private final List<Shot> shots = new ArrayList<>();
  private final List<Flash> flashes = new ArrayList<>();
  private EncounterViewModel model;
  private Starfield starfield;
  private int frame;
  private int youRow;
  /** The geometry of the last paint: where the shots are born and where they land. */
  private int youLeft;
  private int youWidth;
  private int youHeight;
  private int youCentre;
  private int opponentLeft;
  private int opponentWidth;
  private int opponentHeight;
  private int opponentCentre;
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

  /** The log lines of the encounter, already wrapped. */
  public void log(List<String> lines) {
    this.log.clear();
    this.log.addAll(lines);
    invalidate();
  }

  /** The player fires: the shot flies towards the other ship. */
  public void youFire() {
    if(model == null || screenWidth <= 0 || model.opponentHull().value() <= 0) {
      return;
    }
    // The shot aims at the middle of the other ship; the part will say where it lands.
    shots.add(new Shot(youLeft + youWidth, opponentCentre, true, false, false, 0));
    invalidate();
  }

  /** Moves the player ship up or down (the arrows). */
  public void move(int delta) {
    youRow = Math.max(-3, Math.min(6, youRow + delta));
    invalidate();
  }

  /** Moves the stars and the fight: the window timer calls it on every frame. */
  public void tick() {
    frame++;
    if(starfield != null) {
      starfield.advance();
    }
    for(int i = shots.size() - 1; i >= 0; i--) {
      Shot shot = shots.get(i);
      int next = shot.x() + (shot.mine() ? SHOT_SPEED : -SHOT_SPEED);
      boolean impact = shot.mine() ? next >= opponentLeft : next <= youLeft + youWidth;
      boolean edge = shot.mine() ? next >= screenWidth - 1 : next <= 1;
      if(shot.hit() && impact) {
        land(shot);
        shots.remove(i);
      } else if(edge) {
        shots.remove(i);
      } else {
        shots.set(i, shot.moved(next));
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

  /** Plays a round with the game already resolved: fates, smoke and explosions. */
  private void play(EncounterViewModel before, EncounterViewModel after) {
    // The fate of the oldest shot of the player still in the air.
    for(int i = 0; i < shots.size(); i++) {
      Shot shot = shots.get(i);
      if(shot.mine() && !shot.fated()) {
        int y = after.youHit() ? opponentCentre : opponentCentre - opponentHeight / 2 - 1;
        shots.set(i, new Shot(shot.x(), y, true, true, after.youHit(), after.youDamage()));
        break;
      }
    }
    boolean down = after.opponentHull().value() <= 0;
    if(down && before.opponentHull().value() > 0) {
      for(int i = 0; i < 4; i++) {
        flashes.add(new Flash(opponentLeft + opponentWidth / 2 - 2 + i, opponentCentre - 1 + i % 2,
            String.valueOf(BURST), i % 2 == 0 ? TextColor.ANSI.YELLOW_BRIGHT : TextColor.ANSI.RED_BRIGHT,
            BURST_FRAMES, false));
      }
      return;
    }
    if(!down && !after.opponentDisabled()) {
      int y = after.oppHit() ? youCentre : youCentre - youHeight / 2 - 1;
      shots.add(new Shot(opponentLeft, y, false, true, after.oppHit(), after.oppDamage()));
    }
  }

  /** The shot landed: sparks (or a shield flash) and the damage number. */
  private void land(Shot shot) {
    boolean shield = shot.mine() ? model.opponentShield().value() > 0 : model.youShield().value() > 0;
    int x = shot.mine() ? opponentLeft + opponentWidth / 2 : youLeft + youWidth / 2;
    int y = shot.mine() ? opponentCentre : youCentre;
    TextColor color = shield ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.YELLOW_BRIGHT;
    for(int i = 0; i < 3; i++) {
      flashes.add(new Flash(x - 1 + i, y + i % 2, String.valueOf(shield ? SPARK : BURST), color, SPARK_FRAMES, false));
    }
    if(shot.damage() > 0) {
      flashes.add(new Flash(x, y - 2, "-" + shot.damage(), TextColor.ANSI.RED_BRIGHT, NUMBER_FRAMES, true));
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
    youCentre = BARS_ROWS + youRow + Math.max(0, you.height() / 2);
    opponentLeft = shipLeft(half + 1, width - half - 1, opponent);
    opponentWidth = opponent.width();
    opponentHeight = opponent.height();
    opponentCentre = BARS_ROWS + Math.max(0, opponent.height() / 2);
    boolean down = model.opponentHull().value() <= 0;
    EditorText.picture(graphics, 0, BARS_ROWS + youRow, half, logTop - 1, you);
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

  /** The projectiles in the air and the flashes of the round. */
  private void drawFight(TextGUIGraphics graphics) {
    for(Shot shot : shots) {
      graphics.setForegroundColor(shot.mine() ? TextColor.ANSI.YELLOW_BRIGHT : TextColor.ANSI.RED_BRIGHT);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      graphics.setCharacter(shot.x(), shot.y(), SHOT);
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
  }

  /** The left of a ship picture centred in a column. */
  private static int shipLeft(int left, int width, ShipPicture picture) {
    return left + Math.max(0, (width - picture.width()) / 2);
  }
}
