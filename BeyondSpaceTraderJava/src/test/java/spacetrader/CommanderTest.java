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
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class CommanderTest {
  @Test
  void tradeShipWithoutEnoughCashShowsAlert() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);

    boolean traded = game.Commander().TradeShip(Consts.ShipSpecs.get(Consts.MaxShip), 5000);

    assertFalse(traded);
    assertEquals(List.of(AlertType.ShipBuyIF), dialogs.alerts());
  }

  @Test
  void tradeShipToAShipWithoutQuartersShowsTheCrewAlert() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    game.Commander().setCash(1000000);

    for(ShipType type : List.of(ShipType.Bottle, ShipType.Custom)) {
      assertFalse(game.Commander().TradeShip(Consts.ShipSpecs.get(type.CastToInt()), 0),
          type + " has no quarters for the commander");
    }

    assertEquals(List.of(AlertType.ShipBuyCrewQuarters, AlertType.ShipBuyCrewQuarters), dialogs.alerts(),
        "the commander is never named as a passenger");
    assertEquals(ShipType.Gnat, game.Commander().getShip().Type(), "the ship in flight never changed");
  }

  @Test
  void tradeShipWithPassengersStillNamesThem() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    game.Commander().setCash(1000000);
    Ship beetle = new Ship(ShipType.Beetle);
    beetle.Crew()[0] = game.Commander();
    beetle.Crew()[1] = game.Mercenaries()[CrewMemberId.Jarek.CastToInt()];
    game.Commander().setShip(beetle);

    assertFalse(game.Commander().TradeShip(Consts.ShipSpecs.get(ShipType.Bottle.CastToInt()), 0));

    assertEquals(List.of(AlertType.ShipBuyPassengerQuarters), dialogs.alerts(),
        "the passenger branch still warns when there is a real passenger");
    assertEquals(ShipType.Beetle, game.Commander().getShip().Type(), "the ship in flight never changed");
  }
}
