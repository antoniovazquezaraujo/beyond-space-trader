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
import org.gts.bst.view.ChartType;
import org.gts.bst.view.ChartViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.LanternaChartView;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelViewModel;
import org.gts.bst.view.QuestsViewModel;
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListViewModel;
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
  private int menuIndex;
  private int panelWidth = NAVIGATION_PANEL_WIDTH;

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
      graphics.putString(0, 0, "Window too small");
      return;
    }
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    String blank = " ".repeat(width);
    for(int row = 0; row < height; row++) {
      graphics.putString(0, row, blank);
    }
    Game game = gameSupplier.get();
    Commander cmdr = game == null ? null : game.Commander();
    drawHeader(graphics, width, cmdr);
    panelWidth = Math.max(20, Math.min(width - 24, panelWidthFor(panel)));
    int chartWidth = width - panelWidth - 2;
    int chartHeight = height - 7;
    drawChart(graphics, chartWidth, chartHeight, game, cmdr);
    drawPanel(graphics, width - panelWidth, chartWidth, height);
    drawFooter(graphics, width, height);
    drawMenu(graphics, width, height);
  }

  private void drawHeader(TextGUIGraphics graphics, int width, Commander cmdr) {
    if(cmdr == null) {
      graphics.putString(1, 0, Strings.MainNoGame);
      return;
    }
    Ship ship = cmdr.getShip();
    String line1 = cmdr.Name() + " · " + Functions.StringVars(Strings.MainDay, "" + cmdr.getDays())
        + " · " + Functions.FormatMoney(cmdr.getCash())
        + " · " + Functions.StringVars(Strings.MainDebt, Functions.FormatMoney(cmdr.getDebt()));
    String line2 = Functions.StringVars(Strings.MainFuel, "" + ship.getFuel(), "" + ship.FuelTanks())
        + " · " + Functions.StringVars(Strings.MainHull, "" + ship.getHull(), "" + ship.HullStrength())
        + " · " + Functions.StringVars(Strings.MainShields, "" + ship.ShieldCharge(), "" + ship.ShieldStrength())
        + " · " + Functions.StringVars(Strings.MainCargo, "" + ship.FilledCargoBays(), "" + ship.CargoBays())
        + " · " + Functions.StringVars(Strings.MainPolice, PoliceRecord.GetPoliceRecordFromScore(cmdr.getPoliceRecordScore()).Name());
    graphics.putString(1, 0, cut(line1, width - 2));
    graphics.putString(1, 1, cut(line2, width - 2));
    graphics.drawLine(0, 2, width - 1, 2, '─');
  }

  private void drawChart(TextGUIGraphics graphics, int chartWidth, int chartHeight, Game game, Commander cmdr) {
    String title = chartType == ChartType.GALACTIC ? Strings.MainChartGalactic : Strings.MainChartShortRange;
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(1, 3, title);
    if(game == null || cmdr == null) {
      return;
    }
    TerminalSize size = new TerminalSize(chartWidth, chartHeight);
    LanternaChartView chart = new LanternaChartView(
        graphics.newTextGraphics(new TerminalPosition(1, 4), size), size);
    chart.render(chartModel(game, cmdr));
  }

  private ChartViewModel chartModel(Game game, Commander cmdr) {
    StarSystem[] universe = game.Universe();
    StarSystem current = cmdr.CurrentSystem();
    StarSystem chosen = game.SelectedSystem() == null ? current : game.SelectedSystem();
    int selected = chosen.Id().CastToInt();
    int tracked = game.getTrackedSystemId().CastToInt();
    int warp = game.WarpSystem() == null ? -1 : game.WarpSystem().Id().CastToInt();
    List<ChartSystem> systems = new ArrayList<>(universe.length);
    for(int i = 0; i < universe.length; i++) {
      StarSystem system = universe[i];
      systems.add(new ChartSystem(system.X(), system.Y(), system.Name(), system.Visited(),
          Util.BruteSeek(game.Wormholes(), i) >= 0, i == warp, i == tracked, i == selected));
    }
    int fuel = cmdr.getShip().getFuel();
    if(chartType == ChartType.GALACTIC) {
      return ChartViewModel.galactic(systems, current.X(), current.Y(), chosen.X(), chosen.Y(),
          fuel, UniverseGenerator.GalaxyWidth, UniverseGenerator.GalaxyHeight);
    }
    String trackedText = null;
    if(game.TrackedSystem() != null && game.Options().getShowTrackedRange()) {
      trackedText = Functions.StringVars(Strings.MainTarget, game.TrackedSystem().Name(),
          "" + Functions.Distance(current, game.TrackedSystem()));
    }
    return ChartViewModel.shortRange(systems, current.X(), current.Y(), fuel, Consts.MaxRange, trackedText);
  }

  private static int panelWidthFor(MainPanel panel) {
    switch(panel) {
      case Trade:
        return 48;
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
        return 60;
      default:
        return NAVIGATION_PANEL_WIDTH;
    }
  }

  private void drawPanel(TextGUIGraphics graphics, int x, int chartWidth, int height) {
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.drawLine(chartWidth + 1, 3, chartWidth + 1, height - 4, '│');
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
      wrap(lines, keys, panelWidth);
    }
    int row = height - 4 - Math.max(0, lines.size() - 1);
    for(String line : lines) {
      graphics.putString(x, row++, cut(line, panelWidth));
    }
  }

  private void drawBankPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.BankTitle);
    int row = 4;
    if(bank == null) {
      return;
    }
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.BankDebt, bank.currentDebt()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.BankMaxLoan, bank.maxLoan()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.BankShipValue, bank.shipValue()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.BankNoClaim, bank.noClaim()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.BankInsurance, bank.insuranceCost()), panelWidth));
    graphics.putString(x, row + 1, cut(bank.insuranceButtonText(), panelWidth));
  }

  private void drawPersonnelPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.PersonnelTitle);
    if(personnel == null) {
      return;
    }
    int row = 4;
    graphics.putString(x, row++, Strings.PersonnelCrew);
    int index = 0;
    for(String entry : personnel.crewEntries()) {
      if(row >= height - 5) {
        return;
      }
      graphics.putString(x, row++, cut((index == personnelIndex ? ">" : " ") + " " + entry, panelWidth));
      index++;
    }
    row++;
    if(row < height - 5) {
      graphics.putString(x, row++, Strings.PersonnelForHire);
    }
    for(String entry : personnel.forHireEntries()) {
      if(row >= height - 5) {
        return;
      }
      graphics.putString(x, row++, cut((index == personnelIndex ? ">" : " ") + " " + entry, panelWidth));
      index++;
    }
    if(personnelInfo != null && personnelInfo.visible() && row < height - 5) {
      row++;
      if(row < height - 5) {
        graphics.putString(x, row++, cut(personnelInfo.name() + (personnelInfo.rateVisible() ? "  " + personnelInfo.rate() : ""), panelWidth));
      }
      if(row < height - 5) {
        graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderSkills, new String[]{
            personnelInfo.pilot(), personnelInfo.fighter(), personnelInfo.trader(), personnelInfo.engineer()}), panelWidth));
      }
      if(row < height - 5) {
        graphics.putString(x, row, cut(personnelInfo.hireFireText(), panelWidth));
      }
    }
  }

  private void drawCommanderPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.CommanderTitle);
    if(commander == null) {
      return;
    }
    int row = 4;
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderHeader, commander.name(), commander.difficulty()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderTime, commander.time()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderSkills, new String[]{
        commander.pilot(), commander.fighter(), commander.trader(), commander.engineer()}), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderCash, new String[]{
        commander.cash(), commander.debt(), commander.netWorth()}), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderKills, commander.kills()), panelWidth));
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.CommanderRecord, commander.record(),
        commander.reputation()), panelWidth));
    if(commander.bounty().visible()) {
      graphics.putString(x, row, cut(Functions.StringVars(Strings.CommanderBounty, commander.bounty().label(),
          commander.bounty().amount()), panelWidth));
    }
  }

  private void drawShipPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.ShipTitle);
    if(ship == null) {
      return;
    }
    int row = 4;
    graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipType, ship.type()), panelWidth));
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
        graphics.putString(x, row++, line);
      }
    }
  }

  private void drawShipListPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.ShipListTitle);
    if(shipList == null) {
      return;
    }
    int row = 4;
    for(int i = 0; i < shipList.rows().size() && row < height - 5; i++) {
      ShipListViewModel.Row item = shipList.rows().get(i);
      graphics.putString(x, row++, cut(String.format("%s %-14s %14s",
          i == shipListIndex ? ">" : " ", item.name(), item.price()), panelWidth));
    }
    if(shipInfo == null || row >= height - 5) {
      return;
    }
    row++;
    if(row < height - 5) {
      graphics.putString(x, row++, cut(shipInfo.name() + "  " + shipInfo.size(), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipInfoBays, shipInfo.bays())
          + " · " + Functions.StringVars(Strings.ShipInfoRange, shipInfo.range()), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipInfoHull, shipInfo.hull())
          + " · " + Functions.StringVars(Strings.ShipInfoCrew, shipInfo.crew()), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.ShipInfoWeapon, shipInfo.weapon())
          + " · " + Functions.StringVars(Strings.ShipInfoShield, shipInfo.shield())
          + " · " + Functions.StringVars(Strings.ShipInfoGadget, shipInfo.gadget()), panelWidth));
    }
  }

  private void drawEquipmentPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.EquipmentTitle);
    if(equipment == null) {
      return;
    }
    int row = 4;
    row = drawEquipmentSection(graphics, x, row, height, Strings.EquipmentBuySection, 0,
        equipment.buyWeapons().size() + equipment.buyShields().size() + equipment.buyGadgets().size());
    if(row < height - 5) {
      graphics.putString(x, row++, cut(Strings.EquipmentSellSection, panelWidth));
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
        graphics.putString(x, row++, cut(equipmentInfo.name() + "  " + Functions.StringVars(Strings.EquipmentTypeLabel, equipmentInfo.type()), panelWidth));
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
          graphics.putString(x, row++, cut(prices, panelWidth));
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
    graphics.putString(x, row++, cut(title, panelWidth));
    List<String> buy = new ArrayList<>();
    buy.addAll(equipment.buyWeapons());
    buy.addAll(equipment.buyShields());
    buy.addAll(equipment.buyGadgets());
    return drawEquipmentEntries(graphics, x, row, height, buy, first);
  }

  private int drawEquipmentEntries(TextGUIGraphics graphics, int x, int row, int height, List<String> entries, int first) {
    for(int i = 0; i < entries.size() && row < height - 5; i++) {
      graphics.putString(x, row++, cut((first + i == equipmentIndex ? "> " : "  ") + entries.get(i), panelWidth));
    }
    return row;
  }

  private void drawOptionsPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.OptionsTitle);
    int row = 4;
    for(int i = 0; i < optionsLines.size() && row < height - 5; i++) {
      graphics.putString(x, row++, cut((i == optionsIndex ? "> " : "  ") + optionsLines.get(i), panelWidth));
    }
  }

  private void drawHighScoresPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.HighScoresTitle);
    if(highScores == null) {
      return;
    }
    int row = 5;
    for(HighScoresViewModel.Row score : highScores.rows()) {
      if(!score.filled() || row >= height - 5) {
        continue;
      }
      graphics.putString(x, row++, cut(score.name() + "   " + score.score(), panelWidth));
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
    int row = 3;
    graphics.putString(x, row++, cut(designer.title(), panelWidth));
    List<String> welcome = new ArrayList<>();
    wrap(welcome, designer.welcome(), panelWidth);
    for(String line : welcome) {
      if(row < height - 5) {
        graphics.putString(x, row++, line);
      }
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(marker(0) + Functions.StringVars(Strings.DesignerSize,
          designer.sizes().isEmpty() ? "" : designer.sizes().get(designer.sizeIndex())), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(marker(1) + Functions.StringVars(Strings.DesignerTemplate,
          designer.templates().isEmpty() ? "" : designer.templates().get(designer.templateIndex())), panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row++, cut(marker(2) + Functions.StringVars(Strings.DesignerName, designer.name()), panelWidth));
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
      graphics.putString(x, row++, cut(marker(3 + i) + text, panelWidth));
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
      graphics.putString(x, row++, cut(marker(10) + Strings.DesignerConstruct, panelWidth));
    }
    if(row < height - 5) {
      graphics.putString(x, row, cut(marker(11) + Strings.DesignerSave, panelWidth));
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
    int boxWidth = Strings.MenuTitle.length();
    for(String item : menuItems) {
      boxWidth = Math.max(boxWidth, item.length() + 2);
    }
    boxWidth += 4;
    boxWidth = Math.min(boxWidth, width - 4);
    int boxHeight = menuItems.size() + 2;
    int left = 2;
    int top = 3;
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    String blank = " ".repeat(boxWidth);
    for(int y = top; y < top + boxHeight && y < height; y++) {
      graphics.putString(left, y, blank);
    }
    String title = " " + Strings.MenuTitle + " ";
    graphics.putString(left, top, "┌─" + title + "─".repeat(Math.max(0, boxWidth - 3 - title.length())) + "┐");
    for(int i = 0; i < menuItems.size(); i++) {
      String marker = i == menuIndex ? ">" : " ";
      String text = marker + " " + menuItems.get(i);
      graphics.putString(left, top + 1 + i, "│ " + text + " ".repeat(Math.max(0, boxWidth - 4 - text.length())) + " │");
    }
    graphics.putString(left, top + boxHeight - 1, "└" + "─".repeat(boxWidth - 2) + "┘");
  }

  private void drawNewsPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, cut(Strings.NewsTitle + " · " + newsHead, panelWidth));
    int row = 5;
    int last = Math.min(newsLines.size(), newsScroll + (height - 9));
    for(int i = newsScroll; i < last; i++) {
      graphics.putString(x, row++, cut(newsLines.get(i), panelWidth));
    }
    graphics.putString(x, height - 4, cut(Functions.StringVars(Strings.NewsPosition,
        "" + (newsLines.isEmpty() ? 0 : newsScroll + 1), "" + newsLines.size()), panelWidth));
  }

  private void drawQuestsPanel(TextGUIGraphics graphics, int x, int height) {
    graphics.putString(x, 3, Strings.QuestsTitle);
    int row = 4;
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
    graphics.putString(x, 3, Strings.TradeTitle);
    int row = 4;
    graphics.putString(x, row++, cut(String.format("%s %-11s %10s %10s %5s %5s", " ",
        Strings.TradeItem, Strings.TradeBuy, Strings.TradeSell, Strings.TradeCargo, Strings.TradeHere), panelWidth));
    if(cargo == null) {
      return;
    }
    for(int i = 0; i < cargo.rows().size() && row < height - 5; i++) {
      CargoRowViewModel item = cargo.rows().get(i);
      String name = cut(Consts.TradeItems.get(i).Name(), 11);
      graphics.putString(x, row++, cut(String.format("%s %-11s %10s %10s %5s %5s",
          i == selectedItem ? ">" : " ", name, item.buyPrice(), item.sellPrice(), item.sellQty(), item.buyQty()),
          panelWidth));
    }
  }

  private void drawNavigationPanel(TextGUIGraphics graphics, int x, int height) {
    int row = 3;
    if(system != null && !system.name().isEmpty()) {
      graphics.putString(x, row++, Functions.StringVars(Strings.MainSystem, system.name(), system.size()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainTech, system.tech(), system.polSys()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainResource, system.resource()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainPolice, system.police(), system.pirates()));
      row++;
    }
    if(dock != null) {
      graphics.putString(x, row++, cut(dock.fuelStatus() + (dock.fuelButtonVisible() ? " · " + dock.fuelCost() : ""), panelWidth));
      graphics.putString(x, row++, cut(dock.hullStatus() + (dock.repairButtonVisible() ? " · " + dock.repairCost() : ""), panelWidth));
      row++;
    }
    if(target != null && target.navigationVisible() && !target.name().isEmpty()) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.MainTarget, target.name(), target.distance()), panelWidth));
      row++;
    }
    if(cargo != null) {
      for(int i = 0; i < cargo.rows().size() && row < height - 5; i++) {
        CargoRowViewModel item = cargo.rows().get(i);
        String name = Consts.TradeItems.get(i).Name();
        graphics.putString(x, row++, cut(name + "  " + item.sellPrice() + " / " + item.buyPrice(), panelWidth));
      }
    }
  }

  private void drawFooter(TextGUIGraphics graphics, int width, int height) {
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.drawLine(0, height - 3, width - 1, height - 3, '─');
    if(!log.isEmpty()) {
      graphics.putString(2, height - 3, cut(" " + log.get(log.size() - 1) + " ", width - 2));
    }
    if(panel == MainPanel.Navigation && !directKeys.isEmpty()) {
      List<String> lines = new ArrayList<>();
      wrap(lines, directKeys, width - 2);
      int row = height - 1 - Math.max(0, lines.size() - 1);
      for(String line : lines) {
        graphics.putString(1, row++, cut(line, width - 2));
      }
    }
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
      default:
        return null;
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

  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }
}
