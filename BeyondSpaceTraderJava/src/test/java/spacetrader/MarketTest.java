/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;
import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import spacetrader.util.Hashtable;


class MarketTest {
  private static final int CLEAN_RECORD = 0;
  private static final int VILLAIN_RECORD = Consts.PoliceRecordScoreVillain;
  private static final int TRADER_SKILL = 4;

  @Test
  void startsWithoutPrices() {
    Market market = new Market();

    assertEquals(Consts.TradeItems.size(), market.buy().length);
    assertEquals(Consts.TradeItems.size(), market.sell().length);
    for(int price : market.buy()) {
      assertEquals(0, price);
    }
    for(int price : market.sell()) {
      assertEquals(0, price);
    }
  }

  @Test
  void calculatesThePricesOfARealSystemLikeTradeCalculator() throws Exception {
    StarSystem system = system();
    Market market = new Market();
    Random previous = replaceRandom(new Random(42));
    try {
      int[] expectedSell = TradeCalculator.CalculateSellPrices(system, CLEAN_RECORD);
      int[] expectedBuy = TradeCalculator.CalculateBuyPrices(system, expectedSell, CLEAN_RECORD, TRADER_SKILL);

      replaceRandom(new Random(42));
      market.calculate(system, CLEAN_RECORD, TRADER_SKILL);

      assertArrayEquals(expectedSell, market.sell());
      assertArrayEquals(expectedBuy, market.buy());
    } finally {
      replaceRandom(previous);
    }
  }

  @Test
  void theBuyPricesAreAboveTheSellPricesInARealSystem() {
    StarSystem system = system();
    Market market = new Market();

    market.calculate(system, CLEAN_RECORD, TRADER_SKILL);

    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      if(system.ItemTraded(Consts.TradeItems.get(i))) {
        assertTrue(market.buy()[i] > market.sell()[i], "buy " + market.buy()[i] + " <= sell " + market.sell()[i]);
      } else {
        assertEquals(0, market.buy()[i]);
      }
    }
  }

  @Test
  void recalculatesTheBuyPricesLikeTradeCalculator() {
    StarSystem system = system();
    Market market = new Market();
    int[] sellPrices = {10, 250, 0, 350, 250, 0, 650, 900, 3500, 0};
    market.sell(sellPrices);

    market.recalculateBuyPrices(system, VILLAIN_RECORD, TRADER_SKILL);

    assertArrayEquals(TradeCalculator.CalculateBuyPrices(system, sellPrices, VILLAIN_RECORD, TRADER_SKILL), market.buy());
  }

  @Test
  void theFacadeRecalculatesTheBuyPricesLikeTradeCalculator() {
    Game game = newGame();
    StarSystem system = systemTradingSomething(game);
    int[] sellPrices = game.PriceCargoSell();
    for(int i = 0; i < sellPrices.length; i++) {
      sellPrices[i] = sellPrices[i] * 2 + 1;
    }

    game.RecalculateBuyPrices(system);

    assertArrayEquals(
        TradeCalculator.CalculateBuyPrices(system, sellPrices,
            game.Commander().getPoliceRecordScore(), game.Commander().getShip().Trader()),
        game.PriceCargoBuy());
  }

  @Test
  void raisesTheSellPricesAfterErasingThePoliceRecord() {
    Market market = new Market();
    market.sell(new int[] {90, 100, 0, 45, 10, 19, 200, 350, 0, 1000});
    int[] sell = market.sell();

    market.recalculateSellPrices();

    assertSame(sell, market.sell(), "the raise is done in place, as Game did");
    assertArrayEquals(new int[] {100, 111, 0, 50, 11, 21, 222, 388, 0, 1111}, market.sell());
  }

  @Test
  void recalculatingTheBuyPricesReplacesTheLiveArray() {
    Market market = new Market();
    market.sell(new int[] {10, 250, 0, 350, 250, 0, 650, 900, 3500, 0});
    int[] buy = market.buy();
    int[] sell = market.sell();

    market.recalculateBuyPrices(system(), VILLAIN_RECORD, TRADER_SKILL);

    assertNotSame(buy, market.buy(), "the recalculation derives the buy prices, as Game did");
    assertSame(sell, market.sell());
  }

  @Test
  void calculatingReplacesBothLiveArrays() {
    Market market = new Market();
    int[] buy = market.buy();
    int[] sell = market.sell();

    market.calculate(system(), CLEAN_RECORD, TRADER_SKILL);

    assertNotSame(buy, market.buy());
    assertNotSame(sell, market.sell());
  }

  @Test
  void buyAndSellAreTheLiveArrays() {
    Market market = new Market();

    market.buy()[0] = 42;
    market.sell()[1] = 7;

    assertEquals(42, market.buy()[0]);
    assertEquals(7, market.sell()[1]);
  }

  @Test
  void savedPricesSurviveAHashRoundTrip() {
    Market market = new Market();
    market.buy(new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0});
    market.sell(new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0});
    Hashtable save = new Hashtable();

    market.saveTo(save);
    Market loaded = new Market();
    loaded.loadFrom(save);

    assertArrayEquals(new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0}, loaded.buy());
    assertArrayEquals(new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0}, loaded.sell());
  }

  @Test
  void theFacadeExposesTheMarketPrices() {
    Game game = newGame();

    assertArrayEquals(
        TradeCalculator.CalculateBuyPrices(game.Commander().CurrentSystem(), game.PriceCargoSell(),
            game.Commander().getPoliceRecordScore(), game.Commander().getShip().Trader()),
        game.PriceCargoBuy());
  }

  @Test
  void theFacadeRaisesTheSellPricesWhenTheRecordIsErased() {
    Game game = newGame();
    int[] before = game.PriceCargoSell().clone();

    game.RecalculateSellPrices(game.Commander().CurrentSystem());

    for(int i = 0; i < before.length; i++) {
      assertEquals(before[i] * 100 / 90, game.PriceCargoSell()[i]);
    }
  }

  /**
   * Seeds the shared random source of {@link Functions} so the calculation can
   * be contrasted with a direct call to {@link TradeCalculator}; returns the
   * source to restore afterwards.
   */
  private static Random replaceRandom(Random random) throws ReflectiveOperationException {
    Field rand = Functions.class.getDeclaredField("rand");
    rand.setAccessible(true);
    Random previous = (Random)rand.get(null);
    rand.set(null, random);
    return previous;
  }

  private static StarSystem system() {
    return newGame().Commander().CurrentSystem();
  }

  /**
   * The first system of the universe that trades at least one item, so the
   * price assertions cannot pass vacuously.
   */
  private static StarSystem systemTradingSomething(Game game) {
    for(StarSystem system : game.Universe()) {
      for(int i = 0; i < Consts.TradeItems.size(); i++) {
        if(system.ItemTraded(Consts.TradeItems.get(i))) {
          return system;
        }
      }
    }
    throw new AssertionError("the universe must have a system trading something");
  }

  private static Game newGame() {
    return new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }
}
