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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.cargo.CargoSellOp;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.EncounterType;
import org.gts.bst.ports.MusicTheme;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.Weapon;
import org.gts.bst.ship.equip.WeaponType;
import org.gts.bst.view.AlertDefinition;
import org.gts.bst.view.Alerts;
import org.gts.bst.view.EncounterAction;
import spacetrader.Strings;
import org.gts.bst.ports.EncounterDialogHost;
import org.gts.bst.view.EncounterView;
import org.gts.bst.view.EncounterViewModel;
import org.gts.bst.view.ShipCatalog;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Trade;
import spacetrader.Ship;
import spacetrader.enums.AlertType;


/**
 * Drives an encounter: renders the available actions and the state, and routes the
 * player's commands to the model. It also implements the screens the model asks for
 * while resolving an encounter.
 */
public class EncounterPresenter implements EncounterDialogHost {
  private final Game game;
  private final Ship cmdrship;
  private final EncounterView view;
  private EncounterResult result = EncounterResult.Continue;
  /** The number of the round: every one is a new part for the scene. */
  private int round;
  private boolean running;

  public EncounterPresenter(Game game, EncounterView view) {
    this.game = game;
    this.cmdrship = game.Commander().getShip();
    this.view = view;
  }

  public void start() {
    game.encounter().EncounterBegin();
    view.weapons(() -> strongest(cmdrship), () -> strongest(game.encounter().getOpponent()));
    render();
  }

  public EncounterResult result() {
    return result;
  }

  /**
   * Called by the view timer for automatic attacks or fleeing.
   */
  public void tick() {
    executeAction();
  }

  public void attack() {
    disableAuto();
    if(game.encounter().EncounterVerifyAttack()) {
      executeAction();
    }
  }

  public void board() {
    if(game.encounter().EncounterVerifyBoard(this)) {
      exit(EncounterResult.Normal);
    }
  }

  public void bribe() {
    if(game.encounter().EncounterVerifyBribe()) {
      exit(EncounterResult.Normal);
    }
  }

  public void drink() {
    game.encounter().EncounterDrink();
    exit(EncounterResult.Normal);
  }

  public void flee() {
    disableAuto();
    if(result != EncounterResult.Continue) {
      // The encounter is over: there is nothing to flee from.
      return;
    }
    if(game.encounter().EncounterVerifyFlee()) {
      executeAction();
    }
  }

  public void ignore() {
    disableAuto();
    exit(EncounterResult.Normal);
  }

  public void interest() {
    disableAuto();
    update();
  }

  public void meet() {
    game.encounter().EncounterMeet();
    exit(EncounterResult.Normal);
  }

  public void plunder() {
    disableAuto();
    game.encounter().EncounterPlunder(this);
    exit(EncounterResult.Normal);
  }

  public void submit() {
    int before = cargoItems();
    if(game.encounter().EncounterVerifySubmit()) {
      // The scanner goes over the ship; if they took cargo, the catwalk follows.
      view.inspection(cargoItems() < before);
      exit(cmdrship.IllegalSpecialCargo() ? EncounterResult.Arrested : EncounterResult.Normal);
    }
  }

  private int cargoItems() {
    int total = 0;
    for(int bays : cmdrship.Cargo()) {
      total += bays;
    }
    return total;
  }

  public void surrender() {
    disableAuto();
    int before = cmdrship.FilledCargoBays();
    int[] cargoBefore = cmdrship.Cargo().clone();
    result = game.encounter().EncounterVerifySurrender();
    if(result != EncounterResult.Continue) {
      if(result == EncounterResult.Arrested) {
        // The police read the arrest under their ship, and the scene waits for the
        // player to take it in before it closes (the jail follows on its own).
        view.speechAndWait(alertMessage(AlertType.EncounterArrested));
      }
      if(result == EncounterResult.Normal && game.encounter().getEncounterType() == EncounterType.PirateAttack
          && game.encounter().getOpponent().Type() != ShipType.Mantis) {
        // The pirates looted the ship: the scene shows the transfer only when
        // they actually took cargo; the Mantis case gave up the artifact.
        view.looted(cmdrship.FilledCargoBays() < before);
      }
      String taken = takenCargo(cargoBefore);
      if(!taken.isEmpty()) {
        // What the holds lost is kept in the log of the scene, under the ships.
        view.log(Functions.StringVars(Strings.EncounterPiratesTake, taken));
      }
      view.close();
    }
  }

  /** What the holds lost, as "2 Water, 1 Furs"; empty when nothing was taken. */
  private String takenCargo(int[] before) {
    StringBuilder taken = new StringBuilder();
    for(int i = 0; i < before.length && i < cmdrship.Cargo().length && i < Consts.TradeItems.size(); i++) {
      int lost = before[i] - cmdrship.Cargo()[i];
      if(lost <= 0) {
        continue;
      }
      if(taken.length() > 0) {
        taken.append(", ");
      }
      taken.append(lost).append(' ').append(Consts.TradeItems.get(i).Name());
    }
    return taken.toString();
  }

  public void trade() {
    // The catwalk and the question run inside the offer (the scene of the trade).
    game.encounter().EncounterTrade(this);
    exit(EncounterResult.Normal);
  }

  public void yield() {
    result = game.encounter().EncounterVerifyYield();
    if(result != EncounterResult.Continue) {
      if(result == EncounterResult.Arrested) {
        // Surrendering to the police ends in the arrest.
        view.speechAndWait(alertMessage(AlertType.EncounterArrested));
      } else if(result == EncounterResult.Normal) {
        // The police took the illegal cargo and let us go for cooperating.
        view.speechAndWait(alertMessage(AlertType.EncounterPostMarie));
      }
      view.close();
    }
  }

  /** The message of a predefined alert, to say it under the rival before closing. */
  private static String alertMessage(AlertType type) {
    AlertDefinition definition = Alerts.get(type);
    return definition == null ? "" : definition.message();
  }

  @Override
  public void showJettison() {
    view.showJettison();
  }

  @Override
  public void showPlunder() {
    view.showPlunder();
  }

  @Override
  public void buyTraderCargo(int tradeItem) {
    CargoBuyOffer offer = Trade.CargoBuyOffer(game, tradeItem, CargoBuyOp.BuyTrader);
    if(offer != null) {
      Integer qty = view.askCargoBuyQuantity(offer);
      if(qty != null) {
        Trade.CargoBuy(game, offer, qty);
      }
    }
  }

  @Override
  public void sellTraderCargo(int tradeItem) {
    CargoSellOffer offer = Trade.CargoSellOffer(game, tradeItem, CargoSellOp.SellTrader);
    if(offer != null) {
      Integer qty = view.askCargoSellQuantity(offer);
      if(qty != null) {
        Trade.CargoSell(game, offer, qty);
      }
    }
  }

  private void executeAction() {
    // The auto-attack timer can leave ticks queued, and resolving a round may open a
    // modal alert (which keeps pumping the GUI loop): ignore late and re-entrant ticks
    // once the round is running or the encounter has ended.
    if(running || result != EncounterResult.Continue) {
      return;
    }
    running = true;
    try {
      result = game.encounter().EncounterExecuteAction(this);
    } finally {
      running = false;
    }
    if(result == EncounterResult.Continue) {
      round++;
      update();
      if(game.encounter().getEncounterContinueFleeing() || game.encounter().getEncounterContinueAttacking()) {
        view.startTimer();
      }
    } else {
      // A chase that ends with us getting away: the other ship loses us.
      if(result == EncounterResult.Normal && game.encounter().getEncounterCmdrFleeing()) {
        view.escaped();
      }
      view.close();
    }
  }

  public void update() {
    render();
  }

  /** Renders a part: the tension picks the music and the rest is the model. */
  private void render() {
    view.music(music(game.encounter().getEncounterType()));
    view.render(model(game.encounter().EncounterText(), game.encounter().EncounterAction()));
  }

  /**
   * The music of an encounter: tension for the pirates, the police and the
   * mission hunters; calm for the traders and the rare encounters (captains,
   * bottles, Marie Celeste). Re-evaluated on every part, so a trader that turns
   * violent (or the police showing up) crossfades to tension.
   */
  static MusicTheme music(EncounterType type) {
    switch(type) {
      case PirateAttack:
      case PirateIgnore:
      case PirateFlee:
      case PirateSurrender:
      case PirateDisabled:
      case PoliceAttack:
      case PoliceIgnore:
      case PoliceFlee:
      case PoliceSurrender:
      case PoliceDisabled:
      case PoliceInspect:
      case MarieCelestePolice:
      case DragonflyAttack:
      case FamousCaptainAttack:
      case ScorpionAttack:
      case SpaceMonsterAttack:
      case ScarabAttack:
      case TraderAttack:
        return MusicTheme.TENSE;
      default:
        return MusicTheme.CALM;
    }
  }

  /** The weapon that sounds when a ship fires: the strongest one on board. */
  static WeaponType strongest(Ship ship) {
    return strongest(ship.Weapons());
  }

  static WeaponType strongest(Weapon[] weapons) {
    WeaponType best = null;
    for(Weapon weapon : weapons) {
      if(weapon == null) {
        continue;
      }
      WeaponType type = weapon.Type();
      if(best == null || rank(type) < rank(best)) {
        best = type;
      }
    }
    return best;
  }

  /** The loudest shot first: Morgan's, Military, Beam, Pulse, Quantum, Photon. */
  private static int rank(WeaponType type) {
    switch(type) {
      case MorgansLaser:
        return 0;
      case MilitaryLaser:
        return 1;
      case BeamLaser:
        return 2;
      case PulseLaser:
        return 3;
      case QuantumDistruptor:
        return 4;
      case PhotonDisruptor:
        return 5;
      default:
        return 6;
    }
  }

  private void exit(EncounterResult result) {
    this.result = result;
    view.close();
  }

  private void disableAuto() {
    view.stopTimer();
    game.encounter().setEncounterContinueFleeing(false);
    game.encounter().setEncounterContinueAttacking(false);
  }

  private EncounterViewModel model(String encounterText, String actionText) {
    List<String> opponentItems = new ArrayList<>(ShipArtItems.of(game.encounter().getOpponent()));
    String role = ShipArtItems.role(game.encounter().getEncounterType());
    if(role != null) {
      opponentItems.add(role);
    }
    ShipCatalog catalog = ShipCatalog.shared();
    return new EncounterViewModel(actions(),
        game.encounter().getEncounterContinueAttacking() || game.encounter().getEncounterContinueFleeing(),
        game.encounter().EncounterImageIndex(),
        cmdrship.Name(), bar(cmdrship.getHull(), cmdrship.HullStrength()),
        bar(cmdrship.ShieldCharge(), cmdrship.ShieldStrength()),
        game.encounter().getOpponent().Name(),
        bar(game.encounter().getOpponent().getHull(), game.encounter().getOpponent().HullStrength()),
        bar(game.encounter().getOpponent().ShieldCharge(), game.encounter().getOpponent().ShieldStrength()),
        encounterText, actionText,
        cmdrship.Type(), game.encounter().getOpponent().Type(),
        game.encounter().getEncounterOppHit(), game.encounter().getEncounterCmdrHit(),
        game.encounter().getEncounterOppDamage(), game.encounter().getEncounterCmdrDamage(),
        catalog.picture(cmdrship.Type(), ShipArtItems.of(cmdrship), cmdrship.CargoBays()),
        catalog.picture(game.encounter().getOpponent().Type(), opponentItems,
            game.encounter().getOpponent().CargoBays()).mirrored(),
        game.encounter().getOpponentDisabled(),
        !game.encounter().getEncounterCmdrFleeing(),
        opponentIgnores(),
        game.encounter().getOpponent().Pilot(),
        game.encounter().getEncounterCmdrFleeing(),
        round,
        speech(),
        ShipArtItems.of(cmdrship),
        opponentItems,
        cmdrship.CargoBays(),
        game.encounter().getOpponent().CargoBays());
  }

  /** What the other ship says: the police demand, the trader hail; nothing for the rest. */
  private String speech() {
    String type = game.encounter().getEncounterType().name();
    if(type.equals("PoliceInspect")) {
      return Strings.EncounterSaysPolice;
    }
    if(type.equals("PoliceSurrender")) {
      return Strings.EncounterSaysPoliceArrest;
    }
    if(type.equals("TraderSell")) {
      return Strings.EncounterSaysTraderSells;
    }
    if(type.equals("TraderBuy")) {
      return Strings.EncounterSaysTraderBuys;
    }
    return "";
  }

  /** True when the other ship ignores us: it does not react to our manoeuvres. */
  private boolean opponentIgnores() {
    return opponentLeaves(game.encounter().getEncounterType().name(), cmdrship.Cloaked());
  }

  /**
   * True when the other ship is out of the fight (it ignores us or flees), so it
   * stops blocking the way; a cloaked ship is not seen either. A ship that
   * surrenders does not leave: it stays in front waiting for our decision
   * (attack or plunder it), and so does the police demanding our surrender.
   */
  static boolean opponentLeaves(String encounterType, boolean cloaked) {
    return encounterType.endsWith("Ignore") || encounterType.endsWith("Flee") || cloaked;
  }

  private static EncounterViewModel.Bar bar(int value, int max) {
    return new EncounterViewModel.Bar(value, max);
  }

  private Set<EncounterAction> actions() {
    Set<EncounterAction> actions = EnumSet.noneOf(EncounterAction.class);
    switch(game.encounter().getEncounterType()) {
      case BottleGood:
      case BottleOld:
        actions.add(EncounterAction.Drink);
        actions.add(EncounterAction.Ignore);
        break;
      case CaptainAhab:
      case CaptainConrad:
      case CaptainHuie:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Ignore);
        actions.add(EncounterAction.Meet);
        break;
      case DragonflyAttack:
      case FamousCaptainAttack:
      case ScorpionAttack:
      case SpaceMonsterAttack:
      case TraderAttack:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Flee);
        break;
      case DragonflyIgnore:
      case FamousCaptDisabled:
      case PoliceDisabled:
      case PoliceFlee:
      case PoliceIgnore:
      case PirateFlee:
      case PirateIgnore:
      case ScarabIgnore:
      case ScorpionIgnore:
      case SpaceMonsterIgnore:
      case TraderFlee:
      case TraderIgnore:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Ignore);
        break;
      case MarieCeleste:
        actions.add(EncounterAction.Board);
        actions.add(EncounterAction.Ignore);
        break;
      case MarieCelestePolice:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Flee);
        actions.add(EncounterAction.Yield);
        actions.add(EncounterAction.Bribe);
        break;
      case PirateAttack:
      case PoliceAttack:
      case PoliceSurrender:
      case ScarabAttack:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Flee);
        actions.add(EncounterAction.Surrender);
        break;
      case PirateDisabled:
      case PirateSurrender:
      case TraderDisabled:
      case TraderSurrender:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Plunder);
        break;
      case PoliceInspect:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Flee);
        actions.add(EncounterAction.Submit);
        actions.add(EncounterAction.Bribe);
        break;
      case TraderBuy:
      case TraderSell:
        actions.add(EncounterAction.Attack);
        actions.add(EncounterAction.Ignore);
        actions.add(EncounterAction.Trade);
        break;
    }
    if(game.encounter().getEncounterContinueAttacking() || game.encounter().getEncounterContinueFleeing()) {
      actions.add(EncounterAction.Interrupt);
    }
    return actions;
  }
}

