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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.view.EquipmentInfoViewModel;
import org.gts.bst.view.EquipmentView;
import org.gts.bst.view.EquipmentViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.enums.AlertType;


class EquipmentPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheListsAndTheSelectedWeapon() {
    Game game = newGame(new TestDialogService());
    moveToHighTechSystem(game);
    Ship ship = game.Commander().getShip();
    FakeView view = new FakeView();
    EquipmentPresenter presenter = new EquipmentPresenter(game, view);

    presenter.update();

    assertEquals(ship.Weapons()[0].Name(), view.model.sellWeapons().get(0));
    assertEquals(Strings.EquipmentFreeSlot, view.model.sellGadgets().get(0));
    assertTrue(view.model.buyWeapons().contains(ship.Weapons()[0].Name()));

    presenter.select(EquipmentType.Weapon, false, view.model.buyWeapons().indexOf(ship.Weapons()[0].Name()));
    assertTrue(view.info.visible());
    assertEquals(ship.Weapons()[0].Name(), view.info.name());
    assertTrue(view.info.buyVisible());
    assertFalse(view.info.sellVisible());

    presenter.select(EquipmentType.Weapon, true, 0);
    assertEquals(ship.Weapons()[0].Name(), view.info.name());
    assertTrue(view.info.sellVisible());
    assertFalse(view.info.buyVisible());
    assertTrue(view.info.power().matches("\\d+"));
  }

  @Test
  void buysAGadget() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    moveToHighTechSystem(game);
    Ship ship = game.Commander().getShip();
    game.Commander().setCash(100000);
    FakeView view = new FakeView();
    EquipmentPresenter presenter = new EquipmentPresenter(game, view);
    presenter.update();
    int freeBefore = ship.FreeSlotsGadget();

    presenter.select(EquipmentType.Gadget, false, 0);
    dialogs.setResult(DialogResult.Yes);

    assertTrue(presenter.buy());
    assertEquals(freeBefore - 1, ship.FreeSlotsGadget());
    assertTrue(game.Commander().getCash() < 100000);
    assertFalse(view.info.visible());
  }

  @Test
  void warnsAboutDebt() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    moveToHighTechSystem(game);
    game.Commander().setCash(100000);
    game.Commander().setDebt(10);
    FakeView view = new FakeView();
    EquipmentPresenter presenter = new EquipmentPresenter(game, view);
    presenter.update();
    presenter.select(EquipmentType.Gadget, false, 0);
    dialogs.setResult(DialogResult.Yes);

    assertFalse(presenter.buy());
    assertEquals(List.of(AlertType.DebtNoBuy), dialogs.alerts());
  }

  @Test
  void sellsTheStartingLaser() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    Ship ship = game.Commander().getShip();
    FakeView view = new FakeView();
    EquipmentPresenter presenter = new EquipmentPresenter(game, view);
    presenter.update();
    presenter.select(EquipmentType.Weapon, true, 0);
    dialogs.setResult(DialogResult.Yes);

    assertTrue(presenter.sell());
    assertNull(ship.Weapons()[0]);
    assertEquals(2500, game.Commander().getCash());
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static void moveToHighTechSystem(Game game) {
    StarSystem best = null;
    for(StarSystem system : game.Universe()) {
      if(best == null || system.TechLevel().ordinal() > best.TechLevel().ordinal()) {
        best = system;
      }
    }
    game.Commander().CurrentSystem(best);
  }

  private static class FakeView implements EquipmentView {
    private EquipmentViewModel model;
    private EquipmentInfoViewModel info;

    @Override
    public void render(EquipmentViewModel model) {
      this.model = model;
    }

    @Override
    public void renderInfo(EquipmentInfoViewModel info) {
      this.info = info;
    }
  }
}

