/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.gts.bst.view.BankViewModel;
import org.gts.bst.view.CargoRowViewModel;
import org.gts.bst.view.CargoViewModel;
import org.gts.bst.view.ChartSystem;
import org.gts.bst.view.CommanderViewModel;
import org.gts.bst.view.EquipmentInfoViewModel;
import org.gts.bst.view.EquipmentViewModel;
import org.gts.bst.view.HighScoresViewModel;
import org.gts.bst.view.ChartColor;
import org.gts.bst.view.ChartType;
import org.gts.bst.view.ChartViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.LanternaChartView;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelViewModel;
import org.gts.bst.view.QuestsViewModel;
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListViewModel;
import org.gts.bst.view.ShipSprites;
import org.gts.bst.view.ShipViewModel;
import org.gts.bst.view.ShipyardDesignerViewModel;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.PoliceRecord;
import spacetrader.Ship;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.UniverseGenerator;
import spacetrader.util.Util;


/**
 * The main screen of the text UI: header with the commander state, the chart, the
 * system panel with the cargo table and a status line. It is drawn as one character
 * grid and it is the only focusable component, so it receives the keys.
 */
public final class MainTextComponent extends AbstractInteractableComponent<MainTextComponent> {
  /**
   * Handles a key; returns whether it was consumed.
   */
  @FunctionalInterface
  public interface KeyHandler {
    boolean handle(KeyStroke keyStroke);
  }

  private static final int NAVIGATION_PANEL_WIDTH = 42;
  private static final int TRADE_PANEL_WIDTH = 48;
  private static final int TRADE_PANEL_TARGET_WIDTH = 75;
  /** From this screen width the trade panel shows the target columns. */
  private static final int TRADE_TARGET_COLUMNS_SCREEN = 120;

  private final Supplier<Game> gameSupplier;
  private final KeyHandler keyHandler;
  private final List<String> log = new ArrayList<>();
  private ChartType chartType = ChartType.GALACTIC;
  private MainPanel panel = MainPanel.Navigation;
  private int selectedItem;
  private SystemInfoViewModel system;
  private CargoViewModel cargo;
  private DockViewModel dock;
  private TargetSystemViewModel target;
  private BankViewModel bank;
  private QuestsViewModel quests;
  private PersonnelViewModel personnel;
  private PersonnelInfo personnelInfo;
  private CommanderViewModel commander;
  private ShipViewModel ship;
  private ShipListViewModel shipList;
  private ShipInfoViewModel shipInfo;
  private EquipmentViewModel equipment;
  private EquipmentInfoViewModel equipmentInfo;
  private int personnelIndex;
  private int shipListIndex;
  private int equipmentIndex;
  private final List<String> optionsLines = new ArrayList<>();
  private int optionsIndex;
  private HighScoresViewModel highScores;
  private ShipyardDesignerViewModel designer;
  private int designerField;
  private final List<String> newsLines = new ArrayList<>();
  private String newsHead = "";
  private int newsScroll;
  private String directKeys = "";
  private final List<String> menuItems = new ArrayList<>();
  private boolean menuVisible;
  private String menuTitle = Strings.MenuTitle;
  private int menuIndex;
  private int panelWidth = NAVIGATION_PANEL_WIDTH;
  /** Row where the chart and the panel start; the header takes one line when it fits. */
  private int contentTop = 3;
  private int viewX = -1;
  private int viewY = -1;
  private int viewSystemId = -1;

  public MainTextComponent(Supplier<Game> gameSupplier, KeyHandler keyHandler) {
    this.gameSupplier = gameSupplier;
    this.keyHandler = keyHandler;
  }

  public void system(SystemInfoViewModel system) {
    this.system = system;
  }

  public void cargo(CargoViewModel cargo) {
    this.cargo = cargo;
  }

  public void dock(DockViewModel dock) {
    this.dock = dock;
  }

  public void target(TargetSystemViewModel target) {
    this.target = target;
  }

  public void bank(BankViewModel bank) {
    this.bank = bank;
  }

  public void quests(QuestsViewModel quests) {
    this.quests = quests;
  }

  public void personnel(PersonnelViewModel personnel) {
    this.personnel = personnel;
  }

  public void personnelInfo(PersonnelInfo personnelInfo) {
    this.personnelInfo = personnelInfo;
  }

  public void commander(CommanderViewModel commander) {
    this.commander = commander;
  }

  public void ship(ShipViewModel ship) {
    this.ship = ship;
  }

  public void shipList(ShipListViewModel shipList) {
    this.shipList = shipList;
  }

  public void shipInfo(ShipInfoViewModel shipInfo) {
    this.shipInfo = shipInfo;
  }

  public void equipment(EquipmentViewModel equipment) {
    this.equipment = equipment;
  }

  public void equipmentInfo(EquipmentInfoViewModel equipmentInfo) {
    this.equipmentInfo = equipmentInfo;
  }

  public ShipListViewModel shipList() {
    return shipList;
  }

  public EquipmentViewModel equipment() {
    return equipment;
  }

  public int shipListIndex() {
    return shipListIndex;
  }

  public void shipListIndex(int index) {
    this.shipListIndex = index;
    invalidate();
  }

  public int shipListEntryCount() {
    return shipList == null ? 0 : shipList.rows().size();
  }

  public int equipmentIndex() {
    return equipmentIndex;
  }

  public void equipmentIndex(int index) {
    this.equipmentIndex = index;
    invalidate();
  }

  public void options(List<String> lines) {
    optionsLines.clear();
    optionsLines.addAll(lines);
    invalidate();
  }

  public int optionsIndex() {
    return optionsIndex;
  }

  public void optionsIndex(int index) {
    this.optionsIndex = index;
    invalidate();
  }

  public int optionsCount() {
    return optionsLines.size();
  }

  public void highScores(HighScoresViewModel model) {
    this.highScores = model;
    invalidate();
  }

  public void designer(ShipyardDesignerViewModel designer) {
    this.designer = designer;
    invalidate();
  }

  public ShipyardDesignerViewModel designer() {
    return designer;
  }

  public int designerField() {
    return designerField;
  }

  public void designerField(int field) {
    this.designerField = field;
    invalidate();
  }

  public void news(String head, String text) {
    newsHead = head;
    newsLines.clear();
    wrap(newsLines, text, NAVIGATION_PANEL_WIDTH - 2);
    newsScroll = 0;
    invalidate();
  }

  public int newsScroll() {
    return newsScroll;
  }

  public int newsLineCount() {
    return newsLines.size();
  }

  public void moveNewsScroll(int delta) {
    newsScroll = Math.max(0, Math.min(newsScroll + delta, Math.max(0, newsLines.size() - 1)));
    invalidate();
  }

  public int equipmentEntryCount() {
    if(equipment == null) {
      return 0;
    }
    return equipment.buyWeapons().size() + equipment.buyShields().size() + equipment.buyGadgets().size()
        + equipment.sellWeapons().size() + equipment.sellShields().size() + equipment.sellGadgets().size();
  }

  public int personnelIndex() {
    return personnelIndex;
  }

  public void personnelIndex(int index) {
    this.personnelIndex = index;
    invalidate();
  }

  public int personnelEntryCount() {
    return personnel == null ? 0 : personnel.crewEntries().size() + personnel.forHireEntries().size();
  }

  public int personnelCrewSize() {
    return personnel == null ? 0 : personnel.crewEntries().size();
  }

  public void toggleChart() {
    chartType = chartType == ChartType.GALACTIC ? ChartType.SHORT_RANGE : ChartType.GALACTIC;
  }

  public ChartType chartType() {
    return chartType;
  }

  public MainPanel panel() {
    return panel;
  }

  public void openTrade() {
    panel = MainPanel.Trade;
    invalidate();
  }

  public void openBank() {
    panel = MainPanel.Bank;
    invalidate();
  }

  public void openQuests() {
    panel = MainPanel.Quests;
    invalidate();
  }

  public void openPersonnel() {
    panel = MainPanel.Personnel;
    invalidate();
  }

  public void openCommander() {
    panel = MainPanel.Commander;
    invalidate();
  }

  public void openShip() {
    panel = MainPanel.Ship;
    invalidate();
  }

  public void openAbout() {
    panel = MainPanel.About;
    invalidate();
  }

  public void openShipList() {
    panel = MainPanel.ShipList;
    invalidate();
  }

  public void openEquipment() {
    panel = MainPanel.Equipment;
    invalidate();
  }

  public void openOptions() {
    panel = MainPanel.Options;
    invalidate();
  }

  public void openHighScores() {
    panel = MainPanel.HighScores;
    invalidate();
  }

  public void openDesigner() {
    panel = MainPanel.Designer;
    invalidate();
  }

  public void openNews() {
    panel = MainPanel.News;
    invalidate();
  }

  public void closePanel() {
    panel = MainPanel.Navigation;
    invalidate();
  }

  public int selectedItem() {
    return selectedItem;
  }

  public void moveItemSelection(int delta) {
    if(cargo == null || cargo.rows().isEmpty()) {
      return;
    }
    selectedItem = Math.floorMod(selectedItem + delta, cargo.rows().size());
    invalidate();
  }

  public void log(String message) {
    log.add(message);
    while(log.size() > 3) {
      log.remove(0);
    }
  }

  @Override
  protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
    return keyHandler.handle(keyStroke) ? Interactable.Result.HANDLED : Interactable.Result.UNHANDLED;
  }

  @Override
  protected InteractableRenderer<MainTextComponent> createDefaultRenderer() {
    return new InteractableRenderer<MainTextComponent>() {
      @Override
      public TerminalPosition getCursorLocation(MainTextComponent component) {
        return null;
      }

      @Override
      public TerminalSize getPreferredSize(MainTextComponent component) {
        return TerminalSize.ZERO;
      }

      @Override
      public void drawComponent(TextGUIGraphics graphics, MainTextComponent component) {
        component.paint(graphics);
      }
    };
  }

  private void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    int width = size.getColumns();
    int height = size.getRows();
    if(width < 60 || height < 15) {
      graphics.putString(0, 0, Strings.WindowTooSmall);
      return;
    }
    UiPalette.reset(graphics);
    String blank = " ".repeat(width);
    for(int row = 0; row < height; row++) {
      graphics.putString(0, row, blank);
    }
    Game game = gameSupplier.get();
    Commander cmdr = game == null ? null : game.Commander();
    List<String> footerKeys = panel == MainPanel.Navigation ? footerKeys(width - 2) : new ArrayList<>();
    contentTop = drawHeader(graphics, width, cmdr) + 1;
    panelWidth = Math.max(20, Math.min(width - 24, panelWidthFor(panel, width)));
    int chartWidth = width - panelWidth - 2;
    // The footer keeps two menu rows (the usual case); a longer menu takes one more.
    int chartHeight = height - contentTop - Math.max(2, footerKeys.size()) - 2;
    drawChart(graphics, chartWidth, chartHeight, game, cmdr);
    drawPanel(graphics, width - panelWidth, chartWidth, height);
    drawFooter(graphics, width, height, footerKeys);
    drawMenu(graphics, width, height);
  }

  /**
   * Draws the header and returns its height: one line when every field fits,
   * the commander data and the ship data on one line each otherwise.
   */
  private int drawHeader(TextGUIGraphics graphics, int width, Commander cmdr) {
    if(cmdr == null) {
      UiPalette.line(graphics, 1, 0, Strings.MainNoGame, width - 2);
      UiPalette.reset(graphics);
      graphics.drawLine(0, 1, width - 1, 1, '─');
      return 1;
    }
    Ship ship = cmdr.getShip();
    int maxX = width - 2;
    List<HeaderField> commanderFields = List.of(
        new HeaderField(cmdr.Name(), UiPalette.TEXT),
        new HeaderField(Functions.StringVars(Strings.MainDay, "" + cmdr.getDays()), UiPalette.ACCENT),
        new HeaderField(Functions.FormatMoney(cmdr.getCash()), UiPalette.MONEY),
        new HeaderField(Functions.StringVars(Strings.MainDebt, Functions.FormatMoney(cmdr.getDebt())),
            cmdr.getDebt() > 0 ? UiPalette.BAD : UiPalette.TEXT));
    List<HeaderField> shipFields = List.of(
        new HeaderField(Functions.StringVars(Strings.MainFuel, "" + ship.getFuel(), "" + ship.FuelTanks()),
            UiPalette.statusColor(ship.getFuel(), ship.FuelTanks())),
        new HeaderField(Functions.StringVars(Strings.MainHull, "" + ship.getHull(), "" + ship.HullStrength()),
            UiPalette.statusColor(ship.getHull(), ship.HullStrength())),
        new HeaderField(Functions.StringVars(Strings.MainShields, "" + ship.ShieldCharge(),
            "" + ship.ShieldStrength()), UiPalette.statusColor(ship.ShieldCharge(), ship.ShieldStrength())),
        new HeaderField(Functions.StringVars(Strings.MainCargo, "" + ship.FilledCargoBays(),
            "" + ship.CargoBays()), UiPalette.TEXT),
        new HeaderField(Functions.StringVars(Strings.MainPolice,
            PoliceRecord.GetPoliceRecordFromScore(cmdr.getPoliceRecordScore()).Name()),
            policeColor(cmdr.getPoliceRecordScore())));
    boolean oneLine = fieldsWidth(commanderFields) + 3 + fieldsWidth(shipFields) <= maxX;
    int headerHeight = oneLine ? 1 : 2;
    if(oneLine) {
      List<HeaderField> fields = new ArrayList<>(commanderFields);
      fields.addAll(shipFields);
      drawFields(graphics, 1, 0, fields, maxX);
    } else {
      drawFields(graphics, 1, 0, commanderFields, maxX);
      drawFields(graphics, 1, 1, shipFields, maxX);
    }
    UiPalette.reset(graphics);
    graphics.drawLine(0, headerHeight, width - 1, headerHeight, '─');
    return headerHeight;
  }

  private static int fieldsWidth(List<HeaderField> fields) {
    int width = 0;
    for(HeaderField field : fields) {
      width += field.text().length() + 3;
    }
    return Math.max(0, width - 3);
  }

  private static void drawFields(TextGUIGraphics graphics, int x, int row, List<HeaderField> fields, int maxX) {
    boolean first = true;
    for(HeaderField field : fields) {
      if(!first) {
        x = UiPalette.draw(graphics, x, row, " · ", UiPalette.TEXT, maxX);
      }
      x = UiPalette.draw(graphics, x, row, field.text(), field.color(), maxX);
      first = false;
    }
  }

  /** One field of the header: its text and its colour. */
  private record HeaderField(String text, TextColor color) {
  }

  private static TextColor policeColor(int score) {
    switch(PoliceRecord.GetPoliceRecordFromScore(score).Type()) {
      case Clean:
      case Lawful:
      case Trusted:
      case Liked:
      case Hero:
        return UiPalette.GOOD;
      case Crook:
      case Dubious:
        return UiPalette.WARN;
      default:
        return UiPalette.BAD;
    }
  }

  private void drawChart(TextGUIGraphics graphics, int chartWidth, int chartHeight, Game game, Commander cmdr) {
    String title = chartType == ChartType.GALACTIC ? Strings.MainChartGalactic : Strings.MainChartShortRange;
    UiPalette.title(graphics, 1, contentTop, title, chartWidth);
    if(game == null || cmdr == null) {
      return;
    }
    TerminalSize size = new TerminalSize(chartWidth, chartHeight);
    LanternaChartView chart = new LanternaChartView(
        graphics.newTextGraphics(new TerminalPosition(1, contentTop + 1), size), size);
    chart.render(chartModel(game, cmdr, chartWidth, chartHeight));
  }

  private ChartViewModel chartModel(Game game, Commander cmdr, int chartWidth, int chartHeight) {
    StarSystem[] universe = game.Universe();
    StarSystem current = cmdr.CurrentSystem();
    StarSystem chosen = game.SelectedSystem() == null ? current : game.SelectedSystem();
    int selected = chosen.Id().CastToInt();
    int tracked = game.getTrackedSystemId().CastToInt();
    int warp = game.WarpSystem() == null ? -1 : game.WarpSystem().Id().CastToInt();
    List<ChartSystem> systems = new ArrayList<>(universe.length);
    for(int i = 0; i < universe.length; i++) {
      StarSystem system = universe[i];
      boolean wormhole = Util.BruteSeek(game.Wormholes(), i) >= 0;
      StarSystem pair = wormhole ? Functions.WormholeTarget(i) : null;
      systems.add(new ChartSystem(system.X(), system.Y(), system.Name(), system.Visited(), wormhole,
          i == warp, i == tracked, i == selected, system.Size(), ChartColor.starColor(i, system.Visited()),
          pair == null ? -1 : pair.X(), pair == null ? -1 : pair.Y()));
    }
    int fuel = cmdr.getShip().getFuel();
    if(chartType == ChartType.GALACTIC) {
      return ChartViewModel.galactic(systems, current.X(), current.Y(), fuel,
          UniverseGenerator.GalaxyWidth, UniverseGenerator.GalaxyHeight, game.Options().getGalaxyColumns());
    }
    // The short-range chart follows the cursor: its view only scrolls when the
    // selected system gets close to an edge, and it re-centres after a trip.
    if(current.Id().CastToInt() != viewSystemId) {
      viewSystemId = current.Id().CastToInt();
      viewX = -1;
      viewY = -1;
    }
    viewX = scrollTo(viewX, chosen.X(), chartWidth, UniverseGenerator.GalaxyWidth, chartMargin(chartWidth));
    viewY = scrollTo(viewY, chosen.Y(), chartHeight, UniverseGenerator.GalaxyHeight, chartMargin(chartHeight));
    String trackedText = null;
    if(game.TrackedSystem() != null && game.Options().getShowTrackedRange()) {
      trackedText = Functions.StringVars(Strings.MainTarget, game.TrackedSystem().Name(),
          "" + Functions.Distance(current, game.TrackedSystem()));
    }
    return ChartViewModel.shortRange(systems, current.X(), current.Y(), viewX, viewY, fuel, trackedText);
  }

  /**
   * Keeps the galactic-chart viewport still while the selected system stays inside it,
   * and scrolls just enough to keep it {@code margin} cells away from the edges, so
   * moving the cursor does not drag the whole map. A negative {@code view} centres the
   * viewport on the selection the first time.
   */
  static int scrollTo(int view, int selected, int size, int galaxySize, int margin) {
    int next;
    if(view < 0) {
      next = selected - size / 2;
    } else if(selected < view + margin) {
      next = selected - margin;
    } else if(selected > view + size - 1 - margin) {
      next = selected - (size - 1 - margin);
    } else {
      next = view;
    }
    return Math.max(0, Math.min(next, Math.max(0, galaxySize - size)));
  }

  private static int chartMargin(int size) {
    return Math.max(2, size / 8);
  }

  private static int panelWidthFor(MainPanel panel, int screenWidth) {
    switch(panel) {
      case Trade:
        // With room for it, the target columns (price, +/- and %) fit in the table;
        // on smaller screens they move to the detail line of the selected item.
        return screenWidth >= TRADE_TARGET_COLUMNS_SCREEN ? TRADE_PANEL_TARGET_WIDTH : TRADE_PANEL_WIDTH;
      case Bank:
        return 38;
      case Quests:
        return 60;
      case Personnel:
        return 46;
      case Commander:
        return 42;
      case Ship:
        return 50;
      case ShipList:
        return 52;
      case Equipment:
        return 54;
      case Options:
        return 60;
      case HighScores:
        return 60;
      case Designer:
        return 60;
      case News:
      case About:
        return 60;
      default:
        return NAVIGATION_PANEL_WIDTH;
    }
  }

  private void drawPanel(TextGUIGraphics graphics, int x, int chartWidth, int height) {
    UiPalette.reset(graphics);
    graphics.drawLine(chartWidth + 1, contentTop, chartWidth + 1, height - 4, '│');
    switch(panel) {
      case Trade:
        drawTradePanel(graphics, x, height);
        break;
      case Bank:
        drawBankPanel(graphics, x, height);
        break;
      case Quests:
        drawQuestsPanel(graphics, x, height);
        break;
      case Personnel:
        drawPersonnelPanel(graphics, x, height);
        break;
      case Commander:
        drawCommanderPanel(graphics, x, height);
        break;
      case Ship:
        drawShipPanel(graphics, x, height);
        break;
      case ShipList:
        drawShipListPanel(graphics, x, height);
        break;
      case Equipment:
        drawEquipmentPanel(graphics, x, height);
        break;
      case Options:
        drawOptionsPanel(graphics, x, height);
        break;
      case HighScores:
        drawHighScoresPanel(graphics, x, height);
        break;
      case Designer:
        drawDesignerPanel(graphics, x, height);
        break;
      case News:
        drawNewsPanel(graphics, x, height);
        break;
      case About:
        drawAboutPanel(graphics, x, height);
        break;
      default:
        drawNavigationPanel(graphics, x, height);
        break;
    }
    drawPanelKeys(graphics, x, height);
  }

  private void drawPanelKeys(TextGUIGraphics graphics, int x, int height) {
    if(panel == MainPanel.Navigation) {
      return;
    }
    List<String> lines = new ArrayList<>();
    String keys = keysFor(panel);
    if(keys != null) {
      wrapTokens(lines, keys, panelWidth);
    }
    int row = height - 4 - Math.max(0, lines.size() - 1);
    for(String line : lines) {
      UiPalette.keys(graphics, x, row++, line, panelWidth);
    }
  }

  private void drawBankPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.BankTitle, panelWidth);
    int row = contentTop + 1;
    if(bank == null) {
      return;
    }
    UiPalette.money(graphics, x, row++, Functions.StringVars(Strings.BankDebt, bank.currentDebt()), panelWidth);
    UiPalette.money(graphics, x, row++, Functions.StringVars(Strings.BankMaxLoan, bank.maxLoan()), panelWidth);
    UiPalette.money(graphics, x, row++, Functions.StringVars(Strings.BankShipValue, bank.shipValue()), panelWidth);
    UiPalette.line(graphics, x, row++, Functions.StringVars(Strings.BankNoClaim, bank.noClaim()), panelWidth);
    UiPalette.money(graphics, x, row++, Functions.StringVars(Strings.BankInsurance, bank.insuranceCost()), panelWidth);
    UiPalette.title(graphics, x, row + 1, bank.insuranceButtonText(), panelWidth);
  }

  private void drawPersonnelPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.PersonnelTitle, panelWidth);
    if(personnel == null) {
      return;
    }
    int row = contentTop + 1;
    UiPalette.title(graphics, x, row++, Strings.PersonnelCrew, panelWidth);
    int index = 0;
    for(String entry : personnel.crewEntries()) {
      if(row >= height - 5) {
        return;
      }
      drawRow(graphics, x, row++, index == personnelIndex, (index == personnelIndex ? ">" : " ") + " " + entry);
      index++;
    }
    row++;
    if(row < height - 5) {
      UiPalette.title(graphics, x, row++, Strings.PersonnelForHire, panelWidth);
    }
    for(String entry : personnel.forHireEntries()) {
      if(row >= height - 5) {
        return;
      }
      drawRow(graphics, x, row++, index == personnelIndex, (index == personnelIndex ? ">" : " ") + " " + entry);
      index++;
    }
    if(personnelInfo != null && personnelInfo.visible() && row < height - 5) {
      row++;
      if(row < height - 5) {
        UiPalette.draw(graphics, x, row++, personnelInfo.name()
            + (personnelInfo.rateVisible() ? "  " + personnelInfo.rate() : ""), UiPalette.ACCENT, x + panelWidth);
      }
      if(row < height - 5) {
        graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderSkills, new String[]{
            personnelInfo.pilot(), personnelInfo.fighter(), personnelInfo.trader(), personnelInfo.engineer()}), panelWidth));
      }
      if(row < height - 5) {
        UiPalette.draw(graphics, x, row, personnelInfo.hireFireText(), UiPalette.KEY, x + panelWidth);
      }
    }
  }

  private void drawCommanderPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.CommanderTitle, panelWidth);
    if(commander == null) {
      return;
    }
    int row = contentTop + 1;
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderHeader,
        commander.name(), commander.difficulty()), UiPalette.TEXT);
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderTime, commander.time()),
        UiPalette.ACCENT);
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderSkills, new String[]{
        commander.pilot(), commander.fighter(), commander.trader(), commander.engineer()}), UiPalette.TEXT);
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderCash, new String[]{
        commander.cash(), commander.debt(), commander.netWorth()}), UiPalette.MONEY);
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderKills, commander.kills()),
        UiPalette.TEXT);
    row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderRecord, commander.record(),
        commander.reputation()), UiPalette.ACCENT);
    if(commander.bounty().visible()) {
      drawWrapped(graphics, x, row, Functions.StringVars(Strings.CommanderBounty, commander.bounty().label(),
          commander.bounty().amount()), UiPalette.BAD);
    }
  }

  private void drawShipPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.ShipTitle, panelWidth);
    if(ship == null) {
      return;
    }
    int row = contentTop + 1;
    UiPalette.draw(graphics, x, row++, Functions.StringVars(Strings.ShipType, ship.type()), UiPalette.ACCENT, x + panelWidth);
    for(String artLine : ShipSprites.of(ship.typeId())) {
      if(row >= height - 5) {
        break;
      }
      graphics.putString(x, row++, cut(artLine, panelWidth));
    }
    row++;
    String[] labels = ship.equipmentLabels().split("\n", -1);
    String[] values = ship.equipmentValues().split("\n", -1);
    for(int i = 0; i < labels.length && i < values.length && row < height - 5; i++) {
      graphics.putString(x, row++, cut(labels[i] + " " + values[i], panelWidth));
    }
    if(!ship.specialCargo().isEmpty()) {
      List<String> wrapped = new ArrayList<>();
      wrap(wrapped, Functions.StringVars(Strings.ShipSpecialCargo, ship.specialCargo()), panelWidth);
      for(String line : wrapped) {
        if(row >= height - 5) {
          break;
        }
        UiPalette.draw(graphics, x, row++, line, UiPalette.WARN, x + panelWidth);
      }
    }
  }

  private void drawShipListPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.ShipListTitle, panelWidth);
    if(shipList == null) {
      return;
    }
    int limit = height - 5;
    List<ShipListViewModel.Row> rows = shipList.rows();
    // The selected ship block (blank, name, sprite, three stats) reserves its rows
    // first, so the list scrolls instead of pushing the sprite out of the panel.
    int visible = shipInfo == null ? limit - 4 : Math.max(1, limit - 14);
    int start = Math.max(0, Math.min(shipListIndex - visible + 1, Math.max(0, rows.size() - visible)));
    int row = contentTop + 1;
    for(int i = start; i < rows.size() && i < start + visible && row < limit; i++) {
      ShipListViewModel.Row item = rows.get(i);
      drawRow(graphics, x, row++, i == shipListIndex, String.format("%s %-14s %14s",
          i == shipListIndex ? ">" : " ", item.name(), item.price()));
    }
    if(shipInfo == null || row >= limit) {
      return;
    }
    row++;
    if(row < limit) {
      UiPalette.draw(graphics, x, row++, shipInfo.name() + "  " + shipInfo.size(), UiPalette.ACCENT, x + panelWidth);
    }
    for(String artLine : ShipSprites.of(shipInfo.type())) {
      if(row >= limit) {
        break;
      }
      graphics.putString(x, row++, cut(artLine, panelWidth));
    }
    row++;
    if(row < limit) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipInfoBays, shipInfo.bays())
          + " · " + Functions.StringVars(Strings.ShipInfoRange, shipInfo.range()), panelWidth));
    }
    if(row < limit) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipInfoHull, shipInfo.hull())
          + " · " + Functions.StringVars(Strings.ShipInfoCrew, shipInfo.crew()), panelWidth));
    }
    if(row < limit) {
      graphics.putString(x, row, cut(Functions.StringVars(Strings.ShipInfoWeapon, shipInfo.weapon())
          + " · " + Functions.StringVars(Strings.ShipInfoShield, shipInfo.shield())
          + " · " + Functions.StringVars(Strings.ShipInfoGadget, shipInfo.gadget()), panelWidth));
    }
  }

  private void drawRow(TextGUIGraphics graphics, int x, int row, boolean selected, String text) {
    if(selected) {
      UiPalette.selected(graphics, x, row, text, panelWidth);
    } else {
      UiPalette.line(graphics, x, row, text, panelWidth);
    }
  }

  private void drawDesignerRow(TextGUIGraphics graphics, int x, int row, int field, String text) {
    drawRow(graphics, x, row, designerField == field, text);
  }

  private void drawEquipmentPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.EquipmentTitle, panelWidth);
    if(equipment == null) {
      return;
    }
    int row = contentTop + 1;
    row = drawEquipmentSection(graphics, x, row, height, Strings.EquipmentBuySection, 0,
        equipment.buyWeapons().size() + equipment.buyShields().size() + equipment.buyGadgets().size());
    if(row < height - 5) {
      UiPalette.title(graphics, x, row++, Strings.EquipmentSellSection, panelWidth);
    }
    int firstSell = equipment.buyWeapons().size() + equipment.buyShields().size() + equipment.buyGadgets().size();
    List<String> sell = new ArrayList<>();
    sell.addAll(equipment.sellWeapons());
    sell.addAll(equipment.sellShields());
    sell.addAll(equipment.sellGadgets());
    row = drawEquipmentEntries(graphics, x, row, height, sell, firstSell);
    if(equipmentInfo != null && equipmentInfo.visible() && row < height - 5) {
      row++;
      if(row < height - 5) {
        UiPalette.draw(graphics, x, row++, equipmentInfo.name() + "  "
            + Functions.StringVars(Strings.EquipmentTypeLabel, equipmentInfo.type()), UiPalette.ACCENT, x + panelWidth);
      }
      if(row < height - 5) {
        String prices = "";
        if(!equipmentInfo.buyPrice().isEmpty()) {
          prices = Functions.StringVars(Strings.EquipmentBuyPrice, equipmentInfo.buyPrice());
        }
        if(!equipmentInfo.sellPrice().isEmpty()) {
          prices += (prices.isEmpty() ? "" : " · ") + Functions.StringVars(Strings.EquipmentSellPrice, equipmentInfo.sellPrice());
        }
        if(!prices.isEmpty()) {
          UiPalette.money(graphics, x, row++, prices, panelWidth);
        }
      }
      if(row < height - 5 && !equipmentInfo.power().isEmpty()) {
        graphics.putString(x, row++, cut(Functions.StringVars(Strings.EquipmentPower, equipmentInfo.power()), panelWidth));
      }
      if(row < height - 5 && !equipmentInfo.charge().isEmpty()) {
        graphics.putString(x, row++, cut(Functions.StringVars(Strings.EquipmentCharge, equipmentInfo.charge()), panelWidth));
      }
      List<String> wrapped = new ArrayList<>();
      wrap(wrapped, equipmentInfo.description(), panelWidth);
      for(String line : wrapped) {
        if(row >= height - 5) {
          break;
        }
        graphics.putString(x, row++, line);
      }
    }
  }

  private int drawEquipmentSection(TextGUIGraphics graphics, int x, int row, int height, String title, int first, int count) {
    if(row >= height - 5) {
      return row;
    }
    UiPalette.title(graphics, x, row++, title, panelWidth);
    List<String> buy = new ArrayList<>();
    buy.addAll(equipment.buyWeapons());
    buy.addAll(equipment.buyShields());
    buy.addAll(equipment.buyGadgets());
    return drawEquipmentEntries(graphics, x, row, height, buy, first);
  }

  private int drawEquipmentEntries(TextGUIGraphics graphics, int x, int row, int height, List<String> entries, int first) {
    for(int i = 0; i < entries.size() && row < height - 5; i++) {
      drawRow(graphics, x, row++, first + i == equipmentIndex, (first + i == equipmentIndex ? "> " : "  ") + entries.get(i));
    }
    return row;
  }

  private void drawOptionsPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.OptionsTitle, panelWidth);
    int row = contentTop + 1;
    for(int i = 0; i < optionsLines.size() && row < height - 5; i++) {
      drawRow(graphics, x, row++, i == optionsIndex, (i == optionsIndex ? "> " : "  ") + optionsLines.get(i));
    }
  }

  private void drawHighScoresPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.HighScoresTitle, panelWidth);
    if(highScores == null) {
      return;
    }
    int row = contentTop + 2;
    for(HighScoresViewModel.Row score : highScores.rows()) {
      if(!score.filled() || row >= height - 5) {
        continue;
      }
      int column = UiPalette.draw(graphics, x, row, score.name(), UiPalette.TEXT, x + panelWidth);
      UiPalette.draw(graphics, column, row, "   " + score.score(), UiPalette.MONEY, x + panelWidth);
      row++;
      List<String> wrapped = new ArrayList<>();
      wrap(wrapped, score.status(), panelWidth - 2);
      for(String line : wrapped) {
        if(row >= height - 5) {
          break;
        }
        graphics.putString(x + 2, row++, line);
      }
      row++;
    }
  }

  private void drawDesignerPanel(TextGUIGraphics graphics, int x, int height) {
    if(designer == null) {
      return;
    }
    int row = contentTop;
    UiPalette.title(graphics, x, row++, designer.title(), panelWidth);
    List<String> welcome = new ArrayList<>();
    wrap(welcome, designer.welcome(), panelWidth);
    for(String line : welcome) {
      if(row < height - 5) {
        graphics.putString(x, row++, line);
      }
    }
    if(row < height - 5) {
      drawDesignerRow(graphics, x, row++, 0, marker(0) + Functions.StringVars(Strings.DesignerSize,
          designer.sizes().isEmpty() ? "" : designer.sizes().get(designer.sizeIndex())));
    }
    if(row < height - 5) {
      drawDesignerRow(graphics, x, row++, 1, marker(1) + Functions.StringVars(Strings.DesignerTemplate,
          designer.templates().isEmpty() ? "" : designer.templates().get(designer.templateIndex())));
    }
    if(row < height - 5) {
      drawDesignerRow(graphics, x, row++, 2, marker(2) + Functions.StringVars(Strings.DesignerName, designer.name()));
    }
    String[] labels = {Strings.DesignerCargo, Strings.DesignerFuel, Strings.DesignerHull, Strings.DesignerWeapon,
        Strings.DesignerShield, Strings.DesignerGadget, Strings.DesignerCrew};
    for(int i = 0; i < designer.numerics().size() && i < labels.length && row < height - 5; i++) {
      ShipyardDesignerViewModel.Numeric numeric = designer.numerics().get(i);
      String text;
      if(numeric.min() != null && numeric.max() != null) {
        text = Functions.StringVars(Strings.DesignerNumeric, new String[]{
            labels[i], "" + numeric.value(), "" + numeric.min(), "" + numeric.max()});
      } else {
        text = Functions.StringVars(Strings.DesignerNumericValue, labels[i], "" + numeric.value());
      }
      drawDesignerRow(graphics, x, row++, 3 + i, marker(3 + i) + text);
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.DesignerUnits, designer.unitsUsed(), designer.percent()), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.DesignerCost, new String[]{
          designer.shipCost(), designer.designFee(), designer.penalty(), designer.tradeIn()}), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.DesignerTotal, designer.totalCost()), panelWidth));
    }
    if(row < height - 5) {
      drawDesignerRow(graphics, x, row++, 10, marker(10) + Strings.DesignerConstruct);
    }
    if(row < height - 5) {
      drawDesignerRow(graphics, x, row, 11, marker(11) + Strings.DesignerSave);
    }
  }

  private String marker(int field) {
    return fieldsMarker(field);
  }

  private String fieldsMarker(int field) {
    return designerField == field ? "> " : "  ";
  }

  public void directKeys(String keys) {
    directKeys = keys;
    invalidate();
  }

  public boolean menuVisible() {
    return menuVisible;
  }

  public int menuIndex() {
    return menuIndex;
  }

  public void showMenu(List<String> items) {
    showMenu(Strings.MenuTitle, items);
  }

  public void showMenu(String title, List<String> items) {
    menuTitle = title;
    menuItems.clear();
    menuItems.addAll(items);
    menuIndex = 0;
    menuVisible = true;
    invalidate();
  }

  public void hideMenu() {
    menuVisible = false;
    invalidate();
  }

  public void moveMenuSelection(int delta) {
    if(menuItems.isEmpty()) {
      return;
    }
    menuIndex = Math.floorMod(menuIndex + delta, menuItems.size());
    invalidate();
  }

  private void drawMenu(TextGUIGraphics graphics, int width, int height) {
    if(!menuVisible || menuItems.isEmpty()) {
      return;
    }
    int boxWidth = menuTitle.length();
    for(String item : menuItems) {
      boxWidth = Math.max(boxWidth, item.length() + 2);
    }
    boxWidth += 4;
    boxWidth = Math.min(boxWidth, width - 4);
    int boxHeight = menuItems.size() + 2;
    int left = 2;
    int top = contentTop;
    UiPalette.reset(graphics);
    String blank = " ".repeat(boxWidth);
    for(int y = top; y < top + boxHeight && y < height; y++) {
      graphics.putString(left, y, blank);
    }
    String title = " " + menuTitle + " ";
    UiPalette.title(graphics, left, top,
        "┌─" + title + "─".repeat(Math.max(0, boxWidth - 3 - title.length())) + "┐", boxWidth);
    for(int i = 0; i < menuItems.size(); i++) {
      String marker = i == menuIndex ? ">" : " ";
      String text = marker + " " + menuItems.get(i);
      String line = "│ " + text + " ".repeat(Math.max(0, boxWidth - 4 - text.length())) + " │";
      if(i == menuIndex) {
        UiPalette.selected(graphics, left, top + 1 + i, line, boxWidth);
      } else {
        UiPalette.line(graphics, left, top + 1 + i, line, boxWidth);
      }
    }
    UiPalette.title(graphics, left, top + boxHeight - 1, "└" + "─".repeat(boxWidth - 2) + "┘", boxWidth);
  }

  private void drawNewsPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.NewsTitle + " · " + newsHead, panelWidth);
    int row = contentTop + 2;
    int last = Math.min(newsLines.size(), newsScroll + (height - 9));
    for(int i = newsScroll; i < last; i++) {
      graphics.putString(x, row++, cut(newsLines.get(i), panelWidth));
    }
    graphics.putString(x, height - 4, cut(Functions.StringVars(Strings.NewsPosition,
        "" + (newsLines.isEmpty() ? 0 : newsScroll + 1), "" + newsLines.size()), panelWidth));
  }

  private void drawQuestsPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.QuestsTitle, panelWidth);
    int row = contentTop + 1;
    if(quests == null) {
      return;
    }
    List<String> lines = new ArrayList<>();
    wrap(lines, quests.text(), panelWidth);
    for(int i = 0; i < lines.size() && row < height - 5; i++) {
      graphics.putString(x, row++, cut(lines.get(i), panelWidth));
    }
  }

  private void drawTradePanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.TradeTitle, panelWidth);
    boolean wide = panelWidth >= TRADE_PANEL_TARGET_WIDTH;
    int row = contentTop + 1;
    if(wide) {
      UiPalette.title(graphics, x, row++, String.format("%s %-11s %10s %10s %5s %5s %10s %10s %5s", " ",
          Strings.TradeItem, Strings.TradeBuy, Strings.TradeSell, Strings.TradeCargo, Strings.TradeHere,
          Strings.TradeTarget, Strings.TradeDiff, Strings.TradePct), panelWidth);
    } else {
      UiPalette.title(graphics, x, row++, String.format("%s %-11s %10s %10s %5s %5s", " ",
          Strings.TradeItem, Strings.TradeBuy, Strings.TradeSell, Strings.TradeCargo, Strings.TradeHere), panelWidth);
    }
    if(cargo == null) {
      return;
    }
    int limit = wide ? height - 5 : height - 6;
    for(int i = 0; i < cargo.rows().size() && row < limit; i++) {
      CargoRowViewModel item = cargo.rows().get(i);
      String name = cut(Consts.TradeItems.get(i).Name(), 11);
      String marker = i == selectedItem ? ">" : " ";
      if(i == selectedItem) {
        drawRow(graphics, x, row++, true, tradeRow(marker, name, item, wide));
      } else {
        int column = UiPalette.draw(graphics, x, row, String.format("%s %-11s", marker, name),
            UiPalette.TEXT, x + panelWidth);
        column = UiPalette.draw(graphics, column, row, String.format(" %10s", item.buyPrice()),
            UiPalette.MONEY, x + panelWidth);
        column = UiPalette.draw(graphics, column, row, String.format(" %10s", item.sellPrice()),
            UiPalette.GOOD, x + panelWidth);
        column = UiPalette.draw(graphics, column, row, String.format(" %5s %5s", item.sellQty(), item.buyQty()),
            UiPalette.TEXT, x + panelWidth);
        if(wide) {
          column = UiPalette.draw(graphics, column, row, String.format(" %10s",
              cut(tradeValue(item.targetPrice(), Strings.CargoTargetPriceUnknown), 10)),
              UiPalette.ACCENT, x + panelWidth);
          column = UiPalette.draw(graphics, column, row, String.format(" %10s",
              cut(tradeValue(item.targetDiff(), Strings.CargoTargetDiffUnknown), 10)),
              UiPalette.MONEY, x + panelWidth);
          UiPalette.draw(graphics, column, row, String.format(" %5s",
              cut(tradeValue(item.targetPct(), Strings.CargoTargetPctUnknown), 5)),
              pctColor(item.targetPct()), x + panelWidth);
        }
        row++;
      }
    }
    if(!wide) {
      drawTradeTargetLine(graphics, x, row, height);
    } else if(tradeTargetNote() != null && row < height - 5) {
      UiPalette.line(graphics, x, row, tradeTargetNote(), panelWidth);
    }
  }

  private static String tradeRow(String marker, String name, CargoRowViewModel item, boolean wide) {
    if(wide) {
      return String.format("%s %-11s %10s %10s %5s %5s %10s %10s %5s", marker, name,
          item.buyPrice(), item.sellPrice(), item.sellQty(), item.buyQty(),
          cut(tradeValue(item.targetPrice(), Strings.CargoTargetPriceUnknown), 10),
          cut(tradeValue(item.targetDiff(), Strings.CargoTargetDiffUnknown), 10),
          cut(tradeValue(item.targetPct(), Strings.CargoTargetPctUnknown), 5));
    }
    return String.format("%s %-11s %10s %10s %5s %5s", marker, name,
        item.buyPrice(), item.sellPrice(), item.sellQty(), item.buyQty());
  }

  /**
   * On narrow panels the target columns do not fit: the selected item shows its
   * target price and margin in a single line under the table. When the target prices
   * are unknown, the line explains why instead.
   */
  private void drawTradeTargetLine(TextGUIGraphics graphics, int x, int row, int height) {
    if(row >= height - 5 || selectedItem < 0 || selectedItem >= cargo.rows().size()) {
      return;
    }
    String note = tradeTargetNote();
    if(note == null && Strings.CargoTargetPriceUnknown.equals(cargo.rows().get(selectedItem).targetPrice())) {
      note = Strings.TradeTargetNotTraded;
    }
    if(note != null) {
      UiPalette.line(graphics, x, row, note, panelWidth);
      return;
    }
    CargoRowViewModel item = cargo.rows().get(selectedItem);
    UiPalette.draw(graphics, x, row, Functions.StringVars(Strings.TradeTargetLine, new String[]{
        Consts.TradeItems.get(selectedItem).Name(), target.name(), item.targetPrice(), item.targetPct()}),
        UiPalette.ACCENT, x + panelWidth);
  }

  /** The unknown-values marker, drawn as a blank cell instead of dashes. */
  private static String tradeValue(String value, String unknown) {
    return unknown.equals(value) ? "" : value;
  }

  /**
   * Why the target prices are unknown (no target or out of range), or {@code null}
   * when they can be computed.
   */
  private String tradeTargetNote() {
    if(target == null || target.name().isEmpty()) {
      return Strings.TradeNoTarget;
    }
    return target.warpVisible() ? null : Strings.TradeTargetOutOfRange;
  }

  private static TextColor pctColor(String pct) {
    if(pct.startsWith("+")) {
      return UiPalette.GOOD;
    }
    return pct.startsWith("-") ? UiPalette.BAD : UiPalette.TEXT;
  }

  /** The provenance and the license, mirroring the NOTICE file. */
  private void drawAboutPanel(TextGUIGraphics graphics, int x, int height) {
    UiPalette.title(graphics, x, contentTop, Strings.AboutTitle, panelWidth);
    int row = contentTop + 2;
    row = drawWrapped(graphics, x, row, Strings.AboutOrigin, UiPalette.TEXT) + 1;
    row = drawWrapped(graphics, x, row, Strings.AboutPorts, UiPalette.TEXT) + 1;
    row = drawWrapped(graphics, x, row, Strings.AboutLicense, UiPalette.TEXT) + 1;
    drawWrapped(graphics, x, row, Strings.AboutHome, UiPalette.ACCENT);
  }

  private void drawNavigationPanel(TextGUIGraphics graphics, int x, int height) {
    int row = contentTop;
    if(targetSelected()) {
      row = drawSystemBlock(graphics, x, row, target.name(), target.size(), target.tech(), target.polSys(),
          target.resource(), target.police(), target.pirates());
      String distance = Functions.StringVars(Strings.MainTargetDistance, target.distance());
      if(target.outOfRangeVisible()) {
        distance += " · " + Strings.MainTargetOffRange;
      }
      UiPalette.draw(graphics, x, row++, distance,
          target.outOfRangeVisible() ? UiPalette.BAD : UiPalette.ACCENT, x + panelWidth);
      if(target.wormholeTo() != null && !target.wormholeTo().isEmpty()) {
        UiPalette.draw(graphics, x, row++, Functions.StringVars(Strings.MainWormhole, target.wormholeTo()),
            UiPalette.WORMHOLE, x + panelWidth);
      }
      row++;
    } else {
      if(system != null && !system.name().isEmpty()) {
        row = drawSystemBlock(graphics, x, row, system.name(), system.size(), system.tech(), system.polSys(),
            system.resource(), system.police(), system.pirates());
        if(system.specialVisible() && !system.specialTooltip().isEmpty()) {
          row = drawWrapped(graphics, x, row, Functions.StringVars(Strings.MainSpecial, system.specialTooltip()),
              UiPalette.ACCENT);
        }
        row++;
      }
      if(dock != null) {
        row = drawWrapped(graphics, x, row, dock.fuelStatus(), UiPalette.TEXT);
        if(dock.fuelButtonVisible()) {
          row = drawWrapped(graphics, x, row, dock.fuelCost(), UiPalette.MONEY);
        }
        row = drawWrapped(graphics, x, row, dock.hullStatus(), UiPalette.TEXT);
        if(dock.repairButtonVisible()) {
          row = drawWrapped(graphics, x, row, dock.repairCost(), UiPalette.MONEY);
        }
        row++;
      }
      if(target != null && target.navigationVisible() && !target.name().isEmpty()) {
        UiPalette.draw(graphics, x, row++, Functions.StringVars(Strings.MainTarget, target.name(), target.distance()),
            UiPalette.ACCENT, x + panelWidth);
        row++;
      }
    }
    if(cargo != null) {
      // When another system is shown, the table lists its prices and the margin.
      boolean targetPrices = targetSelected() && !cargo.rows().isEmpty()
          && !cargo.rows().get(0).targetSellPrice().isEmpty();
      if(row < height - 5) {
        UiPalette.title(graphics, x, row++, targetPrices
            ? String.format("%-10s %10s %10s %5s",
                Strings.TradeItem, Strings.TradeSell, Strings.TradeBuy, Strings.TradePct)
            : String.format("%-10s %10s %10s",
                Strings.TradeItem, Strings.TradeSell, Strings.TradeBuy), panelWidth);
      }
      for(int i = 0; i < cargo.rows().size() && row < height - 5; i++) {
        CargoRowViewModel item = cargo.rows().get(i);
        String name = cut(Consts.TradeItems.get(i).Name(), 10);
        int column = UiPalette.draw(graphics, x, row, String.format("%-10s", name),
            UiPalette.TEXT, x + panelWidth);
        column = UiPalette.draw(graphics, column, row, String.format(" %10s",
            targetPrices ? item.targetSellPrice() : item.sellPrice()), UiPalette.GOOD, x + panelWidth);
        column = UiPalette.draw(graphics, column, row, String.format(" %10s",
            targetPrices ? item.targetBuyPrice() : item.buyPrice()), UiPalette.MONEY, x + panelWidth);
        if(targetPrices) {
          UiPalette.draw(graphics, column, row, String.format(" %5s",
              tradeValue(item.targetPct(), Strings.CargoTargetPctUnknown)), pctColor(item.targetPct()), x + panelWidth);
        }
        row++;
      }
    }
  }

  /**
   * Whether the panel must show the selected system instead of the current one.
   */
  private boolean targetSelected() {
    return target != null && target.navigationVisible() && !target.name().isEmpty()
        && (system == null || !target.name().equals(system.name()));
  }

  private int drawSystemBlock(TextGUIGraphics graphics, int x, int row, String name, String size, String tech,
      String polSys, String resource, String police, String pirates) {
    UiPalette.draw(graphics, x, row++, Functions.StringVars(Strings.MainSystem, name, size),
        UiPalette.ACCENT, x + panelWidth);
    UiPalette.line(graphics, x, row++, Functions.StringVars(Strings.MainTech, tech, polSys), panelWidth);
    UiPalette.line(graphics, x, row++, Functions.StringVars(Strings.MainResource, resource), panelWidth);
    UiPalette.line(graphics, x, row++, Functions.StringVars(Strings.MainPoliceActivity, police, pirates), panelWidth);
    return row;
  }

  private void drawFooter(TextGUIGraphics graphics, int width, int height, List<String> lines) {
    UiPalette.reset(graphics);
    // The separator (with the last message) sits right above the menu.
    int separator = height - 1 - Math.max(2, lines.size());
    graphics.drawLine(0, separator, width - 1, separator, '─');
    if(!log.isEmpty()) {
      graphics.putString(2, separator, cut(" " + log.get(log.size() - 1) + " ", width - 2));
    }
    int row = height - lines.size();
    for(String line : lines) {
      UiPalette.keys(graphics, 1, row++, line, width - 2);
    }
  }

  private List<String> footerKeys(int width) {
    List<String> lines = new ArrayList<>();
    if(!directKeys.isEmpty()) {
      wrapTokens(lines, directKeys, width);
    }
    return lines;
  }

  private static String keysFor(MainPanel panel) {
    switch(panel) {
      case Trade:
        return Strings.TradeKeys;
      case Bank:
        return Strings.BankKeys;
      case Quests:
        return Strings.QuestsKeys;
      case Personnel:
        return Strings.PersonnelKeys;
      case Commander:
        return Strings.CommanderKeys;
      case Ship:
        return Strings.ShipKeys;
      case ShipList:
        return Strings.ShipListKeys;
      case Equipment:
        return Strings.EquipmentKeys;
      case Options:
        return Strings.OptionsKeys;
      case HighScores:
        return Strings.HighScoresKeys;
      case Designer:
        return Strings.DesignerKeys;
      case News:
        return Strings.NewsKeys;
      case About:
        return Strings.AboutKeys;
      default:
        return null;
    }
  }

  /**
   * Wraps a line of key tokens (separated by {@code  ·  }) without splitting a token,
   * so a key like {@code [H] repairs} never ends up in two lines.
   */
  static void wrapTokens(List<String> lines, String text, int width) {
    StringBuilder line = new StringBuilder();
    for(String token : text.split(" · ")) {
      if(line.length() > 0 && line.length() + 3 + token.length() > width) {
        lines.add(line.toString());
        line.setLength(0);
      }
      if(line.length() > 0) {
        line.append(" · ");
      }
      line.append(token);
    }
    if(line.length() > 0) {
      lines.add(line.toString());
    }
  }

  static void wrap(List<String> lines, String text, int width) {
    if(text == null) {
      return;
    }
    for(String paragraph : text.split("\n", -1)) {
      String rest = paragraph;
      while(rest.length() > width) {
        int cutAt = rest.lastIndexOf(' ', width);
        if(cutAt <= 0) {
          cutAt = width;
        }
        lines.add(rest.substring(0, cutAt));
        rest = rest.substring(cutAt).trim();
      }
      lines.add(rest);
    }
  }

  /** Draws a text wrapped to the panel width and returns the next free row. */
  private int drawWrapped(TextGUIGraphics graphics, int x, int row, String text, TextColor color) {
    List<String> lines = new ArrayList<>();
    wrap(lines, text, panelWidth);
    for(String line : lines) {
      UiPalette.draw(graphics, x, row++, line, color, x + panelWidth);
    }
    return row;
  }

  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }
}
