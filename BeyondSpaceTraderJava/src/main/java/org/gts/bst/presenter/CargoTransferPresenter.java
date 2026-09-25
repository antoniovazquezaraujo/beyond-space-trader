/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.cargo.CargoSellOp;
import org.gts.bst.view.CargoTransferView;
import org.gts.bst.view.CargoTransferViewModel;
import spacetrader.Consts;
import spacetrader.Game;
import spacetrader.Ship;


/**
 * Fills the cargo transfer screen (jettison or plunder) and mediates the transfer. No
 * front-end types involved.
 */
public class CargoTransferPresenter {
  public enum Mode {
    Jettison,
    Plunder;
  }

  private final Game game;
  private final Ship ship;
  private final CargoTransferView view;
  private final Mode mode;

  public CargoTransferPresenter(Game game, CargoTransferView view, Mode mode) {
    this.game = game;
    this.ship = game.Commander().getShip();
    this.view = view;
    this.mode = mode;
  }

  public void update() {
    Ship source = mode == Mode.Plunder ? game.getOpponent() : ship;
    List<String> quantities = new ArrayList<>(Consts.TradeItems.size());
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      quantities.add("" + source.Cargo()[i]);
    }
    view.render(new CargoTransferViewModel(quantities, ship.FilledCargoBays() + "/" + ship.CargoBays()));
  }

  /**
   * Transfers the given item: all of it or the amount asked to the view.
   */
  public void transfer(int tradeItem, boolean all) {
    if(mode == Mode.Jettison) {
      CargoSellOffer offer = game.CargoSellOffer(tradeItem, CargoSellOp.Jettison);
      if(offer != null) {
        Integer qty = all ? Integer.valueOf(offer.maxAmount()) : view.askSellQuantity(offer);
        if(qty != null) {
          game.CargoSell(offer, qty);
        }
      }
    } else {
      CargoBuyOffer offer = game.CargoBuyOffer(tradeItem, CargoBuyOp.InPlunder);
      if(offer != null) {
        Integer qty = all ? Integer.valueOf(offer.maxAmount()) : view.askBuyQuantity(offer);
        if(qty != null) {
          game.CargoBuy(offer, qty);
        }
      }
    }
    update();
  }
}
