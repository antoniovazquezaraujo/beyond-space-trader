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
import org.gts.bst.view.CargoRowViewModel;
import org.gts.bst.view.CargoViewModel;
import org.gts.bst.view.ChartSystem;
import org.gts.bst.view.ChartType;
import org.gts.bst.view.ChartViewModel;
import org.gts.bst.view.DockViewModel;
import org.gts.bst.view.LanternaChartView;
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

  private static final int PANEL_WIDTH = 32;

  private final Supplier<Game> gameSupplier;
  private final KeyHandler keyHandler;
  private final List<String> log = new ArrayList<>();
  private ChartType chartType = ChartType.GALACTIC;
  private SystemInfoViewModel system;
  private CargoViewModel cargo;
  private DockViewModel dock;
  private TargetSystemViewModel target;

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

  public void toggleChart() {
    chartType = chartType == ChartType.GALACTIC ? ChartType.SHORT_RANGE : ChartType.GALACTIC;
  }

  public ChartType chartType() {
    return chartType;
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
    Game game = gameSupplier.get();
    Commander cmdr = game == null ? null : game.Commander();
    drawHeader(graphics, width, cmdr);
    int chartWidth = width - PANEL_WIDTH - 2;
    int chartHeight = height - 7;
    drawChart(graphics, chartWidth, chartHeight, game, cmdr);
    drawPanel(graphics, width - PANEL_WIDTH, chartWidth, height);
    drawFooter(graphics, width, height);
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

  private void drawPanel(TextGUIGraphics graphics, int x, int chartWidth, int height) {
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.drawLine(chartWidth + 1, 3, chartWidth + 1, height - 3, '│');
    int row = 3;
    if(system != null && !system.name().isEmpty()) {
      graphics.putString(x, row++, Functions.StringVars(Strings.MainSystem, system.name(), system.size()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainTech, system.tech(), system.polSys()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainResource, system.resource()));
      graphics.putString(x, row++, Functions.StringVars(Strings.MainPolice, system.police(), system.pirates()));
      row++;
    }
    if(dock != null) {
      graphics.putString(x, row++, cut(dock.fuelStatus() + (dock.fuelButtonVisible() ? " · " + dock.fuelCost() : ""), PANEL_WIDTH));
      graphics.putString(x, row++, cut(dock.hullStatus() + (dock.repairButtonVisible() ? " · " + dock.repairCost() : ""), PANEL_WIDTH));
      row++;
    }
    if(target != null && target.navigationVisible() && !target.name().isEmpty()) {
      graphics.putString(x, row++, cut(Functions.StringVars(Strings.MainTarget, target.name(), target.distance()), PANEL_WIDTH));
      row++;
    }
    if(cargo != null) {
      for(int i = 0; i < cargo.rows().size() && row < height - 3; i++) {
        CargoRowViewModel item = cargo.rows().get(i);
        String name = Consts.TradeItems.get(i).Name();
        graphics.putString(x, row++, cut(name + "  " + item.sellPrice() + " / " + item.buyPrice(), PANEL_WIDTH));
      }
    }
  }

  private void drawFooter(TextGUIGraphics graphics, int width, int height) {
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.drawLine(0, height - 3, width - 1, height - 3, '─');
    int row = height - 2;
    for(String message : log) {
      graphics.putString(1, row++, cut(message, width - 2));
    }
    graphics.putString(1, height - 1, cut(Strings.MainKeys, width - 2));
  }

  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }
}
