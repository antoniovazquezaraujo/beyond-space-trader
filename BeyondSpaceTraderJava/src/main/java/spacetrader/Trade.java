/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.cargo.CargoSellOp;
import org.gts.bst.cargo.TradeItem;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.ports.DialogResult;
import spacetrader.enums.AlertType;


/**
 * Cargo trade offers and operations (system, trader and plunder). The logic is
 * stateless: Game keeps the state and the prices.
 */
public final class Trade {
  private Trade() {
  }

  public static CargoBuyOffer CargoBuyOffer(Game game, int tradeItem, CargoBuyOp op) {
    int freeBays = game.Commander().getShip().FreeCargoBays();
    int[] items = null;
    int unitPrice = 0;
    int cashToSpend = game.Commander().getCash();
    switch(op) {
      case BuySystem:
        freeBays = Math.max(0, game.Commander().getShip().FreeCargoBays() - game.Options().getLeaveEmpty());
        items = game.Commander().CurrentSystem().TradeItems();
        unitPrice = game.PriceCargoBuy()[tradeItem];
        cashToSpend = game.Commander().CashToSpend();
        break;
      case BuyTrader:
        items = game.encounter().getOpponent().Cargo();
        TradeItem item = Consts.TradeItems.get(tradeItem);
        int chance = item.Illegal() ? 45 : 10;
        double adj = Functions.GetRandom(100) < chance ? 1.1 : (item.Illegal() ? 0.8 : 0.9);
        unitPrice = Math.min(item.MaxTradePrice(), Math.max(item.MinTradePrice(), (int)Math.round(game.PriceCargoBuy()[tradeItem] * adj / item.RoundOff()) * item.RoundOff()));
        break;
      case InPlunder:
        items = game.encounter().getOpponent().Cargo();
        break;
    }
    if(op == CargoBuyOp.BuySystem && game.Commander().getDebt() > Consts.DebtTooLarge) {
      game.Dialogs().alert(AlertType.DebtTooLargeTrade);
      return null;
    }
    if(op == CargoBuyOp.BuySystem && (items[tradeItem] <= 0 || unitPrice <= 0)) {
      game.Dialogs().alert(AlertType.CargoNoneAvailable);
      return null;
    }
    if(freeBays == 0) {
      game.Dialogs().alert(AlertType.CargoNoEmptyBays);
      return null;
    }
    if(op != CargoBuyOp.InPlunder && cashToSpend < unitPrice) {
      game.Dialogs().alert(AlertType.CargoIF);
      return null;
    }
    int maxAmount = Math.min(freeBays, items[tradeItem]);
    if(op == CargoBuyOp.BuySystem) {
      maxAmount = Math.min(maxAmount, game.Commander().CashToSpend() / unitPrice);
    }
    return new CargoBuyOffer(tradeItem, op, unitPrice, maxAmount);
  }

  public static void CargoBuy(Game game, CargoBuyOffer offer, int qty) {
    if(qty <= 0) {
      return;
    }
    int tradeItem = offer.tradeItem();
    int[] items = offer.op() == CargoBuyOp.BuySystem ? game.Commander().CurrentSystem().TradeItems() : game.encounter().getOpponent().Cargo();
    int totalPrice = qty * offer.unitPrice();
    game.Commander().getShip().Cargo()[tradeItem] += qty;
    items[tradeItem] -= qty;
    game.Commander().setCash(game.Commander().getCash() - totalPrice);
    game.Commander().PriceCargo()[tradeItem] += totalPrice;
  }

  public static CargoSellOffer CargoSellOffer(Game game, int tradeItem, CargoSellOp op) {
    int qtyInHand = game.Commander().getShip().Cargo()[tradeItem];
    int unitPrice;
    switch(op) {
      case SellSystem:
        unitPrice = game.PriceCargoSell()[tradeItem];
        break;
      case SellTrader:
        TradeItem item = Consts.TradeItems.get(tradeItem);
        int chance = item.Illegal() ? 45 : 10;
        double adj = Functions.GetRandom(100) < chance ? (item.Illegal() ? 0.8 : 0.9) : 1.1;
        unitPrice = Math.min(item.MaxTradePrice(), Math.max(item.MinTradePrice(), (int)Math.round(game.PriceCargoSell()[tradeItem] * adj / item.RoundOff()) * item.RoundOff()));
        break;
      default:
        unitPrice = 0;
        break;
    }
    if(qtyInHand == 0) {
      game.Dialogs().alert(AlertType.CargoNoneToSell, Strings.CargoSellOps.get(op.CastToInt()));
      return null;
    }
    if(op == CargoSellOp.SellSystem && unitPrice <= 0) {
      game.Dialogs().alert(AlertType.CargoNotInterested);
      return null;
    }
    if(op == CargoSellOp.Jettison && !game.encounter().getLitterWarning() && game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreDubious
        && game.Dialogs().alert(AlertType.EncounterDumpWarning) != DialogResult.Yes) {
      return null;
    }
    int unitCost = 0;
    int maxAmount = op == CargoSellOp.SellTrader ? Math.min(qtyInHand, game.encounter().getOpponent().FreeCargoBays()) : qtyInHand;
    if(op == CargoSellOp.Dump) {
      unitCost = 5 * (game.Difficulty().CastToInt() + 1);
      maxAmount = Math.min(maxAmount, game.Commander().CashToSpend() / unitCost);
    }
    int price = unitPrice > 0 ? unitPrice : -unitCost;
    return new CargoSellOffer(tradeItem, op, price, maxAmount);
  }

  public static void CargoSell(Game game, CargoSellOffer offer, int qty) {
    if(qty <= 0) {
      return;
    }
    int tradeItem = offer.tradeItem();
    int qtyInHand = game.Commander().getShip().Cargo()[tradeItem];
    game.Commander().getShip().Cargo()[tradeItem] -= qty;
    game.Commander().PriceCargo()[tradeItem] = (game.Commander().PriceCargo()[tradeItem] * (qtyInHand - qty)) / qtyInHand;
    game.Commander().setCash(game.Commander().getCash() + qty * offer.price());
    if(offer.op() == CargoSellOp.Jettison
        && Functions.GetRandom(10) < game.Difficulty().CastToInt() + 1) {
      if(game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreDubious) {
        game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreDubious);
      } else {
        game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() - 1);
      }
      game.NewsAddEvent(NewsEvent.CaughtLittering);
    }
  }
}
