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

  public LanternaEncounterView(WindowBasedTextGUI gui, Commands commands, Runnable tick, CargoHost cargoHost) {
    this.gui = gui;
    this.commands = commands;
    this.tick = tick;
    this.cargoHost = cargoHost;
    this.content = new EncounterSceneComponent(this::handleKey);
    window.setHints(Set.of(Window.Hint.MODAL, Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));
    content.setPreferredSize(new TerminalSize(120, 30));
    window.setComponent(content);
    window.setFocusedInteractable(content);
  }

  public Window asWindow() {
    return window;
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
        // The game round and the stars of the background move together.
        gui.getGUIThread().invokeLater(() -> {
          tick.run();
          content.tick();
        });
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

  @Override
  public Integer askCargoBuyQuantity(CargoBuyOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoBuyTitle, item),
        Functions.StringVars(Strings.DialogCargoBuyPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public Integer askCargoSellQuantity(CargoSellOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoSellTitle, item),
        Functions.StringVars(Strings.DialogCargoSellPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  private boolean handleKey(KeyStroke key) {
    if(model == null || key.getKeyType() != KeyType.Character) {
      return false;
    }
    EncounterAction action = KEYS.get(Character.toLowerCase(key.getCharacter()));
    if(action == null || !model.actions().contains(action)) {
      return false;
    }
    commands.execute(action);
    return true;
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
