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

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.presenter.CargoTransferPresenter.Mode;
import org.gts.bst.view.CargoTransferView;
import org.gts.bst.view.CargoTransferViewModel;
import org.gts.bst.view.DialogResult;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.TestDialogService;


class CargoTransferPresenterTest {
  @Test
  void jettisonShowsTheShipQuantities() {
    Game game = newGame(new TestDialogService());
    game.Commander().getShip().Cargo()[0] = 3;
    FakeView view = new FakeView();

    new CargoTransferPresenter(game, view, Mode.Jettison).update();

    assertEquals("3", view.model.quantities().get(0));
    assertEquals("3/15", view.model.bays());
  }

  @Test
  void plunderShowsTheOpponentQuantities() {
    Game game = newGame(new TestDialogService());
    game.getOpponent().Cargo()[0] = 4;
    FakeView view = new FakeView();

    new CargoTransferPresenter(game, view, Mode.Plunder).update();

    assertEquals("4", view.model.quantities().get(0));
  }

  @Test
  void jettisonTransfersEverything() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    game.Commander().getShip().Cargo()[0] = 5;
    game.Commander().PriceCargo()[0] = 50;
    FakeView view = new FakeView();
    CargoTransferPresenter presenter = new CargoTransferPresenter(game, view, Mode.Jettison);
    presenter.update();
    dialogs.setResult(DialogResult.Yes);

    presenter.transfer(0, true);

    assertEquals(0, game.Commander().getShip().Cargo()[0]);
  }

  @Test
  void plunderTransfersTheAskedAmount() {
    Game game = newGame(new TestDialogService());
    game.getOpponent().Cargo()[0] = 4;
    FakeView view = new FakeView();
    view.buyAnswer = 2;
    CargoTransferPresenter presenter = new CargoTransferPresenter(game, view, Mode.Plunder);

    presenter.transfer(0, false);

    assertEquals(2, game.Commander().getShip().Cargo()[0]);
    assertEquals(2, game.getOpponent().Cargo()[0]);
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static class FakeView implements CargoTransferView {
    private CargoTransferViewModel model;
    private Integer sellAnswer;
    private Integer buyAnswer;

    @Override
    public void render(CargoTransferViewModel model) {
      this.model = model;
    }

    @Override
    public Integer askSellQuantity(CargoSellOffer offer) {
      return sellAnswer;
    }

    @Override
    public Integer askBuyQuantity(CargoBuyOffer offer) {
      return buyAnswer;
    }
  }
}
