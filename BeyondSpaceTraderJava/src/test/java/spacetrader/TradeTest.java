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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.cargo.CargoSellOp;
import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class TradeTest {
  @Test
  void buyingWithoutFreeBaysAlerts() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    Ship ship = game.Commander().getShip();
    ship.Cargo()[0] = ship.CargoBays();

    assertNull(Trade.CargoBuyOffer(game, 0, CargoBuyOp.InPlunder));

    assertTrue(dialogs.alerts().contains(AlertType.CargoNoEmptyBays));
  }

  @Test
  void sellingWithoutCargoAlerts() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);

    assertNull(Trade.CargoSellOffer(game, 0, CargoSellOp.SellSystem));

    assertTrue(dialogs.alerts().contains(AlertType.CargoNoneToSell));
  }

  @Test
  void plunderMovesTheCargoFromTheOpponent() {
    Game game = newGame(new TestDialogService());
    game.getOpponent().Cargo()[0] = 5;

    CargoBuyOffer offer = Trade.CargoBuyOffer(game, 0, CargoBuyOp.InPlunder);
    assertNotNull(offer);
    Trade.CargoBuy(game, offer, 2);

    assertEquals(2, game.Commander().getShip().Cargo()[0]);
    assertEquals(3, game.getOpponent().Cargo()[0]);
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }
}
