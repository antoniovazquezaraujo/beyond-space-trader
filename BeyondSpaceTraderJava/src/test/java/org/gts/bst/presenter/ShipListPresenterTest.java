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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ports.DialogResult;
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

    assertEquals(forSaleCount(), view.model.rows().size(), "only the ships with an occurrence are for sale");
    ShipListViewModel.Row owned = rowOf(view, ship.Type());
    assertEquals(Strings.ShipBuyGotOne, owned.price());
    assertFalse(owned.buyVisible());
    ShipSpec flea = Consts.ShipSpecs.get(ShipType.Flea.CastToInt());
    ShipListViewModel.Row fleaRow = rowOf(view, ShipType.Flea);
    assertEquals(flea.Name(), fleaRow.name());
    assertTrue(fleaRow.buyVisible());
    assertEquals(Functions.FormatMoney(flea.getPrice() - ship.Worth(false)), fleaRow.price());

    for(ShipType special : List.of(ShipType.SpaceMonster, ShipType.Dragonfly, ShipType.Mantis,
        ShipType.Scarab, ShipType.Bottle, ShipType.Custom, ShipType.Scorpion)) {
      assertNull(rowOf(view, special), "the special ships are not for sale: " + special);
    }
  }

  @Test
  void marksShipsAsNotSoldInLowTechSystems() {
    Game game = newGame(new TestDialogService());
    moveToSystem(game, false);
    FakeView view = new FakeView();

    new ShipListPresenter(game, view).update();

    assertTrue(view.model.rows().stream().anyMatch(row -> Strings.NotSold.equals(row.price())));
  }

  @Test
  void selectsTheShipInformation() {
    Game game = newGame(new TestDialogService());
    moveToSystem(game, true);
    ShipSpec flea = Consts.ShipSpecs.get(ShipType.Flea.CastToInt());
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

  /**
   * The old presenter cached the ship: after the first trade the row of the Flea
   * kept being buyable and every press paid the old ship's worth again.
   */
  @Test
  void buyingTheFleaOnceDoesNotMintMoney() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    moveToSystem(game, true);
    game.Commander().setCash(1000);
    FakeView view = new FakeView();
    ShipListPresenter presenter = new ShipListPresenter(game, view);
    presenter.update();
    int fleaRow = view.model.rows().indexOf(rowOf(view, ShipType.Flea));
    int price = Consts.ShipSpecs.get(ShipType.Flea.CastToInt()).getPrice() - game.Commander().getShip().Worth(false);
    assertEquals(Functions.FormatMoney(price), rowOf(view, ShipType.Flea).price());
    dialogs.setResult(DialogResult.Yes);

    assertTrue(presenter.buy(fleaRow), "the first trade down is legitimate");
    assertEquals(1000 - price, game.Commander().getCash(), "the price is paid once");
    assertEquals(ShipType.Flea, game.Commander().getShip().Type());

    presenter.update();
    assertEquals(Strings.ShipBuyGotOne, rowOf(view, ShipType.Flea).price(), "now the Flea is the ship in flight");
    assertFalse(rowOf(view, ShipType.Flea).buyVisible());
    ShipListViewModel.Row gnat = rowOf(view, ShipType.Gnat);
    assertTrue(gnat.buyVisible());
    assertEquals(Functions.FormatMoney(Consts.ShipSpecs.get(ShipType.Gnat.CastToInt()).getPrice()
        - game.Commander().getShip().Worth(false)), gnat.price(),
        "the row of the Gnat is priced against the new ship, not the old one");

    int cash = game.Commander().getCash();
    for(int i = 0; i < 5; i++) {
      assertFalse(presenter.buy(fleaRow), "asking for the ship already owned is rejected");
    }
    assertEquals(cash, game.Commander().getCash(), "and it never pays again");
  }

  private static ShipListViewModel.Row rowOf(FakeView view, ShipType type) {
    for(ShipListViewModel.Row row : view.model.rows()) {
      if(row.name().equals(Strings.ShipNames.get(type.CastToInt()))) {
        return row;
      }
    }
    return null;
  }

  private static int forSaleCount() {
    int count = 0;
    for(ShipSpec spec : Consts.ShipSpecs) {
      if(spec.Occurrence() > 0) {
        count++;
      }
    }
    return count;
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
