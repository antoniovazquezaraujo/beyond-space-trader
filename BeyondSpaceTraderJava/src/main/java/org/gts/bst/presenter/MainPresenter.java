/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.cargo.CargoSellOp;
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
import spacetrader.Trade;
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

  public void updateAll() {
    updateCargo();
    updateDock();
    updateShipyard();
    updateStatusBar();
    updateSystemInfo();
    updateTargetSystemInfo();
    updateCharts();
  }

  /**
   * Buys cargo for the current system, asking the player for the quantity unless
   * {@code max} is requested.
   */
  public void buyCargo(int tradeItem, boolean max) {
    Game game = gameSupplier.get();
    if(game != null) {
      CargoBuyOffer offer = Trade.CargoBuyOffer(game, tradeItem, CargoBuyOp.BuySystem);
      if(offer != null) {
        Integer qty = max ? Integer.valueOf(offer.maxAmount()) : view.askCargoBuyQuantity(offer);
        if(qty != null) {
          Trade.CargoBuy(game, offer, qty);
        }
      }
    }
    updateAll();
  }

  /**
   * Sells or dumps cargo: selling when the system buys the item, dumping otherwise.
   */
  public void sellCargo(int tradeItem, boolean all) {
    Game game = gameSupplier.get();
    if(game != null) {
      boolean sellable = game.PriceCargoSell()[tradeItem] > 0;
      CargoSellOffer offer = Trade.CargoSellOffer(game, tradeItem, sellable ? CargoSellOp.SellSystem : CargoSellOp.Dump);
      if(offer != null) {
        Integer qty = sellable && all ? Integer.valueOf(offer.maxAmount()) : view.askCargoSellQuantity(offer);
        if(qty != null) {
          Trade.CargoSell(game, offer, qty);
        }
      }
    }
    updateAll();
  }

  /**
   * Returns true when fuel was bought.
   */
  public boolean buyFuel() {
    Game game = gameSupplier.get();
    if(game == null) {
      return false;
    }
    Commander cmdr = game.Commander();
    Ship ship = cmdr.getShip();
    int maxAmount = Math.min(cmdr.getCash(), (ship.FuelTanks() - ship.getFuel()) * ship.getFuelCost());
    Integer amount = view.askFuelAmount(maxAmount);
    if(amount == null) {
      return false;
    }
    int toAdd = amount / ship.getFuelCost();
    ship.setFuel(ship.getFuel() + toAdd);
    cmdr.setCash(cmdr.getCash() - toAdd * ship.getFuelCost());
    updateAll();
    return true;
  }

  /**
   * Returns true when repairs were bought.
   */
  public boolean buyRepairs() {
    Game game = gameSupplier.get();
    if(game == null) {
      return false;
    }
    Commander cmdr = game.Commander();
    Ship ship = cmdr.getShip();
    int maxAmount = Math.min(cmdr.getCash(), (ship.HullStrength() - ship.getHull()) * ship.getRepairCost());
    Integer amount = view.askRepairsAmount(maxAmount);
    if(amount == null) {
      return false;
    }
    int toAdd = amount / ship.getRepairCost();
    ship.setHull(ship.getHull() + toAdd);
    cmdr.setCash(cmdr.getCash() - toAdd * ship.getRepairCost());
    updateAll();
    return true;
  }

  public void updateStatusBar() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderStatusBar(new MainStatusViewModel("", "", "", Strings.StatusBarNoGame));
      return;
    }
    Commander cmdr = game.Commander();
    view.renderStatusBar(new MainStatusViewModel(
        Functions.StringVars(Strings.StatusBarCash, Functions.FormatMoney(cmdr.getCash())),
        Functions.StringVars(Strings.StatusBarBays, cmdr.getShip().FilledCargoBays() + "/" + cmdr.getShip().CargoBays()),
        Functions.StringVars(Strings.StatusBarCosts, Functions.FormatMoney(game.CurrentCosts())),
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
        Strings.Sizes.get(system.Size().CastToInt()),
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.SpecialResource().name,
        Strings.ActivityLevels.get(system.PoliticalSystem().ActivityPolice().CastToInt()),
        Strings.ActivityLevels.get(system.PoliticalSystem().ActivityPirates().CastToInt()),
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
      List<CargoRowViewModel> emptyRows = new ArrayList<>(Consts.TradeItems.size());
      for(int i = 0; i < Consts.TradeItems.size(); i++) {
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
    List<CargoRowViewModel> rows = new ArrayList<>(Consts.TradeItems.size());
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      int price = warpSys == null ? 0 : Consts.TradeItems.get(i).StandardPrice(warpSys);
      boolean targetKnown = warpSys != null && warpSys.DestOk() && price > 0;
      int cargo = ship.Cargo()[i];
      int stock = cmdr.CurrentSystem().TradeItems()[i];
      int diff = price - buy[i];
      rows.add(new CargoRowViewModel(
          sell[i] > 0 ? Functions.FormatMoney(sell[i]) : Strings.NoTrade,
          "" + cargo,
          sell[i] > 0 ? Strings.CargoSellAll : Strings.CargoDumpButton,
          true,
          buy[i] > 0 ? Functions.FormatMoney(buy[i]) : Strings.NotSold,
          "" + stock,
          buy[i] > 0,
          targetKnown ? Functions.FormatMoney(price) : Strings.CargoTargetPriceUnknown,
          targetKnown && buy[i] > 0 ? (diff > 0 ? "+" : "") + Functions.FormatMoney(diff) : Strings.CargoTargetDiffUnknown,
          targetKnown && buy[i] > 0 ? (diff > 0 ? "+" : "") + Functions.FormatNumber(100 * diff / buy[i]) + "%" : Strings.CargoTargetPctUnknown,
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
        Functions.StringVars(Strings.DockFuelStatus, Functions.Multiples(ship.getFuel(), Strings.DistanceUnit)),
        tanksEmpty > 0
            ? Functions.StringVars(Strings.DockFuelCost, Functions.FormatMoney(tanksEmpty * ship.getFuelCost()))
            : Strings.DockTankFull,
        tanksEmpty > 0,
        Functions.StringVars(Strings.DockHullStatus,
            Functions.FormatNumber((int)Math.floor((double)100 * ship.getHull() / ship.HullStrength()))),
        hullLoss > 0
            ? Functions.StringVars(Strings.DockRepairCost, Functions.FormatMoney(hullLoss * ship.getRepairCost()))
            : Strings.DockNoRepairs,
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
        < Consts.ShipSpecs.get(ShipType.Flea.CastToInt()).MinimumTechLevel().ordinal();
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
    StarSystem pair = Functions.WormholeTarget(system.Id().CastToInt());
    view.renderTargetSystem(new TargetSystemViewModel(
        true,
        system.Name(),
        Strings.Sizes.get(system.Size().CastToInt()),
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.Visited() ? system.SpecialResource().name : Strings.Unknown,
        Strings.ActivityLevels.get(system.PoliticalSystem().ActivityPolice().CastToInt()),
        Strings.ActivityLevels.get(system.PoliticalSystem().ActivityPirates().CastToInt()),
        "" + Functions.Distance(cmdr.CurrentSystem(), system),
        outOfRange,
        system.DestOk(),
        outOfRange && system != game.TrackedSystem(),
        pair == null ? "" : pair.Name()));
  }

  private static TargetSystemViewModel emptyTarget(boolean navigationVisible) {
    return new TargetSystemViewModel(navigationVisible, "", "", "", "", "", "", "", "", false, false, false, "");
  }
}

