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
 * The market: the cargo prices of the current system, what the trader charges
 * for every trade item ({@link #buy()}) and what the trader pays for it
 * ({@link #sell()}). It owns the state and the rules; the game only delegates.
 * The pure price calculation lives in {@link TradeCalculator}.
 */
public final class Market {
  private int[] _priceCargoBuy = new int[Consts.TradeItems.size()];
  private int[] _priceCargoSell = new int[Consts.TradeItems.size()];

  /** The live array of buy prices: what the trader charges the commander per item. */
  public int[] buy() {
    return _priceCargoBuy;
  }

  /** Replaces the buy prices with the ones read from a saved game. */
  public void buy(int[] values) {
    _priceCargoBuy = values;
  }

  /** The live array of sell prices: what the trader pays the commander per item. */
  public int[] sell() {
    return _priceCargoSell;
  }

  /** Replaces the sell prices with the ones read from a saved game. */
  public void sell(int[] values) {
    _priceCargoSell = values;
  }

  /** Calculates the sell prices of the system and derives the buy prices from them. */
  public void calculate(StarSystem system, int policeRecordScore, int traderSkill) {
    _priceCargoSell = TradeCalculator.CalculateSellPrices(system, policeRecordScore);
    recalculateBuyPrices(system, policeRecordScore, traderSkill);
  }

  /** Derives the buy prices from the current sell prices. */
  public void recalculateBuyPrices(StarSystem system, int policeRecordScore, int traderSkill) {
    _priceCargoBuy = TradeCalculator.CalculateBuyPrices(system, _priceCargoSell, policeRecordScore, traderSkill);
  }

  /** Raises the sell prices after the erasure of the police record. */
  public void recalculateSellPrices() {
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      _priceCargoSell[i] = _priceCargoSell[i] * 100 / 90;
    }
  }
}
