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
import java.util.Set;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.presenter.CargoTransferPresenter;
import org.gts.bst.view.CargoTransferView;
import org.gts.bst.view.CargoTransferViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Strings;


/**
 * The jettison and plunder screens of the text UI: the cargo quantities and the
 * number keys to move them, driven by {@link CargoTransferPresenter}.
 */
public final class LanternaCargoTransferView implements CargoTransferView {
  private final WindowBasedTextGUI gui;
  private final CargoTransferPresenter presenter;
  private final BasicWindow window;
  private final TextScreenComponent content;
  private CargoTransferViewModel model;

  public LanternaCargoTransferView(WindowBasedTextGUI gui, Game game, CargoTransferPresenter.Mode mode) {
    this.gui = gui;
    this.presenter = new CargoTransferPresenter(game, this, mode);
    this.content = new TextScreenComponent(this::handleKey);
    this.window = new BasicWindow(mode == CargoTransferPresenter.Mode.Plunder
        ? Strings.CargoPlunderTitle : Strings.CargoJettisonTitle);
    window.setHints(Set.of(Window.Hint.MODAL, Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));
    content.setPreferredSize(new TerminalSize(78, 20));
    window.setComponent(content);
    window.setFocusedInteractable(content);
    presenter.update();
  }

  public Window asWindow() {
    return window;
  }

  @Override
  public void render(CargoTransferViewModel model) {
    this.model = model;
    List<String> lines = new ArrayList<>();
    for(int i = 0; i < model.quantities().size(); i++) {
      lines.add(String.format("%2d  %-10s %s", i + 1, Consts.TradeItems.get(i).Name(), model.quantities().get(i)));
    }
    lines.add("");
    String[] bays = model.bays().split("/");
    if(bays.length == 2) {
      lines.add(Functions.StringVars(Strings.MainCargo, bays[0], bays[1]));
    }
    lines.add(Strings.CargoTransferKeys);
    content.lines(lines);
  }

  @Override
  public Integer askSellQuantity(CargoSellOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoSellTitle, item),
        Functions.StringVars(Strings.DialogCargoSellPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public Integer askBuyQuantity(CargoBuyOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoBuyTitle, item),
        Functions.StringVars(Strings.DialogCargoBuyPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  private boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      window.close();
      return true;
    }
    if(model == null || key.getKeyType() != KeyType.Character) {
      return false;
    }
    char character = key.getCharacter();
    int index;
    if(character >= '1' && character <= '9') {
      index = character - '1';
    } else if(character == '0') {
      index = 9;
    } else {
      return false;
    }
    if(index >= model.quantities().size()) {
      return false;
    }
    presenter.transfer(index, key.isShiftDown());
    return true;
  }
}
