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
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipCatalog;
import org.gts.bst.view.ShipPicture;
import org.gts.bst.view.ShipSites;
import spacetrader.Strings;
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
  private static final int HIT_FRAMES = 8;
  private static final int DEBRIS_FRAMES = 5;
  private static final char[] DEBRIS = {'*', '\u00b7', '+', 'x', '/', '\\', '|', '-'};
  private static final int[][] SPREAD = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
  private static final int SPARK_FRAMES = 3;
  private static final int NUMBER_FRAMES = 5;
  private static final int BURST_FRAMES = 8;
  private static final int BEAM_FRAMES = 3;
  /** Frames between our beam and the reply of the other ship: the exchange is a turn. */
  private static final int RESPONSE_FRAMES = 5;
  private static final int SCAN_FRAMES = 16;
  private static final int EXTEND_FRAMES = 12;
  private static final int HAUL_FRAMES = 12;
  private static final int RETRACT_FRAMES = 8;
  private static final int TRADE_GAP = 12;
  private static final int OPPONENT_LEAVE_FRAMES = 12;
  /** The ships never touch: they stop this many cells apart (their drawings). */
  private static final int MIN_GAP = 2;
  private static final char[] SPARKLE = {'\\', '|', '/'};
  private static final int ENTER_FRAMES = 14;
  private static final char HORIZONTAL = '─';
  private static final char VERTICAL = '│';
  private static final char JOINT = '┼';
  private static final char BRIDGE = '═';
  private static final char BOX = '■';
  /** Cells the ship glides on every frame: the dashes go at double speed. */
  private static final int GLIDE_SPEED = 3;
  private static final int LEGEND_COLUMNS = 24;

  /** A beam: the shot of a ship, a line of light from its nose to where the game says. */
  private record Beam(int x1, int y1, int x2, int y2, TextColor color, int frames) {
    Beam aged() {
      return new Beam(x1, y1, x2, y2, color, frames - 1);
    }
  }

  /** A piece of a hit ship flying away. */
  private record Debris(int x, int y, int dx, int dy, char glyph, TextColor color, int frames) {
    Debris aged() {
      return new Debris(x + dx, y + dy, dx, dy, glyph, color, frames - 1);
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
  private final List<Debris> debris = new ArrayList<>();
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
  private String scanSpeech;
  private boolean catwalkPending;
  private Catwalk catwalk = Catwalk.NONE;
  private int catwalkFrames;
  private boolean catwalkAuto;
  private boolean leaving;
  private String said = "";
  private boolean dealt;
  private int enterFrames;
  private int youHitFrames;
  private int opponentHitFrames;
  private boolean opponentLeaving;
  private boolean opponentGone;
  private boolean exitedRight;
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
    if(before != null && before.commanderFleeing() != model.commanderFleeing()) {
      // The chase starts (or the fight is taken up again) and the ship turns with it.
      youTurned = model.commanderFleeing();
    }
    if(before != null && model.commanderFleeing() && screenWidth > 0) {
      // Every round of the chase brings the other one closer, always behind us.
      int toward = yourX() > opponentX() ? 1 : -1;
      int gap = toward > 0 ? yourX() - (opponentX() + opponentWidth) : opponentX() - (yourX() + youWidth);
      if(gap > 4) {
        opponentColumn += toward * Math.min(3, gap - 4);
      }
    }
    if(before == null) {
      // The encounter opens with the empty sky: both ships come in from the edges.
      enterFrames = ENTER_FRAMES;
    }
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
   * One press does one thing. Sideways: the ship turns away (the sky sells the
   * retreat) or faces the other one again, and advancing forward glides it on,
   * stopping at a distance; the ship never leaves the scene on its own. Vertically
   * it glides to the top or to the bottom, and a press against the glide brakes it.
   */
  public void move(int dx, int dy) {
    if(model == null || screenWidth <= 0 || enterFrames > 0 || catwalk != Catwalk.NONE) {
      return;
    }
    if(dx != 0 && columnTarget != Integer.MIN_VALUE
        && Integer.signum(columnTarget - youColumn) == -Integer.signum(dx)) {
      // A press against the advance brakes the ship; the next one turns it.
      columnTarget = Integer.MIN_VALUE;
      invalidate();
      return;
    }
    if(dy != 0 && rowTarget != Integer.MIN_VALUE
        && Integer.signum(rowTarget - youRow) == -Integer.signum(dy)) {
      rowTarget = Integer.MIN_VALUE;
      invalidate();
      return;
    }
    int restRow = centreRow(true);
    if(dy < 0) {
      rowTarget = areaTop - restRow;
    } else if(dy > 0) {
      rowTarget = Math.max(areaTop - restRow, areaBottom - youHeight - restRow);
    }
    if(dx < 0) {
      if(youTurned) {
        // Advancing away: the ship glides off, still facing away.
        columnTarget = -youLeft - youWidth - 1;
      } else {
        // The half turn: the ship holds its place and the world moves the other way.
        youTurned = true;
      }
    } else if(dx > 0) {
      if(youTurned) {
        // Facing the other one again.
        youTurned = false;
      } else {
        columnTarget = screenWidth - youLeft + 1;
      }
    }
    invalidate();
  }

  /** True while the ship is turned away (running from the other one). */
  public boolean facingAway() {
    return youTurned;
  }

  /** True when the ship left the scene through its right side (a dodge). */
  public boolean exitedRight() {
    return exitedRight;
  }

  /**
   * The chase goes on after an escape attempt: the scene loops. The ship comes
   * back in through the other edge, still running, and the other one behind it.
   */
  public void wrapAround() {
    boolean runningRight = exitedRight;
    exiting = false;
    youTurned = !runningRight;
    int behind;
    if(runningRight) {
      youColumn = -youLeft - youWidth - 1;
      behind = yourX() - 4 - opponentWidth;
    } else {
      youColumn = screenWidth - youLeft + 1;
      behind = yourX() + youWidth + 4;
    }
    columnTarget = 0;
    opponentColumn = behind - opponentLeft;
    opponentRow = youRow;
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
    // Withdrawing (or being chased after a flee) turns the ship around; the
    // manoeuvres of the player always have the last word on where it points.
    return youTurned ? picture.mirrored() : picture;
  }

  /** The phases of the catwalk of a trade (or of a police seizure). */
  private enum Catwalk {
    NONE, EXTEND, HOLD, HAUL, RETRACT
  }

  /** True when the player ship, moving by (dx, dy), keeps a distance from the other one. */
  private boolean canMove(int dx, int dy) {
    return opponentGone || model.opponentHull().value() <= 0
        || farEnough(yourPicture(), yourX() + dx, yourY() + dy,
            model.opponentPicture().cropped(), opponentX(), opponentY());
  }

  /** True when the other ship, moving by (dx, dy), keeps a distance from the player one. */
  private boolean canOpponentMove(int dx, int dy) {
    return farEnough(model.opponentPicture().cropped(), opponentX() + dx, opponentY() + dy,
        yourPicture(), yourX(), yourY());
  }

  /** True when the drawings of two ships stay apart (they never touch, see MIN_GAP). */
  private static boolean farEnough(ShipPicture ship, int left, int top, ShipPicture other, int otherLeft,
      int otherTop) {
    int gapX = Math.max(left - (otherLeft + other.width()), otherLeft - (left + ship.width()));
    int gapY = Math.max(top - (otherTop + other.height()), otherTop - (top + ship.height()));
    return gapX >= MIN_GAP || gapY >= MIN_GAP;
  }

  /** The frames the other ship takes to react: the better its pilot, the fewer. */
  static int reactionFrames(int pilot) {
    return Math.max(1, 5 - pilot / 2);
  }

  /** The rival: it ignores us, mirrors our height at its own pace, or chases us. */
  private void moveOpponent() {
    if(model == null || model.opponentIgnores() || model.opponentHull().value() <= 0 || model.opponentDisabled()
        || catwalk != Catwalk.NONE || opponentGone || opponentLeaving) {
      return;
    }
    if(model.commanderFleeing()) {
      // The chase: it follows us, keeping a distance, until the game decides.
      int toward = yourX() > opponentX() ? 1 : -1;
      int gap = toward > 0 ? yourX() - (opponentX() + opponentWidth) : opponentX() - (yourX() + youWidth);
      if(frame % 2 == 0 && gap > 4 && canOpponentMove(toward, 0)) {
        // It rushes to us when it is far, and closes in calmly when it is near.
        opponentColumn += toward * Math.min(4, Math.max(1, gap / 10));
      }
      return;
    }
    // The mirror: it copies our height, a cell every so many frames.
    if(opponentRow == youRow || frame % reactionFrames(model.opponentPilot()) != 0) {
      return;
    }
    int step = youRow > opponentRow ? 1 : -1;
    int restRow = centreRow(false);
    int topRow = areaTop - restRow;
    int bottomRow = Math.max(topRow, areaBottom - opponentHeight - restRow);
    int row = Math.max(topRow, Math.min(bottomRow, opponentRow + step));
    if(row != opponentRow && canOpponentMove(0, row - opponentRow)) {
      opponentRow = row;
    }
  }

  private int yourX() {
    return youLeft + youColumn + enteringOffset(true);
  }

  private int yourY() {
    return centreRow(true) + youRow;
  }

  private int opponentX() {
    return opponentLeft + opponentColumn + enteringOffset(false);
  }

  /** While the ships are coming in, they are still beyond their edge. */
  private int enteringOffset(boolean yours) {
    if(enterFrames <= 0 || screenWidth <= 0 || (yours ? youWidth : opponentWidth) <= 0) {
      return 0;
    }
    int off = yours ? -(youLeft + youWidth + 2) : screenWidth - opponentLeft + 2;
    return off * enterFrames / ENTER_FRAMES;
  }

  private int opponentY() {
    return centreRow(false) + opponentRow;
  }

  /** The row where a ship rests with no manoeuvre: the middle of the scene. */
  private int centreRow(boolean yours) {
    int shipHeight = yours ? youHeight : opponentHeight;
    return areaTop + Math.max(0, (areaBottom - areaTop - shipHeight) / 2);
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
      // The camera follows the ship: facing forward the sky drifts left, facing
      // away (turning back, or fleeing) it drags the other way.
      starfield.advance(youTurned);
    }
    if(enterFrames > 0 && screenWidth > 0) {
      enterFrames--;
      invalidate();
      return;
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
    for(int i = debris.size() - 1; i >= 0; i--) {
      Debris piece = debris.get(i);
      if(piece.frames() <= 1) {
        debris.remove(i);
      } else {
        debris.set(i, piece.aged());
      }
    }
    if(youHitFrames > 0) {
      youHitFrames--;
    }
    if(opponentHitFrames > 0) {
      opponentHitFrames--;
    }
    if(opponentLeaving && !opponentGone) {
      // It goes away through its side of the scene, losing us: all the way out.
      int distance = Math.max(1, screenWidth + opponentWidth - opponentX());
      opponentColumn += Math.max(3, distance / OPPONENT_LEAVE_FRAMES);
      if(opponentX() >= screenWidth + 2) {
        opponentGone = true;
        opponentLeaving = false;
      }
    }
    if(responseFrames > 0 && --responseFrames == 0) {
      playTheReply();
    }
    if(scanFrames > 0 && --scanFrames == 0) {
      if(catwalkPending) {
        catwalkPending = false;
        startCatwalk(true);
      } else if(scanSpeech != null) {
        // The police are done: they say how it went.
        say(scanSpeech);
        scanSpeech = null;
      }
    }
    if(catwalk != Catwalk.NONE) {
      // The rival closes in, at a distance, while the catwalk stretches out.
      if(catwalk == Catwalk.EXTEND && model != null && frame % 2 == 0
          && opponentX() - (yourX() + youWidth) > TRADE_GAP && canOpponentMove(-1, 0)) {
        opponentColumn--;
      }
      if(catwalkFrames > 0 && --catwalkFrames == 0) {
        advanceCatwalk();
      }
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
      exitedRight = yourX() > screenWidth;
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
    inspection(confiscated, null);
  }

  /** The police scanner; when it ends, they say the outcome (all clear). */
  public void inspection(boolean confiscated, String afterScan) {
    scanFrames = SCAN_FRAMES;
    catwalkPending = confiscated;
    scanSpeech = afterScan;
    invalidate();
  }

  /** The catwalk of a trade: it goes out between the ships and waits there. */
  public void catwalk() {
    startCatwalk(false);
  }

  /** True when the catwalk is fully out and quiet: the question can be asked. */
  public boolean catwalkOut() {
    return catwalk == Catwalk.HOLD;
  }

  /** The boxes cross the catwalk with the goods of the deal. */
  public void haul() {
    if(catwalk == Catwalk.HOLD) {
      catwalk = Catwalk.HAUL;
      catwalkFrames = HAUL_FRAMES;
      invalidate();
    }
  }

  /** The catwalk is taken back in. */
  public void retract() {
    if(catwalk != Catwalk.NONE) {
      catwalk = Catwalk.RETRACT;
      catwalkFrames = RETRACT_FRAMES;
      invalidate();
    }
  }

  /** True when the catwalk is gone: the scene of the trade is over. */
  public boolean catwalkGone() {
    return catwalk == Catwalk.NONE;
  }

  private void startCatwalk(boolean auto) {
    catwalkAuto = auto;
    catwalk = Catwalk.EXTEND;
    catwalkFrames = EXTEND_FRAMES;
    invalidate();
  }

  /** A phase is over: the catwalk waits, or goes on by itself (the police seizure). */
  private void advanceCatwalk() {
    switch(catwalk) {
      case EXTEND:
        catwalk = catwalkAuto ? Catwalk.HAUL : Catwalk.HOLD;
        catwalkFrames = catwalkAuto ? HAUL_FRAMES : 0;
        break;
      case HAUL:
        catwalk = catwalkAuto ? Catwalk.RETRACT : Catwalk.HOLD;
        catwalkFrames = catwalkAuto ? RETRACT_FRAMES : 0;
        break;
      default:
        catwalk = Catwalk.NONE;
        catwalkFrames = 0;
        break;
    }
    invalidate();
  }

  /** What the other ship is saying now (an offer of the trader), under it. */
  public void say(String text) {
    said = text == null ? "" : text;
    dealt = false;
    invalidate();
  }

  /** The speech has been dealt with: it goes away. */
  public void deal() {
    said = "";
    dealt = true;
    invalidate();
  }

  /** The player takes part while the ships come in: the entry stops at once. */
  public void skipEntry() {
    if(enterFrames > 0) {
      enterFrames = 0;
      invalidate();
    }
  }

  /** The ship goes on facing away (it is running, even if the other falls behind). */
  public void turnAway() {
    youTurned = true;
    invalidate();
  }

  /** The other ship loses us: it goes away through its side of the scene. */
  public void opponentLeaves() {
    if(model != null && !opponentGone && !opponentLeaving) {
      opponentLeaving = true;
      invalidate();
    }
  }

  /** The sky of the scene (the tests follow its drift). */
  Starfield sky() {
    return starfield;
  }

  /** The scene shows a result to read: it stays until the player leaves (intro). */
  public void awaitLeave() {
    leaving = true;
    invalidate();
  }

  /** True while the scene has something to finish (the close has to wait for it). */
  public boolean animating() {
    return scanFrames > 0 || catwalk != Catwalk.NONE || catwalkPending || leaving || opponentLeaving;
  }

  /** The reply of the other ship, played a moment after our shot. */
  private void playTheReply() {
    int nose = opponentX();
    int row = opponentMiddleY();
    int targetX = yourX() + youWidth / 2;
    int targetY = yourMiddleY();
    int toX = pendingOppHit ? targetX : 0;
    int toY = pendingOppHit ? targetY : aimedY(nose, row, targetX, targetY, toX);
    beams.add(new Beam(nose, row, toX, toY, TextColor.ANSI.RED_BRIGHT, BEAM_FRAMES));
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

  /**
   * A beam hit. The shield takes it with a flash of sparks; the hull takes it with a
   * crack: the ship breaks up for a moment and the pieces fly out in every direction.
   */
  private void impact(int shipLeft, int row, int damage, boolean onOpponent) {
    boolean shield = onOpponent ? model.opponentShield().value() > 0 : model.youShield().value() > 0;
    int x = shipLeft + (onOpponent ? opponentWidth : youWidth) / 2;
    if(shield) {
      for(int i = 0; i < 3; i++) {
        flashes.add(new Flash(x - 1 + i, row + i % 2, String.valueOf(SPARK), TextColor.ANSI.CYAN_BRIGHT, SPARK_FRAMES, false));
      }
      if(damage > 0) {
        flashes.add(new Flash(x, row - 2, "-" + damage, TextColor.ANSI.RED_BRIGHT, NUMBER_FRAMES, true));
      }
      return;
    }
    if(onOpponent) {
      opponentHitFrames = HIT_FRAMES;
    } else {
      youHitFrames = HIT_FRAMES;
    }
    TextColor burst = onOpponent ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.RED_BRIGHT;
    for(int i = 0; i < SPREAD.length; i++) {
      int[] direction = SPREAD[i];
      debris.add(new Debris(x, row, direction[0], direction[1], DEBRIS[i % DEBRIS.length],
          i % 3 == 0 ? TextColor.ANSI.WHITE : burst, DEBRIS_FRAMES + i % 3));
    }
    for(int i = 0; i < 3; i++) {
      flashes.add(new Flash(x - 1 + i, row + i % 2, String.valueOf(BURST), TextColor.ANSI.YELLOW_BRIGHT, SPARK_FRAMES, false));
    }
    if(damage > 0) {
      flashes.add(new Flash(x, row - 2, "-" + damage, TextColor.ANSI.RED_BRIGHT, NUMBER_FRAMES, true));
    }
  }

  private void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    int width = size.getColumns();
    int height = size.getRows();
    // The blink of the drawings (the badges and the like) goes with the clock.
    boolean blinkOn = frame / 3 % 2 == 0;
    UiPalette.reset(graphics);
    for(int row = 0; row < height; row++) {
      graphics.putString(0, row, " ".repeat(width));
    }
    drawStars(graphics, width, height);
    if(model == null) {
      return;
    }
    int sceneWidth = Math.max(40, width - LEGEND_COLUMNS);
    drawBars(graphics, sceneWidth);
    int logTop = Math.max(BARS_ROWS + 2, height - LOG_ROWS);
    int half = (sceneWidth - 1) / 2;
    ShipPicture you = yourPicture();
    ShipPicture opponent = model.opponentPicture().cropped();
    if(opponentX() < yourX()) {
      // Behind us: it faces forward, chasing.
      opponent = opponent.mirrored();
    }
    screenWidth = sceneWidth;
    youLeft = shipLeft(0, half, you);
    youWidth = you.width();
    youHeight = you.height();
    opponentLeft = shipLeft(half + 1, sceneWidth - half - 1, opponent);
    opponentWidth = opponent.width();
    opponentHeight = opponent.height();
    areaTop = BARS_ROWS;
    areaBottom = Math.max(BARS_ROWS + 1, logTop - 1);
    boolean down = model.opponentHull().value() <= 0;
    if(youHitFrames > 0) {
      crackedPicture(graphics, yourX(), yourY(), height, you, blinkOn);
    } else {
      EditorText.picture(graphics, yourX(), yourY(), you.width(), height, you, blinkOn);
    }
    if(!down && !opponentGone) {
      if(opponentHitFrames > 0) {
        crackedPicture(graphics, opponentX(), opponentY(), height, opponent, blinkOn);
      } else {
        EditorText.picture(graphics, opponentX(), opponentY(), opponentWidth, height, opponent, blinkOn);
      }
      if(model.opponentDisabled()) {
        drawSmoke(graphics);
      }
    }
    drawFight(graphics);
    drawInspection(graphics);
    drawSpeech(graphics);
    drawLegend(graphics, width, height);
    drawLog(graphics, sceneWidth, height, logTop);
  }

  /** The column of the right: the pieces of both ships, each with its glyph. */
  private void drawLegend(TextGUIGraphics graphics, int width, int height) {
    int column = Math.max(0, width - LEGEND_COLUMNS);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    for(int row = BARS_ROWS; row < height; row++) {
      graphics.putString(column, row, " ".repeat(Math.max(0, width - column)));
    }
    graphics.setForegroundColor(UiPalette.TEXT);
    for(int row = BARS_ROWS; row < height; row++) {
      graphics.setCharacter(column, row, VERTICAL);
    }
    int row = BARS_ROWS + 1;
    graphics.setForegroundColor(UiPalette.TITLE);
    graphics.putString(column + 2, row, Strings.EncounterLegend);
    row += 2;
    row = legendPieces(graphics, column, row, model.youPieces(), model.youShip(), model.youCargoBays());
    legendPieces(graphics, column, row, model.opponentPieces(), model.opponentShip(), model.opponentCargoBays());
    UiPalette.reset(graphics);
  }

  /** The pieces of a ship and its cargo gauge: the glyph of each part and its name. */
  private int legendPieces(TextGUIGraphics graphics, int column, int row, List<String> pieces, String ship,
      int cargoBays) {
    if(pieces.isEmpty() && cargoBays <= 0) {
      return row;
    }
    graphics.setForegroundColor(UiPalette.ACCENT);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    graphics.putString(column + 2, row++, EditorText.cut(ship, LEGEND_COLUMNS - 4));
    for(String name : pieces) {
      if(row >= getSize().getRows() - 1) {
        return row;
      }
      ShipArtFile piece = ShipCatalog.shared().piece(name);
      if(piece == null) {
        continue;
      }
      int codePoint = firstCodePoint(piece);
      if(codePoint < 0) {
        continue;
      }
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      EditorText.glyph(graphics, column + 2, row,
          new ShipPicture.Cell(codePoint, piece.color(), piece.bgColor(), piece.blink()));
      graphics.setForegroundColor(UiPalette.TEXT);
      graphics.putString(column + 6, row, EditorText.cut(name, LEGEND_COLUMNS - 7));
      row++;
    }
    if(cargoBays > 0 && row < getSize().getRows() - 1) {
      // The cargo: one braille dot per bay, as the art of the ships paints it.
      String gauge = ShipSites.gauge(cargoBays);
      for(int i = 0; i < gauge.length() && column + 2 + i < column + 6; i++) {
        EditorText.glyph(graphics, column + 2 + i, row,
            new ShipPicture.Cell(gauge.codePointAt(i), "green", "", false));
      }
      graphics.setForegroundColor(UiPalette.TEXT);
      graphics.putString(column + 6, row, Strings.EncounterLegendCargo);
      row++;
    }
    return row + 1;
  }

  /** The first glyph of the art of a piece: the one the legend shows. */
  private static int firstCodePoint(ShipArtFile piece) {
    for(int row = 0; row < piece.height(); row++) {
      for(int column = 0; column < piece.width(); column++) {
        int codePoint = piece.at(row, column);
        if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
          return codePoint;
        }
      }
    }
    return -1;
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
      // A green cross sweeps the ship: the horizontal line goes down while the
      // vertical one goes across, and they meet in a joint.
      int progress = SCAN_FRAMES - scanFrames;
      int left = Math.max(0, yourX() - 2);
      int right = yourX() + youWidth + 2;
      int top = yourY() - 1;
      int bottom = yourY() + youHeight + 1;
      int row = top + progress * Math.max(1, bottom - top) / SCAN_FRAMES;
      int column = left + progress * Math.max(1, right - left) / SCAN_FRAMES;
      graphics.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
      for(int x = left; x < right; x++) {
        graphics.setCharacter(x, row, HORIZONTAL);
      }
      for(int y = top; y < bottom; y++) {
        graphics.setCharacter(column, y, VERTICAL);
      }
      graphics.setCharacter(column, row, JOINT);
    }
    if(catwalk != Catwalk.NONE) {
      int start = yourX() + youWidth;
      int span = Math.max(0, opponentX() - start);
      int shown = span;
      if(catwalk == Catwalk.EXTEND) {
        shown = span * (EXTEND_FRAMES - catwalkFrames) / EXTEND_FRAMES;
      } else if(catwalk == Catwalk.RETRACT) {
        shown = span * catwalkFrames / RETRACT_FRAMES;
      }
      int row = yourMiddleY();
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      for(int x = 0; x < shown; x++) {
        graphics.setCharacter(start + x, row, BRIDGE);
      }
      if(catwalk == Catwalk.HAUL) {
        // The boxes cross the catwalk, one each way.
        int walked = (HAUL_FRAMES - catwalkFrames) * Math.max(1, span) / HAUL_FRAMES;
        graphics.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
        graphics.setCharacter(start + Math.min(walked, Math.max(0, span - 1)), row, BOX);
        graphics.setCharacter(Math.max(start, opponentX() - Math.min(walked, Math.max(0, span - 1)) - 1), row, BOX);
      }
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
      int centered = opponentX() + opponentWidth / 2 - line.length() / 2;
      // Saying goodbye while leaving: the bubble goes out with the ship, no clamp.
      int left = opponentLeaving ? centered
          : Math.max(0, Math.min(screenWidth - line.length() - 1, centered));
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

  /**
   * The hit ship for a moment: some of its cells turn into flying debris and the
   * whole drawing flashes white on alternate frames, as if it were coming apart.
   */
  private void crackedPicture(TextGUIGraphics graphics, int left, int row, int maxRow, ShipPicture picture,
      boolean blinkOn) {
    boolean flash = frame % 2 == 0;
    for(int y = 0; y < picture.height() && row + y < maxRow; y++) {
      for(int x = 0; x < picture.width(); x++) {
        ShipPicture.Cell cell = picture.at(x, y);
        if(cell == null || cell.continuation()) {
          continue;
        }
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        if((x * 7 + y * 5 + frame) % 4 == 0) {
          graphics.setForegroundColor((x + y + frame) % 2 == 0 ? TextColor.ANSI.WHITE : TextColor.ANSI.RED_BRIGHT);
          graphics.setCharacter(left + x, row + y, DEBRIS[(x + y + frame) % DEBRIS.length]);
        } else if(flash) {
          graphics.setForegroundColor(TextColor.ANSI.WHITE);
          graphics.putString(left + x, row + y, new String(Character.toChars(cell.codePoint())));
        } else {
          EditorText.glyph(graphics, left + x, row + y, cell, blinkOn);
        }
      }
    }
  }

  /** The shots of the round and the flashes. */
  private void drawFight(TextGUIGraphics graphics) {
    for(Beam beam : beams) {
      drawBeam(graphics, beam);
    }
    for(Debris piece : debris) {
      if(piece.x() >= 0 && piece.y() >= 0 && piece.x() < graphics.getSize().getColumns()
          && piece.y() < graphics.getSize().getRows()) {
        graphics.setForegroundColor(piece.color());
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.setCharacter(piece.x(), piece.y(), piece.glyph());
      }
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
