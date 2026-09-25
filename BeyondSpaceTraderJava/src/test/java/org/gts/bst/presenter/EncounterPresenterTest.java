package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.EncounterType;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterView;
import org.gts.bst.view.EncounterViewModel;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.TestDialogService;
import spacetrader.enums.StarSystemId;


class EncounterPresenterTest {
  @Test
  void showsTheActionsOfAPirateAttack() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.setEncounterType(EncounterType.PirateAttack);

    new EncounterPresenter(game, view, null).start();

    assertEquals(Set.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender), view.model.actions());
    assertFalse(view.model.continueVisible());
  }

  @Test
  void showsTheActionsOfAnAbandonedShip() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.setEncounterType(EncounterType.MarieCeleste);

    new EncounterPresenter(game, view, null).start();

    assertEquals(Set.of(EncounterAction.Board, EncounterAction.Ignore), view.model.actions());
  }

  @Test
  void showsTheActionsOfATrader() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.setEncounterType(EncounterType.TraderBuy);

    new EncounterPresenter(game, view, null).start();

    assertEquals(Set.of(EncounterAction.Attack, EncounterAction.Ignore, EncounterAction.Trade), view.model.actions());
  }

  @Test
  void showsTheInterruptWhileRepeatingAnAction() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.setEncounterType(EncounterType.PirateAttack);
    EncounterPresenter presenter = new EncounterPresenter(game, view, null);
    presenter.start();

    game.setEncounterContinueAttacking(true);
    presenter.update();

    assertTrue(view.model.continueVisible());
    assertTrue(view.model.actions().contains(EncounterAction.Interrupt));
  }

  @Test
  void ignoringTheEncounterClosesItAsNormal() {
    Game game = newGame();
    FakeView view = new FakeView();
    game.setEncounterType(EncounterType.PirateAttack);
    EncounterPresenter presenter = new EncounterPresenter(game, view, null);
    presenter.start();

    presenter.ignore();

    assertTrue(view.closed);
    assertEquals(EncounterResult.Normal, presenter.result());
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
  }
}
