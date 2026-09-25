package org.gts.bst.presenter;

import java.util.EnumSet;
import java.util.Set;
import jwinforms.WinformPane;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterView;
import org.gts.bst.view.EncounterViewModel;
import spacetrader.Game;
import spacetrader.Ship;


/**
 * Drives an encounter: renders the available actions and the state, and routes the
 * player's commands to the model.
 *
 * <p>The {@code owner} is transitional: the model still opens the jettison and plunder
 * dialogs. It will disappear when those screens are migrated.
 */
public class EncounterPresenter {
  private final Game game;
  private final Ship cmdrship;
  private final EncounterView view;
  private final WinformPane owner;
  private EncounterResult result = EncounterResult.Continue;

  public EncounterPresenter(Game game, EncounterView view, WinformPane owner) {
    this.game = game;
    this.cmdrship = game.Commander().getShip();
    this.view = view;
    this.owner = owner;
  }

  public void start() {
    game.EncounterBegin();
    view.render(model(game.EncounterTextInitial(), game.EncounterActionInitial()));
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
    if(game.EncounterVerifyAttack()) {
      executeAction();
    }
  }

  public void board() {
    if(game.EncounterVerifyBoard(owner)) {
      exit(EncounterResult.Normal);
    }
  }

  public void bribe() {
    if(game.EncounterVerifyBribe()) {
      exit(EncounterResult.Normal);
    }
  }

  public void drink() {
    game.EncounterDrink();
    exit(EncounterResult.Normal);
  }

  public void flee() {
    disableAuto();
    if(game.EncounterVerifyFlee()) {
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
    game.EncounterMeet();
    exit(EncounterResult.Normal);
  }

  public void plunder() {
    disableAuto();
    game.EncounterPlunder(owner);
    exit(EncounterResult.Normal);
  }

  public void submit() {
    if(game.EncounterVerifySubmit()) {
      exit(cmdrship.IllegalSpecialCargo() ? EncounterResult.Arrested : EncounterResult.Normal);
    }
  }

  public void surrender() {
    disableAuto();
    result = game.EncounterVerifySurrender();
    if(result != EncounterResult.Continue) {
      view.close();
    }
  }

  public void trade() {
    game.EncounterTrade(owner);
    exit(EncounterResult.Normal);
  }

  public void yield() {
    result = game.EncounterVerifyYield();
    if(result != EncounterResult.Continue) {
      view.close();
    }
  }

  private void executeAction() {
    result = game.EncounterExecuteAction(owner);
    if(result == EncounterResult.Continue) {
      update();
      if(game.getEncounterContinueFleeing() || game.getEncounterContinueAttacking()) {
        view.startTimer();
      }
    } else {
      view.close();
    }
  }

  public void update() {
    view.render(model(game.EncounterText(), game.EncounterAction()));
  }

  private void exit(EncounterResult result) {
    this.result = result;
    view.close();
  }

  private void disableAuto() {
    view.stopTimer();
    game.setEncounterContinueFleeing(false);
    game.setEncounterContinueAttacking(false);
  }

  private EncounterViewModel model(String encounterText, String actionText) {
    return new EncounterViewModel(actions(),
        game.getEncounterContinueAttacking() || game.getEncounterContinueFleeing(),
        game.EncounterImageIndex(),
        cmdrship.Name(), cmdrship.HullText(), cmdrship.ShieldText(),
        game.getOpponent().Name(), game.getOpponent().HullText(), game.getOpponent().ShieldText(),
        encounterText, actionText);
  }

  private Set<EncounterAction> actions() {
    Set<EncounterAction> actions = EnumSet.noneOf(EncounterAction.class);
    switch(game.getEncounterType()) {
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
    if(game.getEncounterContinueAttacking() || game.getEncounterContinueFleeing()) {
      actions.add(EncounterAction.Interrupt);
    }
    return actions;
  }
}
