/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ports.DialogService;
import org.gts.bst.ship.equip.ShieldType;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.SystemPressure;


/**
 * The arrival at a star system: it moves the commander, marks the system as
 * visited, runs the arrival checks (reactor, tribbles, debt, repairs, pressures
 * and quantities, Easter egg), recalculates the prices, adds the arrival
 * rumours and shows the newspaper when the option asks for it. The game only
 * assembles it with its state and its callbacks and delegates.
 */
public final class Arrival {
  /** The short-range chart tracking that the arrival may switch off. */
  public interface Navigation {
    /** The system the chart tracks, or null when it tracks none. */
    StarSystem trackedSystem();

    /** Stops tracking the current system. */
    void clearTracked();
  }

  /** The mission state of the reactor, read and written by the arrival check. */
  public interface ReactorStatus {
    int reactorStatus();

    void reactorStatus(int status);
  }

  /**
   * The consequences of the arrival that live outside this component. They run
   * in the middle of the flow, so the checks that follow see their effects.
   */
  public interface Consequences {
    void escapeWithPod();

    void destroyed();

    void showNewspaper();
  }

  private final Commander _cmdr;
  private final Universe _universe;
  private final Market _market;
  private final Newspaper _newspaper;
  private final GameOptions _options;
  private final Difficulty _difficulty;
  private final DialogService _dialogs;
  private final QuestStates _quests;
  private final Navigation _navigation;
  private final ReactorStatus _reactor;
  private final Runnable _clearTribbleMessage;
  private final Consequences _consequences;

  public Arrival(Commander cmdr, Universe universe, Market market, Newspaper newspaper, GameOptions options,
      Difficulty difficulty, DialogService dialogs, QuestStates quests, Navigation navigation,
      ReactorStatus reactor, Runnable clearTribbleMessage, Consequences consequences) {
    _cmdr = cmdr;
    _universe = universe;
    _market = market;
    _newspaper = newspaper;
    _options = options;
    _difficulty = difficulty;
    _dialogs = dialogs;
    _quests = quests;
    _navigation = navigation;
    _reactor = reactor;
    _clearTribbleMessage = clearTribbleMessage;
    _consequences = consequences;
  }

  /**
   * Arrives at the given system: applies the state, the checks, the prices and
   * the news in the order the game always did.
   */
  public void arrive(StarSystem system) {
    _cmdr.CurrentSystem(system);
    system.Visited(true);
    _newspaper.paid(false);
    if(_navigation.trackedSystem() == system && _options.getTrackAutoOff()) {
      _navigation.clearTracked();
    }
    checkReactor();
    checkTribbles();
    checkDebt();
    performRepairs();
    updatePressuresAndQuantities(system);
    checkEasterEgg(system);
    _market.calculate(system, _cmdr.getPoliceRecordScore(), _cmdr.getShip().Trader());
    _newspaper.addEventsOnArrival(system, _quests);
    if(_options.getNewsAutoShow()) {
      _consequences.showNewspaper();
    }
  }

  private void checkDebt() {
    // Check for Large Debt - 06/30/01 SRA
    if(_cmdr.getDebt() >= Consts.DebtWarning) {
      _dialogs.alert(AlertType.DebtWarning);
    } else if(_cmdr.getDebt() > 0 && _options.getRemindLoans() && _cmdr.getDays() % 5 == 0) { // Debt Reminder
      _dialogs.alert(AlertType.DebtReminder, Functions.Multiples(_cmdr.getDebt(), Strings.MoneyUnit));
    }
  }

  private void checkEasterEgg(StarSystem system) {
    /* This Easter Egg gives the commander a Lighting Shield */
    if(system.Id() == StarSystemId.Og) {
      boolean egg = true;
      for(int i = 0; i < _cmdr.getShip().Cargo().length && egg; i++) {
        if(_cmdr.getShip().Cargo()[i] != 1) {
          egg = false;
        }
      }
      if(egg && _cmdr.getShip().FreeSlotsShield() > 0) {
        _dialogs.alert(AlertType.Egg);
        _cmdr.getShip().AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
        for(int i = 0; i < _cmdr.getShip().Cargo().length; i++) {
          _cmdr.getShip().Cargo()[i] = 0;
          _cmdr.PriceCargo()[i] = 0;
        }
      }
    }
  }

  private void checkReactor() {
    int status = _reactor.reactorStatus();
    if(status == SpecialEvent.StatusReactorDate) {
      _dialogs.alert(AlertType.ReactorMeltdown);
      _reactor.reactorStatus(SpecialEvent.StatusReactorNotStarted);
      if(_cmdr.getShip().getEscapePod()) {
        _consequences.escapeWithPod();
      } else {
        _dialogs.alert(AlertType.ReactorDestroyed);
        _consequences.destroyed();
      }
    } else {
      // Reactor warnings:
      if(status == SpecialEvent.StatusReactorFuelOk + 1) { // now they know the quest has a time constraint!
        _dialogs.alert(AlertType.ReactorWarningFuel);
      } else if(status == SpecialEvent.StatusReactorDate - 4) { // better deliver it soon!
        _dialogs.alert(AlertType.ReactorWarningFuelGone);
      } else if(status == SpecialEvent.StatusReactorDate - 2) { // last warning!
        _dialogs.alert(AlertType.ReactorWarningTemp);
      }
    }
  }

  private void checkTribbles() {
    Ship ship = _cmdr.getShip();
    if(ship.getTribbles() > 0) {
      int previousTribbles = ship.getTribbles();
      int narc = TradeItemType.Narcotics.CastToInt();
      int food = TradeItemType.Food.CastToInt();
      if(reactorOnBoard()) {
        if(ship.getTribbles() < 20) {
          ship.setTribbles(0);
          _dialogs.alert(AlertType.TribblesAllDied);
        } else {
          ship.setTribbles(ship.getTribbles() / 2);
          _dialogs.alert(AlertType.TribblesHalfDied);
        }
      } else if(ship.Cargo()[narc] > 0) {
        int dead = Math.min(1 + Functions.GetRandom(3), ship.Cargo()[narc]);
        _cmdr.PriceCargo()[narc] = _cmdr.PriceCargo()[narc] * (ship.Cargo()[narc] - dead) / ship.Cargo()[narc];
        ship.Cargo()[narc] -= dead;
        ship.Cargo()[TradeItemType.Furs.CastToInt()] += dead;
        ship.setTribbles(ship.getTribbles() - Math.min(dead * (Functions.GetRandom(5) + 98), ship.getTribbles() - 1));
        _dialogs.alert(AlertType.TribblesMostDied);
      } else {
        if(ship.Cargo()[food] > 0 && ship.getTribbles() < Consts.MaxTribbles) {
          int eaten = ship.Cargo()[food] - Functions.GetRandom(ship.Cargo()[food]);
          _cmdr.PriceCargo()[food] -= _cmdr.PriceCargo()[food] * eaten / ship.Cargo()[food];
          ship.Cargo()[food] -= eaten;
          ship.setTribbles(ship.getTribbles() + (eaten * 100));
          _dialogs.alert(AlertType.TribblesAteFood);
        }
        if(ship.getTribbles() < Consts.MaxTribbles) {
          ship.setTribbles(ship.getTribbles() + (1 + Functions.GetRandom(ship.Cargo()[food] > 0 ? ship.getTribbles() : ship.getTribbles() / 2)));
        }
        if(ship.getTribbles() > Consts.MaxTribbles) {
          ship.setTribbles(Consts.MaxTribbles);
        }
        if((previousTribbles < 100 && ship.getTribbles() >= 100)
            || (previousTribbles < 1000 && ship.getTribbles() >= 1000)
            || (previousTribbles < 10000 && ship.getTribbles() >= 10000)
            || (previousTribbles < 50000 && ship.getTribbles() >= 50000)
            || (previousTribbles < Consts.MaxTribbles && ship.getTribbles() == Consts.MaxTribbles)) {
          String qty = ship.getTribbles() == Consts.MaxTribbles ? Strings.TribbleDangerousNumber : Functions.FormatNumber(ship.getTribbles());
          _dialogs.alert(AlertType.TribblesInspector, qty);
        }
      }
      _clearTribbleMessage.run();
    }
  }

  /**
   * The reactor is on board while its mission is in progress. Same rule as
   * {@code Ship.ReactorOnBoard()}, which reads this same mission state.
   */
  private boolean reactorOnBoard() {
    int status = _reactor.reactorStatus();
    return status > SpecialEvent.StatusReactorNotStarted && status < SpecialEvent.StatusReactorDelivered;
  }

  private void performRepairs() {
    Ship ship = _cmdr.getShip();
    if(ship.getHull() < ship.HullStrength()) {
      ship.setHull(ship.getHull() + Math.min(ship.HullStrength() - ship.getHull(), Functions.GetRandom(ship.Engineer())));
    }
    for(int i = 0; i < ship.Shields().length; ++i) {
      if(ship.Shields()[i] != null) {
        ship.Shields()[i].setCharge(ship.Shields()[i].Power());
      }
    }
    boolean fuelOk = true;
    int toAdd = ship.FuelTanks() - ship.getFuel();
    if(_options.getAutoFuel() && toAdd > 0) {
      if(_cmdr.getCash() >= toAdd * ship.getFuelCost()) {
        ship.setFuel(ship.getFuel() + toAdd);
        _cmdr.setCash(_cmdr.getCash() - (toAdd * ship.getFuelCost()));
      } else {
        fuelOk = false;
      }
    }
    boolean repairOk = true;
    toAdd = ship.HullStrength() - ship.getHull();
    if(_options.getAutoRepair() && toAdd > 0) {
      if(_cmdr.getCash() >= toAdd * ship.getRepairCost()) {
        ship.setHull(ship.getHull() + toAdd);
        _cmdr.setCash(_cmdr.getCash() - (toAdd * ship.getRepairCost()));
      } else {
        repairOk = false;
      }
    }
    if(!fuelOk && !repairOk) {
      _dialogs.alert(AlertType.ArrivalIFFuelRepairs);
    } else if(!fuelOk) {
      _dialogs.alert(AlertType.ArrivalIFFuel);
    } else if(!repairOk) {
      _dialogs.alert(AlertType.ArrivalIFRepairs);
    }
  }

  private void updatePressuresAndQuantities(StarSystem system) {
    StarSystem[] universe = _universe.systems();
    for(int i = 0; i < universe.length; i++) {
      if(Functions.GetRandom(100) < 15) {
        universe[i].SystemPressure((SystemPressure.FromInt(universe[i].SystemPressure() == SystemPressure.None
            ? Functions.GetRandom(SystemPressure.War.CastToInt(), SystemPressure.Employment.CastToInt() + 1) : SystemPressure.None.CastToInt())));
      }
      if(universe[i].CountDown() > 0) {
        universe[i].CountDown(universe[i].CountDown() - 1);
        if(universe[i].CountDown() > countDownStart()) {
          universe[i].CountDown(countDownStart());
        } else if(universe[i].CountDown() <= 0) {
          universe[i].InitializeTradeItems();
        } else {
          for(int j = 0; j < Consts.TradeItems.size(); j++) {
            if(system.ItemTraded(Consts.TradeItems.get(j))) {
              universe[i].TradeItems()[j] = Math.max(0, universe[i].TradeItems()[j] + Functions.GetRandom(-4, 5));
            }
          }
        }
      }
    }
  }

  private int countDownStart() {
    return _difficulty.CastToInt() + 3;
  }
}
