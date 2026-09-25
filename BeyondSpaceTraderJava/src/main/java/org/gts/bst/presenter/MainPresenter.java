package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.CrewMember;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.StarSystem;
import spacetrader.Strings;


/**
 * Fills the main window areas from the model. No front-end types involved. The game
 * reference is provided through a supplier because the application replaces it when a
 * new game is started or a save is loaded.
 */
public class MainPresenter {
  private final Supplier<Game> gameSupplier;
  private final MainView view;

  public MainPresenter(Supplier<Game> gameSupplier, MainView view) {
    this.gameSupplier = gameSupplier;
    this.view = view;
  }

  public void updateStatusBar() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderStatusBar(new MainStatusViewModel("", "", "", "No Game Loaded."));
      return;
    }
    Commander cmdr = game.Commander();
    view.renderStatusBar(new MainStatusViewModel(
        "Cash: " + Functions.FormatMoney(cmdr.getCash()),
        "Bays: " + cmdr.getShip().FilledCargoBays() + "/" + cmdr.getShip().CargoBays(),
        "Current Costs: " + Functions.FormatMoney(game.CurrentCosts()),
        ""));
  }

  public void updateSystemInfo() {
    Game game = gameSupplier.get();
    if(game == null || game.Commander().CurrentSystem() == null) {
      view.renderSystemInfo(new SystemInfoViewModel("", "", "", "", "", "", "", "", false, false, false, false, "", ""));
      return;
    }
    StarSystem system = game.Commander().CurrentSystem();
    CrewMember[] mercs = system.MercenariesForHire();
    boolean mercVisible = mercs.length > 0;
    boolean specialVisible = system.ShowSpecialButton();
    view.renderSystemInfo(new SystemInfoViewModel(
        system.Name(),
        Strings.Sizes[system.Size().CastToInt()],
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.SpecialResource().name,
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPolice().CastToInt()],
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPirates().CastToInt()],
        system.SystemPressure().name,
        true,
        true,
        mercVisible,
        specialVisible,
        mercVisible
            ? Functions.StringVars(Strings.MercenariesForHire,
                mercs.length == 1 ? mercs[0].Name() : mercs.length + Strings.Mercenaries)
            : "",
        specialVisible ? system.SpecialEvent().Title() : ""));
  }

  public void updateCharts() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderCharts(new ChartsViewModel(false, "", false, false));
      return;
    }
    boolean wormhole = game.TargetWormhole();
    view.renderCharts(new ChartsViewModel(wormhole, wormhole ? game.WarpSystem().Name() : "",
        game.getCanSuperWarp(), true));
  }

  public void updateCargo() {
    Game game = gameSupplier.get();
    if(game == null || game.Commander().CurrentSystem() == null) {
      List<CargoRowViewModel> emptyRows = new ArrayList<>(Consts.TradeItems.length);
      for(int i = 0; i < Consts.TradeItems.length; i++) {
        emptyRows.add(CargoRowViewModel.empty());
      }
      view.renderCargo(new CargoViewModel(emptyRows));
      return;
    }
    Commander cmdr = game.Commander();
    Ship ship = cmdr.getShip();
    int[] buy = game.PriceCargoBuy();
    int[] sell = game.PriceCargoSell();
    StarSystem warpSys = game.WarpSystem();
    List<CargoRowViewModel> rows = new ArrayList<>(Consts.TradeItems.length);
    for(int i = 0; i < Consts.TradeItems.length; i++) {
      int price = warpSys == null ? 0 : Consts.TradeItems[i].StandardPrice(warpSys);
      boolean targetKnown = warpSys != null && warpSys.DestOk() && price > 0;
      int cargo = ship.Cargo()[i];
      int stock = cmdr.CurrentSystem().TradeItems()[i];
      int diff = price - buy[i];
      rows.add(new CargoRowViewModel(
          sell[i] > 0 ? Functions.FormatMoney(sell[i]) : "no trade",
          "" + cargo,
          sell[i] > 0 ? "All" : "Dump",
          true,
          buy[i] > 0 ? Functions.FormatMoney(buy[i]) : "not sold",
          "" + stock,
          buy[i] > 0,
          targetKnown ? Functions.FormatMoney(price) : "-----------",
          targetKnown && buy[i] > 0 ? (diff > 0 ? "+" : "") + Functions.FormatMoney(diff) : "------------",
          targetKnown && buy[i] > 0 ? (diff > 0 ? "+" : "") + Functions.FormatNumber(100 * diff / buy[i]) + "%" : "--------",
          sell[i] * cargo > cmdr.PriceCargo()[i],
          targetKnown && buy[i] > 0 && diff > 0 && stock > 0));
    }
    view.renderCargo(new CargoViewModel(rows));
  }

  public void updateDock() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderDock(new DockViewModel("", "", false, "", "", false));
      return;
    }
    Ship ship = game.Commander().getShip();
    int tanksEmpty = ship.FuelTanks() - ship.getFuel();
    int hullLoss = ship.HullStrength() - ship.getHull();
    view.renderDock(new DockViewModel(
        Functions.StringVars("You have fuel to fly ^1.", Functions.Multiples(ship.getFuel(), "parsec")),
        tanksEmpty > 0
            ? Functions.StringVars("A full tank costs ^1", Functions.FormatMoney(tanksEmpty * ship.getFuelCost()))
            : "Your tank is full.",
        tanksEmpty > 0,
        Functions.StringVars("Your hull strength is at ^1%.",
            Functions.FormatNumber((int)Math.floor((double)100 * ship.getHull() / ship.HullStrength()))),
        hullLoss > 0
            ? Functions.StringVars("Full repairs will cost ^1", Functions.FormatMoney(hullLoss * ship.getRepairCost()))
            : "No repairs are needed.",
        hullLoss > 0));
  }

  public void updateShipyard() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderShipyard(new ShipyardViewModel("", false, false, "", false, "", false));
      return;
    }
    StarSystem system = game.Commander().CurrentSystem();
    boolean noTech = system.TechLevel().ordinal()
        < Consts.ShipSpecs[ShipType.Flea.CastToInt()].MinimumTechLevel().ordinal();
    String escapePod;
    boolean podVisible = false;
    if(game.Commander().getShip().getEscapePod()) {
      escapePod = Strings.ShipyardPodInstalled;
    } else if(noTech) {
      escapePod = Strings.ShipyardPodNoSale;
    } else if(game.Commander().getCash() < 2000) {
      escapePod = Strings.ShipyardPodIF;
    } else {
      escapePod = Strings.ShipyardPodCost;
      podVisible = true;
    }
    view.renderShipyard(new ShipyardViewModel(
        noTech ? Strings.ShipyardShipNoSale : Strings.ShipyardShipForSale,
        true,
        system.Shipyard() != null,
        noTech ? Strings.ShipyardEquipNoSale : Strings.ShipyardEquipForSale,
        true,
        escapePod,
        podVisible));
  }

  public void updateTargetSystemInfo() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderTargetSystem(emptyTarget(false));
      return;
    }
    StarSystem system = game.WarpSystem();
    if(system == null) {
      view.renderTargetSystem(emptyTarget(true));
      return;
    }
    Commander cmdr = game.Commander();
    boolean outOfRange = !system.DestOk() && system != cmdr.CurrentSystem();
    view.renderTargetSystem(new TargetSystemViewModel(
        true,
        system.Name(),
        Strings.Sizes[system.Size().CastToInt()],
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.Visited() ? system.SpecialResource().name : Strings.Unknown,
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPolice().CastToInt()],
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPirates().CastToInt()],
        "" + Functions.Distance(cmdr.CurrentSystem(), system),
        outOfRange,
        system.DestOk(),
        outOfRange && system != game.TrackedSystem()));
  }

  private static TargetSystemViewModel emptyTarget(boolean navigationVisible) {
    return new TargetSystemViewModel(navigationVisible, "", "", "", "", "", "", "", "", false, false, false);
  }
}
