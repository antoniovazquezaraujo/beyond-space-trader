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

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.ShipyardDesignerViewModel;
import org.gts.bst.view.ShipyardView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.ShipTemplate;
import spacetrader.Shipyard;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.TestDialogService;


class ShipyardPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheDesignerWithTheCurrentShipTemplate() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    Shipyard yard = game.Commander().CurrentSystem().Shipyard();
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);

    presenter.start();

    assertEquals(Functions.StringVars(Strings.ShipyardTitle, yard.Name()), view.model.title());
    assertFalse(view.model.sizes().isEmpty());
    assertEquals(7, view.model.numerics().size());
    assertEquals(game.Commander().getShip().Name(), view.model.name());
    assertTrue(view.model.constructEnabled());
  }

  @Test
  void changingTheSizeUpdatesTheSpec() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    Shipyard yard = game.Commander().CurrentSystem().Shipyard();
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();

    presenter.onSizeChanged(0);

    assertEquals(0, view.model.sizeIndex());
    assertEquals(yard.AvailableSizes().get(0), yard.ShipSpec().getSize());
  }

  @Test
  void changingValuesSnapsFuelAndHullToWholeUnits() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    Shipyard yard = game.Commander().CurrentSystem().Shipyard();
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();
    int baseFuel = yard.BaseFuel();
    int baseHull = yard.BaseHull();

    presenter.onValuesChanged(20, baseFuel + 1, baseHull + 1, 1, 1, 1, 1);

    assertEquals(0, (yard.ShipSpec().FuelTanks() - baseFuel) % yard.PerUnitFuel());
    assertEquals(0, (yard.ShipSpec().HullStrength() - baseHull) % yard.PerUnitHull());
  }

  @Test
  void theNameEnablesAndDisablesTheConstructButton() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();

    presenter.onNameChanged("");
    assertFalse(view.model.constructEnabled());
    assertFalse(view.model.saveEnabled());

    presenter.onNameChanged("My Ship");
    assertTrue(view.model.constructEnabled());
    assertTrue(view.model.saveEnabled());
  }

  @Test
  void constructingBuildsTheCustomShip() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    moveToAShipyardSystem(game);
    game.Commander().setCash(1000000);
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();
    presenter.onNameChanged("My Ship");
    presenter.customImageLoaded();
    dialogs.setResult(DialogResult.Yes);

    presenter.construct("My Ship");

    assertEquals(ShipType.Custom, game.Commander().getShip().Type());
    assertEquals("My Ship", Strings.ShipNames.get(ShipType.Custom.CastToInt()));
    assertTrue(view.customShipImagesApplied);
    assertTrue(view.closed);
  }

  @Test
  void choosingACustomImageSwitchesThePreview() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();

    presenter.customImageLoaded();

    assertTrue(view.model.customImage());
    assertEquals(Strings.ShipNameCustomShip, view.model.imageName());
  }

  @Test
  void savingWithAnEmptyNameDoesNotAskForAFile() {
    Game game = newGame(new TestDialogService());
    moveToAShipyardSystem(game);
    FakeView view = new FakeView();
    ShipyardPresenter presenter = new ShipyardPresenter(game, view);
    presenter.start();

    presenter.saveTemplate("");

    assertEquals(0, view.saveFileRequests);
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static void moveToAShipyardSystem(Game game) {
    for(StarSystem system : game.Universe()) {
      if(system.Shipyard() != null) {
        game.Commander().CurrentSystem(system);
        return;
      }
    }
    throw new AssertionError("no shipyard in the universe");
  }

  private static class FakeView implements ShipyardView {
    private ShipyardDesignerViewModel model;
    private boolean closed;
    private boolean customShipImagesApplied;
    private int saveFileRequests;

    @Override
    public void render(ShipyardDesignerViewModel model) {
      this.model = model;
    }

    @Override
    public void close() {
      closed = true;
    }

    @Override
    public void showFileError(String fileName, String message) {
      // No action.
    }

    @Override
    public String askSaveTemplateFile() {
      saveFileRequests++;
      return null;
    }

    @Override
    public void adoptTemplateImages(ShipTemplate template) {
      // No action.
    }

    @Override
    public void applyCustomImages(ShipTemplate template) {
      // No action.
    }

    @Override
    public void applyCustomShipImages() {
      customShipImagesApplied = true;
    }
  }
}
