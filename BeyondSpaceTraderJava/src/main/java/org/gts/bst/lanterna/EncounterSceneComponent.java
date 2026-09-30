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
  private static final char BEAM_DOT = '·';
  private static final char SPARK = '✶';
  private static final char BURST = '✱';
  private static final char[] SMOKE = {'░', '▒', '▓'};
  private static final int SPARK_FRAMES = 3;
  private static final int NUMBER_FRAMES = 5;
  private static final int BURST_FRAMES = 8;
  private static final int BEAM_FRAMES = 3;
  /** Frames between our beam and the reply of the other ship: the exchange is a turn. */
  private static final int RESPONSE_FRAMES = 5;
  private static final int SCAN_FRAMES = 16;
  private static final int CATWALK_FRAMES = 18;
  private static final int APPROACH_FRAMES = 8;
  private static final char[] SPARKLE = {'\\', '|', '/'};
  private static final char WAVE = '·';
  private static final char BRIDGE = '═';
  private static final char BOX = '■';
  /** Cells the ship glides on every frame: the dashes go at double speed. */
  private static final int GLIDE_SPEED = 3;

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
  private int opponentRow;
  private int opponentColumn;
  private boolean youTurned;
  private int rowTarget = Integer.MIN_VALUE;
  private int columnTarget = Integer.MIN_VALUE;
  private boolean exiting;
  private Runnable onExit;
  private int responseFrames;
  private boolean pendingOppHit;
  private int pendingOppDamage;
  private int scanFrames;
  private boolean catwalkPending;
  private int catwalkFrames;
  private boolean leaving;
  private String said = "";
  private boolean dealt;
  /** The geometry of the last paint: where the shots are born and where they land. */
  private int youLeft;
  private int youWidth;
  private int youHeight;
  private int opponentLeft;
  private int opponentWidth;
  private int opponentHeight;
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
    dealt = false;
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
   * One press, the whole manoeuvre: the ship glides on its own to the top, to the
   * bottom or out of the screen (advancing or withdrawing). A wall stops it.
   */
  public void move(int dx, int dy) {
    if(model == null || screenWidth <= 0) {
      return;
    }
    if(dy < 0) {
      rowTarget = 0;
    } else if(dy > 0) {
      rowTarget = Math.max(0, areaBottom - areaTop - youHeight);
    }
    if(dx < 0) {
      // Withdrawing turns the ship around; advancing faces it to the other one again.
      youTurned = true;
      columnTarget = -youLeft - youWidth - 1;
    } else if(dx > 0) {
      youTurned = false;
      columnTarget = screenWidth - youLeft + 1;
    }
    invalidate();
  }

  /** Tells the view when the ship has left the screen (it went past or fled). */
  public void onExit(Runnable exit) {
    this.onExit = exit;
  }

  /** Puts both ships back in their places (a failed flee, or the start of the fight). */
  public void resetPosition() {
    youColumn = 0;
    opponentColumn = 0;
    opponentRow = 0;
    youTurned = false;
    exiting = false;
    rowTarget = Integer.MIN_VALUE;
    columnTarget = Integer.MIN_VALUE;
    invalidate();
  }

  /** The drawing of the player ship, turned around when it is withdrawing. */
  private ShipPicture yourPicture() {
    ShipPicture picture = model.youPicture().cropped();
    return youTurned ? picture.mirrored() : picture;
  }

  /** True when the player ship, moving by (dx, dy), does not run into the other drawing. */
  private boolean canMove(int dx, int dy) {
    return !overlaps(yourPicture(), yourX() + dx, yourY() + dy,
        model.opponentPicture().cropped(), opponentX(), opponentY());
  }

  /** True when the other ship, moving by (dx, dy), does not run into the player drawing. */
  private boolean canOpponentMove(int dx, int dy) {
    return !overlaps(model.opponentPicture().cropped(), opponentX() + dx, opponentY() + dy,
        yourPicture(), yourX(), yourY());
  }

  /** True when the ink of a ship at (left, top) meets the ink of the other one. */
  private static boolean overlaps(ShipPicture ship, int left, int top, ShipPicture other, int otherLeft,
      int otherTop) {
    for(int y = 0; y < ship.height(); y++) {
      for(int x = 0; x < ship.width(); x++) {
        ShipPicture.Cell cell = ship.at(x, y);
        if(cell == null || cell.continuation()) {
          continue;
        }
        int column = left + x - otherLeft;
        int row = top + y - otherTop;
        if(column >= 0 && row >= 0 && column < other.width() && row < other.height()) {
          ShipPicture.Cell there = other.at(column, row);
          if(there != null && !there.continuation()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** The frames the other ship takes to react: the better its pilot, the fewer. */
  static int reactionFrames(int pilot) {
    return Math.max(1, 5 - pilot / 2);
  }

  /** The rival: it ignores us, mirrors our height at its own pace, or chases us. */
  private void moveOpponent() {
    if(model == null || model.opponentIgnores() || model.opponentHull().value() <= 0 || model.opponentDisabled()
        || catwalkFrames > 0) {
      return;
    }
    if(model.commanderFleeing()) {
      // The chase: it follows, keeping a distance, until the game decides.
      if(frame % 2 == 0 && opponentX() - (yourX() + youWidth) > 4 && canOpponentMove(-1, 0)) {
        opponentColumn--;
      }
      return;
    }
    // The mirror: it copies our height, a cell every so many frames.
    if(opponentRow == youRow || frame % reactionFrames(model.opponentPilot()) != 0) {
      return;
    }
    int step = youRow > opponentRow ? 1 : -1;
    int maxRow = Math.max(0, areaBottom - areaTop - opponentHeight);
    int row = Math.max(0, Math.min(maxRow, opponentRow + step));
    if(row != opponentRow && canOpponentMove(0, row - opponentRow)) {
      opponentRow = row;
    }
  }

  private int yourX() {
    return youLeft + youColumn;
  }

  private int yourY() {
    return BARS_ROWS + youRow;
  }

  private int opponentX() {
    return opponentLeft + opponentColumn;
  }

  private int opponentY() {
    return BARS_ROWS + opponentRow;
  }

  private int opponentMiddleY() {
    return opponentY() + Math.max(0, opponentHeight / 2);
  }

  private int yourMiddleY() {
    return yourY() + Math.max(0, youHeight / 2);
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
    if(responseFrames > 0 && --responseFrames == 0) {
      playTheReply();
    }
    if(scanFrames > 0 && --scanFrames == 0 && catwalkPending) {
      catwalkPending = false;
      catwalkFrames = CATWALK_FRAMES;
    }
    if(catwalkFrames > 0) {
      // First the ships come closer; then the catwalk goes out.
      if(catwalkFrames > CATWALK_FRAMES && frame % 2 == 0 && opponentX() - (yourX() + youWidth) > 6) {
        opponentColumn--;
      }
      catwalkFrames--;
    }
    glidePlayer();
    moveOpponent();
    invalidate();
  }

  /** The player ship glides to its target a few cells per frame; a wall stops it. */
  private void glidePlayer() {
    for(int i = 0; i < GLIDE_SPEED && rowTarget != Integer.MIN_VALUE && youRow != rowTarget; i++) {
      int step = rowTarget > youRow ? 1 : -1;
      if(!canMove(0, step)) {
        rowTarget = Integer.MIN_VALUE;
        bump();
        break;
      }
      youRow += step;
    }
    // Advancing (to slip through the gap) and escaping go at double speed.
    for(int i = 0; i < GLIDE_SPEED * 2 && columnTarget != Integer.MIN_VALUE && youColumn != columnTarget; i++) {
      int step = columnTarget > youColumn ? 1 : -1;
      if(!canMove(step, 0)) {
        columnTarget = Integer.MIN_VALUE;
        bump();
        break;
      }
      youColumn += step;
    }
    if(rowTarget != Integer.MIN_VALUE && youRow == rowTarget) {
      rowTarget = Integer.MIN_VALUE;
    }
    boolean out = yourX() > screenWidth || yourX() + youWidth < 0;
    if(out && !exiting) {
      exiting = true;
      if(onExit != null) {
        onExit.run();
      }
    }
  }

  /** A knock against the other ship, so the wall reads as a wall. */
  private void bump() {
    flashes.add(new Flash(opponentX(), yourMiddleY(), "*", TextColor.ANSI.WHITE, 2, false));
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
        flashes.add(new Flash(opponentX() + opponentWidth / 2 - 2 + i, opponentMiddleY() - 1 + i % 2,
            String.valueOf(BURST), i % 2 == 0 ? TextColor.ANSI.YELLOW_BRIGHT : TextColor.ANSI.RED_BRIGHT,
            BURST_FRAMES, false));
      }
      return;
    }
    // My beam: aimed at the other ship, where it stops (a hit) or goes on (a miss).
    if(after.youAttacked()) {
      int nose = yourX() + youWidth;
      int row = yourMiddleY();
      int targetX = opponentX() + opponentWidth / 2;
      int targetY = opponentMiddleY();
      int toX = after.youHit() ? targetX : screenWidth - 1;
      int toY = after.youHit() ? targetY : aimedY(nose, row, targetX, targetY, toX);
      beams.add(new Beam(nose, row, toX, toY, TextColor.ANSI.GREEN_BRIGHT, BEAM_FRAMES));
      if(after.youHit()) {
        impact(opponentX(), targetY, after.youDamage(), true);
      }
    }
    // Their beam is kept for a short lapse, so the two shots do not get mixed up.
    if(!down && !after.opponentDisabled()) {
      pendingOppHit = after.oppHit();
      pendingOppDamage = after.oppDamage();
      responseFrames = RESPONSE_FRAMES;
    } else {
      responseFrames = 0;
    }
  }

  /** True while the other ship is about to answer: attacking again has to wait. */
  public boolean responding() {
    return responseFrames > 0;
  }

  /** The police scanner over our ship, and then the catwalk if they take cargo. */
  public void inspection(boolean confiscated) {
    scanFrames = SCAN_FRAMES;
    catwalkPending = confiscated;
    invalidate();
  }

  /** A catwalk between the two ships: the cargo goes over it. */
  public void catwalk() {
    catwalkFrames = CATWALK_FRAMES + APPROACH_FRAMES;
    invalidate();
  }

  /** What the other ship is saying now (an offer of the trader), under it. */
  public void say(String text) {
    said = text == null ? "" : text;
    invalidate();
  }

  /** The speech has been dealt with: it goes away. */
  public void deal() {
    said = "";
    dealt = true;
    invalidate();
  }

  /** The scene shows a result to read: it stays until the player leaves (intro). */
  public void awaitLeave() {
    leaving = true;
    invalidate();
  }

  /** True while the scene has something to finish (the close has to wait for it). */
  public boolean animating() {
    return scanFrames > 0 || catwalkFrames > 0 || catwalkPending || leaving;
  }

  /** The reply of the other ship, played a moment after our shot. */
  private void playTheReply() {
    int nose = opponentX();
    int row = opponentMiddleY();
    int targetX = yourX() + youWidth / 2;
    int targetY = yourMiddleY();
    int toX = pendingOppHit ? targetX : 0;
    int toY = pendingOppHit ? targetY : aimedY(nose, row, targetX, targetY, toX);
    beams.add(new Beam(nose, row, toX, toY, TextColor.ANSI.CYAN_BRIGHT, BEAM_FRAMES));
    if(pendingOppHit) {
      impact(yourX(), targetY, pendingOppDamage, false);
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
    ShipPicture you = yourPicture();
    ShipPicture opponent = model.opponentPicture().cropped();
    screenWidth = width;
    youLeft = shipLeft(0, half, you);
    youWidth = you.width();
    youHeight = you.height();
    opponentLeft = shipLeft(half + 1, width - half - 1, opponent);
    opponentWidth = opponent.width();
    opponentHeight = opponent.height();
    areaTop = BARS_ROWS;
    areaBottom = Math.max(BARS_ROWS + 1, logTop - 1);
    boolean down = model.opponentHull().value() <= 0;
    EditorText.picture(graphics, youLeft + youColumn, BARS_ROWS + youRow, you.width(), height, you);
    if(!down) {
      EditorText.picture(graphics, opponentX(), opponentY(), opponentWidth, height, opponent);
      if(model.opponentDisabled()) {
        drawSmoke(graphics);
      }
    }
    drawFight(graphics);
    drawInspection(graphics);
    drawSpeech(graphics);
    drawLog(graphics, width, height, logTop);
  }

  /** The smoke of a ship with its systems disabled. */
  private void drawSmoke(TextGUIGraphics graphics) {
    for(int i = 0; i < 3; i++) {
      graphics.setForegroundColor(i % 2 == 0 ? TextColor.ANSI.WHITE : new TextColor.Indexed(240));
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      graphics.setCharacter(opponentX() + i * 2, opponentMiddleY() - 3 - i, SMOKE[(frame / 2 + i) % SMOKE.length]);
    }
    UiPalette.reset(graphics);
  }

  /** Draws a beam from its nose to its end as a line of dots (the aim is kept). */
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
    while(true) {
      graphics.setCharacter(x, y, BEAM_DOT);
      if(x == beam.x2() && y == beam.y2()) {
        break;
      }
      int twice = 2 * error;
      if(twice > -dy) {
        error -= dy;
        x += sx;
      }
      if(twice < dx) {
        error += dx;
        y += sy;
      }
    }
  }

  private static final int SPEECH_WIDTH = 44;

  /** The green waves of the scanner and the catwalk of a transfer. */
  private void drawInspection(TextGUIGraphics graphics) {
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    if(scanFrames > 0) {
      int height = Math.max(1, youHeight + 2);
      int row = yourY() - 1 + (SCAN_FRAMES - scanFrames) * height / SCAN_FRAMES;
      graphics.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
      for(int x = Math.max(0, yourX() - 2); x < yourX() + youWidth + 2; x++) {
        graphics.setCharacter(x, row, WAVE);
      }
      graphics.setCharacter(yourX() - 2, row, BOX);
    }
    if(catwalkFrames > 0 && catwalkFrames <= CATWALK_FRAMES) {
      int row = yourMiddleY();
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      for(int x = yourX() + youWidth; x < opponentX(); x++) {
        graphics.setCharacter(x, row, BRIDGE);
      }
      // The boxes cross the catwalk, one each way.
      int span = Math.max(1, opponentX() - (yourX() + youWidth));
      int walked = (CATWALK_FRAMES - catwalkFrames) * span / CATWALK_FRAMES;
      graphics.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
      graphics.setCharacter(yourX() + youWidth + walked, row, BOX);
      graphics.setCharacter(opponentX() - walked - 1, row, BOX);
    }
    UiPalette.reset(graphics);
  }

  /** What the other ship says: the text floating under it, with no frame. */
  private void drawSpeech(TextGUIGraphics graphics) {
    String speech = dealt ? "" : said.isEmpty() && model != null ? model.speech() : said;
    if(speech.isBlank() || screenWidth <= 0) {
      return;
    }
    java.util.List<String> lines = wrap(speech, SPEECH_WIDTH);
    if(lines.size() > 3) {
      lines = lines.subList(0, 3);
    }
    int row = opponentY() + opponentHeight + 1;
    graphics.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    for(String line : lines) {
      int left = Math.max(0, Math.min(screenWidth - line.length() - 1,
          opponentX() + opponentWidth / 2 - line.length() / 2));
      graphics.putString(left, row++, line);
    }
    UiPalette.reset(graphics);
  }

  /** Cuts a text into lines of a width, at the spaces. */
  private static java.util.List<String> wrap(String text, int width) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    for(String paragraph : text.split("\n")) {
      String rest = paragraph.strip();
      while(rest.length() > width) {
        int cut = rest.lastIndexOf(' ', width);
        if(cut <= 0) {
          cut = width;
        }
        lines.add(rest.substring(0, cut).strip());
        rest = rest.substring(cut).strip();
      }
      if(!rest.isEmpty()) {
        lines.add(rest);
      }
    }
    return lines;
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
    if(leaving) {
      UiPalette.draw(graphics, 1, height - 1, "[ENTER] continue", UiPalette.WARN, width - 1);
    }
  }

  /** The left of a ship picture centred in a column. */
  private static int shipLeft(int left, int width, ShipPicture picture) {
    return left + Math.max(0, (width - picture.width()) / 2);
  }
}
