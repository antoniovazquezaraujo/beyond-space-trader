/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;


class TradeCalculatorTest {
  @Test
  void calculatesTheSellPrices() {
    int[] sellPrices = TradeCalculator.CalculateSellPrices(system(), 0);

    assertEquals(Consts.TradeItems.size(), sellPrices.length);
    for(int price : sellPrices) {
      assertTrue(price >= 0, "negative price: " + price);
    }
  }

  @Test
  void theBuyPriceIsAboveTheSellPriceForTradedItems() {
    StarSystem system = system();
    int[] sellPrices = TradeCalculator.CalculateSellPrices(system, 0);

    int[] buyPrices = TradeCalculator.CalculateBuyPrices(system, sellPrices, 0, 5);

    assertEquals(Consts.TradeItems.size(), buyPrices.length);
    for(int i = 0; i < buyPrices.length; i++) {
      if(system.ItemTraded(Consts.TradeItems.get(i))) {
        assertTrue(buyPrices[i] > sellPrices[i], "buy " + buyPrices[i] + " <= sell " + sellPrices[i]);
      } else {
        assertEquals(0, buyPrices[i]);
      }
    }
  }

  private static StarSystem system() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService()).Commander().CurrentSystem();
  }
}
