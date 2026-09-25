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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.CargoRowViewModel;
import org.gts.bst.view.CargoViewModel;
import org.gts.bst.view.ChartsViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.MainStatusViewModel;
import org.gts.bst.view.MainView;
import org.gts.bst.view.ShipyardViewModel;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;


class MainPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void withoutAGameShowsTheEmptyState() {
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> null, view);

    presenter.updateStatusBar();
    presenter.updateSystemInfo();
    presenter.updateCharts();
    presenter.updateCargo();
    presenter.updateDock();
    presenter.updateShipyard();
    presenter.updateTargetSystemInfo();

    assertEquals("", view.status.cash());
    assertEquals("No Game Loaded.", view.status.extra());
    assertEquals("", view.system.name());
    assertFalse(view.system.newsVisible());
    assertFalse(view.charts.wormholeVisible());
    assertFalse(view.charts.findVisible());
    assertEquals(Consts.TradeItems.size(), view.cargo.rows().size());
    assertEquals("", view.cargo.rows().get(0).sellPrice());
    assertFalse(view.cargo.rows().get(0).sellVisible());
    assertEquals("", view.dock.fuelStatus());
    assertFalse(view.dock.fuelButtonVisible());
    assertEquals("", view.shipyard.shipsForSale());
    assertFalse(view.shipyard.buyShipVisible());
    assertFalse(view.target.navigationVisible());
    assertFalse(view.target.warpVisible());
  }

  @Test
  void showsTheStatusBarAndTheCurrentSystem() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateStatusBar();
    presenter.updateSystemInfo();

    assertEquals("Cash: 1,000 cr.", view.status.cash());
    assertEquals("Bays: 0/15", view.status.bays());
    assertEquals("Current Costs: 0 cr.", view.status.costs());
    StarSystem system = game.Commander().CurrentSystem();
    assertEquals(system.Name(), view.system.name());
    assertEquals(Strings.Sizes[system.Size().CastToInt()], view.system.size());
    assertEquals(system.TechLevel().name, view.system.tech());
    assertTrue(view.system.pressurePreVisible());
    assertTrue(view.system.newsVisible());
  }

  @Test
  void showsTheChartControls() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateCharts();
    assertFalse(view.charts.wormholeVisible());
    assertTrue(view.charts.findVisible());
    assertFalse(view.charts.jumpVisible());

    game.SelectedSystemId(StarSystemId.FromInt(0));
    game.TargetWormhole(true);
    presenter.updateCharts();
    assertTrue(view.charts.wormholeVisible());
    assertEquals(game.WarpSystem().Name(), view.charts.wormholeName());
  }

  @Test
  void showsTheCargoTable() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateCargo();

    List<CargoRowViewModel> rows = view.cargo.rows();
    assertEquals(Consts.TradeItems.size(), rows.size());
    for(CargoRowViewModel row : rows) {
      assertEquals("0", row.sellQty());
      assertTrue(row.sellVisible());
      assertEquals("-----------", row.targetPrice());
      assertEquals("------------", row.targetDiff());
      assertEquals("--------", row.targetPct());
      assertFalse(row.buyBold());
    }
    assertEquals("" + game.Commander().CurrentSystem().TradeItems()[0], rows.get(0).buyQty());
  }

  @Test
  void marksCargoSoldAtAProfit() {
    Game game = newGame();
    int sell = game.PriceCargoSell()[0];
    game.Commander().getShip().Cargo()[0] = 3;
    game.Commander().PriceCargo()[0] = 0;
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).updateCargo();

    assertEquals("3", view.cargo.rows().get(0).sellQty());
    assertEquals(sell > 0, view.cargo.rows().get(0).sellBold());
  }

  @Test
  void buysCargoThroughTheDialog() {
    Game game = newGame();
    game.PriceCargoBuy()[0] = 10;
    game.Commander().CurrentSystem().TradeItems()[0] = 5;
    FakeView view = new FakeView();
    view.cargoBuyAnswer = 2;

    new MainPresenter(() -> game, view).buyCargo(0, false);

    assertEquals(2, game.Commander().getShip().Cargo()[0]);
    assertEquals(980, game.Commander().getCash());
    assertEquals(20, game.Commander().PriceCargo()[0]);
  }

  @Test
  void cancellingTheCargoPurchaseChangesNothing() {
    Game game = newGame();
    game.PriceCargoBuy()[0] = 10;
    game.Commander().CurrentSystem().TradeItems()[0] = 5;
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).buyCargo(0, false);

    assertEquals(0, game.Commander().getShip().Cargo()[0]);
    assertEquals(1000, game.Commander().getCash());
  }

  @Test
  void buysTheMaximumCargo() {
    Game game = newGame();
    game.PriceCargoBuy()[0] = 10;
    game.Commander().CurrentSystem().TradeItems()[0] = 5;
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).buyCargo(0, true);

    assertEquals(5, game.Commander().getShip().Cargo()[0]);
    assertEquals(950, game.Commander().getCash());
  }

  @Test
  void sellsCargo() {
    Game game = newGame();
    game.PriceCargoSell()[0] = 10;
    game.Commander().getShip().Cargo()[0] = 3;
    game.Commander().PriceCargo()[0] = 30;
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).sellCargo(0, true);

    assertEquals(0, game.Commander().getShip().Cargo()[0]);
    assertEquals(1030, game.Commander().getCash());
    assertEquals(0, game.Commander().PriceCargo()[0]);
  }

  @Test
  void dumpsCargoWhenThereIsNoSalePrice() {
    Game game = newGame();
    game.PriceCargoSell()[0] = 0;
    game.Commander().getShip().Cargo()[0] = 3;
    game.Commander().PriceCargo()[0] = 30;
    FakeView view = new FakeView();
    view.cargoSellAnswer = 2;
    int unitCost = 5 * (Difficulty.Normal.CastToInt() + 1);

    new MainPresenter(() -> game, view).sellCargo(0, false);

    assertEquals(1, game.Commander().getShip().Cargo()[0]);
    assertEquals(1000 - 2 * unitCost, game.Commander().getCash());
  }

  @Test
  void warnsWhenTheSystemHasNoCargo() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    game.PriceCargoBuy()[0] = 10;
    game.Commander().CurrentSystem().TradeItems()[0] = 0;
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).buyCargo(0, false);

    assertEquals(List.of(AlertType.CargoNoneAvailable), dialogs.alerts());
  }

  @Test
  void buysFuel() {
    Game game = newGame();
    game.Commander().getShip().setFuel(0);
    FakeView view = new FakeView();
    view.fuelAnswer = 14;
    MainPresenter presenter = new MainPresenter(() -> game, view);

    assertTrue(presenter.buyFuel());
    assertEquals(14, game.Commander().getShip().getFuel());
    assertEquals(986, game.Commander().getCash());
    assertEquals("Cash: 986 cr.", view.status.cash());
  }

  @Test
  void cancellingTheFuelDialogChangesNothing() {
    Game game = newGame();
    game.Commander().getShip().setFuel(0);
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    assertFalse(presenter.buyFuel());
    assertEquals(0, game.Commander().getShip().getFuel());
    assertEquals(1000, game.Commander().getCash());
  }

  @Test
  void buysRepairs() {
    Game game = newGame();
    game.Commander().getShip().setHull(90);
    FakeView view = new FakeView();
    view.repairsAnswer = 20;
    MainPresenter presenter = new MainPresenter(() -> game, view);

    assertTrue(presenter.buyRepairs());
    assertEquals(100, game.Commander().getShip().getHull());
    assertEquals(980, game.Commander().getCash());
  }

  @Test
  void updateAllRendersEveryArea() {
    Game game = newGame();
    FakeView view = new FakeView();

    new MainPresenter(() -> game, view).updateAll();

    assertNotNull(view.status);
    assertNotNull(view.system);
    assertNotNull(view.charts);
    assertNotNull(view.cargo);
    assertNotNull(view.dock);
    assertNotNull(view.shipyard);
    assertNotNull(view.target);
  }

  @Test
  void showsTheFuelAndHullState() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateDock();

    assertEquals("You have fuel to fly 14 parsecs.", view.dock.fuelStatus());
    assertEquals("Your tank is full.", view.dock.fuelCost());
    assertFalse(view.dock.fuelButtonVisible());
    assertEquals("Your hull strength is at 100%.", view.dock.hullStatus());
    assertEquals("No repairs are needed.", view.dock.repairCost());
    assertFalse(view.dock.repairButtonVisible());
  }

  @Test
  void showsTheShipyardAccordingToTheSystemTechLevel() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);
    int minTech = Consts.ShipSpecs.get(ShipType.Flea.CastToInt()).MinimumTechLevel().ordinal();

    game.Commander().CurrentSystem(firstSystemWithTech(game, true, minTech));
    presenter.updateShipyard();
    StarSystem highTech = game.Commander().CurrentSystem();
    assertEquals(Strings.ShipyardShipForSale, view.shipyard.shipsForSale());
    assertEquals(Strings.ShipyardEquipForSale, view.shipyard.equipForSale());
    assertEquals(highTech.Shipyard() != null, view.shipyard.designVisible());
    assertEquals(Strings.ShipyardPodIF, view.shipyard.escapePod());
    assertFalse(view.shipyard.podVisible());

    game.Commander().CurrentSystem(firstSystemWithTech(game, false, minTech));
    presenter.updateShipyard();
    assertEquals(Strings.ShipyardShipNoSale, view.shipyard.shipsForSale());
    assertEquals(Strings.ShipyardEquipNoSale, view.shipyard.equipForSale());
    assertEquals(Strings.ShipyardPodNoSale, view.shipyard.escapePod());
  }

  @Test
  void showsTheTargetSystem() {
    Game game = newGame();
    game.SelectedSystemId(StarSystemId.FromInt(0));
    StarSystem target = game.WarpSystem();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateTargetSystemInfo();

    assertTrue(view.target.navigationVisible());
    assertEquals(target.Name(), view.target.name());
    assertEquals("" + Functions.Distance(game.Commander().CurrentSystem(), target), view.target.distance());
    assertEquals(target.DestOk(), view.target.warpVisible());
  }

  private static Game newGame() {
    return newGame(new TestDialogService());
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static StarSystem firstSystemWithTech(Game game, boolean atLeast, int tech) {
    for(StarSystem system : game.Universe()) {
      if(atLeast == (system.TechLevel().ordinal() >= tech)) {
        return system;
      }
    }
    throw new AssertionError("no system with the required tech level");
  }

  private static class FakeView implements MainView {
    private MainStatusViewModel status;
    private SystemInfoViewModel system;
    private ChartsViewModel charts;
    private CargoViewModel cargo;
    private DockViewModel dock;
    private ShipyardViewModel shipyard;
    private TargetSystemViewModel target;
    private Integer fuelAnswer;
    private Integer repairsAnswer;
    private Integer cargoBuyAnswer;
    private Integer cargoSellAnswer;

    @Override
    public void renderStatusBar(MainStatusViewModel model) {
      status = model;
    }

    @Override
    public void renderSystemInfo(SystemInfoViewModel model) {
      system = model;
    }

    @Override
    public void renderCharts(ChartsViewModel model) {
      charts = model;
    }

    @Override
    public void renderCargo(CargoViewModel model) {
      cargo = model;
    }

    @Override
    public void renderDock(DockViewModel model) {
      dock = model;
    }

    @Override
    public void renderShipyard(ShipyardViewModel model) {
      shipyard = model;
    }

    @Override
    public void renderTargetSystem(TargetSystemViewModel model) {
      target = model;
    }

    @Override
    public Integer askFuelAmount(int maxAmount) {
      return fuelAnswer;
    }

    @Override
    public Integer askRepairsAmount(int maxAmount) {
      return repairsAnswer;
    }

    @Override
    public Integer askCargoBuyQuantity(CargoBuyOffer offer) {
      return cargoBuyAnswer;
    }

    @Override
    public Integer askCargoSellQuantity(CargoSellOffer offer) {
      return cargoSellAnswer;
    }
  }
}

