/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ports.DialogService;
import org.gts.bst.view.ShipView;
import org.gts.bst.view.ShipViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.Strings;


class ShipPresenterTest {
  private static final String NL = Strings.newline;

  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheStartingShip() {
    RecordingView view = new RecordingView();

    new ShipPresenter(newGame(), view).update();

    assertEquals("Gnat", view.model.type());
    assertEquals("Equipment:" + NL + NL + "Unfilled:", view.model.equipmentLabels());
    assertEquals("1 Pulse Laser" + NL + NL + "1 gadget slot" + NL, view.model.equipmentValues());
    assertEquals("No special items.", view.model.specialCargo());
  }

  @Test
  void listsTheHoldWithTheAveragePurchasePrice() {
    Game game = newGame();
    game.Commander().getShip().Cargo()[0] = 3;
    game.Commander().PriceCargo()[0] = 75;
    game.Commander().getShip().Cargo()[2] = 2;
    game.Commander().PriceCargo()[2] = 201;
    game.Commander().PriceCargo()[3] = 100; // purchased and sold again: no units left
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    assertEquals(List.of(
        "Water       3   bought at 25 cr.",
        "Food        2   bought at 100 cr."), view.model.cargo());
  }

  @Test
  void marksTheCargoWithoutARecordedCost() {
    Game game = newGame();
    game.Commander().getShip().Cargo()[1] = 2;
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    assertEquals(List.of("Furs        2   —"), view.model.cargo());
  }

  @Test
  void listsNoCargoWhenTheHoldIsEmpty() {
    RecordingView view = new RecordingView();

    new ShipPresenter(newGame(), view).update();

    assertEquals(List.of(), view.model.cargo());
  }

  @Test
  void ordersAndAlignsTheProductsWithTheirAveragePrice() {
    Game game = newGame();
    game.Commander().getShip().Cargo()[0] = 1;
    game.Commander().PriceCargo()[0] = 3;
    game.Commander().getShip().Cargo()[4] = 12;
    game.Commander().PriceCargo()[4] = 600;
    game.Commander().getShip().Cargo()[8] = 10;
    game.Commander().PriceCargo()[8] = 35000;
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    assertEquals(List.of(
        "Water       1   bought at 3 cr.",
        "Games      12   bought at 50 cr.",
        "Narcotics  10   bought at 3,500 cr."), view.model.cargo());
  }

  @Test
  void mixesBoughtAndLootedCargoInOrder() {
    Game game = newGame();
    game.Commander().getShip().Cargo()[0] = 2; // plundered: no recorded cost
    game.Commander().getShip().Cargo()[1] = 3;
    game.Commander().PriceCargo()[1] = 750;
    game.Commander().getShip().Cargo()[6] = 4;
    game.Commander().PriceCargo()[6] = 100;
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    assertEquals(List.of(
        "Water       2   —",
        "Furs        3   bought at 250 cr.",
        "Medicine    4   bought at 25 cr."), view.model.cargo());
  }

  @Test
  void showsSpecialCargo() {
    Game game = newGame();
    game.Commander().getShip().setTribbles(5);
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    assertEquals("5 cute, furry tribbles.", view.model.specialCargo());
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
  }

  private static class RecordingView implements ShipView {
    private ShipViewModel model;

    @Override
    public void render(ShipViewModel model) {
      this.model = model;
    }
  }
}

