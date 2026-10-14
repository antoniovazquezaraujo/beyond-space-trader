/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;

import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterType;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.EncounterDialogHost;
import spacetrader.enums.AlertType;
import spacetrader.enums.SkillType;


/**
 * The state of the current encounter and its rules: the round resolution, the
 * opponent reactions and the player checks. The texts and the actions are built
 * here too. The encounter generation lives in {@link EncounterGenerator}.
 */
public final class Encounter {
  private final Game game;

  private Ship _opponent = new Ship(ShipType.Gnat);
  private boolean _opponentDisabled = false;
  private boolean _raided = false; // True when the commander has been raided during the trip
  private boolean _inspected = false; // True when the commander has been inspected during the trip
  private boolean _litterWarning = false; // Warning against littering has been issued.
  private EncounterType _encounterType = EncounterType.FromInt(0); // Type of current encounter
  private boolean _justLootedMarie = false; // flag to indicate whether player looted Marie Celeste
  private boolean _encounterContinueFleeing = false;
  private boolean _encounterContinueAttacking = false;
  private boolean _encounterCmdrFleeing = false;
  private boolean _encounterCmdrHit = false;
  private int _encounterCmdrDamage = 0;
  private boolean _encounterOppFleeingPrev = false;
  private boolean _encounterOppFleeing = false;
  private boolean _encounterOppHit = false;
  private int _encounterOppDamage = 0;

  public Encounter(Game game) {
    this.game = game;
  }

  private boolean EncounterExecuteAttack(Ship attacker, Ship defender, boolean fleeing) {
    boolean hit = false;
    // On beginner level, if you flee, you will escape unharmed.
    // Otherwise, Fighterskill attacker is pitted against pilotskill defender;
    // if defender is fleeing the attacker has a free shot, but the chance to hit is smaller
    // JAF - if the opponent is disabled and attacker has targeting system, they WILL be hit.
    if(!(game.Difficulty() == Difficulty.Beginner && defender.CommandersShip() && fleeing) && (attacker.CommandersShip() && getOpponentDisabled()
        && attacker.HasGadget(GadgetType.TargetingSystem) || Functions.GetRandom(attacker.Fighter() + defender.getSize().CastToInt()) >= (fleeing ? 2 : 1)
        * Functions.GetRandom(5 + defender.Pilot() / 2))) {
      // If the defender is disabled, it only takes one shot to destroy it completely.
      if(attacker.CommandersShip() && getOpponentDisabled()) {
        defender.setHull(0);
      } else {
        int attackerLasers = attacker.WeaponStrength(WeaponType.PulseLaser, WeaponType.MorgansLaser);
        int attackerDisruptors = attacker.WeaponStrength(WeaponType.PhotonDisruptor, WeaponType.QuantumDistruptor);
        if(defender.Type() == ShipType.Scarab) {
          attackerLasers -= attacker.WeaponStrength(WeaponType.BeamLaser, WeaponType.MilitaryLaser);
          attackerDisruptors -= attacker.WeaponStrength(WeaponType.PhotonDisruptor, WeaponType.PhotonDisruptor);
        }
        int attackerWeapons = attackerLasers + attackerDisruptors;
        int disrupt = 0;
        // Attempt to disable the opponent if they're not already disabled, their shields are down, we have disabling weapons, and the option is checked.
        if(defender.Disableable() && defender.ShieldCharge() == 0 && !getOpponentDisabled()
            && game.Options().getDisableOpponents() && attackerDisruptors > 0) {
          disrupt = Functions.GetRandom(attackerDisruptors * (100 + 2 * attacker.Fighter()) / 100);
        } else {
          int damage = attackerWeapons == 0 ? 0 : Functions.GetRandom(attackerWeapons * (100 + 2 * attacker.Fighter()) / 100);
          if(damage > 0) {
            hit = true;
            // Reactor on board -- damage is boosted!
            if(defender.ReactorOnBoard()) {
              damage *= (int)(1 + (game.Difficulty().CastToInt() + 1) * (game.Difficulty().CastToInt() < Difficulty.Normal.CastToInt() ? 0.25 : 0.33));
            }
            // First, shields are depleted
            for(int i = 0; i < defender.Shields().length && defender.Shields()[i] != null && damage > 0; i++) {
              int applied = Math.min(defender.Shields()[i].getCharge(), damage);
              defender.Shields()[i].setCharge(defender.Shields()[i].getCharge() - applied);
              damage -= applied;
            }
            // If there still is damage after the shields have been depleted, this is subtracted from the hull, modified by the engineering skill of the defender.
            // JAF - If the player only has disabling weapons, no damage will be done to the hull.
            if(damage > 0) {
              damage = Math.max(1, damage - Functions.GetRandom(defender.Engineer()));
              disrupt = damage * attackerDisruptors / attackerWeapons;
              // Only that damage coming from Lasers will deplete the hull.
              damage -= disrupt;
              // At least 2 shots on Normal level are needed to destroy the hull
              // (3 on Easy, 4 on Beginner, 1 on Hard or Impossible). For opponents, it is always 2.
              damage = Math.min(damage, defender.HullStrength() / (defender.CommandersShip() ? Math.max(1, Difficulty.Impossible.CastToInt()
                  - game.Difficulty().CastToInt()) : 2));
              // If the hull is hardened, damage is halved.
              if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDone) {
                damage /= 2;
              }
              defender.setHull(Math.max(0, defender.getHull() - damage));
            }
          }
        }
        // Did the opponent get disabled? (Disruptors are 3 times more effective against the ship's systems than they are against the shields).
        if(defender.getHull() > 0 && defender.Disableable() && Functions.GetRandom(100) < disrupt * Consts.DisruptorSystemsMultiplier * 100 / defender.getHull()) {
          setOpponentDisabled(true);
        }
        // Make sure the Scorpion doesn't get destroyed.
        if(defender.Type() == ShipType.Scorpion && defender.getHull() == 0) {
          defender.setHull(1);
          setOpponentDisabled(true);
        }
      }
    }
    return hit;
  }

  private void EncounterDefeatDragonfly() {
    game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
    game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
    game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyDestroyed);
  }

  private void EncounterDefeatScarab() {
    game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
    game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
    game.setQuestStatusScarab(SpecialEvent.StatusScarabDestroyed);
  }

  private void EncounterDefeatScorpion() {
    game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
    game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
    game.setQuestStatusPrincess(SpecialEvent.StatusPrincessRescued);
  }

  private void EncounterScoop(EncounterDialogHost host) {
    // Chance 50% to pick something up on Normal level, 33% on Hard level, 25% on Impossible level, and 100% on Easy or Beginner.
    if((game.Difficulty().CastToInt() < Difficulty.Normal.CastToInt() || Functions.GetRandom(game.Difficulty().CastToInt()) == 0)
        && getOpponent().FilledCargoBays() > 0) {
      // Changed this to actually pick a good that was in the opponent's cargo hold - JAF.
      int index = Functions.GetRandom(getOpponent().FilledCargoBays());
      int tradeItem = -1;
      for(int sum = 0; sum <= index; sum += getOpponent().Cargo()[++tradeItem]) {
      }
      if(game.Dialogs().alert(AlertType.EncounterScoop, Consts.TradeItems.get(tradeItem).Name()) == DialogResult.Yes) {
        boolean jettisoned = false;
        if(game.Commander().getShip().FreeCargoBays() == 0 && game.Dialogs().alert(AlertType.EncounterScoopNoRoom) == DialogResult.Yes) {
          host.showJettison();
          jettisoned = true;
        }
        if(game.Commander().getShip().FreeCargoBays() > 0) {
          game.Commander().getShip().Cargo()[tradeItem]++;
        } else if(jettisoned) {
          game.Dialogs().alert(AlertType.EncounterScoopNoScoop);
        }
      }
    }
  }

  private void EncounterUpdateEncounterType(int prevCmdrHull, int prevOppHull) {
    int chance = Functions.GetRandom(100);
    if(getOpponent().getHull() < prevOppHull || getOpponentDisabled()) {
      switch(getEncounterType()) {
        case FamousCaptainAttack:
          if(getOpponentDisabled()) {
            setEncounterType(EncounterType.FamousCaptDisabled);
          }
          break;
        case PirateAttack:
        case PirateFlee:
        case PirateSurrender:
          if(getOpponentDisabled()) {
            setEncounterType(EncounterType.PirateDisabled);
          } else if(getOpponent().getHull() < (prevOppHull * 2) / 3) {
            if(game.Commander().getShip().getHull() < (prevCmdrHull * 2) / 3) {
              if(chance < 60) {
                setEncounterType(EncounterType.PirateFlee);
              }
            } else {
              if(chance < 10 && getOpponent().Type() != ShipType.Mantis) {
                setEncounterType(EncounterType.PirateSurrender);
              } else {
                setEncounterType(EncounterType.PirateFlee);
              }
            }
          }
          break;
        case PoliceAttack:
        case PoliceFlee:
          if(getOpponentDisabled()) {
            setEncounterType(EncounterType.PoliceDisabled);
          } else if(getOpponent().getHull() < prevOppHull / 2 && (game.Commander().getShip().getHull() >= prevCmdrHull / 2 || chance < 40)) {
            setEncounterType(EncounterType.PoliceFlee);
          }
          break;
        case TraderAttack:
        case TraderFlee:
        case TraderSurrender:
          if(getOpponentDisabled()) {
            setEncounterType(EncounterType.TraderDisabled);
          } else if(getOpponent().getHull() < (prevOppHull * 2) / 3) {
            if(chance < 60) {
              setEncounterType(EncounterType.TraderSurrender);
            } else {
              setEncounterType(EncounterType.TraderFlee);
            }
          } else if(getOpponent().getHull() < (prevOppHull * 9) / 10 && (game.Commander().getShip().getHull() < (prevCmdrHull * 2) / 3 && chance < 20
              || game.Commander().getShip().getHull() < (prevCmdrHull * 9) / 10 && chance < 60 || game.Commander().getShip().getHull() >= (prevCmdrHull * 9) / 10)) {
            // If you get damaged a lot, the trader tends to keep shooting;
            // if you get damaged a little, the trader may keep shooting;
            // if you get damaged very little or not at all, the trader will flee.
            setEncounterType(EncounterType.TraderFlee);
          }
          break;
        default:
          break;
      }
    }
  }

  private void EncounterWon(EncounterDialogHost host) {
    if(getEncounterType().CastToInt() >= EncounterType.PirateAttack.CastToInt()
        && getEncounterType().CastToInt() <= EncounterType.PirateDisabled.CastToInt()
        && getOpponent().Type() != ShipType.Mantis
        && game.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreDubious) {
      game.Dialogs().alert(AlertType.EncounterPiratesBounty, Strings.EncounterPiratesDestroyed, "", Functions.Multiples(getOpponent().Bounty(), Strings.MoneyUnit));
    } else {
      game.Dialogs().alert(AlertType.EncounterYouWin);
    }
    switch(getEncounterType()) {
      case FamousCaptainAttack:
        game.Commander().setKillsTrader(game.Commander().getKillsTrader() + 1);
        if(game.Commander().getReputationScore() < Consts.ReputationScoreDangerous) {
          game.Commander().setReputationScore(Consts.ReputationScoreDangerous);
        } else {
          game.Commander().setReputationScore(game.Commander().getReputationScore() + Consts.ScoreKillCaptain);
        }
        // bump news flag from attacked to ship destroyed
        game.NewsReplaceEvent(game.NewsLatestEvent(), NewsEvent.FromInt(game.NewsLatestEvent() + 1).CastToInt());
        break;
      case DragonflyAttack:
        EncounterDefeatDragonfly();
        break;
      case PirateAttack:
      case PirateFlee:
      case PirateSurrender:
        game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
        if(getOpponent().Type() != ShipType.Mantis) {
          if(game.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreDubious) {
            game.Commander().setCash(game.Commander().getCash() + getOpponent().Bounty());
          }
          game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
          EncounterScoop(host);
        }
        break;
      case PoliceAttack:
      case PoliceFlee:
        game.Commander().setKillsPolice(game.Commander().getKillsPolice() + 1);
        game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPolice);
        break;
      case ScarabAttack:
        EncounterDefeatScarab();
        break;
      case SpaceMonsterAttack:
        game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
        game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
        game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterDestroyed);
        break;
      case TraderAttack:
      case TraderFlee:
      case TraderSurrender:
        game.Commander().setKillsTrader(game.Commander().getKillsTrader() + 1);
        game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillTrader);
        EncounterScoop(host);
        break;
      default:
        break;
    }
    game.Commander().setReputationScore(game.Commander().getReputationScore() + (getOpponent().Type().CastToInt() / 2 + 1));
  }

  public EncounterResult EncounterExecuteAction(EncounterDialogHost host) {
    EncounterResult result = EncounterResult.Continue;
    int prevCmdrHull = game.Commander().getShip().getHull();
    int prevOppHull = getOpponent().getHull();
    setEncounterCmdrHit(false);
    setEncounterOppHit(false);
    _encounterCmdrDamage = 0;
    _encounterOppDamage = 0;
    setEncounterOppFleeingPrev(getEncounterOppFleeing());
    setEncounterOppFleeing(false);
    // Fire shots
    switch(getEncounterType()) {
      case DragonflyAttack:
      case FamousCaptainAttack:
      case MarieCelestePolice:
      case PirateAttack:
      case PoliceAttack:
      case ScarabAttack:
      case ScorpionAttack:
      case SpaceMonsterAttack:
      case TraderAttack:
        setEncounterCmdrHit(EncounterExecuteAttack(getOpponent(), game.Commander().getShip(), getEncounterCmdrFleeing()));
        setEncounterOppHit(!getEncounterCmdrFleeing() && EncounterExecuteAttack(game.Commander().getShip(), getOpponent(), false));
        break;
      case PirateFlee:
      case PirateSurrender:
      case PoliceFlee:
      case TraderFlee:
      case TraderSurrender:
        setEncounterOppHit(!getEncounterCmdrFleeing() && EncounterExecuteAttack(game.Commander().getShip(), getOpponent(), true));
        setEncounterOppFleeing(true);
        break;
      default:
        setEncounterOppHit(!getEncounterCmdrFleeing() && EncounterExecuteAttack(game.Commander().getShip(), getOpponent(), false));
        break;
    }
    // What the round did (the hull lost on each side), for the views.
    _encounterCmdrDamage = Math.max(0, prevCmdrHull - game.Commander().getShip().getHull());
    _encounterOppDamage = Math.max(0, prevOppHull - getOpponent().getHull());
    // Determine whether someone gets destroyed
    if(game.Commander().getShip().getHull() <= 0) {
      if(game.Commander().getShip().getEscapePod()) {
        result = EncounterResult.EscapePod;
      } else {
        game.Dialogs().alert(getOpponent().getHull() <= 0 ? AlertType.EncounterBothDestroyed : AlertType.EncounterYouLose);
        result = EncounterResult.Killed;
      }
    } else if(getOpponentDisabled()) {
      if(getOpponent().Type() == ShipType.Dragonfly || getOpponent().Type() == ShipType.Scarab || getOpponent().Type() == ShipType.Scorpion) {
        String str2 = "";
        switch(getOpponent().Type()) {
          case Dragonfly:
            EncounterDefeatDragonfly();
            break;
          case Scarab:
            EncounterDefeatScarab();
            break;
          case Scorpion:
            str2 = Strings.EncounterPrincessRescued;
            EncounterDefeatScorpion();
            break;
          default:
            break;
        }
        game.Dialogs().alert(AlertType.EncounterDisabledOpponent, EncounterShipText(), str2);
        game.Commander().setReputationScore(game.Commander().getReputationScore() + (getOpponent().Type().CastToInt() / 2 + 1));
        result = EncounterResult.Normal;
      } else {
        EncounterUpdateEncounterType(prevCmdrHull, prevOppHull);
        setEncounterOppFleeing(false);
      }
    } else if(getOpponent().getHull() <= 0) {
      EncounterWon(host);
      result = EncounterResult.Normal;
    } else {
      boolean escaped = false;
      // Determine whether someone gets away.
      if(getEncounterCmdrFleeing()
          && (game.Difficulty() == Difficulty.Beginner || (Functions.GetRandom(7) + game.Commander().getShip().Pilot() / 3) * 2 >= Functions.GetRandom(getOpponent().Pilot())
          * (2 + game.Difficulty().CastToInt()))) {
        game.Dialogs().alert(getEncounterCmdrHit() ? AlertType.EncounterEscapedHit : AlertType.EncounterEscaped);
        escaped = true;
      } else if(getEncounterOppFleeing() && Functions.GetRandom(game.Commander().getShip().Pilot()) * 4 <= Functions.GetRandom(7 + getOpponent().Pilot() / 3) * 2) {
        game.Dialogs().alert(AlertType.EncounterOpponentEscaped);
        escaped = true;
      }

      if(escaped) {
        result = EncounterResult.Normal;
      } else {
        // Determine whether the opponent's actions must be changed
        EncounterType prevEncounter = getEncounterType();
        EncounterUpdateEncounterType(prevCmdrHull, prevOppHull);
        // Update the opponent fleeing flag.
        switch(getEncounterType()) {
          case PirateFlee:
          case PirateSurrender:
          case PoliceFlee:
          case TraderFlee:
          case TraderSurrender:
            setEncounterOppFleeing(true);
            break;
          default:
            setEncounterOppFleeing(false);
            break;
        }
        if(game.Options().getContinuousAttack()
            && (getEncounterCmdrFleeing() || !getEncounterOppFleeing() || game.Options().getContinuousAttackFleeing()
            && (getEncounterType() == prevEncounter || getEncounterType() != EncounterType.PirateSurrender
            && getEncounterType() != EncounterType.TraderSurrender))) {
          if(getEncounterCmdrFleeing()) {
            setEncounterContinueFleeing(true);
          } else {
            setEncounterContinueAttacking(true);
          }
        }
      }
    }
    return result;
  }

  public EncounterResult EncounterVerifySurrender() {
    EncounterResult result = EncounterResult.Continue;
    if(getOpponent().Type() == ShipType.Mantis) {
      if(game.Commander().getShip().ArtifactOnBoard()) {
        if(game.Dialogs().alert(AlertType.EncounterAliensSurrender) == DialogResult.Yes) {
          game.Dialogs().alert(AlertType.ArtifactRelinquished);
          game.setQuestStatusArtifact(SpecialEvent.StatusArtifactNotStarted);
          result = EncounterResult.Normal;
        }
      } else {
        game.Dialogs().alert(AlertType.EncounterSurrenderRefused);
      }
    } else if(getEncounterType() == EncounterType.PoliceAttack || getEncounterType() == EncounterType.PoliceSurrender) {
      if(game.Commander().getPoliceRecordScore() <= Consts.PoliceRecordScorePsychopath) {
        game.Dialogs().alert(AlertType.EncounterSurrenderRefused);
      } else if(game.Dialogs().alert(AlertType.EncounterPoliceSurrender, new String[]{
            game.Commander().getShip().IllegalSpecialCargoDescription(Strings.EncounterPoliceSurrenderCargo, true, false),
            game.Commander().getShip().IllegalSpecialCargoActions()}) == DialogResult.Yes) {
        result = EncounterResult.Arrested;
      }
    } else if(game.Commander().getShip().PrincessOnBoard() && !game.Commander().getShip().HasGadget(GadgetType.HiddenCargoBays)) {
      game.Dialogs().alert(AlertType.EncounterPiratesSurrenderPrincess);
    } else {
      setRaided(true);
      if(game.Commander().getShip().HasGadget(GadgetType.HiddenCargoBays)) {
        ArrayList<String> precious = new ArrayList<>();
        if(game.Commander().getShip().PrincessOnBoard()) {
          precious.add(Strings.EncounterHidePrincess);
        }
        if(game.Commander().getShip().SculptureOnBoard()) {
          precious.add(Strings.EncounterHideSculpture);
        }
        game.Dialogs().alert(AlertType.PreciousHidden, Functions.StringVars(Strings.ListStrings.get(precious.size()), precious.toArray(new String[0])));
      } else if(game.Commander().getShip().SculptureOnBoard()) {
        game.setQuestStatusSculpture(SpecialEvent.StatusSculptureNotStarted);
        game.Dialogs().alert(AlertType.EncounterPiratesTakeSculpture);
      }
      ArrayList<Integer> cargoToSteal = game.Commander().getShip().StealableCargo();
      if(cargoToSteal.size() == 0) {
        int blackmail = Math.min(25000, Math.max(500, game.Commander().Worth() / 20));
        int cashPayment = Math.min(game.Commander().getCash(), blackmail);
        game.Commander().setDebt(game.Commander().getDebt() + (blackmail - cashPayment));
        game.Commander().setCash(game.Commander().getCash() - cashPayment);
        game.Dialogs().alert(AlertType.EncounterPiratesFindNoCargo, Functions.Multiples(blackmail, Strings.MoneyUnit));
      } else {
        game.Dialogs().alert(AlertType.EncounterLooting);
        // Pirates steal as much as they have room for, which could be everything - JAF.
        // Take most high-priced items - JAF.
        while(getOpponent().FreeCargoBays() > 0 && cargoToSteal.size() > 0) {
          int item = cargoToSteal.get(0);
          game.Commander().PriceCargo()[item] -= game.Commander().PriceCargo()[item] / game.Commander().getShip().Cargo()[item];
          game.Commander().getShip().Cargo()[item]--;
          getOpponent().Cargo()[item]++;
          cargoToSteal.remove(0);
        }
      }
      if(game.Commander().getShip().WildOnBoard()) {
        if(getOpponent().getCrewQuarters() > 1) { // Wild hops onto Pirate Ship
          game.setQuestStatusWild(SpecialEvent.StatusWildNotStarted);
          game.Dialogs().alert(AlertType.WildGoesPirates);
        } else { // no room on pirate ship
          game.Dialogs().alert(AlertType.WildChatsPirates);
        }
      }
      // pirates puzzled by reactor
      if(game.Commander().getShip().ReactorOnBoard()) {
        game.Dialogs().alert(AlertType.EncounterPiratesExamineReactor);
      }
      result = EncounterResult.Normal;
    }
    return result;
  }

  public EncounterResult EncounterVerifyYield() {
    EncounterResult result = EncounterResult.Continue;
    if(game.Commander().getShip().IllegalSpecialCargo()) {
      if(game.Dialogs().alert(AlertType.EncounterPoliceSurrender, new String[]{
            game.Commander().getShip().IllegalSpecialCargoDescription(Strings.EncounterPoliceSurrenderCargo, true, true),
            game.Commander().getShip().IllegalSpecialCargoActions()}) == DialogResult.Yes) {
        result = EncounterResult.Arrested;
      }
    } else {
      String str1 = game.Commander().getShip().IllegalSpecialCargoDescription("", false, true);
      if(game.Dialogs().alert(AlertType.EncounterPoliceSubmit, str1, "") == DialogResult.Yes) {
        // Police Record becomes dubious, if it wasn't already.
        if(game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreDubious) {
          game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreDubious);
        }
        game.Commander().getShip().RemoveIllegalGoods();
        result = EncounterResult.Normal;
      }
    }
    return result;
  }

  public EncounterType getEncounterType() {
    return _encounterType;
  }

  public Ship getOpponent() {
    return _opponent;
  }

  public String EncounterAction() {
    String action;
    if(getOpponentDisabled()) {
      action = Functions.StringVars(Strings.EncounterOppDisabled, EncounterShipText());
    } else if(getEncounterOppFleeing()) {
      if(getEncounterType() == EncounterType.PirateSurrender || getEncounterType() == EncounterType.TraderSurrender) {
        action = Functions.StringVars(Strings.EncounterOppSurrender, EncounterShipText());
      } else {
        action = Functions.StringVars(Strings.EncounterOppFleeing, EncounterShipText());
      }
    } else {
      action = Functions.StringVars(Strings.EncounterActionOppAttacks, EncounterShipText());
    }
    return action;
  }

  public String EncounterActionInitial() {
    String text = "";
    // Set up the fleeing variable initially.
    setEncounterOppFleeing(false);
    switch(getEncounterType()) {
      case BottleGood:
      case BottleOld:
        text = Strings.EncounterTextBottle;
        break;
      case CaptainAhab:
      case CaptainConrad:
      case CaptainHuie:
        text = Strings.EncounterTextFamousCaptain;
        break;
      case DragonflyAttack:
      case PirateAttack:
      case PoliceAttack:
      case ScarabAttack:
      case ScorpionAttack:
      case SpaceMonsterAttack:
        text = Strings.EncounterTextOpponentAttack;
        break;
      case DragonflyIgnore:
      case PirateIgnore:
      case PoliceIgnore:
      case ScarabIgnore:
      case ScorpionIgnore:
      case SpaceMonsterIgnore:
      case TraderIgnore:
        text = game.Commander().getShip().Cloaked() ? Strings.EncounterTextOpponentNoNotice : Strings.EncounterTextOpponentIgnore;
        break;
      case MarieCeleste:
        text = Strings.EncounterTextMarieCeleste;
        break;
      case MarieCelestePolice:
        text = Strings.EncounterTextPolicePostMarie;
        break;
      case PirateFlee:
      case PoliceFlee:
      case TraderFlee:
        text = Strings.EncounterTextOpponentFlee;
        setEncounterOppFleeing(true);
        break;
      case PoliceInspect:
        text = Strings.EncounterTextPoliceInspection;
        break;
      case PoliceSurrender:
        text = Strings.EncounterTextPoliceSurrender;
        break;
      case TraderBuy:
      case TraderSell:
        text = Strings.EncounterTextTrader;
        break;
      case FamousCaptainAttack:
      case FamousCaptDisabled:
      case PoliceDisabled:
      case PirateDisabled:
      case PirateSurrender:
      case TraderAttack:
      case TraderDisabled:
      case TraderSurrender:
        // These should never be the initial encounter type.
        break;
    }
    return text;
  }

  public String EncounterShipText() {
    String shipText = getOpponent().Name();
    switch(getEncounterType()) {
      case FamousCaptainAttack:
      case FamousCaptDisabled:
        shipText = Strings.EncounterShipCaptain;
        break;
      case PirateAttack:
      case PirateDisabled:
      case PirateFlee:
      case PirateSurrender:
        shipText = getOpponent().Type() == ShipType.Mantis ? Strings.EncounterShipMantis : Strings.EncounterShipPirate;
        break;
      case PoliceAttack:
      case PoliceDisabled:
      case PoliceFlee:
        shipText = Strings.EncounterShipPolice;
        break;
      case TraderAttack:
      case TraderDisabled:
      case TraderFlee:
      case TraderSurrender:
        shipText = Strings.EncounterShipTrader;
        break;
      default:
        break;
    }
    return shipText;
  }

  public String EncounterText() {
    String cmdrStatus;
    if(getEncounterCmdrFleeing()) {
      cmdrStatus = Functions.StringVars(Strings.EncounterOppFollowing, EncounterShipText());
    } else if(getEncounterOppHit()) {
      cmdrStatus = Functions.StringVars(Strings.EncounterYouHit, EncounterShipText());
    } else {
      cmdrStatus = Functions.StringVars(Strings.EncounterYouMissed, EncounterShipText());
    }
    String oppStatus;
    if(getEncounterOppFleeingPrev()) {
      oppStatus = Functions.StringVars(Strings.EncounterOppNoEscape, EncounterShipText());
    } else if(getEncounterCmdrHit()) {
      oppStatus = Functions.StringVars(Strings.EncounterOppHits, EncounterShipText());
    } else {
      oppStatus = Functions.StringVars(Strings.EncounterOppMissed, EncounterShipText());
    }
    return cmdrStatus + Strings.newline + oppStatus;
  }

  public String EncounterTextInitial() {
    String encounterPretext = "";
    switch(getEncounterType()) {
      case BottleGood:
      case BottleOld:
        encounterPretext = Strings.EncounterPretextBottle;
        break;
      case DragonflyAttack:
      case DragonflyIgnore:
      case ScarabAttack:
      case ScarabIgnore:
        encounterPretext = Strings.EncounterPretextStolen;
        break;
      case CaptainAhab:
        encounterPretext = Strings.EncounterPretextCaptainAhab;
        break;
      case CaptainConrad:
        encounterPretext = Strings.EncounterPretextCaptainConrad;
        break;
      case CaptainHuie:
        encounterPretext = Strings.EncounterPretextCaptainHuie;
        break;
      case MarieCeleste:
        encounterPretext = Strings.EncounterPretextMarie;
        break;
      case MarieCelestePolice:
      case PoliceAttack:
      case PoliceFlee:
      case PoliceIgnore:
      case PoliceInspect:
      case PoliceSurrender:
        encounterPretext = Strings.EncounterPretextPolice;
        break;
      case PirateAttack:
      case PirateFlee:
      case PirateIgnore:
        if(getOpponent().Type() == ShipType.Mantis) {
          encounterPretext = Strings.EncounterPretextAlien;
        } else {
          encounterPretext = Strings.EncounterPretextPirate;
        }
        break;
      case ScorpionAttack:
      case ScorpionIgnore:
        encounterPretext = Strings.EncounterPretextScorpion;
        break;
      case SpaceMonsterAttack:
      case SpaceMonsterIgnore:
        encounterPretext = Strings.EncounterPretextSpaceMonster;
        break;
      case TraderBuy:
      case TraderFlee:
      case TraderIgnore:
      case TraderSell:
        encounterPretext = Strings.EncounterPretextTrader;
        break;
      case FamousCaptainAttack:
      case FamousCaptDisabled:
      case PoliceDisabled:
      case PirateDisabled:
      case PirateSurrender:
      case TraderAttack:
      case TraderDisabled:
      case TraderSurrender:
        // These should never be the initial encounter type.
        break;
    }
    return Functions.StringVars(Strings.EncounterText,
        new String[]{
          Functions.Multiples(game.getClicks(), "click"), game.WarpSystem().Name(), encounterPretext, getOpponent().Name().toLowerCase()
        });
  }

  public boolean EncounterVerifyAttack() {
    boolean attack = true;
    if(game.Commander().getShip().WeaponStrength() == 0) {
      game.Dialogs().alert(AlertType.EncounterAttackNoWeapons);
      attack = false;
    } else if(!getOpponent().Disableable() && game.Commander().getShip().WeaponStrength(WeaponType.PulseLaser, WeaponType.MorgansLaser) == 0) {
      game.Dialogs().alert(AlertType.EncounterAttackNoLasers);
      attack = false;
    } else if(getOpponent().Type() == ShipType.Scorpion && game.Commander().getShip().WeaponStrength(WeaponType.PhotonDisruptor, WeaponType.QuantumDistruptor) == 0) {
      game.Dialogs().alert(AlertType.EncounterAttackNoDisruptors);
      attack = false;
    } else {
      switch(getEncounterType()) {
        case DragonflyIgnore:
        case PirateIgnore:
        case ScarabIgnore:
        case ScorpionIgnore:
        case SpaceMonsterIgnore:
          setEncounterType(EncounterType.FromInt(getEncounterType().CastToInt() - 1));
          break;
        case PoliceInspect:
          if(!game.Commander().getShip().DetectableIllegalCargoOrPassengers() && game.Dialogs().alert(AlertType.EncounterPoliceNothingIllegal) != DialogResult.Yes) {
            attack = false;
          }
          // Fall through...
          if(!attack) {
            break;
          } // goto case PoliceIgnore;
        case MarieCelestePolice:
        case PoliceFlee:
        case PoliceIgnore:
        case PoliceSurrender:
          if(game.Commander().getPoliceRecordScore() <= Consts.PoliceRecordScoreCriminal || game.Dialogs().alert(AlertType.EncounterAttackPolice) == DialogResult.Yes) {
            if(game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
              game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreCriminal);
            }
            game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreAttackPolice);
            if(getEncounterType() != EncounterType.PoliceFlee) {
              setEncounterType(EncounterType.PoliceAttack);
            }
          } else {
            attack = false;
          }
          break;
        case TraderBuy:
        case TraderIgnore:
        case TraderSell:
          if(game.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreClean) {
            game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreAttackTrader);
          } else if(game.Dialogs().alert(AlertType.EncounterAttackTrader) == DialogResult.Yes) {
            game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreDubious);
          } else {
            attack = false;
          }
          // Fall through...
          if(!attack) {
            break;
          }// else goto case TraderAttack;
        case TraderAttack:
        case TraderSurrender:
          if(Functions.GetRandom(Consts.ReputationScoreElite) <= game.Commander().getReputationScore() * 10 / (getOpponent().Type().CastToInt() + 1)
              || getOpponent().WeaponStrength() == 0) {
            setEncounterType(EncounterType.TraderFlee);
          } else {
            setEncounterType(EncounterType.TraderAttack);
          }
          break;
        case CaptainAhab:
        case CaptainConrad:
        case CaptainHuie:
          if(game.Dialogs().alert(AlertType.EncounterAttackCaptain) == DialogResult.Yes) {
            if(game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreVillain) {
              game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreVillain);
            }
            game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreAttackTrader);
            switch(getEncounterType()) {
              case CaptainAhab:
                game.NewsAddEvent(NewsEvent.CaptAhabAttacked);
                break;
              case CaptainConrad:
                game.NewsAddEvent(NewsEvent.CaptConradAttacked);
                break;
              case CaptainHuie:
                game.NewsAddEvent(NewsEvent.CaptHuieAttacked);
                break;
              default:
                break;
            }
            setEncounterType(EncounterType.FamousCaptainAttack);
          } else {
            attack = false;
          }
          break;
        default:
          break;
      }
      // Make sure the fleeing flag isn't set if we're attacking.
      if(attack) {
        setEncounterCmdrFleeing(false);
      }
    }
    return attack;
  }

  public boolean EncounterVerifyBoard(EncounterDialogHost host) {
    boolean board = false;
    if(game.Dialogs().alert(AlertType.EncounterMarieCeleste) == DialogResult.Yes) {
      board = true;
      int narcs = game.Commander().getShip().Cargo()[TradeItemType.Narcotics.CastToInt()];
      host.showPlunder();
      if(game.Commander().getShip().Cargo()[TradeItemType.Narcotics.CastToInt()] > narcs) {
        setJustLootedMarie(true);
      }
    }
    return board;
  }

  public boolean EncounterVerifyBribe() {
    boolean bribed = false;
    if(getEncounterType() == EncounterType.MarieCelestePolice) {
      game.Dialogs().alert(AlertType.EncounterMarieCelesteNoBribe);
    } else if(game.WarpSystem().PoliticalSystem().BribeLevel() <= 0) {
      game.Dialogs().alert(AlertType.EncounterPoliceBribeCant);
    } else if(game.Commander().getShip().DetectableIllegalCargoOrPassengers() || game.Dialogs().alert(AlertType.EncounterPoliceNothingIllegal) == DialogResult.Yes) {
      // Bribe depends on how easy it is to bribe the police and commander's current worth
      int diffMod = 10 + 5 * (Difficulty.Impossible.CastToInt() - game.Difficulty().CastToInt());
      int passMod = game.Commander().getShip().IllegalSpecialCargo() ? (game.Difficulty().CastToInt() <= Difficulty.Normal.CastToInt() ? 2 : 3) : 1;
      int bribe = Math.max(100, Math.min(10000, (int)Math.ceil((double)game.Commander().Worth() / game.WarpSystem().PoliticalSystem().BribeLevel() / diffMod / 100) * 100 * passMod));
      if(game.Dialogs().alert(AlertType.EncounterPoliceBribe, Functions.Multiples(bribe, Strings.MoneyUnit)) == DialogResult.Yes) {
        if(game.Commander().getCash() >= bribe) {
          game.Commander().setCash(game.Commander().getCash() - bribe);
          bribed = true;
        } else {
          game.Dialogs().alert(AlertType.EncounterPoliceBribeLowCash);
        }
      }
    }
    return bribed;
  }

  public boolean EncounterVerifyFlee() {
    setEncounterCmdrFleeing(false);
    if(getEncounterType() != EncounterType.PoliceInspect || game.Commander().getShip().DetectableIllegalCargoOrPassengers()
        || game.Dialogs().alert(AlertType.EncounterPoliceNothingIllegal) == DialogResult.Yes) {
      setEncounterCmdrFleeing(true);
      if(getEncounterType() == EncounterType.MarieCelestePolice && game.Dialogs().alert(AlertType.EncounterPostMarieFlee) == DialogResult.No) {
        setEncounterCmdrFleeing(false);
      } else if(getEncounterType() == EncounterType.PoliceInspect || getEncounterType() == EncounterType.MarieCelestePolice) {
        int scoreMod = getEncounterType() == EncounterType.PoliceInspect ? Consts.ScoreFleePolice : Consts.ScoreAttackPolice;
        int scoreMin = getEncounterType() == EncounterType.PoliceInspect
            ? Consts.PoliceRecordScoreDubious - (game.Difficulty().CastToInt() < Difficulty.Normal.CastToInt() ? 0 : 1) : Consts.PoliceRecordScoreCriminal;
        setEncounterType(EncounterType.PoliceAttack);
        game.Commander().setPoliceRecordScore(Math.min(game.Commander().getPoliceRecordScore() + scoreMod, scoreMin));
      }
    }
    return getEncounterCmdrFleeing();
  }

  public boolean EncounterVerifySubmit() {
    boolean submit = false;
    if(game.Commander().getShip().DetectableIllegalCargoOrPassengers()) {
      String str1 = game.Commander().getShip().IllegalSpecialCargoDescription("", true, true);
      String str2 = game.Commander().getShip().IllegalSpecialCargo() ? Strings.EncounterPoliceSubmitArrested : "";
      if(game.Dialogs().alert(AlertType.EncounterPoliceSubmit, str1, str2) == DialogResult.Yes) {
        submit = true;
        // If you carry illegal goods, they are impounded and you are fined
        if(game.Commander().getShip().DetectableIllegalCargo()) {
          game.Commander().getShip().RemoveIllegalGoods();
          int fine = (int)Math.max(100, Math.min(10000,
              Math.ceil((double)game.Commander().Worth() / ((Difficulty.Impossible.CastToInt() - game.Difficulty().CastToInt() + 2) * 10) / 50) * 50));
          int cashPayment = Math.min(game.Commander().getCash(), fine);
          game.Commander().setDebt(game.Commander().getDebt() + (fine - cashPayment));
          game.Commander().setCash(game.Commander().getCash() - cashPayment);
          game.Dialogs().alert(AlertType.EncounterPoliceFine, Functions.Multiples(fine, Strings.MoneyUnit));
          game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreTrafficking);
        }
      }
    } else {
      submit = true;
      // If you aren't carrying illegal cargo or passengers, the police will increase your lawfulness record
      game.Dialogs().alert(AlertType.EncounterPoliceNothingFound);
      game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() - Consts.ScoreTrafficking);
    }
    return submit;
  }

  public boolean getEncounterCmdrFleeing() {
    return _encounterCmdrFleeing;
  }

  public int getEncounterCmdrDamage() {
    return _encounterCmdrDamage;
  }

  public int getEncounterOppDamage() {
    return _encounterOppDamage;
  }

  public boolean getEncounterCmdrHit() {
    return _encounterCmdrHit;
  }

  public boolean getEncounterContinueAttacking() {
    return _encounterContinueAttacking;
  }

  public boolean setEncounterContinueAttacking(boolean encounterContinueAttacking) {
    _encounterContinueAttacking = encounterContinueAttacking;
    return encounterContinueAttacking;
  }

  public boolean getEncounterContinueFleeing() {
    return _encounterContinueFleeing;
  }

  public boolean getEncounterOppFleeing() {
    return _encounterOppFleeing;
  }

  public boolean getEncounterOppFleeingPrev() {
    return _encounterOppFleeingPrev;
  }

  public boolean getEncounterOppHit() {
    return _encounterOppHit;
  }

  public boolean getInspected() {
    return _inspected;
  }

  public boolean getJustLootedMarie() {
    return _justLootedMarie;
  }

  public boolean getLitterWarning() {
    return _litterWarning;
  }

  public boolean getOpponentDisabled() {
    return _opponentDisabled;
  }

  public boolean setOpponentDisabled(boolean opponentDisabled) {
    _opponentDisabled = opponentDisabled;
    return opponentDisabled;
  }

  public boolean getRaided() {
    return _raided;
  }

  public int EncounterImageIndex() {
    int encounterImage = -1;
    switch(getEncounterType()) {
      case BottleGood:
      case BottleOld:
      case CaptainAhab:
      case CaptainConrad:
      case CaptainHuie:
      case MarieCeleste:
        encounterImage = Consts.EncounterImgSpecial;
        break;
      case DragonflyAttack:
      case DragonflyIgnore:
      case ScarabAttack:
      case ScarabIgnore:
      case ScorpionAttack:
      case ScorpionIgnore:
        encounterImage = Consts.EncounterImgPirate;
        break;
      case MarieCelestePolice:
      case PoliceAttack:
      case PoliceFlee:
      case PoliceIgnore:
      case PoliceInspect:
      case PoliceSurrender:
        encounterImage = Consts.EncounterImgPolice;
        break;
      case PirateAttack:
      case PirateFlee:
      case PirateIgnore:
        if(getOpponent().Type() == ShipType.Mantis) {
          encounterImage = Consts.EncounterImgAlien;
        } else {
          encounterImage = Consts.EncounterImgPirate;
        }
        break;
      case SpaceMonsterAttack:
      case SpaceMonsterIgnore:
        encounterImage = Consts.EncounterImgAlien;
        break;
      case TraderBuy:
      case TraderFlee:
      case TraderIgnore:
      case TraderSell:
        encounterImage = Consts.EncounterImgTrader;
        break;
      case FamousCaptainAttack:
      case FamousCaptDisabled:
      case PoliceDisabled:
      case PirateDisabled:
      case PirateSurrender:
      case TraderAttack:
      case TraderDisabled:
      case TraderSurrender:
        // These should never be the initial encounter type.
        break;
    }
    return encounterImage;
  }

  public void EncounterBegin() {
    // Set up the encounter variables.
    setEncounterContinueFleeing(setEncounterContinueAttacking(setOpponentDisabled(false)));
  }

  public void EncounterDrink() {
    if(game.Dialogs().alert(AlertType.EncounterDrinkContents) == DialogResult.Yes) {
      if(getEncounterType() == EncounterType.BottleGood) {
        // two points if you're on beginner-normal, one otherwise
        game.Commander().IncreaseRandomSkill();
        if(game.Difficulty().CastToInt() <= Difficulty.Normal.CastToInt()) {
          game.Commander().IncreaseRandomSkill();
        }
        game.Dialogs().alert(AlertType.EncounterTonicConsumedGood);
      } else {
        game.Commander().TonicTweakRandomSkill();
        game.Dialogs().alert(AlertType.EncounterTonicConsumedStrange);
      }
    }
  }

  public void EncounterMeet() {
    AlertType initialAlert = AlertType.Alert;
    int skill = 0;
    EquipmentType equipType = EquipmentType.Gadget;
    Object equipSubType = null;

    switch(getEncounterType()) {
      case CaptainAhab:
        // Trade a reflective shield for skill points in piloting?
        initialAlert = AlertType.MeetCaptainAhab;
        equipType = EquipmentType.Shield;
        equipSubType = ShieldType.Reflective;
        skill = SkillType.Pilot.CastToInt();
        break;
      case CaptainConrad:
        // Trade a military laser for skill points in engineering?
        initialAlert = AlertType.MeetCaptainConrad;
        equipType = EquipmentType.Weapon;
        equipSubType = WeaponType.MilitaryLaser;
        skill = SkillType.Engineer.CastToInt();
        break;
      case CaptainHuie:
        // Trade a military laser for skill points in trading?
        initialAlert = AlertType.MeetCaptainHuie;
        equipType = EquipmentType.Weapon;
        equipSubType = WeaponType.MilitaryLaser;
        skill = SkillType.Trader.CastToInt();
        break;
      default:
        break;
    }
    if(game.Dialogs().alert(initialAlert) == DialogResult.Yes) {
      // Remove the equipment we're trading.
      game.Commander().getShip().RemoveEquipment(equipType, equipSubType);
      // Add points to the appropriate skill - two points if beginner-normal, one otherwise.
      game.Commander().Skills()[skill] = Math.min(Consts.MaxSkill, game.Commander().Skills()[skill] + (game.Difficulty().CastToInt() <= Difficulty.Normal.CastToInt() ? 2 : 1));
      game.Dialogs().alert(AlertType.SpecialTrainingCompleted);
    }
  }

  public void EncounterPlunder(EncounterDialogHost host) {
    host.showPlunder();
    if(getEncounterType().CastToInt() >= EncounterType.TraderAttack.CastToInt()) {
      game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScorePlunderTrader);
      if(getOpponentDisabled()) {
        game.Commander().setKillsTrader(game.Commander().getKillsTrader() + 1);
      }
    } else if(getOpponentDisabled()) {
      if(game.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreDubious) {
        game.Dialogs().alert(AlertType.EncounterPiratesBounty, Strings.EncounterPiratesDisabled,
            Strings.EncounterPiratesLocation, Functions.Multiples(getOpponent().Bounty(), Strings.MoneyUnit));
        game.Commander().setCash(game.Commander().getCash() + getOpponent().Bounty());
      }
      game.Commander().setKillsPirate(game.Commander().getKillsPirate() + 1);
      game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScoreKillPirate);
    } else {
      game.Commander().setPoliceRecordScore(game.Commander().getPoliceRecordScore() + Consts.ScorePlunderPirate);
    }
    game.Commander().setReputationScore(game.Commander().getReputationScore() + (getOpponent().Type().CastToInt() / 2 + 1));
  }

  public void EncounterTrade(EncounterDialogHost host) {
    boolean buy = (getEncounterType() == EncounterType.TraderBuy);
    int item = (buy ? game.Commander().getShip() : getOpponent()).GetRandomTradeableItem();
    String alertStr = buy ? Strings.TradeSelling : Strings.TradeBuying;
    int cash = game.Commander().getCash();
    if(getEncounterType() == EncounterType.TraderBuy) {
      host.sellTraderCargo(item);
    } else { // EncounterType.TraderSell
      host.buyTraderCargo(item);
    }
    if(game.Commander().getCash() != cash) {
      game.Dialogs().alert(AlertType.EncounterTradeCompleted, alertStr, Consts.TradeItems.get(item).Name());
    }
  }

  public void setEncounterCmdrFleeing(boolean encounterCmdrFleeing) {
    _encounterCmdrFleeing = encounterCmdrFleeing;
  }

  public void setEncounterCmdrHit(boolean encounterCmdrHit) {
    _encounterCmdrHit = encounterCmdrHit;
  }

  public void setEncounterContinueFleeing(boolean encounterContinueFleeing) {
    _encounterContinueFleeing = encounterContinueFleeing;
  }

  public void setEncounterOppFleeing(boolean encounterOppFleeing) {
    _encounterOppFleeing = encounterOppFleeing;
  }

  public void setEncounterOppFleeingPrev(boolean encounterOppFleeingPrev) {
    _encounterOppFleeingPrev = encounterOppFleeingPrev;
  }

  public void setEncounterOppHit(boolean encounterOppHit) {
    _encounterOppHit = encounterOppHit;
  }

  public void setEncounterType(EncounterType encounterType) {
    _encounterType = encounterType;
  }

  public void setInspected(boolean inspected) {
    _inspected = inspected;
  }

  public void setJustLootedMarie(boolean justLootedMarie) {
    _justLootedMarie = justLootedMarie;
  }

  public void setLitterWarning(boolean litterWarning) {
    _litterWarning = litterWarning;
  }

  public void setOpponent(Ship opponent) {
    _opponent = opponent;
  }

  public void setRaided(boolean raided) {
    _raided = raided;
  }
}
