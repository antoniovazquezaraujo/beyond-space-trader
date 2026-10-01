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
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterView;
import org.gts.bst.view.EncounterViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Strings;


/**
 * The encounter screen of the text UI: the ships, the texts and the available
 * actions with their keys. The presenter drives it; this view only renders and
 * forwards the keys.
 */
public final class LanternaEncounterView implements EncounterView {
  /**
   * Runs an encounter action (the presenter methods).
   */
  @FunctionalInterface
  public interface Commands {
    void execute(EncounterAction action);
  }

  /**
   * Opens the cargo transfer screen; {@code true} for plunder, {@code false} for
   * jettison.
   */
  @FunctionalInterface
  public interface CargoHost {
    void show(boolean plunder);
  }

  private static final int TICK_MILLIS = 1000;
  private static final int FRAME_MILLIS = 110;
  private static final int CLOSE_MILLIS = 2200;
  private static final int TEXT_WIDTH = 110;
  private static final Map<Character, EncounterAction> KEYS = Map.ofEntries(
      Map.entry('a', EncounterAction.Attack),
      Map.entry('o', EncounterAction.Board),
      Map.entry('b', EncounterAction.Bribe),
      Map.entry('d', EncounterAction.Drink),
      Map.entry('f', EncounterAction.Flee),
      Map.entry('i', EncounterAction.Ignore),
      Map.entry('x', EncounterAction.Interrupt),
      Map.entry('m', EncounterAction.Meet),
      Map.entry('p', EncounterAction.Plunder),
      Map.entry('u', EncounterAction.Submit),
      Map.entry('s', EncounterAction.Surrender),
      Map.entry('t', EncounterAction.Trade),
      Map.entry('y', EncounterAction.Yield));

  private final WindowBasedTextGUI gui;
  private final Commands commands;
  private final Runnable tick;
  private final CargoHost cargoHost;
  private final BasicWindow window = new BasicWindow(Strings.EncounterTitle);
  private final EncounterSceneComponent content;
  private EncounterViewModel model;
  private Timer timer;
  private Timer stars;
  private Runnable onClose;
  private boolean closing;
  private boolean awaitingLeave;

  public LanternaEncounterView(WindowBasedTextGUI gui, Commands commands, Runnable tick, CargoHost cargoHost) {
    this.gui = gui;
    this.commands = commands;
    this.tick = tick;
    this.cargoHost = cargoHost;
    this.content = new EncounterSceneComponent(this::handleKey);
    window.setHints(Set.of(Window.Hint.MODAL, Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
    content.onExit(() -> {
      if(awaitingLeave) {
        // The scene is over: the ship leaves for real.
        awaitingLeave = false;
        closeNow();
        return;
      }
      // Leaving the scene: if it has no interest in us, it loses us and goes;
      // if it chases us, the game decides (a failed attempt loops the scene).
      if(model.opponentIgnores()) {
        // It was not interested in us: it goes away through the other side.
        content.opponentLeaves(!content.exitedRight());
        execute(EncounterAction.Ignore);
      } else {
        execute(EncounterAction.Flee);
      }
    });
    content.setPreferredSize(new TerminalSize(120, 30));
    window.setComponent(content);
    window.setFocusedInteractable(content);
    // The sky moves like the one of the title screen, with its own clock.
    stars = new Timer("encounter-stars", true);
    stars.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          gui.getGUIThread().invokeLater(content::tick);
        } catch(IllegalStateException e) {
          return;
        }
      }
    }, FRAME_MILLIS, FRAME_MILLIS);
  }

  public Window asWindow() {
    return window;
  }

  @Override
  public void escaped() {
    if(!content.exitedRight()) {
      // Running away: the ship goes on facing away and the other loses us, leaving.
      content.turnAway();
      content.opponentLeaves(true);
    }
    // Dodging past it: we are already out; the scene just ends.
  }

  @Override
  public void chaseGoesOn() {
    // A failed escape: the scene loops, both ships coming back in.
    content.wrapAround();
  }

  @Override
  public void inspection(boolean confiscated) {
    // After the scan the ship stays until the player reads the outcome and leaves.
    awaitLeave();
    content.inspection(confiscated, confiscated ? null : Strings.EncounterSaysPoliceAllClear);
  }

  /** The scene is over: it waits for the player to leave (intro, escape or flying away). */
  private void awaitLeave() {
    awaitingLeave = true;
    content.awaitLeave();
  }

  /** A quiet alert of the game: one more line of the log of the scene. */
  public void log(String line) {
    content.addAlert(line);
  }

  /** Tells the window that the encounter is over (to stop sending it log lines). */
  public void onClose(Runnable close) {
    this.onClose = close;
  }

  @Override
  public void render(EncounterViewModel model) {
    this.model = model;
    List<String> lines = new ArrayList<>();
    addWrapped(lines, model.encounterText());
    lines.add("");
    addWrapped(lines, model.actionText());
    content.model(model);
    content.log(lines);
  }

  @Override
  public void close() {
    stopTimer();
    if(awaitingLeave) {
      // The scene waits for the player: the presenter has already set the result.
      return;
    }
    if(content.animating() && !closing) {
      // Let the scene finish (the scanner, the catwalk) before the window goes.
      closing = true;
      java.util.Timer timer = new java.util.Timer("encounter-close", true);
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          gui.getGUIThread().invokeLater(LanternaEncounterView.this::closeNow);
        }
      }, CLOSE_MILLIS);
      return;
    }
    closeNow();
  }

  private void closeNow() {
    if(awaitingLeave) {
      // The scene is waiting for the player: no pending close takes it away.
      return;
    }
    // Never close under a dialog of the encounter (a trade, a question): closing the
    // owner leaves the main screen with no way in. Wait for the dialog to go first.
    if(gui.getActiveWindow() != null && gui.getActiveWindow() != window) {
      java.util.Timer timer = new java.util.Timer("encounter-close", true);
      timer.schedule(new java.util.TimerTask() {
        @Override
        public void run() {
          gui.getGUIThread().invokeLater(LanternaEncounterView.this::closeNow);
        }
      }, CLOSE_MILLIS);
      return;
    }
    if(stars != null) {
      stars.cancel();
      stars = null;
    }
    if(onClose != null) {
      onClose.run();
    }
    window.close();
  }

  @Override
  public void startTimer() {
    if(timer != null) {
      return;
    }
    timer = new Timer("encounter-tick", true);
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        gui.getGUIThread().invokeLater(tick);
      }
    }, TICK_MILLIS, TICK_MILLIS);
  }

  @Override
  public void stopTimer() {
    if(timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  @Override
  public void showJettison() {
    cargoHost.show(false);
  }

  @Override
  public void showPlunder() {
    cargoHost.show(true);
  }

  /** The item name, in lowercase, to fit in the sentence the other ship says. */
  private static String inSpeech(String item) {
    return item == null || item.isEmpty() ? item
        : Character.toLowerCase(item.charAt(0)) + item.substring(1);
  }

  /**
   * The trade as a cutscene: the other ship says the offer, comes closer with the
   * catwalk out, the question is asked at the bottom, the boxes cross if there is a
   * deal and then the catwalk comes back in. The scene waits for the player after it.
   */
  @Override
  public Integer askCargoBuyQuantity(CargoBuyOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    content.say(Functions.StringVars(Strings.EncounterSaysOffer, inSpeech(item), Functions.FormatMoney(offer.unitPrice())));
    content.catwalk();
    playUntil(content::catwalkOut);
    String title = Functions.StringVars(Strings.DialogCargoBuyTitle, item) + "  "
        + Functions.FormatMoney(offer.unitPrice());
    Integer qty = LanternaDialogs.askAmountAtBottom(gui, title,
        Functions.StringVars(Strings.DialogCargoBuyPrompt, "" + offer.maxAmount()), offer.maxAmount());
    if(qty != null && qty > 0) {
      content.haul();
      playUntil(content::catwalkOut);
    }
    content.deal();
    if(qty != null && qty > 0) {
      // The deal is done: the trader thanks, on its ship.
      content.say(Strings.EncounterSaysTradeThanks);
    }
    content.retract();
    playUntil(content::catwalkGone);
    awaitLeave();
    return qty;
  }

  @Override
  public Integer askCargoSellQuantity(CargoSellOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    content.say(Functions.StringVars(Strings.EncounterSaysWanted, inSpeech(item), Functions.FormatMoney(offer.price())));
    content.catwalk();
    playUntil(content::catwalkOut);
    String title = Functions.StringVars(Strings.DialogCargoSellTitle, item) + "  "
        + Functions.FormatMoney(offer.price());
    Integer qty = LanternaDialogs.askAmountAtBottom(gui, title,
        Functions.StringVars(Strings.DialogCargoSellPrompt, "" + offer.maxAmount()), offer.maxAmount());
    if(qty != null && qty > 0) {
      content.haul();
      playUntil(content::catwalkOut);
    }
    content.deal();
    if(qty != null && qty > 0) {
      // The deal is done: the trader thanks, on its ship.
      content.say(Strings.EncounterSaysTradeThanks);
    }
    content.retract();
    playUntil(content::catwalkGone);
    awaitLeave();
    return qty;
  }

  /** Runs the scene by hand while a cutscene of the trade lasts. */
  private void playUntil(java.util.function.BooleanSupplier done) {
    for(int frames = 0; frames < 300 && !done.getAsBoolean(); frames++) {
      content.tick();
      try {
        gui.updateScreen();
        Thread.sleep(FRAME_MILLIS);
      } catch(java.io.IOException e) {
        return;
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  private boolean handleKey(KeyStroke key) {
    if(model == null) {
      return false;
    }
    // The ships come in fast when the player takes part: the keys answer at once.
    content.skipEntry();
    if(awaitingLeave) {
      // The scene is over: the player leaves with intro, escape or flying the ship away.
      if(key.getKeyType() == KeyType.Enter || key.getKeyType() == KeyType.Escape) {
        awaitingLeave = false;
        closeNow();
        return true;
      }
      manoeuvre(key);
      return true;
    }
    if(manoeuvre(key)) {
      return true;
    }
    if(key.getKeyType() == KeyType.Enter) {
      return accept();
    }
    if(key.getKeyType() != KeyType.Character) {
      return false;
    }
    char character = Character.toLowerCase(key.getCharacter());
    if(character == ' ') {
      // The other ship has to answer first: no two of our shots in one exchange.
      if(!content.responding()) {
        commands.execute(EncounterAction.Attack);
      }
      return true;
    }
    EncounterAction action = KEYS.get(character);
    if(action == null || !model.actions().contains(action)) {
      return false;
    }
    execute(action);
    return true;
  }

  /**
   * The half turn, and the advance away: the ship holds its place while the sky
   * moves the other way. Advancing away is an escape attempt: the game decides
   * whether the other one follows (it closes in) or loses us (it leaves the scene).
   */
  private void advanceAway() {
    // The first press turns the ship where it stands; the next one sends it away
    // (the stars sell the retreat). Leaving the scene asks the game for the escape.
    content.move(-1, 0);
  }

  /** The manoeuvres: the arrows and the vim keys move the ship. */
  private boolean manoeuvre(KeyStroke key) {
    switch(key.getKeyType()) {
      case ArrowUp:
        content.move(0, -1);
        return true;
      case ArrowDown:
        content.move(0, 1);
        return true;
      case ArrowLeft:
        advanceAway();
        return true;
      case ArrowRight:
        content.move(1, 0);
        return true;
      case Character:
        char character = Character.toLowerCase(key.getCharacter());
        if(character == 'k') {
          content.move(0, -1);
          return true;
        }
        if(character == 'j') {
          content.move(0, 1);
          return true;
        }
        if(character == 'h') {
          advanceAway();
          return true;
        }
        if(character == 'l') {
          content.move(1, 0);
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  /**
   * The intro key: the natural action of the encounter (deal with the trader,
   * allow the search, meet the captain, take the drink, board a disabled ship) or,
   * if there is none, giving up (surrender, submit or yield the cargo).
   */
  private boolean accept() {
    for(EncounterAction action : java.util.List.of(EncounterAction.Trade, EncounterAction.Submit,
        EncounterAction.Meet, EncounterAction.Drink, EncounterAction.Board, EncounterAction.Surrender,
        EncounterAction.Yield)) {
      if(model.actions().contains(action)) {
        // The speech has been dealt with: it goes away and the scene plays.
        content.deal();
        execute(action);
        return true;
      }
    }
    return true;
  }

  /**
   * Runs an action of the encounter. The trade ends with the scene waiting for the
   * player (they leave with intro, escape or flying the ship away), so the window
   * must not go when the presenter closes it after the deal.
   */
  private void execute(EncounterAction action) {
    if(action == EncounterAction.Trade) {
      awaitingLeave = true;
    }
    commands.execute(action);
  }

  private static void addWrapped(List<String> lines, String text) {
    if(text == null) {
      return;
    }
    for(String paragraph : text.split("\n", -1)) {
      String rest = paragraph;
      while(rest.length() > TEXT_WIDTH) {
        int cut = rest.lastIndexOf(' ', TEXT_WIDTH);
        if(cut <= 0) {
          cut = TEXT_WIDTH;
        }
        lines.add(rest.substring(0, cut));
        rest = rest.substring(cut).trim();
      }
      lines.add(rest);
    }
  }
}
