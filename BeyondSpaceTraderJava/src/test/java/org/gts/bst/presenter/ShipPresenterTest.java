package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.ShipView;
import org.gts.bst.view.ShipViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.Strings;


class ShipPresenterTest {
  private static final String NL = Strings.newline;

  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheStartingShip() {
    RecordingView view = new RecordingView();

    new ShipPresenter(newGame(), view).update();

    assertEquals("Gnat", view.model.type());
    assertEquals("Equipment:" + NL + NL + "Unfilled:", view.model.equipmentLabels());
    assertEquals("1 Pulse Laser" + NL + NL + "1 gadget slot" + NL, view.model.equipmentValues());
    assertEquals("No special items.", view.model.specialCargo());
  }

  @Test
  void showsSpecialCargo() {
    Game game = newGame();
    game.Commander().getShip().setTribbles(5);
    RecordingView view = new RecordingView();

    new ShipPresenter(game, view).update();

    // StringsJoin appends the separator after every element, including the last one.
    assertEquals("5 cute, furry tribbles." + NL + NL, view.model.specialCargo());
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
  }

  private static class RecordingView implements ShipView {
    private ShipViewModel model;

    @Override
    public void render(ShipViewModel model) {
      this.model = model;
    }
  }
}
