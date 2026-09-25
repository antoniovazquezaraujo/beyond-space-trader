/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;


/**
 * Price calculation for the trade items of a system: the sell price (what the
 * trader pays for the item) and the buy price (what the trader charges).
 */
public final class TradeCalculator {
  private TradeCalculator() {
  }

  /**
   * The sell price of every trade item; 0 when the item is not traded.
   */
  public static int[] CalculateSellPrices(StarSystem system, int policeRecordScore) {
    int[] sellPrices = new int[Consts.TradeItems.size()];
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      int price = Consts.TradeItems.get(i).StandardPrice(system);
      if(price > 0) {
        // In case of a special status, adapt price accordingly
        if(Consts.TradeItems.get(i).PressurePriceHike() == system.SystemPressure()) {
          price = price * 3 / 2;
        }
        // Randomize price a bit
        int variance = Math.min(Consts.TradeItems.get(i).PriceVariance(), price - 1);
        price += Functions.GetRandom(-variance, variance + 1);
        // Criminals have to pay off an intermediary
        if(policeRecordScore < Consts.PoliceRecordScoreDubious) {
          price = price * 90 / 100;
        }
      }
      sellPrices[i] = price;
    }
    return sellPrices;
  }

  /**
   * The buy price of every trade item; 0 when the item is not traded there.
   */
  public static int[] CalculateBuyPrices(StarSystem system, int[] sellPrices, int policeRecordScore, int traderSkill) {
    int[] buyPrices = new int[Consts.TradeItems.size()];
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      if(!system.ItemTraded(Consts.TradeItems.get(i))) {
        buyPrices[i] = 0;
      } else {
        buyPrices[i] = sellPrices[i];
        if(policeRecordScore < Consts.PoliceRecordScoreDubious) {
          buyPrices[i] = buyPrices[i] * 100 / 90;
        }
        // BuyPrice = SellPrice + 1 to 12% (depending on trader skill (minimum is 1, max 12))
        buyPrices[i] = buyPrices[i] * (103 + Consts.MaxSkill - traderSkill) / 100;
        if(buyPrices[i] <= sellPrices[i]) {
          buyPrices[i] = sellPrices[i] + 1;
        }
      }
    }
    return buyPrices;
  }
}
