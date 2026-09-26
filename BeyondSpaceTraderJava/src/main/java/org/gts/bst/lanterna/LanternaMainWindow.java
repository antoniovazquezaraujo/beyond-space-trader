/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.Set;
import java.util.function.Supplier;
import jwinforms.ImageList;
import jwinforms.WfImage;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.CargoViewModel;
import org.gts.bst.view.ChartsViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.GameWindow;
import org.gts.bst.view.MainStatusViewModel;
import org.gts.bst.view.MainView;
import org.gts.bst.view.MainWindow;
import org.gts.bst.view.ShipyardViewModel;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.StarSystem;
import spacetrader.Strings;


/**
 * The main window of the text UI. It renders the view models through
 * {@link MainTextComponent} and forwards the keys to the game and the presenter.
 */
public final class LanternaMainWindow implements MainView, MainWindow, GameWindow {
  private final Supplier<Game> gameSupplier;
  private final WindowBasedTextGUI gui;
  private final BasicWindow window = new BasicWindow();
  private final MainTextComponent content;
  private MainPresenter presenter;

  public LanternaMainWindow(Supplier<Game> gameSupplier, WindowBasedTextGUI gui) {
    this.gameSupplier = gameSupplier;
    this.gui = gui;
    this.content = new MainTextComponent(gameSupplier, this::handleKey);
    window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
    window.setComponent(content);
    window.setFocusedInteractable(content);
  }

  public Window asWindow() {
    return window;
  }

  public void setPresenter(MainPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void refresh() {
    if(presenter != null) {
      presenter.updateAll();
    }
    content.invalidate();
  }

  @Override
  public void renderStatusBar(MainStatusViewModel model) {
    // The header is drawn from the game state, the status bar model is not needed.
  }

  @Override
  public void renderSystemInfo(SystemInfoViewModel model) {
    content.system(model);
  }

  @Override
  public void renderCharts(ChartsViewModel model) {
    // The chart is drawn from the game state.
  }

  @Override
  public void renderCargo(CargoViewModel model) {
    content.cargo(model);
  }

  @Override
  public void renderDock(DockViewModel model) {
    content.dock(model);
  }

  @Override
  public void renderShipyard(ShipyardViewModel model) {
    // The shipyard panel is not implemented in the text UI yet.
  }

  @Override
  public void renderTargetSystem(TargetSystemViewModel model) {
    content.target(model);
  }

  @Override
  public Integer askFuelAmount(int maxAmount) {
    return askAmount(Strings.DialogFuelTitle,
        Functions.StringVars(Strings.DialogFuelPrompt, "" + maxAmount), maxAmount);
  }

  @Override
  public Integer askRepairsAmount(int maxAmount) {
    return askAmount(Strings.DialogRepairsTitle,
        Functions.StringVars(Strings.DialogRepairsPrompt, "" + maxAmount), maxAmount);
  }

  @Override
  public Integer askCargoBuyQuantity(CargoBuyOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return askAmount(Functions.StringVars(Strings.DialogCargoBuyTitle, item),
        Functions.StringVars(Strings.DialogCargoBuyPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public Integer askCargoSellQuantity(CargoSellOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return askAmount(Functions.StringVars(Strings.DialogCargoSellTitle, item),
        Functions.StringVars(Strings.DialogCargoSellPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public EncounterResult showEncounter() {
    content.log(Strings.MainEncounterUnsupported);
    return EncounterResult.Continue;
  }

  @Override
  public void UpdateStatusBar() {
    content.invalidate();
  }

  @Override
  public void UpdateAll() {
    refresh();
  }

  @Override
  public ImageList ShipImages() {
    return null;
  }

  @Override
  public ImageList EquipmentImages() {
    return null;
  }

  @Override
  public ImageList DirectionImages() {
    return null;
  }

  @Override
  public WfImage[] CustomShipImages() {
    return new WfImage[0];
  }

  @Override
  public void setCustomShipImages(WfImage[] images) {
    // The text UI does not draw sprites.
  }

  private Integer askAmount(String title, String prompt, int maxAmount) {
    String input = TextInputDialog.showDialog(gui, title, prompt, "0");
    if(input == null) {
      return null;
    }
    try {
      int value = Integer.parseInt(input.trim());
      return value >= 0 && value <= maxAmount ? value : null;
    } catch(NumberFormatException e) {
      return null;
    }
  }

  private boolean handleKey(KeyStroke key) {
    Game game = gameSupplier.get();
    if(game == null) {
      return false;
    }
    switch(key.getKeyType()) {
      case Tab:
        content.toggleChart();
        return true;
      case ArrowLeft:
        moveSelection(game, -1, 0);
        return true;
      case ArrowRight:
        moveSelection(game, 1, 0);
        return true;
      case ArrowUp:
        moveSelection(game, 0, -1);
        return true;
      case ArrowDown:
        moveSelection(game, 0, 1);
        return true;
      case Enter:
        trackSelection(game);
        return true;
      case Escape:
        window.close();
        return true;
      case Character:
        return handleCharacter(game, Character.toLowerCase(key.getCharacter()));
      default:
        return false;
    }
  }

  private boolean handleCharacter(Game game, char character) {
    switch(character) {
      case 'q':
        window.close();
        return true;
      case 't':
        trackSelection(game);
        return true;
      case 'f':
        buyFuel();
        return true;
      case 'h':
        buyRepairs();
        return true;
      default:
        return false;
    }
  }

  private void moveSelection(Game game, int dx, int dy) {
    StarSystem selected = game.SelectedSystem() == null
        ? game.Commander().CurrentSystem() : game.SelectedSystem();
    StarSystem best = null;
    double bestScore = Double.MAX_VALUE;
    for(StarSystem system : game.Universe()) {
      int vx = system.X() - selected.X();
      int vy = system.Y() - selected.Y();
      if(vx == 0 && vy == 0) {
        continue;
      }
      double dot = vx * dx + vy * dy;
      if(dot <= 0) {
        continue;
      }
      double distance = Math.sqrt(vx * vx + vy * vy);
      double score = distance * (2.0 - dot / distance);
      if(score < bestScore) {
        bestScore = score;
        best = system;
      }
    }
    if(best != null) {
      game.SelectedSystemId(best.Id());
      if(presenter != null) {
        presenter.updateTargetSystemInfo();
        presenter.updateCharts();
      }
      content.invalidate();
    }
  }

  private void trackSelection(Game game) {
    game.setTrackedSystemId(game.SelectedSystemId());
    content.log(Functions.StringVars(Strings.MainTracking, game.SelectedSystem().Name()));
    if(presenter != null) {
      presenter.updateTargetSystemInfo();
    }
    content.invalidate();
  }

  private void buyFuel() {
    if(presenter != null && presenter.buyFuel()) {
      presenter.updateAll();
    }
    content.invalidate();
  }

  private void buyRepairs() {
    if(presenter != null && presenter.buyRepairs()) {
      presenter.updateAll();
    }
    content.invalidate();
  }
}
