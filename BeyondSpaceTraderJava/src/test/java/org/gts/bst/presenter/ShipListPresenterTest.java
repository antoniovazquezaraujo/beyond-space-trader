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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListView;
import org.gts.bst.view.ShipListViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.ShipSpec;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.TestDialogService;


class ShipListPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheShipRows() {
    Game game = newGame(new TestDialogService());
    moveToSystem(game, true);
    Ship ship = game.Commander().getShip();
    FakeView view = new FakeView();

    new ShipListPresenter(game, view).update();

    assertEquals(Consts.ShipSpecs.length, view.model.rows().size());
    ShipListViewModel.Row owned = view.model.rows().get(ship.Type().CastToInt());
    assertEquals(Strings.ShipBuyGotOne, owned.price());
    assertFalse(owned.buyVisible());
    ShipSpec flea = Consts.ShipSpecs[ShipType.Flea.CastToInt()];
    ShipListViewModel.Row fleaRow = view.model.rows().get(ShipType.Flea.CastToInt());
    assertEquals(flea.Name(), fleaRow.name());
    assertTrue(fleaRow.buyVisible());
    assertEquals(Functions.FormatMoney(flea.getPrice() - ship.Worth(false)), fleaRow.price());
  }

  @Test
  void marksShipsAsNotSoldInLowTechSystems() {
    Game game = newGame(new TestDialogService());
    moveToSystem(game, false);
    FakeView view = new FakeView();

    new ShipListPresenter(game, view).update();

    assertTrue(view.model.rows().stream().anyMatch(row -> "not sold".equals(row.price())));
  }

  @Test
  void selectsTheShipInformation() {
    Game game = newGame(new TestDialogService());
    moveToSystem(game, true);
    ShipSpec flea = Consts.ShipSpecs[ShipType.Flea.CastToInt()];
    FakeView view = new FakeView();
    ShipListPresenter presenter = new ShipListPresenter(game, view);

    presenter.select(ShipType.Flea.CastToInt());

    assertEquals(flea.Name(), view.info.name());
    assertEquals(Functions.FormatNumber(flea.CargoBays()), view.info.bays());
    assertEquals(flea.ImageIndex(), view.info.imageIndex());
  }

  @Test
  void buysAShip() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    moveToSystem(game, true);
    game.Commander().setCash(100000);
    FakeView view = new FakeView();
    ShipListPresenter presenter = new ShipListPresenter(game, view);
    presenter.update();
    dialogs.setResult(DialogResult.Yes);

    assertTrue(presenter.buy(ShipType.Flea.CastToInt()));

    assertEquals(ShipType.Flea, game.Commander().getShip().Type());
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static void moveToSystem(Game game, boolean highestTech) {
    StarSystem best = null;
    for(StarSystem system : game.Universe()) {
      if(best == null
          || (highestTech && system.TechLevel().ordinal() > best.TechLevel().ordinal())
          || (!highestTech && system.TechLevel().ordinal() < best.TechLevel().ordinal())) {
        best = system;
      }
    }
    game.Commander().CurrentSystem(best);
  }

  private static class FakeView implements ShipListView {
    private ShipListViewModel model;
    private ShipInfoViewModel info;

    @Override
    public void render(ShipListViewModel model) {
      this.model = model;
    }

    @Override
    public void renderInfo(ShipInfoViewModel info) {
      this.info = info;
    }
  }
}

