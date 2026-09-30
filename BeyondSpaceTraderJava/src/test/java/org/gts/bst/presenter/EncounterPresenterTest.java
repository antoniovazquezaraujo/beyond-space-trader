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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.EncounterType;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterView;
import org.gts.bst.view.EncounterViewModel;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;


class EncounterPresenterTest {
  @Test
  void showsTheActionsOfAPirateAttack() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.PirateAttack);

    new EncounterPresenter(game, view).start();

    assertEquals(Set.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender), view.model.actions());
    assertFalse(view.model.continueVisible());
  }

  @Test
  void showsTheActionsOfAnAbandonedShip() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.MarieCeleste);

    new EncounterPresenter(game, view).start();

    assertEquals(Set.of(EncounterAction.Board, EncounterAction.Ignore), view.model.actions());
  }

  @Test
  void showsTheActionsOfATrader() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.TraderBuy);

    new EncounterPresenter(game, view).start();

    assertEquals(Set.of(EncounterAction.Attack, EncounterAction.Ignore, EncounterAction.Trade), view.model.actions());
  }

  @Test
  void thePoliceSaysTheDemandOnItsShip() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.PoliceInspect);

    new EncounterPresenter(game, view).start();

    assertEquals(Strings.EncounterSaysPolice, view.model.speech(), "the demand of the inspection");

    game.encounter().setEncounterType(EncounterType.PoliceSurrender);
    new EncounterPresenter(game, view).start();

    assertEquals(Strings.EncounterSaysPoliceArrest, view.model.speech(), "the demand of the surrender");
  }

  @Test
  void theTraderSaysWhatItWantsOnItsShip() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.TraderSell);

    new EncounterPresenter(game, view).start();
    assertEquals(Strings.EncounterSaysTraderSells, view.model.speech());

    game.encounter().setEncounterType(EncounterType.TraderBuy);
    new EncounterPresenter(game, view).start();
    assertEquals(Strings.EncounterSaysTraderBuys, view.model.speech());
  }

  @Test
  void aPirateSaysNothing() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.PirateAttack);

    new EncounterPresenter(game, view).start();

    assertEquals("", view.model.speech());
  }

  @Test
  void showsTheInterruptWhileRepeatingAnAction() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.PirateAttack);
    EncounterPresenter presenter = new EncounterPresenter(game, view);
    presenter.start();

    game.encounter().setEncounterContinueAttacking(true);
    presenter.update();

    assertTrue(view.model.continueVisible());
    assertTrue(view.model.actions().contains(EncounterAction.Interrupt));
  }

  @Test
  void ignoringTheEncounterClosesItAsNormal() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.encounter().setEncounterType(EncounterType.PirateAttack);
    EncounterPresenter presenter = new EncounterPresenter(game, view);
    presenter.start();

    presenter.ignore();

    assertTrue(view.closed);
    assertEquals(EncounterResult.Normal, presenter.result());
  }

  @Test
  void theJettisonScreenGoesThroughTheView() {
    Game game = newGame();
    FakeView view = new FakeView();
    EncounterPresenter presenter = new EncounterPresenter(game, view);

    presenter.showJettison();

    assertTrue(view.jettisonShown);
  }

  @Test
  void theModelCarriesTheRoundResult() {
    Game game = newGame();
    game.encounter().setEncounterType(EncounterType.PirateAttack);
    // The starting Gnat carries a pulse laser: without it the player's shot can never
    // hit, so the round result is deterministic.
    game.Commander().getShip().RemoveEquipment(EquipmentType.Weapon, 0);
    FakeView view = new FakeView();
    EncounterPresenter presenter = new EncounterPresenter(game, view);
    presenter.start();

    presenter.tick();

    assertFalse(view.model.youHit());
    assertEquals(0, view.model.youDamage());
    assertTrue(view.model.oppDamage() >= 0);
  }

  @Test
  void fleeingAtBeginnerDifficultyEscapesUnhurt() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Antonio", Difficulty.Beginner, 4, 4, 4, 4, null, dialogs);
    game.SelectedSystemId(StarSystemId.FromInt(0));
    game.encounter().setEncounterType(EncounterType.PirateAttack);
    FakeView view = new FakeView();
    EncounterPresenter presenter = new EncounterPresenter(game, view);
    presenter.start();
    int hull = game.Commander().getShip().getHull();

    presenter.flee();

    assertEquals(hull, game.Commander().getShip().getHull(), "on Beginner fleeing is unharmed");
    assertTrue(dialogs.alerts().contains(AlertType.EncounterEscaped), dialogs.alerts().toString());
    assertTrue(view.closed);
  }

  @Test
  void ignoresLateTicksOnceTheEncounterEnded() {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    game.SelectedSystemId(StarSystemId.FromInt(0));
    game.encounter().setEncounterType(EncounterType.PirateAttack);
    game.Commander().getShip().setHull(0);
    FakeView view = new FakeView();
    EncounterPresenter presenter = new EncounterPresenter(game, view);

    presenter.start();
    presenter.tick();
    presenter.tick();

    assertEquals(1, dialogs.alerts().stream().filter(alert -> alert == AlertType.EncounterYouLose).count(),
        dialogs.alerts().toString());
    assertTrue(view.closed);
  }

  @Test
  void buyingFromTheTraderUsesTheCargoOffer() {
    Game game = newGame();
    game.encounter().getOpponent().Cargo()[0] = 3;
    game.Commander().setCash(10000);
    FakeView view = new FakeView();
    view.cargoBuyAnswer = 1;
    EncounterPresenter presenter = new EncounterPresenter(game, view);

    presenter.buyTraderCargo(0);

    assertEquals(2, game.encounter().getOpponent().Cargo()[0]);
    assertEquals(1, game.Commander().getShip().Cargo()[0]);
  }

  private static Game newGame() {
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.SelectedSystemId(StarSystemId.FromInt(0));
    return game;
  }

  private static class FakeView implements EncounterView {
    private EncounterViewModel model;
    private boolean closed;
    private boolean timer;
    private boolean jettisonShown;
    private boolean plunderShown;
    private Integer cargoBuyAnswer;
    private Integer cargoSellAnswer;

    @Override
    public void render(EncounterViewModel model) {
      this.model = model;
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public void startTimer() {
      timer = true;
    }

    @Override
    public void stopTimer() {
      timer = false;
    }

    @Override
    public void showJettison() {
      jettisonShown = true;
    }

    @Override
    public void showPlunder() {
      plunderShown = true;
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

  @Test
  void theOtherShipStopsBlockingWhenItLeavesTheFight() {
    assertTrue(EncounterPresenter.opponentLeaves("PirateIgnore", false), "it ignores us");
    assertTrue(EncounterPresenter.opponentLeaves("PirateFlee", false), "it flees");
    assertTrue(EncounterPresenter.opponentLeaves("PoliceSurrender", false), "it surrenders");
    assertTrue(EncounterPresenter.opponentLeaves("PirateAttack", true), "it cannot see a cloaked ship");
    assertFalse(EncounterPresenter.opponentLeaves("PirateAttack", false), "while it attacks, it blocks");
    assertFalse(EncounterPresenter.opponentLeaves("TraderBuy", false));
  }
}

