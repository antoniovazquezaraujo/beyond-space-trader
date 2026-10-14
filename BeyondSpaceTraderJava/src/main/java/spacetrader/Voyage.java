/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.function.IntConsumer;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.DialogService;
import org.gts.bst.ports.GameWindow;
import org.gts.bst.ship.equip.WeaponType;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;
import spacetrader.util.Hashtable;


/**
 * The trip between two systems: the departure (costs, fuel, day and countdown),
 * the click-by-click travel loop with its encounters, the timespace rip and the
 * arrival. It owns the trip state (the clicks left and whether the commander
 * arrived through a wormhole); the game only assembles it with its objects, its
 * ports and its consequences, and delegates.
 */
public final class Voyage {
  /** The destination of the trip, read by the departure and switched by the timespace rip. */
  public interface Navigation {
    /** The system selected for the warp, or null when there is none. */
    StarSystem warpSystem();

    /** Selects the system the timespace rip switched the trip to. */
    void selectSystemForRip(StarSystemId systemId);
  }

  /** The costs charged on departure: the mercenaries, the insurance and the wormhole toll. */
  public interface DepartureCosts {
    int mercenaryCosts();

    int insuranceCosts();

    int wormholeCosts();
  }

  /**
   * The consequences of the trip that live outside this component. They run in
   * the middle of the flow, so the steps that follow see their effects.
   */
  public interface Consequences {
    /** The police arrested the commander: the trip ends where it was. */
    void arrested();

    /** The commander escaped in a pod: the trip ends where it was. */
    void escapeWithPod();

    /** The commander was destroyed: the game ends. */
    void destroyed();

    /** The trip is over: the arrival runs. */
    void arrival();
  }

  private final Commander _cmdr;
  private final Universe _universe;
  private final Market _market;
  private final Newspaper _newspaper;
  private final Quests _quests;
  private final Encounter _encounter;
  private final EncounterContext _context;
  private final Difficulty _difficulty;
  private final GameWindow _window;
  private final DialogService _dialogs;
  private final Navigation _navigation;
  private final DepartureCosts _costs;
  private final IntConsumer _advanceDays;
  private final Consequences _consequences;
  private int _clicks = 0; // Distance from target system, 0 = arrived
  private boolean _arrivedViaWormhole = false; // whether the player arrived on the current planet via wormhole

  public Voyage(Commander cmdr, Universe universe, Market market, Newspaper newspaper, Quests quests,
      Encounter encounter, EncounterContext context, Difficulty difficulty, GameWindow window, DialogService dialogs,
      Navigation navigation, DepartureCosts costs, IntConsumer advanceDays, Consequences consequences) {
    _cmdr = cmdr;
    _universe = universe;
    _market = market;
    _newspaper = newspaper;
    _quests = quests;
    _encounter = encounter;
    _context = context;
    _difficulty = difficulty;
    _window = window;
    _dialogs = dialogs;
    _navigation = navigation;
    _costs = costs;
    _advanceDays = advanceDays;
    _consequences = consequences;
  }

  /** Writes the trip state to a saved game with the keys and types of the old format. */
  public void saveTo(Hashtable hash) {
    hash.add("_clicks", _clicks);
    hash.add("_arrivedViaWormhole", _arrivedViaWormhole);
  }

  /** Restores the trip state of a saved game from its hash, with the keys and types of the old format. */
  public void loadFrom(Hashtable hash) {
    _clicks = STSerializableObject.GetValueFromHash(hash, "_clicks", _clicks);
    _arrivedViaWormhole = STSerializableObject.GetValueFromHash(hash, "_arrivedViaWormhole", _arrivedViaWormhole);
  }

  /**
   * Warps to the selected system: checks the debt and the departure costs,
   * checks Wild on board, charges the departure, starts the countdown of the
   * destination, recalculates its prices, runs the trip and arrives.
   */
  public void warp(boolean viaSingularity) {
    if(_cmdr.getDebt() > Consts.DebtTooLarge) {
      _dialogs.alert(AlertType.DebtTooLargeGrounded);
    } else if(_cmdr.getCash() < _costs.mercenaryCosts()) {
      _dialogs.alert(AlertType.LeavingIFMercenaries);
    } else if(_cmdr.getCash() < _costs.mercenaryCosts() + _costs.insuranceCosts()) {
      _dialogs.alert(AlertType.LeavingIFInsurance);
    } else if(_cmdr.getCash() < _costs.mercenaryCosts() + _costs.insuranceCosts() + _costs.wormholeCosts()) {
      _dialogs.alert(AlertType.LeavingIFWormholeTax);
    } else {
      boolean wildOk = true;
      // if Wild is aboard, make sure ship is armed!
      if(_cmdr.getShip().WildOnBoard() && !_cmdr.getShip().HasWeapon(WeaponType.BeamLaser, false)) {
        if(_dialogs.alert(AlertType.WildWontStayAboardLaser, _cmdr.CurrentSystem().Name()) == DialogResult.Cancel) {
          wildOk = false;
        } else {
          _dialogs.alert(AlertType.WildLeavesShip, _cmdr.CurrentSystem().Name());
          _quests.questStatusWild(SpecialEvent.StatusWildNotStarted);
        }
      }
      if(wildOk) {
        setArrivedViaWormhole(Functions.WormholeExists(_cmdr.CurrentSystem(), _navigation.warpSystem()));
        if(viaSingularity) {
          _newspaper.add(NewsEvent.ExperimentArrival);
        } else {
          normalDeparture(viaSingularity || getArrivedViaWormhole() ? 0
              : Functions.Distance(_cmdr.CurrentSystem(), _navigation.warpSystem()));
        }
        _cmdr.CurrentSystem().CountDown(countDownStart());
        _newspaper.reset();
        _market.calculate(_navigation.warpSystem(), _cmdr.getPoliceRecordScore(), _cmdr.getShip().Trader());
        // Clicks will be -1 if we were arrested or used the escape pod.
        if(!travel()) {
          _dialogs.alert(AlertType.TravelUneventfulTrip);
        }
        _consequences.arrival();
      }
    }
  }

  /**
   * Runs the trip click by click; returns true when an encounter occurred. The
   * timespace rip may switch the destination at the start, the encounters
   * interrupt the trip and the ship repairs itself on every click.
   */
  public boolean travel() {
    // if timespace is ripped, we may switch the warp system here.
    if(_quests.questStatusExperiment() == SpecialEvent.StatusExperimentPerformed && _quests.fabricRipProbability() > 0
        && (_quests.fabricRipProbability() == Consts.FabricRipInitialProbability
            || Functions.GetRandom(100) < _quests.fabricRipProbability())) {
      _dialogs.alert(AlertType.SpecialTimespaceFabricRip);
      _navigation.selectSystemForRip(StarSystemId.FromInt(Functions.GetRandom(_universe.systems().length)));
    }
    boolean uneventful = true;
    _encounter.setRaided(false);
    _encounter.setInspected(false);
    _encounter.setLitterWarning(false);
    setClicks(Consts.StartClicks);
    while(getClicks() > 0) {
      _cmdr.getShip().PerformRepairs();
      if(new EncounterGenerator(_context).determine()) {
        uneventful = false;
        EncounterResult result = _window.showEncounter();
        _window.UpdateStatusBar();
        switch(result) {
          case Arrested:
            setClicks(0);
            _consequences.arrested();
            break;
          case EscapePod:
            setClicks(0);
            _consequences.escapeWithPod();
            break;
          case Killed:
            _consequences.destroyed();
            break;
          default:
            break;
        }
      }
      setClicks(getClicks() - 1);
    }
    return !uneventful;
  }

  /** The countdown the destination starts after the departure. */
  public int countDownStart() {
    return _difficulty.CastToInt() + 3;
  }

  public boolean getArrivedViaWormhole() {
    return _arrivedViaWormhole;
  }

  public int getClicks() {
    return _clicks;
  }

  public void setArrivedViaWormhole(boolean arrivedViaWormhole) {
    _arrivedViaWormhole = arrivedViaWormhole;
  }

  public void setClicks(int clicks) {
    _clicks = clicks;
  }

  private void normalDeparture(int fuel) {
    _cmdr.setCash(_cmdr.getCash() - (_costs.mercenaryCosts() + _costs.insuranceCosts() + _costs.wormholeCosts()));
    _cmdr.getShip().setFuel(_cmdr.getShip().getFuel() - fuel);
    _cmdr.PayInterest();
    _advanceDays.accept(1);
  }
}
