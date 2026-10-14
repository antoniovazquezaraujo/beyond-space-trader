/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.DialogService;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import spacetrader.enums.AlertType;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;
import spacetrader.util.Hashtable;


/**
 * The quest and special-event area of a game: the status of the thirteen
 * mission threads, the flags they keep (the fabric rip probability and the
 * portable singularity) and the rules that move them (the special events of a
 * system and the passage of days). It owns the state and the logic; the game
 * only delegates.
 */
public final class Quests implements Arrival.ReactorStatus {
  /** The consequences of a special event that only the game can run. */
  public interface Consequences {
    /** The commander retires on the moon: the game ends. */
    void retired();
  }

  private final Commander _cmdr;
  private final Universe _universe;
  private final Market _market;
  private final Newspaper _newspaper;
  private final CrewMember[] _mercenaries;
  private final Ship _spaceMonster;
  private final Difficulty _difficulty;
  private final DialogService _dialogs;
  private final Consequences _consequences;
  private int _questStatusArtifact = 0; // 0 = not given yet, 1 = Artifact on board, 2 = Artifact no longer on board (either delivered or lost)
  private int _questStatusDragonfly = 0; // 0 = not available, 1 = Go to Baratas, 2 = Go to Melina, 3 = Go to Regulas, 4 = Go to Zalkon, 5 = Dragonfly destroyed, 6 = Got Shield
  private int _questStatusExperiment = 0; // 0 = not given yet, 1-11 = days from start; 12 = performed, 13 = cancelled
  private int _questStatusGemulon = 0; // 0 = not given yet, 1-7 = days from start, 8 = too late, 9 = in time, 10 = done
  private int _questStatusJapori = 0; // 0 = no disease, 1 = Go to Japori (always at least 10 medicine cannisters), 2 = Assignment finished or canceled
  private int _questStatusJarek = 0; // 0 = not delivered, 1-11 = on board, 12 = delivered
  private int _questStatusMoon = 0; // 0 = not bought, 1 = bought, 2 = claimed
  private int _questStatusPrincess = 0; // 0 = not available, 1 = Go to Centauri, 2 = Go to Inthara, 3 = Go to Qonos, 4 = Princess Rescued, 5-14 = On Board, 15 = Princess Returned, 16 = Got Quantum Disruptor
  private int _questStatusReactor = 0; // 0 = not encountered, 1-20 = days of mission (bays of fuel left = 10 - (ReactorStatus / 2), 21 = delivered, 22 = Done
  private int _questStatusScarab = 0; // 0 = not given yet, 1 = not destroyed, 2 = destroyed - upgrade not performed, 3 = destroyed - hull upgrade performed
  private int _questStatusSculpture = 0; // 0 = not given yet, 1 = on board, 2 = delivered, 3 = done
  private int _questStatusSpaceMonster = 0; // 0 = not available, 1 = Space monster is in Acamar system, 2 = Space monster is destroyed, 3 = Claimed reward
  private int _questStatusWild = 0; // 0 = not delivered, 1-11 = on board, 12 = delivered
  private int _fabricRipProbability = 0; // if Experiment = 12, this is the probability of being warped to a random planet.
  private boolean _canSuperWarp = false; // Do you have the Portable Singularity on board?

  /**
   * Creates the quest component over the game objects its rules touch: the
   * commander, the universe, the market, the newspaper, the mercenaries, the
   * space monster and the difficulty, with the dialog port and the game
   * consequences.
   */
  public Quests(Commander cmdr, Universe universe, Market market, Newspaper newspaper, CrewMember[] mercenaries,
      Ship spaceMonster, Difficulty difficulty, DialogService dialogs, Consequences consequences) {
    _cmdr = cmdr;
    _universe = universe;
    _market = market;
    _newspaper = newspaper;
    _mercenaries = mercenaries;
    _spaceMonster = spaceMonster;
    _difficulty = difficulty;
    _dialogs = dialogs;
    _consequences = consequences;
  }

  public int questStatusArtifact() {
    return _questStatusArtifact;
  }

  public void questStatusArtifact(int questStatusArtifact) {
    _questStatusArtifact = questStatusArtifact;
  }

  public int questStatusDragonfly() {
    return _questStatusDragonfly;
  }

  public void questStatusDragonfly(int questStatusDragonfly) {
    _questStatusDragonfly = questStatusDragonfly;
  }

  public int questStatusExperiment() {
    return _questStatusExperiment;
  }

  public void questStatusExperiment(int questStatusExperiment) {
    _questStatusExperiment = questStatusExperiment;
  }

  public int questStatusGemulon() {
    return _questStatusGemulon;
  }

  public void questStatusGemulon(int questStatusGemulon) {
    _questStatusGemulon = questStatusGemulon;
  }

  public int questStatusJapori() {
    return _questStatusJapori;
  }

  public void questStatusJapori(int questStatusJapori) {
    _questStatusJapori = questStatusJapori;
  }

  public int questStatusJarek() {
    return _questStatusJarek;
  }

  public void questStatusJarek(int questStatusJarek) {
    _questStatusJarek = questStatusJarek;
  }

  public int questStatusMoon() {
    return _questStatusMoon;
  }

  public void questStatusMoon(int questStatusMoon) {
    _questStatusMoon = questStatusMoon;
  }

  public int questStatusPrincess() {
    return _questStatusPrincess;
  }

  public void questStatusPrincess(int questStatusPrincess) {
    _questStatusPrincess = questStatusPrincess;
  }

  public int questStatusScarab() {
    return _questStatusScarab;
  }

  public void questStatusScarab(int questStatusScarab) {
    _questStatusScarab = questStatusScarab;
  }

  public int questStatusSculpture() {
    return _questStatusSculpture;
  }

  public void questStatusSculpture(int questStatusSculpture) {
    _questStatusSculpture = questStatusSculpture;
  }

  public int questStatusSpaceMonster() {
    return _questStatusSpaceMonster;
  }

  public void questStatusSpaceMonster(int questStatusSpaceMonster) {
    _questStatusSpaceMonster = questStatusSpaceMonster;
  }

  public int questStatusWild() {
    return _questStatusWild;
  }

  public void questStatusWild(int questStatusWild) {
    _questStatusWild = questStatusWild;
  }

  public int fabricRipProbability() {
    return _fabricRipProbability;
  }

  public void fabricRipProbability(int fabricRipProbability) {
    _fabricRipProbability = fabricRipProbability;
  }

  public boolean canSuperWarp() {
    return _canSuperWarp;
  }

  public void canSuperWarp(boolean canSuperWarp) {
    _canSuperWarp = canSuperWarp;
  }

  /** Writes the quest state to a saved game. */
  public void saveTo(Hashtable hash) {
    hash.add("_questStatusArtifact", _questStatusArtifact);
    hash.add("_questStatusDragonfly", _questStatusDragonfly);
    hash.add("_questStatusExperiment", _questStatusExperiment);
    hash.add("_questStatusGemulon", _questStatusGemulon);
    hash.add("_questStatusJapori", _questStatusJapori);
    hash.add("_questStatusJarek", _questStatusJarek);
    hash.add("_questStatusMoon", _questStatusMoon);
    hash.add("_questStatusPrincess", _questStatusPrincess);
    hash.add("_questStatusReactor", _questStatusReactor);
    hash.add("_questStatusScarab", _questStatusScarab);
    hash.add("_questStatusSculpture", _questStatusSculpture);
    hash.add("_questStatusSpaceMonster", _questStatusSpaceMonster);
    hash.add("_questStatusWild", _questStatusWild);
    hash.add("_fabricRipProbability", _fabricRipProbability);
    hash.add("_canSuperWarp", _canSuperWarp);
  }

  /** Restores the quest state of a saved game from its hash, with the keys and types of the old format. */
  public void loadFrom(Hashtable hash) {
    _questStatusArtifact = STSerializableObject.GetValueFromHash(hash, "_questStatusArtifact", _questStatusArtifact);
    _questStatusDragonfly = STSerializableObject.GetValueFromHash(hash, "_questStatusDragonfly", _questStatusDragonfly);
    _questStatusExperiment = STSerializableObject.GetValueFromHash(hash, "_questStatusExperiment", _questStatusExperiment);
    _questStatusGemulon = STSerializableObject.GetValueFromHash(hash, "_questStatusGemulon", _questStatusGemulon);
    _questStatusJapori = STSerializableObject.GetValueFromHash(hash, "_questStatusJapori", _questStatusJapori);
    _questStatusJarek = STSerializableObject.GetValueFromHash(hash, "_questStatusJarek", _questStatusJarek);
    _questStatusMoon = STSerializableObject.GetValueFromHash(hash, "_questStatusMoon", _questStatusMoon);
    _questStatusPrincess = STSerializableObject.GetValueFromHash(hash, "_questStatusPrincess", _questStatusPrincess);
    _questStatusReactor = STSerializableObject.GetValueFromHash(hash, "_questStatusReactor", _questStatusReactor);
    _questStatusScarab = STSerializableObject.GetValueFromHash(hash, "_questStatusScarab", _questStatusScarab);
    _questStatusSculpture = STSerializableObject.GetValueFromHash(hash, "_questStatusSculpture", _questStatusSculpture);
    _questStatusSpaceMonster = STSerializableObject.GetValueFromHash(hash, "_questStatusSpaceMonster", _questStatusSpaceMonster);
    _questStatusWild = STSerializableObject.GetValueFromHash(hash, "_questStatusWild", _questStatusWild);
    _fabricRipProbability = STSerializableObject.GetValueFromHash(hash, "_fabricRipProbability", _fabricRipProbability);
    _canSuperWarp = STSerializableObject.GetValueFromHash(hash, "_canSuperWarp", _canSuperWarp);
  }

  /**
   * Handles the special event of the current system: the alerts, the rewards,
   * the effects on the commander and the ship and the mission state it moves.
   * The event leaves the system unless the rules keep it there.
   */
  public void handleSpecialEvent() {
    StarSystem curSys = _cmdr.CurrentSystem();
    Ship ship = _cmdr.getShip();
    boolean remove = true;
    switch(curSys.SpecialEventType()) {
      case Artifact:
        questStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
        break;
      case ArtifactDelivery:
        questStatusArtifact(SpecialEvent.StatusArtifactDone);
        break;
      case CargoForSale:
        _dialogs.alert(AlertType.SpecialSealedCanisters);
        int tradeItem = Functions.GetRandom(Consts.TradeItems.size());
        ship.Cargo()[tradeItem] += 3;
        _cmdr.PriceCargo()[tradeItem] += _cmdr.CurrentSystem().SpecialEvent().Price();
        break;
      case Dragonfly:
      case DragonflyBaratas:
      case DragonflyMelina:
      case DragonflyRegulas:
        questStatusDragonfly(questStatusDragonfly() + 1);
        break;
      case DragonflyDestroyed:
        curSys.SpecialEventType(SpecialEventType.DragonflyShield);
        remove = false;
        break;
      case DragonflyShield:
        if(ship.FreeSlotsShield() == 0) {
          _dialogs.alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          _dialogs.alert(AlertType.EquipmentLightningShield);
          ship.AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
          questStatusDragonfly(SpecialEvent.StatusDragonflyDone);
        }
        break;
      case EraseRecord:
        _dialogs.alert(AlertType.SpecialCleanRecord);
        _cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreClean);
        _market.recalculateSellPrices();
        break;
      case Experiment:
        questStatusExperiment(SpecialEvent.StatusExperimentStarted);
        break;
      case ExperimentFailed:
        // The failure is narrative only: it changes neither the ship nor the quest,
        // so the event is kept in the system for the player to reread its story.
        remove = false;
        break;
      case ExperimentStopped:
        questStatusExperiment(SpecialEvent.StatusExperimentCancelled);
        canSuperWarp(true);
        break;
      case Gemulon:
        questStatusGemulon(SpecialEvent.StatusGemulonStarted);
        break;
      case GemulonFuel:
        if(ship.FreeSlotsGadget() == 0) {
          _dialogs.alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          _dialogs.alert(AlertType.EquipmentFuelCompactor);
          ship.AddEquipment(Consts.Gadgets.get(GadgetType.FuelCompactor.asInteger()));
          questStatusGemulon(SpecialEvent.StatusGemulonDone);
        }
        break;
      case GemulonRescued:
        curSys.SpecialEventType(SpecialEventType.GemulonFuel);
        questStatusGemulon(SpecialEvent.StatusGemulonFuel);
        remove = false;
        break;
      case GemulonInvaded:
        // Like ExperimentFailed, the invasion report is narrative only: the event is
        // kept so its bad news can be read again instead of vanishing.
        remove = false;
        break;
      case Japori:
        // The japori quest should not be removed since you can fail and start it over again.
        remove = false;
        if(ship.FreeCargoBays() < 10) {
          _dialogs.alert(AlertType.CargoNoEmptyBays);
        } else {
          _dialogs.alert(AlertType.AntidoteOnBoard);
          questStatusJapori(SpecialEvent.StatusJaporiInTransit);
        }
        break;
      case JaporiDelivery:
        questStatusJapori(SpecialEvent.StatusJaporiDone);
        _cmdr.IncreaseRandomSkill();
        _cmdr.IncreaseRandomSkill();
        break;
      case Jarek:
        if(ship.FreeCrewQuarters() == 0) {
          _dialogs.alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else {
          CrewMember jarek = _mercenaries[CrewMemberId.Jarek.CastToInt()];
          _dialogs.alert(AlertType.SpecialPassengerOnBoard, jarek.Name());
          ship.Hire(jarek);
          questStatusJarek(SpecialEvent.StatusJarekStarted);
        }
        break;
      case JarekGetsOut:
        questStatusJarek(SpecialEvent.StatusJarekDone);
        ship.Fire(CrewMemberId.Jarek);
        break;
      case Lottery:
        break;
      case Moon:
        _dialogs.alert(AlertType.SpecialMoonBought);
        questStatusMoon(SpecialEvent.StatusMoonBought);
        break;
      case MoonRetirement:
        questStatusMoon(SpecialEvent.StatusMoonDone);
        _consequences.retired();
        break; // the game's callback always throws; the break keeps the switch closed if it returns
      case Princess:
        curSys.SpecialEventType(SpecialEventType.PrincessReturned);
        remove = false;
        questStatusPrincess(questStatusPrincess() + 1);
        break;
      case PrincessCentauri:
      case PrincessInthara:
        questStatusPrincess(questStatusPrincess() + 1);
        break;
      case PrincessQonos:
        if(ship.FreeCrewQuarters() == 0) {
          _dialogs.alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else {
          CrewMember princess = _mercenaries[CrewMemberId.Princess.CastToInt()];
          _dialogs.alert(AlertType.SpecialPassengerOnBoard, princess.Name());
          ship.Hire(princess);
        }
        break;
      case PrincessQuantum:
        if(ship.FreeSlotsWeapon() == 0) {
          _dialogs.alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          _dialogs.alert(AlertType.EquipmentQuantumDisruptor);
          ship.AddEquipment(Consts.WeapObjs.get(WeaponType.QuantumDistruptor.id));
          questStatusPrincess(SpecialEvent.StatusPrincessDone);
        }
        break;
      case PrincessReturned:
        ship.Fire(CrewMemberId.Princess);
        curSys.SpecialEventType(SpecialEventType.PrincessQuantum);
        questStatusPrincess(SpecialEvent.StatusPrincessReturned);
        remove = false;
        break;
      case Reactor:
        if(ship.FreeCargoBays() < 15) {
          _dialogs.alert(AlertType.CargoNoEmptyBays);
          remove = false;
        } else {
          if(ship.WildOnBoard()) {
            if(_dialogs.alert(AlertType.WildWontStayAboardReactor, curSys.Name()) == DialogResult.OK) {
              _dialogs.alert(AlertType.WildLeavesShip, curSys.Name());
              questStatusWild(SpecialEvent.StatusWildNotStarted);
            } else {
              remove = false;
            }
          }
          if(remove) {
            _dialogs.alert(AlertType.ReactorOnBoard);
            reactorStatus(SpecialEvent.StatusReactorFuelOk);
          }
        }
        break;
      case ReactorDelivered:
        curSys.SpecialEventType(SpecialEventType.ReactorLaser);
        reactorStatus(SpecialEvent.StatusReactorDelivered);
        remove = false;
        break;
      case ReactorLaser:
        if(ship.FreeSlotsWeapon() == 0) {
          _dialogs.alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          _dialogs.alert(AlertType.EquipmentMorgansLaser);
          ship.AddEquipment(Consts.WeapObjs.get(WeaponType.MorgansLaser.id));
          reactorStatus(SpecialEvent.StatusReactorDone);
        }
        break;
      case Scarab:
        questStatusScarab(SpecialEvent.StatusScarabHunting);
        break;
      case ScarabDestroyed:
        questStatusScarab(SpecialEvent.StatusScarabDestroyed);
        curSys.SpecialEventType(SpecialEventType.ScarabUpgradeHull);
        remove = false;
        break;
      case ScarabUpgradeHull:
        _dialogs.alert(AlertType.ShipHullUpgraded);
        ship.setHullUpgraded(true);
        ship.setHull(ship.getHull() + Consts.HullUpgrade);
        questStatusScarab(SpecialEvent.StatusScarabDone);
        remove = false;
        break;
      case Sculpture:
        questStatusSculpture(SpecialEvent.StatusSculptureInTransit);
        break;
      case SculptureDelivered:
        questStatusSculpture(SpecialEvent.StatusSculptureDelivered);
        curSys.SpecialEventType(SpecialEventType.SculptureHiddenBays);
        remove = false;
        break;
      case SculptureHiddenBays:
        questStatusSculpture(SpecialEvent.StatusSculptureDone);
        if(ship.FreeSlotsGadget() == 0) {
          _dialogs.alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          _dialogs.alert(AlertType.EquipmentHiddenCompartments);
          ship.AddEquipment(Consts.Gadgets.get(GadgetType.HiddenCargoBays.asInteger()));
          questStatusSculpture(SpecialEvent.StatusSculptureDone);
        }
        break;
      case Skill:
        _dialogs.alert(AlertType.SpecialSkillIncrease);
        _cmdr.IncreaseRandomSkill();
        break;
      case SpaceMonster:
        questStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
        break;
      case SpaceMonsterKilled:
        questStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterDone);
        break;
      case Tribble:
        _dialogs.alert(AlertType.TribblesOwn);
        ship.setTribbles(1);
        break;
      case TribbleBuyer:
        _dialogs.alert(AlertType.TribblesGone);
        _cmdr.setCash(_cmdr.getCash() + (ship.getTribbles() / 2));
        ship.setTribbles(0);
        break;
      case Wild:
        if(ship.FreeCrewQuarters() == 0) {
          _dialogs.alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else if(!ship.HasWeapon(WeaponType.BeamLaser, false)) {
          _dialogs.alert(AlertType.WildWontBoardLaser);
          remove = false;
        } else if(ship.ReactorOnBoard()) {
          _dialogs.alert(AlertType.WildWontBoardReactor);
          remove = false;
        } else {
          CrewMember wild = _mercenaries[CrewMemberId.Wild.CastToInt()];
          _dialogs.alert(AlertType.SpecialPassengerOnBoard, wild.Name());
          ship.Hire(wild);
          questStatusWild(SpecialEvent.StatusWildStarted);
          if(ship.SculptureOnBoard()) {
            _dialogs.alert(AlertType.WildSculpture);
          }
        }
        break;
      case WildGetsOut:
        // Zeethibal has a 10 in player's lowest score, an 8 in the next lowest score, and 5 elsewhere.
        CrewMember zeethibal = _mercenaries[CrewMemberId.Zeethibal.CastToInt()];
        zeethibal.CurrentSystem(_universe.systems()[StarSystemId.Kravat.CastToInt()]);
        int lowest1 = _cmdr.NthLowestSkill(1);
        int lowest2 = _cmdr.NthLowestSkill(2);
        for(int i = 0; i < zeethibal.Skills().length; i++) {
          zeethibal.Skills()[i] = (i == lowest1 ? 10 : (i == lowest2 ? 8 : 5));
        }
        questStatusWild(SpecialEvent.StatusWildDone);
        _cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreClean);
        ship.Fire(CrewMemberId.Wild);
        _market.recalculateSellPrices();
        break;
      default:
        break;
    }
    if(curSys.SpecialEvent().Price() != 0) {
      _cmdr.setCash(_cmdr.getCash() - curSys.SpecialEvent().Price());
    }
    if(remove) {
      curSys.SpecialEventType(SpecialEventType.NA);
    }
  }

  /**
   * Passes the given days: the commander, the insurance, the police record
   * drifting towards neutral, the space monster recovering, and the Gemulon,
   * reactor, experiment, Jarek, princess and Wild timers.
   */
  public void incDays(int num) {
    _cmdr.setDays(_cmdr.getDays() + num);
    if(_cmdr.getInsurance()) {
      _cmdr.NoClaim(_cmdr.NoClaim() + num);
    }
    // Police Record will gravitate towards neutral (0).
    if(_cmdr.getPoliceRecordScore() > Consts.PoliceRecordScoreClean) {
      _cmdr.setPoliceRecordScore(Math.max(Consts.PoliceRecordScoreClean, _cmdr.getPoliceRecordScore() - num / 3));
    } else if(_cmdr.getPoliceRecordScore() < Consts.PoliceRecordScoreDubious) {
      _cmdr.setPoliceRecordScore(Math.min(Consts.PoliceRecordScoreDubious, _cmdr.getPoliceRecordScore()
          + num / (_difficulty.CastToInt() <= Difficulty.Normal.CastToInt() ? 1 : _difficulty.CastToInt())));
    }
    // The Space Monster's strength increases 5% per day until it is back to full strength.
    if(_spaceMonster.getHull() < _spaceMonster.HullStrength()) {
      _spaceMonster.setHull(Math.min(_spaceMonster.HullStrength(), (int)(_spaceMonster.getHull() * Math.pow(1.05, num))));
    }
    if(questStatusGemulon() > SpecialEvent.StatusGemulonNotStarted && questStatusGemulon() < SpecialEvent.StatusGemulonTooLate) {
      questStatusGemulon(Math.min(questStatusGemulon() + num, SpecialEvent.StatusGemulonTooLate));
      if(questStatusGemulon() == SpecialEvent.StatusGemulonTooLate) {
        StarSystem gemulon = _universe.systems()[StarSystemId.Gemulon.CastToInt()];
        gemulon.SpecialEventType(SpecialEventType.GemulonInvaded);
        gemulon.TechLevel(TechLevel.t0);
        gemulon.PoliticalSystemType(PoliticalSystemType.Anarchy);
      }
    }
    if(_cmdr.getShip().ReactorOnBoard()) {
      reactorStatus(Math.min(reactorStatus() + num, SpecialEvent.StatusReactorDate));
    }
    if(questStatusExperiment() > SpecialEvent.StatusExperimentNotStarted
        && questStatusExperiment() < SpecialEvent.StatusExperimentPerformed) {
      questStatusExperiment(Math.min(questStatusExperiment() + num, SpecialEvent.StatusExperimentPerformed));
      if(questStatusExperiment() == SpecialEvent.StatusExperimentPerformed) {
        fabricRipProbability(Consts.FabricRipInitialProbability);
        _universe.systems()[StarSystemId.Daled.CastToInt()].SpecialEventType(SpecialEventType.ExperimentFailed);
        _dialogs.alert(AlertType.SpecialExperimentPerformed);
        _newspaper.add(NewsEvent.ExperimentPerformed);
      }
    } else if(questStatusExperiment() == SpecialEvent.StatusExperimentPerformed && fabricRipProbability() > 0) {
      fabricRipProbability(fabricRipProbability() - num);
    }
    if(_cmdr.getShip().JarekOnBoard()) {
      if(questStatusJarek() == SpecialEvent.StatusJarekImpatient / 2) {
        _dialogs.alert(AlertType.SpecialPassengerConcernedJarek);
      } else if(questStatusJarek() == SpecialEvent.StatusJarekImpatient - 1) {
        _dialogs.alert(AlertType.SpecialPassengerImpatientJarek);
        _mercenaries[CrewMemberId.Jarek.CastToInt()].Pilot(0);
        _mercenaries[CrewMemberId.Jarek.CastToInt()].Fighter(0);
        _mercenaries[CrewMemberId.Jarek.CastToInt()].Trader(0);
        _mercenaries[CrewMemberId.Jarek.CastToInt()].Engineer(0);
      }
      if(questStatusJarek() < SpecialEvent.StatusJarekImpatient) {
        questStatusJarek(questStatusJarek() + 1);
      }
    }
    if(_cmdr.getShip().PrincessOnBoard()) {
      if(questStatusPrincess() == (SpecialEvent.StatusPrincessImpatient + SpecialEvent.StatusPrincessRescued) / 2) {
        _dialogs.alert(AlertType.SpecialPassengerConcernedPrincess);
      } else if(questStatusPrincess() == SpecialEvent.StatusPrincessImpatient - 1) {
        _dialogs.alert(AlertType.SpecialPassengerImpatientPrincess);
        _mercenaries[CrewMemberId.Princess.CastToInt()].Pilot(0);
        _mercenaries[CrewMemberId.Princess.CastToInt()].Fighter(0);
        _mercenaries[CrewMemberId.Princess.CastToInt()].Trader(0);
        _mercenaries[CrewMemberId.Princess.CastToInt()].Engineer(0);
      }
      if(questStatusPrincess() < SpecialEvent.StatusPrincessImpatient) {
        questStatusPrincess(questStatusPrincess() + 1);
      }
    }
    if(_cmdr.getShip().WildOnBoard()) {
      if(questStatusWild() == SpecialEvent.StatusWildImpatient / 2) {
        _dialogs.alert(AlertType.SpecialPassengerConcernedWild);
      } else if(questStatusWild() == SpecialEvent.StatusWildImpatient - 1) {
        _dialogs.alert(AlertType.SpecialPassengerImpatientWild);
        _mercenaries[CrewMemberId.Wild.CastToInt()].Pilot(0);
        _mercenaries[CrewMemberId.Wild.CastToInt()].Fighter(0);
        _mercenaries[CrewMemberId.Wild.CastToInt()].Trader(0);
        _mercenaries[CrewMemberId.Wild.CastToInt()].Engineer(0);
      }
      if(questStatusWild() < SpecialEvent.StatusWildImpatient) {
        questStatusWild(questStatusWild() + 1);
      }
    }
  }

  @Override
  public int reactorStatus() {
    return _questStatusReactor;
  }

  @Override
  public void reactorStatus(int status) {
    _questStatusReactor = status;
  }
}
