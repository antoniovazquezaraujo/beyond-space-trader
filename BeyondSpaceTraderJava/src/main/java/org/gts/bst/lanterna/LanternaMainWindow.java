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
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Set;
import java.util.function.Supplier;
import jwinforms.ImageList;
import jwinforms.WfImage;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.presenter.BankPresenter;
import org.gts.bst.presenter.CargoTransferPresenter;
import org.gts.bst.presenter.CommanderPresenter;
import org.gts.bst.presenter.EncounterPresenter;
import org.gts.bst.presenter.EquipmentPresenter;
import org.gts.bst.presenter.PersonnelPresenter;
import org.gts.bst.presenter.QuestsPresenter;
import org.gts.bst.presenter.ShipListPresenter;
import org.gts.bst.presenter.ShipPresenter;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.BankView;
import org.gts.bst.view.BankViewModel;
import org.gts.bst.view.CargoViewModel;
import org.gts.bst.view.ChartsViewModel;
import org.gts.bst.view.CommanderView;
import org.gts.bst.view.CommanderViewModel;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.EquipmentInfoViewModel;
import org.gts.bst.view.EquipmentView;
import org.gts.bst.view.EquipmentViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.GameWindow;
import org.gts.bst.view.MainStatusViewModel;
import org.gts.bst.view.MainView;
import org.gts.bst.view.MainWindow;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelView;
import org.gts.bst.view.PersonnelViewModel;
import org.gts.bst.view.QuestsView;
import org.gts.bst.view.QuestsViewModel;
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListView;
import org.gts.bst.view.ShipListViewModel;
import org.gts.bst.view.ShipView;
import org.gts.bst.view.ShipViewModel;
import org.gts.bst.view.ShipyardViewModel;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.GameEndException;
import spacetrader.StarSystem;
import spacetrader.Strings;
import org.gts.bst.ship.equip.EquipmentType;
import spacetrader.enums.AlertType;


/**
 * The main window of the text UI. It renders the view models through
 * {@link MainTextComponent} and forwards the keys to the game and the presenter.
 */
public final class LanternaMainWindow
    implements MainView, MainWindow, GameWindow, BankView, QuestsView,
    PersonnelView, CommanderView, ShipView, ShipListView, EquipmentView {
  private final Supplier<Game> gameSupplier;
  private final WindowBasedTextGUI gui;
  private final BasicWindow window = new BasicWindow();
  private final MainTextComponent content;
  private MainPresenter presenter;
  private BankPresenter bankPresenter;
  private QuestsPresenter questsPresenter;
  private PersonnelPresenter personnelPresenter;
  private ShipListPresenter shipListPresenter;
  private EquipmentPresenter equipmentPresenter;
  private Runnable newGameAction;
  private Runnable saveGameAction;
  private Runnable loadGameAction;

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

  /**
   * The actions of the new game, save and load keys; the application sets them because
   * it owns the current game and the dialogs.
   */
  public void setGameActions(Runnable newGameAction, Runnable saveGameAction, Runnable loadGameAction) {
    this.newGameAction = newGameAction;
    this.saveGameAction = saveGameAction;
    this.loadGameAction = loadGameAction;
  }

  /**
   * Called after the current game changed (new game or loaded game).
   */
  public void gameChanged() {
    bankPresenter = null;
    questsPresenter = null;
    personnelPresenter = null;
    shipListPresenter = null;
    equipmentPresenter = null;
    content.closePanel();
    if(presenter != null) {
      presenter.updateAll();
    }
    content.invalidate();
  }

  public void log(String message) {
    content.log(message);
    content.invalidate();
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
    return LanternaDialogs.askAmount(gui, Strings.DialogFuelTitle,
        Functions.StringVars(Strings.DialogFuelPrompt, "" + maxAmount), maxAmount);
  }

  @Override
  public Integer askRepairsAmount(int maxAmount) {
    return LanternaDialogs.askAmount(gui, Strings.DialogRepairsTitle,
        Functions.StringVars(Strings.DialogRepairsPrompt, "" + maxAmount), maxAmount);
  }

  @Override
  public Integer askCargoBuyQuantity(CargoBuyOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoBuyTitle, item),
        Functions.StringVars(Strings.DialogCargoBuyPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public Integer askCargoSellQuantity(CargoSellOffer offer) {
    String item = Consts.TradeItems.get(offer.tradeItem()).Name();
    return LanternaDialogs.askAmount(gui, Functions.StringVars(Strings.DialogCargoSellTitle, item),
        Functions.StringVars(Strings.DialogCargoSellPrompt, "" + offer.maxAmount()), offer.maxAmount());
  }

  @Override
  public EncounterResult showEncounter() {
    Game game = gameSupplier.get();
    if(game == null) {
      return EncounterResult.Normal;
    }
    EncounterPresenter[] presenter = new EncounterPresenter[1];
    LanternaEncounterView view = new LanternaEncounterView(gui,
        action -> dispatch(presenter[0], action), () -> presenter[0].tick(), this::showCargoTransfer);
    presenter[0] = new EncounterPresenter(game, view);
    gui.addWindow(view.asWindow());
    presenter[0].start();
    gui.waitForWindowToClose(view.asWindow());
    return presenter[0].result();
  }

  private static void dispatch(EncounterPresenter presenter, EncounterAction action) {
    switch(action) {
      case Attack:
        presenter.attack();
        break;
      case Board:
        presenter.board();
        break;
      case Bribe:
        presenter.bribe();
        break;
      case Drink:
        presenter.drink();
        break;
      case Flee:
        presenter.flee();
        break;
      case Ignore:
        presenter.ignore();
        break;
      case Interrupt:
        presenter.interest();
        break;
      case Meet:
        presenter.meet();
        break;
      case Plunder:
        presenter.plunder();
        break;
      case Submit:
        presenter.submit();
        break;
      case Surrender:
        presenter.surrender();
        break;
      case Trade:
        presenter.trade();
        break;
      case Yield:
        presenter.yield();
        break;
      default:
        break;
    }
  }

  private void showCargoTransfer(boolean plunder) {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    LanternaCargoTransferView view = new LanternaCargoTransferView(gui, game,
        plunder ? CargoTransferPresenter.Mode.Plunder : CargoTransferPresenter.Mode.Jettison);
    gui.addWindow(view.asWindow());
    gui.waitForWindowToClose(view.asWindow());
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

  private boolean handleKey(KeyStroke key) {
    Game game = gameSupplier.get();
    if(game == null) {
      return false;
    }
    if(key.getKeyType() == KeyType.Escape) {
      if(content.panel() != MainPanel.Navigation) {
        content.closePanel();
        return true;
      }
      window.close();
      return true;
    }
    if(content.panel() == MainPanel.Trade) {
      return handleTradeKey(key);
    }
    if(content.panel() == MainPanel.Bank) {
      return handleBankKey(key);
    }
    if(content.panel() == MainPanel.Quests) {
      return false;
    }
    if(content.panel() == MainPanel.Personnel) {
      return handlePersonnelKey(key);
    }
    if(content.panel() == MainPanel.Commander || content.panel() == MainPanel.Ship) {
      return false;
    }
    if(content.panel() == MainPanel.ShipList) {
      return handleShipListKey(key);
    }
    if(content.panel() == MainPanel.Equipment) {
      return handleEquipmentKey(key);
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
      case F2:
        return runAction(newGameAction, Strings.MainNewGameUnavailable);
      case F5:
        return runAction(saveGameAction, Strings.MainSaveUnavailable);
      case F9:
        return runAction(loadGameAction, Strings.MainLoadUnavailable);
      case Character:
        return handleCharacter(game, Character.toLowerCase(key.getCharacter()));
      default:
        return false;
    }
  }

  private boolean runAction(Runnable action, String unavailableMessage) {
    if(action == null) {
      content.log(unavailableMessage);
      content.invalidate();
      return true;
    }
    action.run();
    return true;
  }

  private boolean handleBankKey(KeyStroke key) {
    if(key.getKeyType() != KeyType.Character || bankPresenter == null) {
      return false;
    }
    switch(Character.toLowerCase(key.getCharacter())) {
      case 'g':
        bankPresenter.getLoan();
        return true;
      case 'p':
        bankPresenter.payBack();
        return true;
      case 'i':
        bankPresenter.toggleInsurance();
        return true;
      default:
        return false;
    }
  }

  private void openBank() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(bankPresenter == null) {
      bankPresenter = new BankPresenter(game, this);
    }
    bankPresenter.update();
    content.openBank();
  }

  private void openQuests() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(questsPresenter == null) {
      questsPresenter = new QuestsPresenter(game, this);
    }
    questsPresenter.update();
    content.openQuests();
  }

  @Override
  public void render(BankViewModel model) {
    content.bank(model);
  }

  @Override
  public Integer askLoanAmount(int maxAmount) {
    return LanternaDialogs.askAmount(gui, Strings.DialogLoanTitle,
        Functions.StringVars(Strings.DialogLoanPrompt, "" + maxAmount), maxAmount);
  }

  @Override
  public Integer askPayBackAmount() {
    Game game = gameSupplier.get();
    int max = Math.min(game.Commander().getDebt(), game.Commander().getCash());
    return LanternaDialogs.askAmount(gui, Strings.DialogPayBackTitle,
        Functions.StringVars(Strings.DialogPayBackPrompt, "" + max), max);
  }

  @Override
  public void render(QuestsViewModel model) {
    content.quests(model);
  }

  @Override
  public void render(PersonnelViewModel model) {
    content.personnel(model);
  }

  @Override
  public void renderInfo(PersonnelInfo info) {
    content.personnelInfo(info);
  }

  @Override
  public void render(CommanderViewModel model) {
    content.commander(model);
  }

  @Override
  public void render(ShipViewModel model) {
    content.ship(model);
  }

  @Override
  public void render(ShipListViewModel model) {
    content.shipList(model);
  }

  @Override
  public void renderInfo(ShipInfoViewModel info) {
    content.shipInfo(info);
  }

  @Override
  public void render(EquipmentViewModel model) {
    content.equipment(model);
  }

  @Override
  public void renderInfo(EquipmentInfoViewModel info) {
    content.equipmentInfo(info);
  }

  private boolean handleShipListKey(KeyStroke key) {
    if(shipListPresenter == null) {
      return false;
    }
    switch(key.getKeyType()) {
      case ArrowUp:
        selectShipListEntry(content.shipListIndex() - 1);
        return true;
      case ArrowDown:
        selectShipListEntry(content.shipListIndex() + 1);
        return true;
      case Character:
        if(Character.toLowerCase(key.getCharacter()) != 'b') {
          return false;
        }
        ShipListViewModel model = content.shipList();
        int index = content.shipListIndex();
        if(model != null && index < model.rows().size() && model.rows().get(index).buyVisible()) {
          shipListPresenter.buy(index);
          selectShipListEntry(0);
        }
        return true;
      default:
        return false;
    }
  }

  private void selectShipListEntry(int index) {
    int count = content.shipListEntryCount();
    if(count == 0) {
      return;
    }
    int selected = Math.floorMod(index, count);
    content.shipListIndex(selected);
    shipListPresenter.select(selected);
  }

  private boolean handleEquipmentKey(KeyStroke key) {
    if(equipmentPresenter == null) {
      return false;
    }
    switch(key.getKeyType()) {
      case ArrowUp:
        selectEquipmentEntry(content.equipmentIndex() - 1);
        return true;
      case ArrowDown:
        selectEquipmentEntry(content.equipmentIndex() + 1);
        return true;
      case Character:
        char character = Character.toLowerCase(key.getCharacter());
        if(character == 'b') {
          equipmentPresenter.buy();
          selectEquipmentEntry(content.equipmentIndex());
          return true;
        }
        if(character == 's') {
          equipmentPresenter.sell();
          selectEquipmentEntry(content.equipmentIndex());
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  private void selectEquipmentEntry(int index) {
    int count = content.equipmentEntryCount();
    EquipmentViewModel model = content.equipment();
    if(count == 0 || model == null) {
      return;
    }
    int selected = Math.floorMod(index, count);
    content.equipmentIndex(selected);
    int weapons = model.buyWeapons().size();
    int shields = model.buyShields().size();
    int gadgets = model.buyGadgets().size();
    int buyCount = weapons + shields + gadgets;
    if(selected < buyCount) {
      if(selected < weapons) {
        equipmentPresenter.select(EquipmentType.Weapon, false, selected);
      } else if(selected < weapons + shields) {
        equipmentPresenter.select(EquipmentType.Shield, false, selected - weapons);
      } else {
        equipmentPresenter.select(EquipmentType.Gadget, false, selected - weapons - shields);
      }
    } else {
      int sellIndex = selected - buyCount;
      int sellWeapons = model.sellWeapons().size();
      int sellShields = model.sellShields().size();
      if(sellIndex < sellWeapons) {
        equipmentPresenter.select(EquipmentType.Weapon, true, sellIndex);
      } else if(sellIndex < sellWeapons + sellShields) {
        equipmentPresenter.select(EquipmentType.Shield, true, sellIndex - sellWeapons);
      } else {
        equipmentPresenter.select(EquipmentType.Gadget, true, sellIndex - sellWeapons - sellShields);
      }
    }
  }

  private void openShipList() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(shipListPresenter == null) {
      shipListPresenter = new ShipListPresenter(game, this);
    }
    content.shipListIndex(0);
    shipListPresenter.update();
    shipListPresenter.notifyTribblesTradeInIfNeeded();
    selectShipListEntry(0);
    content.openShipList();
  }

  private void openEquipment() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(equipmentPresenter == null) {
      equipmentPresenter = new EquipmentPresenter(game, this);
    }
    content.equipmentIndex(0);
    equipmentPresenter.update();
    selectEquipmentEntry(0);
    content.openEquipment();
  }

  private void buyEscapePod() {
    Game game = gameSupplier.get();
    if(game.Commander().getShip().getEscapePod()) {
      content.log(Strings.MainEscapePodAlready);
      return;
    }
    if(game.Dialogs().alert(AlertType.EquipmentEscapePod) != DialogResult.Yes) {
      return;
    }
    if(game.Commander().getCash() < 2000) {
      game.Dialogs().alert(AlertType.EquipmentIF);
      return;
    }
    game.Commander().setCash(game.Commander().getCash() - 2000);
    game.Commander().getShip().setEscapePod(true);
    content.log(Strings.MainEscapePodBought);
    refresh();
  }

  private boolean handlePersonnelKey(KeyStroke key) {
    if(personnelPresenter == null) {
      return false;
    }
    switch(key.getKeyType()) {
      case ArrowUp:
        selectPersonnelEntry(content.personnelIndex() - 1);
        return true;
      case ArrowDown:
        selectPersonnelEntry(content.personnelIndex() + 1);
        return true;
      case Character:
        if(Character.toLowerCase(key.getCharacter()) != 'h') {
          return false;
        }
        if(personnelPresenter.hireFire()) {
          selectPersonnelEntry(Math.min(content.personnelIndex(),
              Math.max(0, content.personnelEntryCount() - 1)));
        }
        return true;
      default:
        return false;
    }
  }

  private void selectPersonnelEntry(int index) {
    int count = content.personnelEntryCount();
    if(count == 0) {
      return;
    }
    int selected = Math.floorMod(index, count);
    content.personnelIndex(selected);
    int crewSize = content.personnelCrewSize();
    if(selected < crewSize) {
      personnelPresenter.selectCrew(selected);
    } else {
      personnelPresenter.selectForHire(selected - crewSize);
    }
  }

  private void openPersonnel() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(personnelPresenter == null) {
      personnelPresenter = new PersonnelPresenter(game, this);
    }
    personnelPresenter.update();
    selectPersonnelEntry(0);
    content.openPersonnel();
  }

  private void openCommander() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    new CommanderPresenter(game, this).update();
    content.openCommander();
  }

  private void openShip() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    new ShipPresenter(game, this).update();
    content.openShip();
  }

  private boolean handleTradeKey(KeyStroke key) {
    switch(key.getKeyType()) {
      case ArrowUp:
        content.moveItemSelection(-1);
        return true;
      case ArrowDown:
        content.moveItemSelection(1);
        return true;
      case Character:
        char character = Character.toLowerCase(key.getCharacter());
        if(character == 'b') {
          presenter.buyCargo(content.selectedItem(), key.isShiftDown());
          return true;
        }
        if(character == 's') {
          presenter.sellCargo(content.selectedItem(), key.isShiftDown());
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  private boolean handleCharacter(Game game, char character) {
    switch(character) {
      case 'c':
        content.openTrade();
        return true;
      case 'b':
        openBank();
        return true;
      case 'q':
        openQuests();
        return true;
      case 'p':
        openPersonnel();
        return true;
      case 'v':
        openShip();
        return true;
      case 'i':
        openCommander();
        return true;
      case 'l':
        openShipList();
        return true;
      case 'e':
        openEquipment();
        return true;
      case 'o':
        buyEscapePod();
        return true;
      case 't':
        trackSelection(game);
        return true;
      case 'f':
        buyFuel();
        return true;
      case 'j':
        jump();
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

  private void jump() {
    Game game = gameSupplier.get();
    if(game.WarpSystem() == null) {
      game.Dialogs().alert(AlertType.ChartJumpNoSystemSelected);
      return;
    }
    if(game.WarpSystem() == game.Commander().CurrentSystem()) {
      game.Dialogs().alert(AlertType.ChartJumpCurrent);
      return;
    }
    if(game.Dialogs().alert(AlertType.ChartJump, game.WarpSystem().Name()) != DialogResult.Yes) {
      return;
    }
    try {
      game.setCanSuperWarp(false);
      game.Warp(true);
    } catch(GameEndException e) {
      content.log(Strings.MainGameOver);
      window.close();
      return;
    }
    refresh();
  }

  private void buyRepairs() {
    if(presenter != null && presenter.buyRepairs()) {
      presenter.updateAll();
    }
    content.invalidate();
  }
}
