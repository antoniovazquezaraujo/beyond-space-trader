package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
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
    assertEquals(Consts.TradeItems.length, view.cargo.rows().size());
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
    assertEquals(Consts.TradeItems.length, rows.size());
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
    int minTech = Consts.ShipSpecs[ShipType.Flea.CastToInt()].MinimumTechLevel().ordinal();

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
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
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
  }
}
