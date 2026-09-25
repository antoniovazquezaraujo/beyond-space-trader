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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class CommanderTest {
  @Test
  void tradeShipWithoutEnoughCashShowsAlert() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);

    boolean traded = game.Commander().TradeShip(Consts.ShipSpecs[Consts.MaxShip], 5000);

    assertFalse(traded);
    assertEquals(List.of(AlertType.ShipBuyIF), dialogs.alerts());
  }
}

