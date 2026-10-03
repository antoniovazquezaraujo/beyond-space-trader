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
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.FileDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoSellOffer;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.presenter.BankPresenter;
import org.gts.bst.presenter.CargoTransferPresenter;
import org.gts.bst.presenter.CommanderPresenter;
import org.gts.bst.presenter.EncounterPresenter;
import org.gts.bst.presenter.EquipmentPresenter;
import org.gts.bst.presenter.HighScoresPresenter;
import org.gts.bst.presenter.PersonnelPresenter;
import org.gts.bst.presenter.QuestsPresenter;
import org.gts.bst.presenter.ShipListPresenter;
import org.gts.bst.presenter.ShipPresenter;
import org.gts.bst.presenter.ShipyardPresenter;
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
import org.gts.bst.view.HighScoresView;
import org.gts.bst.view.HighScoresViewModel;
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
import org.gts.bst.view.ShipyardDesignerViewModel;
import org.gts.bst.view.ShipyardView;
import org.gts.bst.view.ShipyardViewModel;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.GameEndException;
import spacetrader.StarSystem;
import spacetrader.enums.StarSystemId;
import org.gts.bst.ship.ShipType;
import spacetrader.Strings;
import org.gts.bst.ship.equip.EquipmentType;
import spacetrader.CrewMember;
import spacetrader.GameOptions;
import spacetrader.enums.GameEndType;
import spacetrader.HighScoreRecord;
import spacetrader.HighScores;
import spacetrader.enums.AlertType;


/**
 * The main window of the text UI. It renders the view models through
 * {@link MainTextComponent} and forwards the keys to the game and the presenter.
 */
public final class LanternaMainWindow
    implements MainView, MainWindow, GameWindow, BankView, QuestsView,
    PersonnelView, CommanderView, ShipView, ShipListView, EquipmentView, HighScoresView,
    ShipyardView {
  private static final int DESIGNER_FIELDS = 12;
  private static final String AUTOSAVE_DEPARTURE = "autosave_departure.sav";
  private static final String AUTOSAVE_ARRIVAL = "autosave_arrival.sav";
  /** The quiet alerts of the encounter that the rival says aloud, under its ship. */
  private static final Set<AlertType> SPEECH_ALERTS = Set.of(
      AlertType.EncounterPoliceFine,
      AlertType.EncounterPoliceBribeCant,
      AlertType.EncounterMarieCelesteNoBribe,
      AlertType.EncounterSurrenderRefused);

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
  private ShipyardPresenter shipyardPresenter;
  private LanternaEncounterView encounterView;
  private boolean gameOver;
  private final List<Runnable> menuActions = new ArrayList<>();
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
    gameOver = false;
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

  /** True for the quiet alerts that speak under the rival instead of going to the log. */
  static boolean speaksUnderTheRival(AlertType type) {
    return SPEECH_ALERTS.contains(type);
  }

  /** A quiet alert: under the rival while the encounter is open, or to the main log. */
  public void alertLog(AlertType type, String message) {
    if(encounterView == null) {
      log(message);
    } else if(speaksUnderTheRival(type)) {
      encounterView.speech(message);
    } else {
      encounterView.log(message);
    }
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
    content.directKeys(directKeys());
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
  public void showNewspaper() {
    openNews();
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
    view.header(game::Commander);
    encounterView = view;
    view.onClose(() -> encounterView = null);
    presenter[0] = new EncounterPresenter(game, view);
    gui.addWindow(view.asWindow());
    try {
      presenter[0].start();
      gui.waitForWindowToClose(view.asWindow());
      return presenter[0].result();
    } catch(GameEndException e) {
      // The game ended inside the encounter (killed in combat): close the window and
      // let the travel flow show the game end.
      view.close();
      throw e;
    }
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

  /** The panels that only show information: they close with space (or escape). */
  private static boolean isReadOnlyPanel(MainPanel panel) {
    switch(panel) {
      case Quests:
      case Commander:
      case Ship:
      case HighScores:
      case About:
        return true;
      default:
        return false;
    }
  }

  private boolean handleKey(KeyStroke key) {
    if(content.titleScreen()) {
      // Any key enters the program from the title screen, and the menu keys do their
      // job at once: F2 there starts a game instead of being swallowed.
      content.titleScreen(false);
      switch(key.getKeyType()) {
        case F2:
        case F3:
        case F8:
        case F9:
        case F10:
          return handleTitleKey(key);
        default:
          return true;
      }
    }
    Game game = gameSupplier.get();
    if(game == null) {
      return handleTitleKey(key);
    }
    // Vim keys: in the menu and the panels j and k move down and up like the arrows;
    // on the map they move the cursor (see handleCharacter).
    if(key.getKeyType() == KeyType.Character
        && (content.menuVisible() || content.panel() != MainPanel.Navigation)) {
      char vim = Character.toLowerCase(key.getCharacter());
      if(vim == 'j') {
        key = new KeyStroke(KeyType.ArrowDown, false, false);
      } else if(vim == 'k') {
        key = new KeyStroke(KeyType.ArrowUp, false, false);
      }
    }
    if(key.getKeyType() == KeyType.F10) {
      toggleMenu();
      return true;
    }
    if(content.menuVisible()) {
      return handleMenuKey(key);
    }
    if(gameOver) {
      return handleGameOverKey(key);
    }
    if(key.getKeyType() == KeyType.Escape) {
      if(content.panel() != MainPanel.Navigation) {
        if(content.panel() == MainPanel.Designer) {
          shipyardPresenter = null;
        }
        content.closePanel();
        return true;
      }
      quit();
      return true;
    }
    if(key.getKeyType() == KeyType.Character && key.getCharacter() == ' ' && isReadOnlyPanel(content.panel())) {
      content.closePanel();
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
    if(content.panel() == MainPanel.Commander || content.panel() == MainPanel.Ship
        || content.panel() == MainPanel.About) {
      return false;
    }
    if(content.panel() == MainPanel.ShipList) {
      return handleShipListKey(key);
    }
    if(content.panel() == MainPanel.Equipment) {
      return handleEquipmentKey(key);
    }
    if(content.panel() == MainPanel.Options) {
      return handleOptionsKey(key);
    }
    if(content.panel() == MainPanel.HighScores) {
      return false;
    }
    if(content.panel() == MainPanel.Designer) {
      return handleDesignerKey(key);
    }
    if(content.panel() == MainPanel.News) {
      return handleNewsKey(key);
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
      case F3:
        openHighScores();
        return true;
      case F8:
        openOptions();
        return true;
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

  private void openTrade() {
    if(presenter != null) {
      presenter.updateAll();
    }
    content.openTrade();
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
  public void render(HighScoresViewModel model) {
    content.highScores(model);
  }

  @Override
  public void render(ShipyardDesignerViewModel model) {
    content.designer(model);
  }

  @Override
  public void close() {
    shipyardPresenter = null;
    content.closePanel();
    content.invalidate();
  }

  @Override
  public void showFileError(String fileName, String message) {
    content.log(fileName + ": " + message);
    content.invalidate();
  }

  @Override
  public String askSaveTemplateFile() {
    FileDialog dialog = new FileDialog(Strings.DesignerSave, Strings.DialogSaveDescription,
        Strings.DialogSaveAction, new TerminalSize(60, 15), false, new File(Consts.CustomTemplatesDirectory));
    dialog.setCloseWindowWithEscape(true);
    File file = dialog.showDialog(gui);
    if(file == null) {
      return null;
    }
    String path = file.getPath();
    return path.endsWith(".sst") ? path : path + ".sst";
  }

  private void openDesigner() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    if(game.Commander().CurrentSystem().Shipyard() == null) {
      content.log(Strings.MainDesignUnavailable);
      content.invalidate();
      return;
    }
    shipyardPresenter = new ShipyardPresenter(game, this);
    content.designerField(0);
    shipyardPresenter.start();
    content.openDesigner();
  }

  private boolean handleDesignerKey(KeyStroke key) {
    ShipyardDesignerViewModel model = content.designer();
    if(shipyardPresenter == null || model == null) {
      return false;
    }
    switch(key.getKeyType()) {
      case ArrowUp:
        content.designerField(Math.floorMod(content.designerField() - 1, DESIGNER_FIELDS));
        return true;
      case ArrowDown:
        content.designerField(Math.floorMod(content.designerField() + 1, DESIGNER_FIELDS));
        return true;
      case ArrowLeft:
        return changeDesignerField(model, -1);
      case ArrowRight:
        return changeDesignerField(model, 1);
      case Enter:
        return activateDesignerField(model);
      case Character:
        char character = Character.toLowerCase(key.getCharacter());
        if(character == 'n') {
          askDesignerName(model);
          return true;
        }
        if(character == 'c') {
          shipyardPresenter.construct(model.name());
          return true;
        }
        if(character == 'v') {
          shipyardPresenter.saveTemplate(model.name());
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  private boolean changeDesignerField(ShipyardDesignerViewModel model, int delta) {
    int field = content.designerField();
    if(field == 0 && !model.sizes().isEmpty()) {
      shipyardPresenter.onSizeChanged(Math.floorMod(model.sizeIndex() + delta, model.sizes().size()));
      return true;
    }
    if(field == 1 && !model.templates().isEmpty()) {
      shipyardPresenter.loadSelectedTemplate(Math.floorMod(model.templateIndex() + delta, model.templates().size()));
      return true;
    }
    if(field >= 3 && field <= 9) {
      int index = field - 3;
      List<ShipyardDesignerViewModel.Numeric> values = model.numerics();
      if(values.size() < 7 || index >= values.size()) {
        return false;
      }
      ShipyardDesignerViewModel.Numeric numeric = values.get(index);
      int step = numeric.increment() == null ? 1 : numeric.increment();
      int maximum = numeric.max() == null ? Integer.MAX_VALUE : numeric.max();
      int minimum = numeric.min() == null ? 0 : numeric.min();
      int next = Math.max(minimum, Math.min(maximum, numeric.value() + delta * step));
      if(next == numeric.value()) {
        return true;
      }
      int[] numbers = new int[7];
      for(int i = 0; i < numbers.length; i++) {
        numbers[i] = values.get(i).value();
      }
      numbers[index] = next;
      shipyardPresenter.onValuesChanged(numbers[0], numbers[1], numbers[2], numbers[3], numbers[4], numbers[5], numbers[6]);
      return true;
    }
    return false;
  }

  private boolean activateDesignerField(ShipyardDesignerViewModel model) {
    switch(content.designerField()) {
      case 2:
        askDesignerName(model);
        return true;
      case 10:
        shipyardPresenter.construct(model.name());
        return true;
      case 11:
        shipyardPresenter.saveTemplate(model.name());
        return true;
      default:
        return false;
    }
  }

  private void askDesignerName(ShipyardDesignerViewModel model) {
    String name = InputDialog.show(gui, Strings.DialogShipNameTitle, Strings.DialogShipNamePrompt, model.name());
    if(name != null && !name.trim().isEmpty()) {
      shipyardPresenter.onNameChanged(name.trim());
    }
  }

  private void openHighScores() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    new HighScoresPresenter(Functions.GetHighScores(game.Dialogs()), this).update();
    content.openHighScores();
  }

  private void openOptions() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    content.options(optionLines(game));
    content.optionsIndex(0);
    content.openOptions();
  }

  private boolean handleOptionsKey(KeyStroke key) {
    Game game = gameSupplier.get();
    if(game == null) {
      return false;
    }
    if(key.getKeyType() == KeyType.ArrowUp || key.getKeyType() == KeyType.ArrowDown) {
      int count = content.optionsCount();
      if(count == 0) {
        return true;
      }
      int delta = key.getKeyType() == KeyType.ArrowUp ? -1 : 1;
      content.optionsIndex(Math.floorMod(content.optionsIndex() + delta, count));
      return true;
    }
    if(key.getKeyType() == KeyType.Enter) {
      toggleOption(game, content.optionsIndex());
      return true;
    }
    if(key.getKeyType() != KeyType.Character) {
      return false;
    }
    char character = Character.toLowerCase(key.getCharacter());
    if(character == 's') {
      game.Options().SaveAsDefaults(game.Dialogs());
      return true;
    }
    if(character == 'l') {
      game.Options().LoadFromDefaults(true, game.Dialogs());
      content.options(optionLines(game));
      return true;
    }
    return false;
  }

  private List<String> optionLines(Game game) {
    GameOptions options = game.Options();
    List<String> lines = new ArrayList<>();
    lines.add(optionLine(Strings.OptionAutoFuel, options.getAutoFuel()));
    lines.add(optionLine(Strings.OptionAutoRepair, options.getAutoRepair()));
    lines.add(optionLine(Strings.OptionNewsAutoPay, options.getNewsAutoPay()));
    lines.add(optionLine(Strings.OptionNewsAutoShow, options.getNewsAutoShow()));
    lines.add(optionLine(Strings.OptionRemindLoans, options.getRemindLoans()));
    lines.add(optionLine(Strings.OptionShowTrackedRange, options.getShowTrackedRange()));
    lines.add(optionLine(Strings.OptionTrackAutoOff, options.getTrackAutoOff()));
    lines.add(optionLine(Strings.OptionReserveMoney, options.getReserveMoney()));
    lines.add(Functions.StringVars(Strings.OptionsValue, Strings.OptionLeaveEmpty, "" + options.getLeaveEmpty()));
    lines.add(optionLine(Strings.OptionIgnorePirates, options.getAlwaysIgnorePirates()));
    lines.add(optionLine(Strings.OptionIgnorePolice, options.getAlwaysIgnorePolice()));
    lines.add(optionLine(Strings.OptionIgnoreTraders, options.getAlwaysIgnoreTraders()));
    lines.add(optionLine(Strings.OptionIgnoreTradeInOrbit, options.getAlwaysIgnoreTradeInOrbit()));
    lines.add(optionLine(Strings.OptionContinuousAttack, options.getContinuousAttack()));
    lines.add(optionLine(Strings.OptionContinuousAttackFleeing, options.getContinuousAttackFleeing()));
    lines.add(optionLine(Strings.OptionDisableOpponents, options.getDisableOpponents()));
    lines.add(optionLine(Strings.OptionAutoSave, game.getAutoSave()));
    lines.add(Functions.StringVars(Strings.OptionsValue, Strings.OptionGalaxyColumns,
        "" + options.getGalaxyColumns()));
    return lines;
  }

  private static String optionLine(String label, boolean value) {
    return Functions.StringVars(Strings.OptionsValue, label, value ? Strings.OptionsOn : Strings.OptionsOff);
  }

  private void toggleOption(Game game, int index) {
    GameOptions options = game.Options();
    switch(index) {
      case 0:
        options.setAutoFuel(!options.getAutoFuel());
        break;
      case 1:
        options.setAutoRepair(!options.getAutoRepair());
        break;
      case 2:
        options.setNewsAutoPay(!options.getNewsAutoPay());
        break;
      case 3:
        options.setNewsAutoShow(!options.getNewsAutoShow());
        break;
      case 4:
        options.setRemindLoans(!options.getRemindLoans());
        break;
      case 5:
        options.setShowTrackedRange(!options.getShowTrackedRange());
        break;
      case 6:
        options.setTrackAutoOff(!options.getTrackAutoOff());
        break;
      case 7:
        options.setReserveMoney(!options.getReserveMoney());
        break;
      case 8:
        Integer value = LanternaDialogs.askAmount(gui, Strings.DialogLeaveEmptyTitle,
            Functions.StringVars(Strings.DialogLeaveEmptyPrompt, "99"), 99);
        if(value != null) {
          options.setLeaveEmpty(value);
        }
        break;
      case 9:
        options.setAlwaysIgnorePirates(!options.getAlwaysIgnorePirates());
        break;
      case 10:
        options.setAlwaysIgnorePolice(!options.getAlwaysIgnorePolice());
        break;
      case 11:
        options.setAlwaysIgnoreTraders(!options.getAlwaysIgnoreTraders());
        break;
      case 12:
        options.setAlwaysIgnoreTradeInOrbit(!options.getAlwaysIgnoreTradeInOrbit());
        break;
      case 13:
        options.setContinuousAttack(!options.getContinuousAttack());
        break;
      case 14:
        options.setContinuousAttackFleeing(!options.getContinuousAttackFleeing());
        break;
      case 15:
        options.setDisableOpponents(!options.getDisableOpponents());
        break;
      case 16:
        game.setAutoSave(!game.getAutoSave());
        break;
      case 17:
        options.setGalaxyColumns(options.getGalaxyColumns() >= 3 ? 1 : options.getGalaxyColumns() + 1);
        break;
      default:
        break;
    }
    content.options(optionLines(game));
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

  /** The keys of the title screen (no game): the menu and the program actions. */
  private boolean handleTitleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.F10) {
      toggleMenu();
      return true;
    }
    if(content.menuVisible()) {
      return handleMenuKey(key);
    }
    switch(key.getKeyType()) {
      case F2:
        return runAction(newGameAction, Strings.MainNewGameUnavailable);
      case F9:
        return runAction(loadGameAction, Strings.MainLoadUnavailable);
      case F3:
        openHighScores();
        return true;
      case F8:
        openOptions();
        return true;
      case Escape:
        window.close();
        return true;
      case Character:
        if(Character.toLowerCase(key.getCharacter()) == 'a') {
          openAbout();
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  /** Shows the title screen (the splash, or the banner when it does not fit) until a key is pressed. */
  public void showTitleScreen() {
    content.titleScreen(true);
  }

  private void openAbout() {
    content.openAbout();
  }

  private void openShip() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    new ShipPresenter(game, this).update();
    content.openShip();
  }

  /**
   * Whether the key asks for the maximum amount. Most terminals do not report the
   * shift modifier for letters, so the uppercase letter counts as well.
   */
  static boolean allAmount(KeyStroke key) {
    Character character = key.getCharacter();
    return key.isShiftDown() || (character != null && Character.isUpperCase(character));
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
          presenter.buyCargo(content.selectedItem(), allAmount(key));
          return true;
        }
        if(character == 's') {
          presenter.sellCargo(content.selectedItem(), allAmount(key));
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  private boolean handleCharacter(Game game, char character) {
    switch(character) {
      case 'a':
        openAbout();
        return true;
      case 'c':
        openTrade();
        return true;
      case 'b':
        openBank();
        return true;
      case 'q':
        openQuests();
        return true;
      case 'n':
        openNews();
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
      case 's':
        openShipList();
        return true;
      case 'e':
        openEquipment();
        return true;
      case 'o':
        buyEscapePod();
        return true;
      case 'd':
        openDesigner();
        return true;
      case 't':
        trackSelection(game);
        return true;
      case 'f':
        buyFuel();
        return true;
      case 'g':
        jump();
        return true;
      case ' ':
        warp();
        return true;
      case 'r':
        buyRepairs();
        return true;
      case 'h':
        moveSelection(game, -1, 0);
        return true;
      case 'j':
        moveSelection(game, 0, 1);
        return true;
      case 'k':
        moveSelection(game, 0, -1);
        return true;
      case 'l':
        moveSelection(game, 1, 0);
        return true;
      case '/':
        find();
        return true;
      case 'y':
        acceptSpecialEvent();
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
      selectSystem(game, best);
    }
  }

  /**
   * Accepts the special event of the current system (the quests, the moon, the
   * deliveries...), like the Special button of the Swing front-end.
   */
  private void acceptSpecialEvent() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    StarSystem system = game.Commander().CurrentSystem();
    if(system == null || !system.ShowSpecialButton() || system.SpecialEvent() == null) {
      content.log(Strings.MainSpecialNone);
      content.invalidate();
      return;
    }
    if(game.Commander().CashToSpend() < system.SpecialEvent().Price()) {
      game.Dialogs().alert(AlertType.SpecialIF);
      return;
    }
    try {
      game.HandleSpecialEvent();
    } catch(GameEndException e) {
      showGameEnd(game);
      return;
    }
    refresh();
  }

  /** Selects a system as the target and refreshes what depends on it. */
  private void selectSystem(Game game, StarSystem system) {
    game.SelectedSystemId(system.Id());
    if(presenter != null) {
      presenter.updateTargetSystemInfo();
      presenter.updateCharts();
      // The target prices of the trade panel follow the chart cursor.
      presenter.updateCargo();
    }
    content.invalidate();
  }

  /** Finds a system by name and selects it; asks which one when several match. */
  private void find() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    String text = InputDialog.show(gui, Strings.DialogFindTitle, Strings.DialogFindPrompt, "");
    if(text == null || text.trim().isEmpty()) {
      return;
    }
    String name = text.trim().toLowerCase(Locale.ROOT);
    List<StarSystem> matches = new ArrayList<>();
    for(StarSystem system : game.Universe()) {
      if(system.Name().toLowerCase(Locale.ROOT).startsWith(name)) {
        matches.add(system);
      }
    }
    if(matches.isEmpty()) {
      content.log(Strings.FindNone);
      content.invalidate();
      return;
    }
    if(matches.size() == 1) {
      selectedSystem(game, matches.get(0));
      return;
    }
    List<String> items = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    for(StarSystem system : matches) {
      items.add(system.Name() + " · " + Functions.StringVars(Strings.MainTargetDistance,
          "" + Functions.Distance(game.Commander().CurrentSystem(), system)));
      actions.add(() -> selectedSystem(game, system));
    }
    menuActions.clear();
    menuActions.addAll(actions);
    content.showMenu(Strings.FindTitle, items);
  }

  /** Selects a found system and logs it as the target. */
  private void selectedSystem(Game game, StarSystem system) {
    selectSystem(game, system);
    content.log(Functions.StringVars(Strings.MainTarget, system.Name(),
        "" + Functions.Distance(game.Commander().CurrentSystem(), system)));
    content.invalidate();
  }

  private void trackSelection(Game game) {
    StarSystem selected = game.SelectedSystem() == null
        ? game.Commander().CurrentSystem() : game.SelectedSystem();
    if(selected == game.TrackedSystem()) {
      game.setTrackedSystemId(StarSystemId.NA);
      content.log(Functions.StringVars(Strings.MainUntracking, selected.Name()));
    } else {
      game.setTrackedSystemId(selected.Id());
      content.log(Functions.StringVars(Strings.MainTracking, selected.Name()));
    }
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
    if(!game.getCanSuperWarp()) {
      content.log(Strings.MainJumpNoSingularity);
      content.invalidate();
      return;
    }
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
      if(game.getAutoSave()) {
        Functions.SaveFile(new File(Consts.SaveDirectory, AUTOSAVE_DEPARTURE).getPath(), game.Serialize(), game.Dialogs());
      }
      game.Warp(true);
      if(game.getAutoSave()) {
        Functions.SaveFile(new File(Consts.SaveDirectory, AUTOSAVE_ARRIVAL).getPath(), game.Serialize(), game.Dialogs());
      }
    } catch(GameEndException e) {
      showGameEnd(game);
      return;
    }
    refresh();
  }

  /**
   * The destination of the SPACE key: the selected system, or the far end of the
   * current system's wormhole when the cursor rests on the current system (or nothing
   * is selected). Without a wormhole it returns the plain target, so {@link #warp()}
   * keeps its usual messages.
   */
  StarSystem warpTarget(Game game) {
    StarSystem current = game.Commander().CurrentSystem();
    StarSystem target = game.WarpSystem();
    if(current != null && (target == null || target == current)) {
      StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
      if(pair != null) {
        game.SelectedSystemId(current.Id());
        game.TargetWormhole(true);
        return game.WarpSystem();
      }
    }
    return target;
  }

  /**
   * The normal trip: warps to the target system, spending the fuel of the
   * distance (unless a wormhole connects both systems) and advancing a day. The
   * Portable Singularity Jump is a separate action ({@link #jump()}).
   */
  private void warp() {
    Game game = gameSupplier.get();
    StarSystem target = warpTarget(game);
    if(target == null) {
      content.log(Strings.MainWarpNoTarget);
      content.invalidate();
      return;
    }
    if(target == game.Commander().CurrentSystem()) {
      content.log(Strings.MainWarpCurrent);
      content.invalidate();
      return;
    }
    if(!target.DestOk()) {
      content.log(Strings.MainWarpOutOfRange);
      content.invalidate();
      return;
    }
    try {
      if(game.getAutoSave()) {
        Functions.SaveFile(new File(Consts.SaveDirectory, AUTOSAVE_DEPARTURE).getPath(), game.Serialize(), game.Dialogs());
      }
      game.Warp(false);
      if(game.getAutoSave()) {
        Functions.SaveFile(new File(Consts.SaveDirectory, AUTOSAVE_ARRIVAL).getPath(), game.Serialize(), game.Dialogs());
      }
    } catch(GameEndException e) {
      showGameEnd(game);
      return;
    }
    refresh();
  }

  private boolean handleMenuKey(KeyStroke key) {
    switch(key.getKeyType()) {
      case ArrowUp:
        content.moveMenuSelection(-1);
        return true;
      case ArrowDown:
        content.moveMenuSelection(1);
        return true;
      case Enter:
        activateMenuEntry();
        return true;
      case Escape:
        content.hideMenu();
        return true;
      default:
        return false;
    }
  }

  /**
   * Leaves the program, asking first when a game is loaded: the progress lives only
   * in memory until the player saves it (F5) or the departure/arrival autosaves run.
   * The question is the abandon confirmation of New Game and Load, so a player who
   * quits by mistake is told the same thing in every way out.
   */
  private void quit() {
    if(confirmQuit()) {
      window.close();
    }
  }

  /** True when quitting is confirmed (or there is nothing to lose: no game loaded). */
  private boolean confirmQuit() {
    Game game = gameSupplier.get();
    if(game == null) {
      return true;
    }
    return game.Dialogs().alert(AlertType.GameAbandonConfirm) == DialogResult.Yes;
  }

  private void toggleMenu() {
    if(content.menuVisible()) {
      content.hideMenu();
      return;
    }
    // The menu also opens on the title screen: its actions handle the empty game.
    List<String> items = new ArrayList<>();
    menuActions.clear();
    addMenuItem(items, Strings.MenuScores, this::openHighScores);
    addMenuItem(items, Strings.MenuOptions, this::openOptions);
    addMenuItem(items, Strings.MenuSave, () -> runAction(saveGameAction, Strings.MainSaveUnavailable));
    addMenuItem(items, Strings.MenuLoad, () -> runAction(loadGameAction, Strings.MainLoadUnavailable));
    addMenuItem(items, Strings.MenuNewGame, () -> runAction(newGameAction, Strings.MainNewGameUnavailable));
    addMenuItem(items, Strings.MenuAbout, this::openAbout);
    addMenuItem(items, Strings.MenuQuit, this::quit);
    content.showMenu(items);
  }

  private void addMenuItem(List<String> items, String label, Runnable action) {
    items.add(label);
    menuActions.add(action);
  }

  private void activateMenuEntry() {
    int index = content.menuIndex();
    content.hideMenu();
    if(index >= 0 && index < menuActions.size()) {
      menuActions.get(index).run();
    }
  }

  private String directKeys() {
    Game game = gameSupplier.get();
    if(game == null || game.Commander().CurrentSystem() == null) {
      return Strings.DirectKeys + " · " + Strings.DirectMenu;
    }
    StarSystem system = game.Commander().CurrentSystem();
    boolean noTech = system.TechLevel().ordinal()
        < Consts.ShipSpecs.get(ShipType.Flea.CastToInt()).MinimumTechLevel().ordinal();
    List<String> contextual = new ArrayList<>();
    if(system.ShowSpecialButton()) {
      contextual.add(Strings.NavSpecial);
    }
    if(game.getCanSuperWarp()) {
      contextual.add(Strings.NavJump);
    }
    if(!noTech) {
      contextual.add(Strings.NavShips);
      contextual.add(Strings.NavEquip);
      if(system.Shipyard() != null) {
        contextual.add(Strings.NavDesign);
      }
      if(!game.Commander().getShip().getEscapePod()) {
        contextual.add(Strings.NavPod);
      }
    }
    if(hasCrew(game)) {
      contextual.add(Strings.NavCrew);
    }
    StringBuilder keys = new StringBuilder(Strings.DirectKeys);
    for(String token : contextual) {
      keys.append(" · ").append(token);
    }
    // The program menu always goes last, after the contextual actions.
    keys.append(" · ").append(Strings.DirectMenu);
    return keys.toString();
  }

  private static boolean hasCrew(Game game) {
    CrewMember[] crew = game.Commander().getShip().Crew();
    for(int i = 1; i < crew.length; i++) {
      if(crew[i] != null) {
        return true;
      }
    }
    return false;
  }

  private boolean handleNewsKey(KeyStroke key) {
    switch(key.getKeyType()) {
      case ArrowUp:
        content.moveNewsScroll(-1);
        return true;
      case ArrowDown:
        content.moveNewsScroll(1);
        return true;
      case PageUp:
        content.moveNewsScroll(-10);
        return true;
      case PageDown:
        content.moveNewsScroll(10);
        return true;
      default:
        return false;
    }
  }

  private void openNews() {
    Game game = gameSupplier.get();
    if(game == null) {
      return;
    }
    content.news(game.NewspaperHead(), game.NewspaperText());
    content.openNews();
  }

  private boolean handleGameOverKey(KeyStroke key) {
    switch(key.getKeyType()) {
      case F2:
        return runAction(newGameAction, Strings.MainNewGameUnavailable);
      case F3:
        openHighScores();
        return true;
      case Escape:
        window.close();
        return true;
      case Character:
        if(Character.toLowerCase(key.getCharacter()) == 'q') {
          window.close();
          return true;
        }
        return false;
      default:
        return false;
    }
  }

  /**
   * The game ended: show the result, insert the score in the table and leave the
   * high scores on the panel. Only the new game, the scores and quitting work now.
   */
  private void showGameEnd(Game game) {
    gameOver = true;
    game.Dialogs().alert(endAlert(game.getEndStatus()));
    int score = game.Score();
    game.Dialogs().alert(AlertType.GameEndScore,
        Functions.FormatNumber(score / 10), Functions.FormatNumber(score % 10));
    HighScoreRecord candidate = new HighScoreRecord(game.Commander().Name(), score, game.getEndStatus(),
        game.Commander().getDays(), game.Commander().Worth(), game.Difficulty());
    if(HighScores.Qualifies(candidate, Functions.GetHighScores(game.Dialogs()))) {
      if(game.getCheatEnabled()) {
        game.Dialogs().alert(AlertType.GameEndHighScoreCheat);
      } else {
        HighScores.Add(Consts.HighScoreFile, candidate, game.Dialogs());
        game.Dialogs().alert(AlertType.GameEndHighScoreAchieved);
      }
    } else {
      game.Dialogs().alert(AlertType.GameEndHighScoreMissed);
    }
    new HighScoresPresenter(Functions.GetHighScores(game.Dialogs()), this).update();
    content.openHighScores();
    content.log(Strings.MainGameOver);
    content.invalidate();
  }

  private static AlertType endAlert(GameEndType status) {
    switch(status) {
      case Killed:
        return AlertType.GameEndKilled;
      case Retired:
        return AlertType.GameEndRetired;
      case BoughtMoon:
        return AlertType.GameEndBoughtMoon;
      case BoughtMoonGirl:
        return AlertType.GameEndBoughtMoonGirl;
      default:
        return AlertType.Alert;
    }
  }

  private void buyRepairs() {
    if(presenter != null && presenter.buyRepairs()) {
      presenter.updateAll();
    }
    content.invalidate();
  }
}
