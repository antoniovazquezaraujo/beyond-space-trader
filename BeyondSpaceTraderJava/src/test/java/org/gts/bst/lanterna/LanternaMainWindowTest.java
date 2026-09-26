/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.GameWindow;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.StarSystem;
import spacetrader.Strings;


class LanternaMainWindowTest {
  @Test
  void rendersTheGameStateAndSwitchesCharts() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());

      gui.updateScreen();

      String header = row(screen, 0);
      assertTrue(header.contains("Antonio"), header);
      assertTrue(header.contains(Strings.MainDay.substring(0, 3)), header);
      assertTrue(row(screen, 3).contains(Strings.MainChartGalactic), row(screen, 3));
      assertTrue(areaContains(screen, 1, 4, 60, 26, "·•◦✧✦✶"), "no chart markers");
      assertTrue(screenText(screen).contains("[TAB] map · [C] trade · [B] bank"), screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(5, 10).getBackgroundColor(),
          "the window background must be black");
      assertTrue(screenText(screen).contains(holder[0].Commander().CurrentSystem().Name()),
          "current system not in the panel");
      String cargoHeader = String.format("%-10s %10s %10s", Strings.TradeItem, Strings.TradeSell, Strings.TradeBuy);
      assertTrue(screenText(screen).contains(cargoHeader), screenText(screen));
      int sell = holder[0].PriceCargoSell()[0];
      int buy = holder[0].PriceCargoBuy()[0];
      String cargoRow = String.format("%-10s %10s %10s", Consts.TradeItems.get(0).Name(),
          sell > 0 ? Functions.FormatMoney(sell) : Strings.NoTrade,
          buy > 0 ? Functions.FormatMoney(buy) : Strings.NotSold);
      assertTrue(screenText(screen).contains(cargoRow), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Tab));
      gui.updateScreen();

      assertTrue(row(screen, 3).contains(Strings.MainChartShortRange), row(screen, 3));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheTradePanelAndMovesTheSelection() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();
      String trade = screenText(screen);
      assertTrue(trade.contains(Strings.TradeTitle), trade);
      assertTrue(trade.contains("> Water"), trade);
      assertTrue(trade.contains("[B] buy"), trade);
      assertTrue(trade.contains("[Shift]"), trade);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Furs"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains("> Water"), screenText(screen));
      assertTrue(row(screen, 3).contains(Strings.MainChartGalactic), row(screen, 3));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheBankAndQuestsPanels() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('b', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.BankTitle), screenText(screen));
      assertTrue(screenText(screen).contains("[G] get loan"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains("[G] get loan"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.QuestsKeys));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheInformationPanels() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('i', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.CommanderTitle), screenText(screen));
      assertTrue(screenText(screen).contains("Antonio"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('v', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.ShipTitle), screenText(screen));
      assertTrue(screenText(screen).contains("| o o >"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.PersonnelTitle), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.PersonnelCrew), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.PersonnelForHire), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.PersonnelTitle));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheShipListAndEquipmentPanels() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('l', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.ShipListTitle), screenText(screen));
      assertTrue(screenText(screen).contains("> Flea"), screenText(screen));
      assertTrue(screenText(screen).contains("| o o >"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Gnat"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('e', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.EquipmentTitle), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.EquipmentBuySection), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.EquipmentSellSection), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.EquipmentKeys));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void logsWhenTheGameActionsAreNotAvailable() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F5));
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Strings.MainSaveUnavailable), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheOptionsAndHighScoresPanels() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F8));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.OptionsTitle), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.OptionAutoSave), screenText(screen));
      String off = Functions.StringVars(Strings.OptionsValue, Strings.OptionAutoFuel, Strings.OptionsOff);
      assertTrue(screenText(screen).contains(off), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      String on = Functions.StringVars(Strings.OptionsValue, Strings.OptionAutoFuel, Strings.OptionsOn);
      assertTrue(screenText(screen).contains(on), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F3));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.HighScoresTitle), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.HighScoresTitle));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheDesignerAtAShipyard() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      StarSystem shipyard = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system.Shipyard() != null) {
          shipyard = system;
          break;
        }
      }
      assertNotNull(shipyard, "the universe must have shipyards");
      holder[0].Commander().CurrentSystem(shipyard);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('d', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("Size:"), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.DesignerConstruct), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.DesignerConstruct));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void logsWhenThereIsNoShipyardHere() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('d', false, false));
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Strings.MainDesignUnavailable), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheNewspaperPanel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.NewsTitle), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.NewsKeys), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.NewsTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.NewsTitle));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheMenuAndActivatesAnEntry() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      gui.updateScreen();
      String menu = screenText(screen);
      assertTrue(menu.contains(Strings.MenuTitle), menu);
      assertTrue(menu.contains(Strings.MenuScores), menu);
      assertTrue(menu.contains(Strings.MenuQuit), menu);
      assertFalse(menu.contains("Commander"), menu);

      // The cursor starts on "High scores"; moving down goes to "Options"
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.OptionsTitle), screenText(screen));
      assertFalse(screenText(screen).contains(Strings.MenuQuit), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void paintsTheScreenWithTheAnsiPalette() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      assertEquals(TextColor.ANSI.CYAN, foregroundAt(screen, 1, 3), "the chart title is cyan");
      int[] key = find(screen, "[C]");
      assertNotNull(key, screenText(screen));
      assertEquals(TextColor.ANSI.YELLOW, foregroundAt(screen, key[0], key[1]), "the keys are yellow");

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('l', false, false));
      gui.updateScreen();
      int[] title = find(screen, Strings.ShipListTitle);
      assertNotNull(title, screenText(screen));
      assertEquals(TextColor.ANSI.CYAN, foregroundAt(screen, title[0], title[1]), "the panel title is cyan");
      int[] selected = find(screen, "> Flea");
      assertNotNull(selected, screenText(screen));
      assertEquals(TextColor.ANSI.CYAN, screen.getBackCharacter(selected[0], selected[1]).getBackgroundColor(),
          "the selected row is highlighted");

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      gui.updateScreen();
      int[] item = find(screen, Strings.MenuScores);
      assertNotNull(item, screenText(screen));
      assertEquals(TextColor.ANSI.CYAN, screen.getBackCharacter(item[0], item[1]).getBackgroundColor(),
          "the selected menu entry is highlighted");
      assertEquals(TextColor.ANSI.CYAN, foregroundAt(screen, 4, 3), "the menu frame is cyan");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void panelsKeepTheMapVisibleAndUseTheirOwnWidth() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();
      assertTrue(row(screen, 3).contains(Strings.MainChartGalactic), row(screen, 3));
      assertTrue(row(screen, 3).contains(Strings.TradeTitle), row(screen, 3));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertTrue(row(screen, 3).contains(Strings.MainChartGalactic), row(screen, 3));
      assertFalse(row(screen, 3).contains(Strings.TradeTitle), row(screen, 3));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static TextColor foregroundAt(Screen screen, int x, int y) {
    return screen.getBackCharacter(x, y).getForegroundColor();
  }

  private static int[] find(Screen screen, String needle) {
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      int x = row(screen, y).indexOf(needle);
      if(x >= 0) {
        return new int[] {x, y};
      }
    }
    return null;
  }

  @Test
  void warpsToTheSelectedSystemAndSpendsFuel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem current = holder[0].Commander().CurrentSystem();
      StarSystem target = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != current && system.DestOk()) {
          target = system;
          break;
        }
      }
      assertNotNull(target, "the galaxy must have a reachable system");
      holder[0].SelectedSystemId(target.Id());
      presenter.updateAll();
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[W] warp"), screenText(screen));

      int fuel = holder[0].Commander().getShip().getFuel();
      boolean wormhole = Functions.WormholeExists(current, target);
      int distance = Functions.Distance(current, target);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('w', false, false));
      gui.updateScreen();

      assertSame(target, holder[0].Commander().CurrentSystem(), screenText(screen));
      assertEquals(wormhole ? fuel : fuel - distance, holder[0].Commander().getShip().getFuel(),
          "the fuel of the distance must be spent");
      assertEquals(1, holder[0].Commander().getDays(), "the trip must advance a day");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void doesNotWarpOutOfRangeOrToTheCurrentSystem() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem current = holder[0].Commander().CurrentSystem();
      StarSystem far = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != current && !system.DestOk()) {
          far = system;
          break;
        }
      }
      assertNotNull(far, "the galaxy must have a system out of range");
      holder[0].SelectedSystemId(far.Id());
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('w', false, false));
      gui.updateScreen();
      assertSame(current, holder[0].Commander().CurrentSystem());
      assertTrue(screenText(screen).contains(Strings.MainWarpOutOfRange), screenText(screen));

      holder[0].SelectedSystemId(current.Id());
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('w', false, false));
      gui.updateScreen();
      assertSame(current, holder[0].Commander().CurrentSystem());
      assertTrue(screenText(screen).contains(Strings.MainWarpCurrent), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void doesNotJumpWithoutThePortableSingularity() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem current = holder[0].Commander().CurrentSystem();
      StarSystem far = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != current && !system.DestOk()) {
          far = system;
          break;
        }
      }
      assertNotNull(far, "the galaxy must have a system out of range");
      holder[0].SelectedSystemId(far.Id());

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('j', false, false));
      gui.updateScreen();
      assertSame(current, holder[0].Commander().CurrentSystem());
      assertTrue(screenText(screen).contains(Strings.MainJumpNoSingularity), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void jumpsWithThePortableSingularityWithoutSpendingFuel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(),
          (type, messageArgs) -> DialogResult.Yes);
      holder[0].setAutoSave(false);
      holder[0].setCanSuperWarp(true);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();
      assertTrue(screenText(screen).contains("[J] Jump"), screenText(screen));

      StarSystem current = holder[0].Commander().CurrentSystem();
      StarSystem far = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != current && !system.DestOk()) {
          far = system;
          break;
        }
      }
      assertNotNull(far, "the galaxy must have a system out of range");
      holder[0].SelectedSystemId(far.Id());

      int fuel = holder[0].Commander().getShip().getFuel();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('j', false, false));
      gui.updateScreen();

      assertSame(far, holder[0].Commander().CurrentSystem(), screenText(screen));
      assertEquals(fuel, holder[0].Commander().getShip().getFuel(), "the singularity spends no fuel");
      assertFalse(holder[0].getCanSuperWarp(), "the singularity is used only once");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheTargetPriceOfTheSelectedItemInTheTradePanel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem target = reachableSystemTradingWater(holder[0]);
      assertNotNull(target, "the galaxy must have a reachable system trading Water");
      holder[0].SelectedSystemId(target.Id());
      presenter.updateAll();
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();

      int price = Consts.TradeItems.get(0).StandardPrice(target);
      int buy = holder[0].PriceCargoBuy()[0];
      int diff = price - buy;
      String pct = buy > 0 ? (diff > 0 ? "+" : "") + Functions.FormatNumber(100 * diff / buy) + "%"
          : Strings.CargoTargetPctUnknown;
      String line = Functions.StringVars(Strings.TradeTargetLine, new String[]{
          Consts.TradeItems.get(0).Name(), target.Name(), Functions.FormatMoney(price), pct});
      assertTrue(screenText(screen).contains(line), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheTargetColumnsWhenThereIsRoom() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(130, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem target = reachableSystemTradingWater(holder[0]);
      assertNotNull(target, "the galaxy must have a reachable system trading Water");
      holder[0].SelectedSystemId(target.Id());
      presenter.updateAll();
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.TradeTarget), text);
      assertTrue(text.contains(Strings.TradeDiff), text);
      assertTrue(text.contains(Strings.TradePct), text);
      assertTrue(text.contains(Functions.FormatMoney(Consts.TradeItems.get(0).StandardPrice(target))), text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static StarSystem reachableSystemTradingWater(Game game) {
    StarSystem current = game.Commander().CurrentSystem();
    for(StarSystem system : game.Universe()) {
      if(system != current && system.DestOk() && Consts.TradeItems.get(0).StandardPrice(system) > 0) {
        return system;
      }
    }
    return null;
  }

  @Test
  void theNavigationPanelShowsTheTargetDetailsWhenAnotherSystemIsSelected() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      StarSystem current = holder[0].Commander().CurrentSystem();
      String dockLine = Functions.StringVars(Strings.DockHullStatus, "100");
      assertTrue(screenText(screen).contains(current.Name()), screenText(screen));
      assertTrue(screenText(screen).contains(dockLine), screenText(screen));

      // Selecting another system shows its data instead of the current one.
      StarSystem target = current;
      int best = -1;
      for(StarSystem system : holder[0].Universe()) {
        int distance = Functions.Distance(current, system);
        if(distance > best) {
          best = distance;
          target = system;
        }
      }
      holder[0].SelectedSystemId(target.Id());
      presenter.updateAll();
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Functions.StringVars(Strings.MainTech, target.TechLevel().name,
          target.PoliticalSystem().Name())), text);
      assertTrue(text.contains(Strings.MainTargetOffRange), text);
      assertFalse(text.contains(dockLine), "the dock data belongs to the current system");
      assertFalse(text.contains(current.Name()), text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theGalacticChartAlwaysShowsTheWholeGalaxy() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      int[] before = findInverted(screen);
      assertNotNull(before, "the current system must be on the galactic chart:\n" + screenText(screen));

      // The whole galaxy fits, so selecting the farthest system does not scroll it.
      holder[0].SelectedSystemId(farthestSystem(holder[0]).Id());
      presenter.updateAll();
      gui.updateScreen();

      int[] after = findInverted(screen);
      assertNotNull(after, "the whole galaxy must fit:\n" + screenText(screen));
      assertEquals(before[0], after[0]);
      assertEquals(before[1], after[1]);
      assertTrue(chartHasCrosshair(screen), "the selected system must be visible:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theLocalChartFollowsTheCursor() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Tab));
      gui.updateScreen();
      assertTrue(row(screen, 3).contains(Strings.MainChartShortRange), row(screen, 3));
      int[] before = findInverted(screen);
      assertNotNull(before, "the current system must be on the short-range chart:\n" + screenText(screen));

      // Selecting a system outside the view makes the chart follow it.
      holder[0].SelectedSystemId(farthestSystem(holder[0]).Id());
      presenter.updateAll();
      gui.updateScreen();

      assertTrue(chartHasCrosshair(screen), "the chart must follow the selection:\n" + screenText(screen));
      int[] after = findInverted(screen);
      assertTrue(after == null || after[0] != before[0] || after[1] != before[1],
          "the chart must have scrolled:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void tracksTheCurrentSystemWhenNothingIsSelectedYet() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('t', false, false));
      gui.updateScreen();

      StarSystem current = holder[0].Commander().CurrentSystem();
      assertEquals(current.Id().CastToInt(), holder[0].getTrackedSystemId().CastToInt());
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.MainTracking, current.Name())),
          screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static StarSystem farthestSystem(Game game) {
    StarSystem current = game.Commander().CurrentSystem();
    StarSystem far = current;
    int bestDistance = -1;
    for(StarSystem system : game.Universe()) {
      int distance = Functions.Distance(current, system);
      if(distance > bestDistance) {
        bestDistance = distance;
        far = system;
      }
    }
    return far;
  }

  /** The selected system is marked with a cross: its horizontal arm is a dash. */
  private static boolean chartHasCrosshair(Screen screen) {
    for(int y = 4; y < 27; y++) {
      for(int x = 1; x < 64; x++) {
        if(screen.getBackCharacter(x, y).getCharacter() == '─') {
          return true;
        }
      }
    }
    return false;
  }

  /** The current system is the only cell with a background other than black. */
  private static int[] findInverted(Screen screen) {
    for(int y = 4; y < 27; y++) {
      for(int x = 1; x < 64; x++) {
        if(screen.getBackCharacter(x, y).getBackgroundColor() != TextColor.ANSI.BLACK) {
          return new int[] {x, y};
        }
      }
    }
    return null;
  }

  /**
   * A game host that resolves every encounter immediately, so travel can be tested
   * without blocking on the encounter window.
   */
  private static final class QuietHost implements GameWindow {
    @Override
    public EncounterResult showEncounter() {
      return EncounterResult.Normal;
    }

    @Override
    public void showNewspaper() {
    }

    @Override
    public void UpdateStatusBar() {
    }

    @Override
    public void UpdateAll() {
    }
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      text.append(row(screen, y)).append('\n');
    }
    return text.toString();
  }

  private static String row(Screen screen, int y) {
    StringBuilder line = new StringBuilder();
    for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
      line.append(screen.getBackCharacter(x, y).getCharacter());
    }
    return line.toString();
  }

  private static boolean areaContains(Screen screen, int x1, int y1, int x2, int y2, String characters) {
    for(int y = y1; y <= y2; y++) {
      for(int x = x1; x <= x2; x++) {
        if(characters.indexOf(screen.getBackCharacter(x, y).getCharacter()) >= 0) {
          return true;
        }
      }
    }
    return false;
  }
}
