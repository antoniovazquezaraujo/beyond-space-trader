/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.EncounterType;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.Alerts;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.GameWindow;
import org.gts.bst.view.LanternaDialogService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.CrewMember;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.SpecialEvent;
import spacetrader.StarSystem;
import spacetrader.enums.AlertType;
import spacetrader.enums.StarSystemId;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.TradeCalculator;


class LanternaMainWindowTest {
  @Test
  void theTitleScreenAnswersToTheMenuKeys() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      boolean[] newGame = {false};
      window.setGameActions(() -> newGame[0] = true, null, null);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();
      assertTrue(screenText(screen).contains("Beyond"), "the title shows the logo");

      // any key leaves the title and enters the program
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('x', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.MainNoGame), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.MenuNewGame),
          "the empty screen points at the menu: " + screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F2));
      assertTrue(newGame[0], "F2 starts a new game from the program");

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.MenuTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('a', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.AboutTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theTitleLogoKeepsItsShapeWhenTheSplashDoesNotFit() throws IOException {
    TitleSplash splash = TitleSplash.shared();
    // A screen deliberately one cell too small for the splash (but still above the
    // minimum window size): the title falls back to the centred banner.
    int columns = splash == null ? 60 : Math.max(60, splash.width() - 1);
    int rows = splash == null ? 15 : Math.max(15, splash.height() - 1);
    Assumptions.assumeTrue(splash == null || splash.width() > columns || splash.height() > rows,
        "the splash does not fit on this screen");
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(columns, rows)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      String[] lines = Strings.MainBanner.split("\n", -1);
      int block = 0;
      for(String line : lines) {
        block = Math.max(block, EditorText.width(line));
      }
      int left = 1 + (columns - block) / 2;
      int top = -1;
      for(int y = 0; y < rows; y++) {
        if(row(screen, y).contains(lines[0])) {
          top = y;
          break;
        }
      }
      assertTrue(top >= 0, "the logo is on the title screen:\n" + screenText(screen));
      // The block is centred as a whole: every line starts on the same column,
      // so the ASCII art keeps its shape.
      for(int i = 0; i < lines.length; i++) {
        assertEquals(lines[i], row(screen, top + i).substring(left, left + lines[i].length()),
            "the logo keeps its shape, line " + i + ":\n" + screenText(screen));
      }
      assertEquals(lines[1].indexOf('|') + 1, lines[0].indexOf('_'),
          "the B keeps its leading space");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void spaceOnTheTitleScreenDoesNotTryToWarp() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // The first key leaves the title screen; with no game loaded, SPACE must not
      // reach the warp code (there is no commander to warp with).
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Strings.MainNoGame), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theTitleScreenActsOnTheMenuKeysAtOnce() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      boolean[] newGame = {false};
      window.setGameActions(() -> newGame[0] = true, null, null);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // F2 on the title screen starts a game in one press, not the second
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F2));
      assertTrue(newGame[0], "F2 works on the first press");

      // and F10 opens the menu there too
      window.showTitleScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.MenuTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheSplashOnTheTitleScreen() throws IOException {
    TitleSplash splash = TitleSplash.shared();
    assertNotNull(splash, "the splash resource is packaged");
    // A screen with a margin on all four sides. The expectations are derived from
    // the drawing itself, so editing the art or its colours does not move them.
    TerminalSize size = new TerminalSize(splash.width() + 8, splash.height() + 6);
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(size));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      int left = (size.getColumns() - splash.width()) / 2;
      int top = (size.getRows() - splash.height()) / 2;
      int ink = 0;
      for(int y = 0; y < splash.height(); y++) {
        for(int x = 0; x < splash.width(); x++) {
          int codePoint = splash.codePointAt(x, y);
          if(codePoint == ' ' || codePoint == TitleSplash.CONTINUATION) {
            continue;
          }
          ink++;
          assertEquals(splash.colorAt(x, y), foregroundAt(screen, left + x, top + y),
              "the splash colour of the cell " + x + "," + y);
          assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(left + x, top + y).getBackgroundColor(),
              "the sky behind the splash stays black");
        }
      }
      assertTrue(ink > 0, "the splash has ink:\n" + screenText(screen));

      // The block is centred: the logo line lands where the splash puts it.
      int logoRow = rowOf(splash, "Beyond");
      assertTrue(logoRow >= 0, "the splash carries the logo");
      int[] beyond = find(screen, "Beyond");
      assertNotNull(beyond, screenText(screen));
      String logoLine = splash.lines().get(logoRow);
      int logoColumn = EditorText.width(logoLine.substring(0, logoLine.indexOf("Beyond")));
      assertEquals(left + logoColumn, beyond[0], "the splash is centred as a block");
      assertEquals(top + logoRow, beyond[1], "the splash is centred as a block");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theTitleScreenIsStillWhenNothingChanges() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();
      String first = screenText(screen);

      // The old title timer moved the starfield every 110 ms: wait past that
      // and force a repaint; the drawing must be exactly the same.
      Thread.sleep(350);
      window.refresh();
      gui.updateScreen();

      assertEquals(first, screenText(screen), "the title changed without any input");
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("interrupted while waiting", e);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSkyShowsThroughTheSpacesOfTheSplash() throws IOException {
    TitleSplash splash = TitleSplash.shared();
    assertNotNull(splash, "the splash resource is packaged");
    // The screen just fits the splash with a margin, so the block does not cover it.
    TerminalSize size = new TerminalSize(splash.width() + 8, splash.height() + 6);
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(size));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      int left = (size.getColumns() - splash.width()) / 2;
      int top = (size.getRows() - splash.height()) / 2;
      int[] space = null;
      for(int y = 0; y < splash.height() && space == null; y++) {
        for(int x = 0; x < splash.width(); x++) {
          if(splash.codePointAt(x, y) == ' ') {
            space = new int[] {x, y};
            break;
          }
        }
      }
      assertNotNull(space, "the splash has empty cells");
      assertTrue(left > 0 || top > 0, "the splash does not cover the whole screen");

      // A space is transparent: the cell keeps the black sky of the cleared
      // screen, exactly like the cell at the top-left corner, outside the block.
      TextCharacter inside = screen.getBackCharacter(left + space[0], top + space[1]);
      TextCharacter sky = screen.getBackCharacter(0, 0);
      assertEquals(sky, inside, "a space of the splash leaves the sky untouched");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theHeaderFitsInOneLineOnWideScreens() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(140, 30)));
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
      assertTrue(header.contains(holder[0].Commander().Name()), header);
      assertTrue(header.contains(Strings.MainFuel.substring(0, 4)), header);
      assertTrue(header.contains(Strings.MainCargo.substring(0, 5)), header);
      assertTrue(header.contains(Strings.MainPolice.substring(0, 7)), header);
      assertTrue(row(screen, 1).startsWith("─"), row(screen, 1));
      assertTrue(row(screen, 2).contains(Strings.MainChartGalactic), row(screen, 2));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

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
      assertTrue(areaContains(screen, 1, 4, 60, 26, "·•◦✧✦✶◉"), "no chart markers");
      assertTrue(screenText(screen).contains("[TAB] map · [C] trade · [B] bank"), screenText(screen));
      assertTrue(screenText(screen).contains("[T] track"), screenText(screen));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(1, 0).getBackgroundColor(),
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
      assertTrue(trade.contains("[Shift+B/S] all"), trade);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Furs"), screenText(screen));

      // The vim keys move the list too.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('k', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Water"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('j', false, false));
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
  void theTradeNAndPMoveTheSelectionAndTheBKeyStillBuys() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      // The first item has stock, so the max-buy shortcut has something to buy.
      holder[0].Commander().CurrentSystem().TradeItems()[0] = 50;
      holder[0].Commander().setCash(100000);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Water"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Furs"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Water"), screenText(screen));

      // Shift+B (the uppercase letter) buys the maximum without asking.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('B', false, false));
      gui.updateScreen();
      assertTrue(holder[0].Commander().getShip().Cargo()[0] > 0,
          "the B shortcut keeps buying after the n/p navigation");
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
  void theBankPKeyStillPaysBackAndTheNKeyOpensNothing() throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(100000);
      holder[0].Commander().setDebt(5000);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('b', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.BankTitle), screenText(screen));

      // The bank is no list: P still pays the debt back instead of moving a selection.
      Thread worker = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('p', false, false)));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui, window.asWindow());
      // The main component is locked while the worker waits in the dialog, so the
      // test must not repaint: it reads the dialog title and closes it.
      assertEquals(Strings.DialogPayBackTitle, dialog.getTitle(),
          "P must open the pay back dialog");
      dialog.handleInput(new KeyStroke(KeyType.Escape));
      worker.join(5000);
      assertFalse(worker.isAlive(), "the pay back dialog should close with ESCAPE");
      gui.updateScreen();

      // N is not a bank key: it opens no dialog and the panel stays on screen.
      int openWindows = gui.getWindows().size();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertEquals(openWindows, gui.getWindows().size(), "n must not open anything in the bank");
      assertTrue(screenText(screen).contains(Strings.BankTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsPanelSetsTheSelectedTargetOnEnter() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      holder[0].setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      StarSystem utopia = holder[0].Universe()[StarSystemId.Utopia.CastToInt()];
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestMoon), screenText(screen));
      assertTrue(screenText(screen).contains("→ " + utopia.Name()), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();

      assertSame(utopia, holder[0].SelectedSystem(),
          "ENTER points the map at the destination of the selected quest");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsPanelArrowsMoveTheSelection() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      holder[0].setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      holder[0].setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();

      assertSame(holder[0].Universe()[StarSystemId.Utopia.CastToInt()], holder[0].SelectedSystem(),
          "the arrow moved the selection to the second quest");
      assertNotSame(holder[0].Universe()[StarSystemId.Acamar.CastToInt()], holder[0].SelectedSystem());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsNAndPMoveTheSelection() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      holder[0].setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      holder[0].setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(holder[0].Universe()[StarSystemId.Utopia.CastToInt()], holder[0].SelectedSystem(),
          "n moves down to the second quest");

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(holder[0].Universe()[StarSystemId.Acamar.CastToInt()], holder[0].SelectedSystem(),
          "p moves back up to the first quest");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsNAndPWithASingleEntryKeepItSelected() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // Only the moon quest: the list has one entry.
      holder[0].setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      StarSystem utopia = holder[0].Universe()[StarSystemId.Utopia.CastToInt()];
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestMoon), screenText(screen));

      // N and P (uppercase too) wrap onto the only entry instead of losing it.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('N', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('P', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("→ " + utopia.Name()),
          "the only entry stays selected: " + screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(utopia, holder[0].SelectedSystem(),
          "ENTER targets the only entry after the N/P presses");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsPanelScrollsToTheLastQuest() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      Game game = holder[0];
      game.setQuestStatusGemulon(SpecialEvent.StatusGemulonStarted);
      game.setQuestStatusExperiment(SpecialEvent.StatusExperimentStarted);
      game.setQuestStatusReactor(SpecialEvent.StatusReactorFuelOk);
      game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      game.setQuestStatusJapori(SpecialEvent.StatusJaporiInTransit);
      game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyBaratas);
      game.setQuestStatusPrincess(SpecialEvent.StatusPrincessFlyCentauri);
      game.setQuestStatusScarab(SpecialEvent.StatusScarabHunting);
      game.setQuestStatusSculpture(SpecialEvent.StatusSculptureInTransit);
      game.setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
      game.Commander().getShip().setTribbles(1);
      game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      String open = screenText(screen);
      assertTrue(open.contains(Strings.QuestReactor), open);
      assertFalse(open.contains(Strings.QuestMoon), open);

      // The moon is the last of the twelve entries: only scrolling reaches it.
      for(int i = 0; i < 11; i++) {
        window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestMoon), screenText(screen));

      // ENTER sets the destination of the last entry as the map target and closes.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(game.Universe()[StarSystemId.Utopia.CastToInt()], game.SelectedSystem(),
          "ENTER targets the last quest (the moon)");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));

      // Reopening resets the cursor; 25 presses wrap around the twelve entries
      // instead of overflowing the index (25 = 2 × 12 + 1: the second entry).
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      for(int i = 0; i < 25; i++) {
        window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      }
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(game.Universe()[StarSystemId.Daled.CastToInt()], game.SelectedSystem(),
          "many arrow presses wrap to the second quest without overflowing");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsPanelWithNoDestinationsHasNoCursorAndSetsNothing() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      holder[0].Commander().getShip().setTribbles(2);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains(Strings.QuestTribbles), text);
      assertFalse(text.contains("→"), "an entry without a destination has no marker: " + text);
      assertFalse(questPanelHasSelection(screen), "no cursor without destinations: " + text);

      // The arrows do not turn the tribbles into a target.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      gui.updateScreen();
      assertNull(holder[0].SelectedSystem());
      assertFalse(questPanelHasSelection(screen), "the arrows paint no cursor: " + screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertNull(holder[0].SelectedSystem(), "ENTER on a quest without a destination sets no target");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aDynamicQuestWithoutAPlacedEventKeepsNoMarker() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // The artifact is on board but its delivery event is not placed anywhere:
      // the quest must be listed without a destination and without crashing.
      Game game = holder[0];
      game.setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
      Consts.SpecialEvents.get(SpecialEventType.ArtifactDelivery.CastToInt())
          .Location(game.Universe()).SpecialEventType(SpecialEventType.NA);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains("Deliver the alien artifact"), text);
      assertFalse(text.contains("→"), "an unplaced destination has no marker: " + text);
      assertFalse(questPanelHasSelection(screen), "no cursor without a destination: " + text);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertNull(game.SelectedSystem(), "the missing destination cannot be targeted");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuestsPanelEnterWiresTheTargetAndTheMapFollows() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      Game game = holder[0];
      game.Commander().getShip().setFuel(game.Commander().getShip().FuelTanks());

      // A quest whose destination is within range of some other system, so the
      // panel can set the target and SPACE can travel there right away.
      StarSystemId chosen = null;
      StarSystem home = null;
      StarSystem target = null;
      for(StarSystemId id : QUEST_DESTINATIONS) {
        StarSystem destination = game.Universe()[id.CastToInt()];
        for(StarSystem system : game.Universe()) {
          if(system != destination
              && Functions.Distance(system, destination) <= game.Commander().getShip().getFuel()) {
            chosen = id;
            home = system;
            target = destination;
            break;
          }
        }
        if(chosen != null) {
          break;
        }
      }
      assertNotNull(chosen, "the galaxy must have a quest destination within range");
      game.Commander().CurrentSystem(home);
      activateQuest(game, chosen);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("→ " + target.Name()), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(target, game.SelectedSystem(), "ENTER points the map at the quest destination");
      assertFalse(screenText(screen).contains(Strings.QuestsTitle));

      // The panel refresh: the navigation panel shows the new target instead of
      // the current system and the trade table gains the target prices columns.
      String text = screenText(screen);
      assertTrue(text.contains(Functions.StringVars(Strings.MainSystem, target.Name(),
          Strings.Sizes.get(target.Size().CastToInt()))),
          "the navigation panel must show the new target: " + text);
      assertTrue(text.contains(String.format("%-10s %10s %10s %5s", Strings.TradeItem, Strings.TradeSell,
          Strings.TradeBuy, Strings.TradePct)), "the target prices must be refreshed: " + text);

      // The flow goes on: SPACE travels to the destination the panel set.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertSame(target, game.Commander().CurrentSystem(),
          "SPACE travels to the quest target: " + screenText(screen));
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
      assertTrue(screenText(screen).contains(Strings.ShipCargoLabel), screenText(screen));
      assertTrue(screenText(screen).contains(Strings.ShipCargoNone), screenText(screen));
      assertFalse(screenText(screen).contains("| o o >"), "the old sprite is gone: " + screenText(screen));
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
  void theMapKeepsNAndPAsNewsAndPersonnel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // On the map n and p keep opening the newspaper and the personnel panels.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.NewsTitle), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.PersonnelTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void thePersonnelNAndPMoveTheSelectionAndTheHKeyStillReachesThePresenter() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      // Two mercenaries in one system: the list has two rows to move between.
      CrewMember[] mercs = holder[0].Mercenaries();
      StarSystem system = holder[0].Universe()[0];
      int moved = 0;
      for(int i = 1; i < mercs.length && moved < 2; i++) {
        if(mercs[i] != null) {
          mercs[i].CurrentSystem(system);
          moved++;
        }
      }
      assertEquals(2, moved, "the universe must have two mercenaries");
      holder[0].Commander().CurrentSystem(system);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.PersonnelTitle), screenText(screen));
      int firstRow = selectedRow(screen);
      assertTrue(firstRow > 0, "a personnel row is selected: " + screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      int downRow = selectedRow(screen);
      assertTrue(downRow > firstRow, "n moves the personnel selection down: " + screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertEquals(firstRow, selectedRow(screen), "p moves the personnel selection back up");

      // H still reaches the presenter: the Gnat has no free crew quarter, so it
      // answers with its own alert instead of hiring.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('h', false, false));
      gui.updateScreen();
      assertTrue(dialogs.alerts().contains(AlertType.CrewNoQuarters),
          "the H key still reaches the personnel presenter: " + dialogs.alerts());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipPanelListsTheCargoWithItsAverageCost() throws IOException {
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
      holder[0].Commander().getShip().Cargo()[0] = 3;
      holder[0].Commander().PriceCargo()[0] = 75;
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('v', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.ShipCargoLabel), text);
      assertTrue(text.contains("Water"), text);
      assertTrue(text.contains(Functions.StringVars(Strings.ShipCargoBoughtAt, Functions.FormatMoney(25))), text);
      assertFalse(text.contains(Strings.ShipCargoNone), "the hold is not empty: " + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void cutsTheCargoLineToThePanelWidth() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 31)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      holder[0].Commander().getShip().Cargo()[8] = 1; // Narcotics, the longest name
      holder[0].Commander().PriceCargo()[8] = Integer.MAX_VALUE;
      // At 60 columns the ship panel is 36 wide: min(width - 24, 50).
      String line = String.format("%-10s %2d   %s", "Narcotics", 1,
          Functions.StringVars(Strings.ShipCargoBoughtAt, Functions.FormatMoney(Integer.MAX_VALUE)));
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('v', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(line.length() > 36, "the scenario needs a line wider than the panel: " + line);
      assertTrue(text.contains(line.substring(0, 36)), "the line is cut to the panel width: " + text);
      assertFalse(text.contains(line), "the price tail does not fit in the panel: " + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void hidesTheCargoLinesThatDoNotFitInThePanel() throws IOException {
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
      fillTheHold(holder[0]);
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('v', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.ShipCargoLabel), text);
      assertTrue(text.contains("Machines"), "the last line that fits is drawn: " + text);
      assertFalse(text.contains("Narcotics"), "the first line that does not fit is hidden: " + text);
      assertFalse(text.contains("Robots"), "the rest of the hold is hidden: " + text);
      // Row height - 5 stays reserved: the ship panel occupies the right 50 columns.
      assertTrue(row(screen, 25).substring(50).isBlank(), "nothing is drawn on the reserved row: " + row(screen, 25));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void keepsTheCargoInsideAShortPanel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 21)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      fillTheHold(holder[0]);
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('v', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.ShipTitle), text);
      assertTrue(text.contains(Strings.ShipKeys), "the panel keys survive a hold that does not fit: " + text);
      for(int y = 16; y < 21; y++) { // height - 5 and below: the keys area
        String panel = row(screen, y).substring(50);
        for(int i = 0; i < Consts.TradeItems.size(); i++) {
          assertFalse(panel.contains(Consts.TradeItems.get(i).Name()),
              "nothing spills past the height limit, row " + y + ": " + panel);
        }
      }
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('s', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.ShipListTitle), screenText(screen));
      assertTrue(screenText(screen).contains("> Flea"), screenText(screen));
      assertFalse(screenText(screen).contains("| o o >"), "the old sprite is gone: " + screenText(screen));
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
  void theShipListNAndPMoveTheSelectionAndTheBKeyStillBuys() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      // The most technological system sells the Firefly, so the third row is buyable.
      StarSystem best = holder[0].Universe()[0];
      for(StarSystem candidate : holder[0].Universe()) {
        if(candidate.TechLevel().ordinal() > best.TechLevel().ordinal()) {
          best = candidate;
        }
      }
      holder[0].Commander().CurrentSystem(best);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('s', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Flea"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Gnat"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Flea"), screenText(screen));

      // B keeps buying: the handler runs on the third (buyable) row and the cursor
      // returns to the first ship; without it the cursor would stay on the Firefly.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Firefly"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('b', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Flea"),
          "the B shortcut keeps working after the n/p navigation: " + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theEquipmentNAndPMoveTheSelectionAndTheBKeyStillReachesThePresenter() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      // The most technological system sells every weapon, so the buy list starts
      // with the pulse and beam lasers.
      StarSystem best = holder[0].Universe()[0];
      for(StarSystem candidate : holder[0].Universe()) {
        if(candidate.TechLevel().ordinal() > best.TechLevel().ordinal()) {
          best = candidate;
        }
      }
      holder[0].Commander().CurrentSystem(best);
      holder[0].Commander().setCash(1000000);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      String pulse = Consts.EquipmentForSale.get(0).Name();
      String beam = Consts.EquipmentForSale.get(1).Name();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('e', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> " + pulse), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> " + beam), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> " + pulse), screenText(screen));

      // B still buys: the starting Gnat already mounts a pulse laser and has no
      // free weapon slot, so the presenter answers with its own alert (proving
      // the key was not eaten by the navigation).
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('b', false, false));
      gui.updateScreen();
      assertTrue(dialogs.alerts().contains(AlertType.EquipmentNotEnoughSlots),
          "the B key still reaches the equipment presenter: " + dialogs.alerts());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShiftHintAlsoAcceptsTheUppercaseLetter() {
    // Most terminals do not report the shift modifier for letters: Shift+B arrives
    // as 'B', so the uppercase letter must count as "all".
    assertTrue(LanternaMainWindow.allAmount(new KeyStroke('B', false, false)));
    assertTrue(LanternaMainWindow.allAmount(new KeyStroke('S', false, false)));
    assertFalse(LanternaMainWindow.allAmount(new KeyStroke('b', false, false)));
    assertFalse(LanternaMainWindow.allAmount(new KeyStroke('s', false, false)));
    assertTrue(LanternaMainWindow.allAmount(new KeyStroke('B', true, false)));
  }

  @Test
  void theListKeysTurnNAndPIntoTheArrows() {
    assertEquals(KeyType.ArrowDown, LanternaMainWindow.listKey(new KeyStroke('n', false, false)).getKeyType());
    assertEquals(KeyType.ArrowDown, LanternaMainWindow.listKey(new KeyStroke('N', false, false)).getKeyType());
    assertEquals(KeyType.ArrowUp, LanternaMainWindow.listKey(new KeyStroke('p', false, false)).getKeyType());
    assertEquals(KeyType.ArrowUp, LanternaMainWindow.listKey(new KeyStroke('P', true, false)).getKeyType());
    // The other letters of the panels keep their own meaning.
    for(char letter : new char[] {'b', 's', 'h', 'c', 'v', 'r', 'j', 'k', 'q'}) {
      KeyStroke stroke = new KeyStroke(letter, false, false);
      assertSame(stroke, LanternaMainWindow.listKey(stroke), "listKey must not touch " + letter);
    }
    KeyStroke arrow = new KeyStroke(KeyType.ArrowUp);
    assertSame(arrow, LanternaMainWindow.listKey(arrow), "listKey must not touch the arrows");
  }

  @Test
  void vimKeysMoveTheChartCursor() throws IOException {
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

      String before = row(screen, 3);
      for(char key : new char[] {'h', 'j', 'k', 'l'}) {
        window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(key, false, false));
        gui.updateScreen();
        if(!row(screen, 3).equals(before)) {
          break;
        }
      }
      assertNotEquals(before, row(screen, 3), "hjkl must move the chart cursor:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void acceptsTheSpecialEventWithTheYKey() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setConfirmResult(DialogResult.Yes);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(600000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(SpecialEvent.StatusMoonBought, holder[0].getQuestStatusMoon(),
          "the moon offer must be accepted");
      assertEquals(100000, holder[0].Commander().getCash(), "the moon price was paid");
      assertEquals(SpecialEventType.NA, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the accepted offer leaves the system");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theNoKeyLeavesTheSpecialEventOfferUntouched() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setConfirmResult(DialogResult.No);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(600000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(SpecialEvent.StatusMoonNotStarted, holder[0].getQuestStatusMoon());
      assertEquals(600000, holder[0].Commander().getCash(), "a No spends nothing");
      assertEquals(SpecialEventType.Moon, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the offer stays in the system");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theOfferShowsTheTitleAndTheStoryBeforeAsking() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setConfirmResult(DialogResult.Yes);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(600000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      SpecialEvent offer = holder[0].Commander().CurrentSystem().SpecialEvent();
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(List.of(offer.Title()), dialogs.confirmTitles());
      assertEquals(1, dialogs.confirms().size());
      String shown = dialogs.confirms().get(0);
      assertTrue(shown.contains(offer.String()), shown);
      assertTrue(shown.contains(Functions.StringVars(Strings.SpecialEventCost,
          Functions.Multiples(offer.Price(), Strings.MoneyUnit))), "the cost line is shown: " + shown);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void acceptsAMessageOnlySpecialEventWithTheOkButton() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setMessageResult(DialogResult.OK);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(1000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Lottery);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(2000, holder[0].Commander().getCash(), "the lottery prize was paid");
      assertEquals(SpecialEventType.NA, holder[0].Commander().CurrentSystem().SpecialEventType());
      assertEquals(1, dialogs.messages().size(), "a message-only event shows a message");
      assertTrue(dialogs.confirms().isEmpty(), "a message-only event asks no question");
      assertEquals(List.of(Strings.SpecialEventTitles.get(SpecialEventType.Lottery.CastToInt())),
          dialogs.messageTitles());
      assertTrue(dialogs.messages().get(0).contains(Functions.StringVars(Strings.SpecialEventReward,
          Functions.Multiples(1000, Strings.MoneyUnit))),
          "the reward line is shown: " + dialogs.messages().get(0));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void withNoUserInterfaceTheOfferIsNotApplied() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(600000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      // Without a front-end there is nobody to accept the offer.
      assertEquals(SpecialEvent.StatusMoonNotStarted, holder[0].getQuestStatusMoon());
      assertEquals(600000, holder[0].Commander().getCash());
      assertEquals(SpecialEventType.Moon, holder[0].Commander().CurrentSystem().SpecialEventType());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theNarrativeFailuresKeepTheirEventAfterBeingRead() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setMessageResult(DialogResult.OK);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.ExperimentFailed);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      assertEquals(SpecialEventType.ExperimentFailed, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the failure story can be read again");

      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.GemulonInvaded);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      assertEquals(SpecialEventType.GemulonInvaded, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the invasion story can be read again");
      assertEquals(2, dialogs.messages().size());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void retiringToTheMoonEndsTheGameAfterTheConfirmation() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setConfirmResult(DialogResult.Yes);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(250000);
      holder[0].setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.MoonRetirement);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(SpecialEvent.StatusMoonDone, holder[0].getQuestStatusMoon(),
          "the retirement was accepted");
      assertEquals(250000, holder[0].Commander().getCash(), "retiring costs nothing");
      assertEquals(1, dialogs.confirms().size(), "the retirement is a yes/no question");
      String offered = dialogs.confirms().get(0);
      assertTrue(offered.contains(
          Strings.SpecialEventStrings.get(SpecialEventType.MoonRetirement.CastToInt())), offered);
      assertFalse(offered.contains(Strings.SpecialEventCost), "a free event shows no cost line");
      assertFalse(offered.contains(Strings.SpecialEventReward), "a free event shows no reward line");
      assertTrue(dialogs.alerts().contains(AlertType.GameEndBoughtMoon),
          "the moon ending is announced: " + dialogs.alerts());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aRewardOfferIsNeverBlockedByTheEmptyPurse() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setConfirmResult(DialogResult.Yes);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(0);
      holder[0].Commander().setPoliceRecordScore(-10);
      holder[0].Commander().setReputationScore(50);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Sculpture);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      // The -2000 price is an advance for the carrier: an empty purse must not refuse it.
      assertEquals(SpecialEvent.StatusSculptureInTransit, holder[0].getQuestStatusSculpture());
      assertEquals(2000, holder[0].Commander().getCash(), "the 2000 credit advance is paid");
      assertEquals(SpecialEventType.NA, holder[0].Commander().CurrentSystem().SpecialEventType());
      assertEquals(1, dialogs.confirms().size());
      assertTrue(dialogs.confirms().get(0).contains(Functions.StringVars(Strings.SpecialEventReward,
          Functions.Multiples(2000, Strings.MoneyUnit))),
          "the reward line is shown: " + dialogs.confirms().get(0));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void withNoUserInterfaceAMessageOnlyOfferIsNotApplied() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(1000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Lottery);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      // The lottery only has an OK button: with no front-end nobody presses it.
      assertEquals(1000, holder[0].Commander().getCash(), "the prize is not paid");
      assertEquals(SpecialEventType.Lottery, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the offer stays in the system");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theOfferProtectsItselfFromAMissingStoryAndOmitsTheZeroPrice() throws Exception {
    assertOffer("", new SpecialEvent(null, 0, 0, false) {
      @Override
      public String String() {
        return null;
      }
    });
    assertOffer(Functions.StringVars(Strings.SpecialEventCost,
        Functions.Multiples(1000, Strings.MoneyUnit)), new SpecialEvent(null, 1000, 3, false) {
      @Override
      public String String() {
        return "   ";
      }
    });
    assertOffer(Functions.StringVars(Strings.SpecialEventReward,
        Functions.Multiples(15000, Strings.MoneyUnit)), new SpecialEvent(null, -15000, 0, true) {
      @Override
      public String String() {
        return "";
      }
    });
  }

  private static void assertOffer(String expected, SpecialEvent event) throws Exception {
    Method offer = LanternaMainWindow.class.getDeclaredMethod("offer", SpecialEvent.class);
    offer.setAccessible(true);
    assertEquals(expected, offer.invoke(null, event));
  }

  @Test
  void doesNotAcceptTheSpecialEventWithoutEnoughMoney() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(450000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertEquals(SpecialEvent.StatusMoonNotStarted, holder[0].getQuestStatusMoon());
      assertEquals(450000, holder[0].Commander().getCash(), "the failed offer spends nothing");
      assertEquals(SpecialEventType.Moon, holder[0].Commander().CurrentSystem().SpecialEventType(),
          "the offer stays in the system");
      assertTrue(dialogs.alerts().contains(AlertType.SpecialIF), dialogs.alerts().toString());
      assertTrue(dialogs.confirms().isEmpty(), "the offer is not even asked");
      assertTrue(dialogs.messages().isEmpty(), "no message is shown either");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheSpecialEventInTheNavigationPanel() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().setCash(600000);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.SpecialEventTitles.get(SpecialEventType.Moon.CastToInt())), text);
      assertTrue(text.contains(Strings.NavSpecial), text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void logsWhenThereIsNoSpecialEvent() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      holder[0].Commander().CurrentSystem().SpecialEventType(SpecialEventType.NA);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('y', false, false));
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Strings.MainSpecialNone), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void opensTheAboutPanelWithTheCredits() throws IOException {
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('a', false, false));
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains(Strings.AboutTitle), text);
      assertTrue(text.contains("Space Trader (Palm OS"), text);
      assertTrue(text.contains("General Public License"), text);
      assertTrue(text.contains("Pieter"), text);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.AboutTitle), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void findsASystemByNameAndSelectsIt() throws IOException, InterruptedException {
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

      StarSystem target = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != holder[0].Commander().CurrentSystem()) {
          target = system;
          break;
        }
      }
      assertNotNull(target);
      String name = uniqueName(holder[0], target);

      Thread worker = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('/', false, false)));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui, window.asWindow());
      type(dialog, name);
      dialog.handleInput(new KeyStroke(KeyType.Enter));
      worker.join(5000);
      assertFalse(worker.isAlive(), "the find should end when the name is accepted");
      gui.updateScreen();

      assertSame(target, holder[0].WarpSystem(), "the found system is the target");
      assertTrue(screenText(screen).contains(target.Name()), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void asksWhichSystemWhenSeveralMatchTheName() throws IOException, InterruptedException {
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

      StarSystem first = null;
      String prefix = null;
      for(StarSystem system : holder[0].Universe()) {
        String candidate = system.Name().substring(0, Math.min(3, system.Name().length()));
        List<StarSystem> found = matches(holder[0], candidate);
        if(found.size() > 1) {
          first = found.get(0);
          prefix = candidate;
          break;
        }
      }
      assertNotNull(prefix, "the galaxy must have two systems starting with the same letters");

      Thread worker = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('/', false, false)));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui, window.asWindow());
      type(dialog, prefix);
      dialog.handleInput(new KeyStroke(KeyType.Enter));
      worker.join(5000);
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.FindTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertSame(first, holder[0].WarpSystem(), "the first match is selected");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void logsWhenNoSystemMatchesTheName() throws IOException, InterruptedException {
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

      String text = "zzz";
      while(!matches(holder[0], text).isEmpty()) {
        text += "z";
      }

      Thread worker = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('/', false, false)));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui, window.asWindow());
      type(dialog, text);
      dialog.handleInput(new KeyStroke(KeyType.Enter));
      worker.join(5000);
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Strings.FindNone), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void wrapsTheLongLinesOfTheCommanderPanel() throws IOException {
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
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('i', false, false));
      gui.updateScreen();

      // The skills and the cash do not fit in the 42-column panel: they wrap.
      assertTrue(screenText(screen).contains("Engineer 4 (4)"), screenText(screen));
      assertTrue(screenText(screen).contains("Net worth:"), screenText(screen));
      assertTrue(screenText(screen).contains("11,000 cr."), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void spaceClosesTheReadOnlyPanels() throws IOException {
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('i', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.CommanderTitle), screenText(screen));
      assertTrue(screenText(screen).contains("[SPACE] close"), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.CommanderTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));
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

      // The last option cycles the galaxy chart width (1, 2, 3).
      for(int i = 0; i < 17; i++) {
        window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionGalaxyColumns, "2")), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionGalaxyColumns, "3")), screenText(screen));

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
  void theOptionsNAndPMoveTheSelection() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F8));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoFuel, Strings.OptionsOff)), screenText(screen));

      // n moves down to "Auto-repair" and ENTER toggles it.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoRepair, Strings.OptionsOn)), screenText(screen));

      // p comes back up to "Auto-fuel" and ENTER toggles it too.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoFuel, Strings.OptionsOn)), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theOptionsNAndPWrapAroundTheList() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F8));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoFuel, Strings.OptionsOff)), screenText(screen));

      // p at the first entry wraps up to the last one: ENTER changes the galaxy columns.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionGalaxyColumns, "3")), screenText(screen));
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoFuel, Strings.OptionsOff)), "p must not toggle the first entry");

      // n at the last entry wraps down to the first one.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.OptionsValue,
          Strings.OptionAutoFuel, Strings.OptionsOn)), "n must wrap to the first entry");
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
  void theDesignerNAndPMoveTheSelectionAndRNamesTheDesign() throws IOException, InterruptedException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
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
      assertTrue(screenText(screen).contains("> Size:"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Template:"), screenText(screen));
      assertFalse(screenText(screen).contains("> Size:"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Size:"), screenText(screen));

      // n no longer renames: it navigates, so no dialog opens and the marker moves.
      Thread probe = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('n', false, false)));
      probe.setDaemon(true);
      probe.start();
      probe.join(500);
      assertFalse(probe.isAlive(), "n must navigate in the designer, not open the rename dialog");
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Template:"), screenText(screen));

      // r renames the design again.
      Thread worker = new Thread(() -> window.asWindow().getFocusedInteractable()
          .handleInput(new KeyStroke('r', false, false)));
      worker.setDaemon(true);
      worker.start();
      Window dialog = waitForDialog(gui, window.asWindow());
      type(dialog, "Halcon");
      dialog.handleInput(new KeyStroke(KeyType.Enter));
      worker.join(5000);
      assertFalse(worker.isAlive(), "the rename should end when the name is accepted");
      gui.updateScreen();
      assertTrue(screenText(screen).contains("Halcon"), screenText(screen));
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
  void theNewsNAndPScrollThePaper() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      // A moon story in a nearby system gives the paper more than one line.
      StarSystem near = null;
      for(StarSystem candidate : holder[0].Universe()) {
        if(candidate != holder[0].Commander().CurrentSystem() && candidate.DestOk()) {
          near = candidate;
          break;
        }
      }
      assertNotNull(near, "the current system must have a neighbour in range");
      near.SpecialEventType(SpecialEventType.Moon);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.NewsTitle), screenText(screen));

      String top = screenText(screen);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      gui.updateScreen();
      assertNotEquals(top, screenText(screen), "n scrolls the paper down");

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      gui.updateScreen();
      assertEquals(top, screenText(screen), "p scrolls the paper back up");
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

      // The cursor starts on "High scores"; the vim key moves down to "Options"
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('j', false, false));
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
  void theMenuNAndPMoveTheSelection() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = newGame();
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // The menu starts on "High scores": n goes down to "Options".
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.OptionsTitle), screenText(screen));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      // p comes back up: from "Options" the cursor returns to "High scores".
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('n', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('p', false, false));
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.HighScoresTitle), screenText(screen));
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('s', false, false));
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

  @Test
  void escapeOnTheMapAsksBeforeQuitting() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // "No" leaves the game running.
      dialogs.setResult(DialogResult.No);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      assertEquals(List.of(AlertType.GameAbandonConfirm), dialogs.alerts(),
          "the escape asks before abandoning the unmapped progress");
      assertTrue(gui.getWindows().contains(window.asWindow()), "No keeps the game open");

      // "Yes" closes the window.
      dialogs.setResult(DialogResult.Yes);
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      assertEquals(2, dialogs.alerts().size(), "the second escape asks again");
      assertFalse(gui.getWindows().contains(window.asWindow()), "Yes closes the window");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void escapeClosesAnOpenPanelWithoutAsking() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      dialogs.setResult(DialogResult.Yes);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.TradeTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();

      assertTrue(dialogs.alerts().isEmpty(), "closing a panel is not quitting: " + dialogs.alerts());
      assertTrue(gui.getWindows().contains(window.asWindow()), "the window stays open");
      assertFalse(screenText(screen).contains(Strings.TradeTitle), "the panel is closed");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void escapeWithNoGameClosesWithoutAsking() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));

      assertFalse(gui.getWindows().contains(window.asWindow()),
          "with no game there is nothing to lose: the window closes at once");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuitMenuItemAsksBeforeLeavingAGame() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestDialogService dialogs = new TestDialogService();
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      // "No" leaves the game running; the menu lists Quit as its last entry.
      dialogs.setResult(DialogResult.No);
      activateQuitMenuItem(window);
      assertEquals(List.of(AlertType.GameAbandonConfirm), dialogs.alerts(),
          "the Quit entry asks before abandoning the unmapped progress");
      assertTrue(gui.getWindows().contains(window.asWindow()), "No keeps the game open");

      // "Yes" closes the window.
      dialogs.setResult(DialogResult.Yes);
      activateQuitMenuItem(window);
      assertEquals(2, dialogs.alerts().size(), "the second Quit asks again");
      assertFalse(gui.getWindows().contains(window.asWindow()), "Yes closes the window");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theQuitMenuItemClosesWithNoGameWithoutAsking() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      LanternaMainWindow window = new LanternaMainWindow(() -> null, gui);
      MainPresenter presenter = new MainPresenter(() -> null, window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      activateQuitMenuItem(window);

      assertFalse(gui.getWindows().contains(window.asWindow()),
          "with no game the Quit entry closes at once");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** Opens the F10 menu, moves to its last entry (Quit) and activates it. */
  private static void activateQuitMenuItem(LanternaMainWindow window) {
    window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.F10));
    for(int i = 0; i < 6; i++) {
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
    }
    window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
  }

  private static TextColor foregroundAt(Screen screen, int x, int y) {
    return screen.getBackCharacter(x, y).getForegroundColor();
  }

  /** The first drawing row of the splash that contains a text, or -1. */
  private static int rowOf(TitleSplash splash, String text) {
    for(int y = 0; y < splash.height(); y++) {
      if(splash.lines().get(y).contains(text)) {
        return y;
      }
    }
    return -1;
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
  void warpTargetCrossesTheWormholeWhenTheCurrentSystemIsSelected() {
    Game game = newGame();
    StarSystem current = systemWithWormhole(game);
    StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
    game.Commander().CurrentSystem(current);
    game.SelectedSystemId(current.Id());
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertSame(pair, window.warpTarget(game));
    assertSame(current, game.SelectedSystem(), "the cursor stays on the current system");
    assertSame(pair, game.WarpSystem());
    assertTrue(game.TargetWormhole(), "the trip must be marked as a wormhole jump");
  }

  @Test
  void warpTargetWithNoSelectionCrossesTheWormhole() {
    Game game = newGame();
    // Not the first slot of the wormhole map, so a bug that always jumped through
    // the first slot cannot pass this test by chance.
    StarSystem current = systemWithWormholeAwayFromTheFirstSlot(game);
    StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
    game.Commander().CurrentSystem(current);
    game.SelectedSystemId(StarSystemId.NA);
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertSame(pair, window.warpTarget(game));
    assertSame(current, game.SelectedSystem());
    assertSame(pair, game.WarpSystem());
    assertTrue(game.TargetWormhole(), "the trip must be marked as a wormhole jump");
  }

  @Test
  void warpTargetWithoutAWormholeStaysOnTheCurrentSystem() {
    Game game = newGame();
    StarSystem current = systemWithoutWormhole(game);
    game.Commander().CurrentSystem(current);
    game.SelectedSystemId(current.Id());
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertSame(current, window.warpTarget(game));
    assertSame(current, game.WarpSystem());
    assertFalse(game.TargetWormhole());
  }

  @Test
  void warpTargetWithNoSelectionAndNoWormholeStaysEmpty() {
    Game game = newGame();
    game.Commander().CurrentSystem(systemWithoutWormhole(game));
    game.SelectedSystemId(StarSystemId.NA);
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertNull(window.warpTarget(game));
    assertNull(game.WarpSystem());
    assertFalse(game.TargetWormhole());
  }

  @Test
  void warpTargetKeepsAnotherSelectedSystem() {
    Game game = newGame();
    StarSystem current = systemWithWormhole(game);
    StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
    game.Commander().CurrentSystem(current);
    StarSystem other = null;
    for(StarSystem system : game.Universe()) {
      if(system != current && system != pair) {
        other = system;
        break;
      }
    }
    assertNotNull(other, "the galaxy must have a third system");
    game.SelectedSystemId(other.Id());
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertSame(other, window.warpTarget(game));
    assertSame(other, game.WarpSystem());
    assertFalse(game.TargetWormhole(), "a plain selection is not a wormhole target");
  }

  @Test
  void warpTargetKeepsThePairSelected() {
    Game game = newGame();
    StarSystem current = systemWithWormhole(game);
    StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
    game.Commander().CurrentSystem(current);
    game.SelectedSystemId(pair.Id());
    LanternaMainWindow window = new LanternaMainWindow(() -> game, null);

    assertSame(pair, window.warpTarget(game));
    assertSame(pair, game.WarpSystem());
    assertFalse(game.TargetWormhole());
  }

  @Test
  void spaceTravelsThroughTheWormholeOfTheCurrentSystem() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      StarSystem end = systemWithWormholeAwayFromTheFirstSlot(holder[0]);
      StarSystem pair = Functions.WormholeTarget(end.Id().CastToInt());
      holder[0].Commander().CurrentSystem(end);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      int fuel = holder[0].Commander().getShip().getFuel();
      int cash = holder[0].Commander().getCash();
      int toll = Consts.WormDist * holder[0].Commander().getShip().getFuelCost();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();

      assertSame(pair, holder[0].Commander().CurrentSystem(), screenText(screen));
      assertSame(pair, holder[0].WarpSystem(), "the arrival keeps the wormhole as the target");
      assertTrue(holder[0].getArrivedViaWormhole(), "the arrival came through the wormhole");
      assertEquals(fuel, holder[0].Commander().getShip().getFuel(), "a wormhole trip spends no fuel");
      assertEquals(cash - toll, holder[0].Commander().getCash(),
          "a wormhole trip pays the toll and nothing else");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void spaceWithoutASelectionOrAWormholeKeepsTheNoTargetMessage() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      // No wormhole here, no selection either: the old "no target" warning stays.
      StarSystem current = systemWithoutWormhole(holder[0]);
      holder[0].Commander().CurrentSystem(current);
      holder[0].SelectedSystemId(StarSystemId.NA);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();

      assertSame(current, holder[0].Commander().CurrentSystem(), "the ship does not move");
      assertFalse(holder[0].TargetWormhole(), "nothing marks a wormhole");
      assertTrue(screenText(screen).contains(Strings.MainWarpNoTarget), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void movingTheCursorAfterAWormholeJumpClearsTheWormholeMark() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setAutoSave(false);
      StarSystem current = systemWithWormhole(holder[0]);
      StarSystem pair = Functions.WormholeTarget(current.Id().CastToInt());
      holder[0].Commander().CurrentSystem(current);
      holder[0].SelectedSystemId(current.Id());
      // No money for the toll: SPACE targets the wormhole and the trip does not happen.
      holder[0].Commander().setCash(0);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertSame(current, holder[0].Commander().CurrentSystem(), "without the toll the ship stays");
      assertSame(pair, holder[0].WarpSystem());
      assertTrue(holder[0].TargetWormhole(), "the wormhole mark is on while the target is the wormhole");
      // The panel labels the target with the far end of its own wormhole.
      StarSystem destination = Functions.WormholeTarget(pair.Id().CastToInt());
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.MainWormhole, destination.Name())),
          screenText(screen));

      // Moving the cursor takes the target over: the wormhole mark must not stick.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('l', false, false));
      gui.updateScreen();
      if(holder[0].SelectedSystem() == current) {
        window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('h', false, false));
        gui.updateScreen();
      }
      assertNotSame(current, holder[0].SelectedSystem(), "the cursor moved to another system");
      StarSystem moved = holder[0].SelectedSystem();
      assertFalse(holder[0].TargetWormhole(), "moving the cursor clears the wormhole mark");
      assertSame(moved, holder[0].WarpSystem(), "the cursor drives the target again");
      StarSystem movedPair = Functions.WormholeTarget(moved.Id().CastToInt());
      if(movedPair == null) {
        assertFalse(screenText(screen).contains(Strings.MainWormhole), screenText(screen));
      } else {
        assertTrue(screenText(screen).contains(Functions.StringVars(Strings.MainWormhole, movedPair.Name())),
            screenText(screen));
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
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
      assertTrue(screenText(screen).contains("[Space] warp"), screenText(screen));

      int fuel = holder[0].Commander().getShip().getFuel();
      boolean wormhole = Functions.WormholeExists(current, target);
      int distance = Functions.Distance(current, target);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
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
      // Without a wormhole, SPACE on the current system keeps warning "already here".
      holder[0].Commander().CurrentSystem(systemWithoutWormhole(holder[0]));
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
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      gui.updateScreen();
      assertSame(current, holder[0].Commander().CurrentSystem());
      assertTrue(screenText(screen).contains(Strings.MainWarpOutOfRange), screenText(screen));

      holder[0].SelectedSystemId(current.Id());
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('g', false, false));
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
      assertTrue(screenText(screen).contains("[G] jump"), screenText(screen));

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
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('g', false, false));
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

      // The real flow: select in the chart and open the trade panel (no manual refresh).
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
  void explainsWhenTheTargetPricesAreUnknown() throws IOException {
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

      // With no target selected the panel says so.
      holder[0].SelectedSystemId(StarSystemId.NA);
      presenter.updateAll();
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.TradeNoTarget), screenText(screen));
      assertFalse(screenText(screen).contains(Strings.CargoTargetPriceUnknown),
          "the unknown values must not be drawn as dashes:\n" + screenText(screen));

      // With a target out of range it says that instead.
      StarSystem far = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system != holder[0].Commander().CurrentSystem() && !system.DestOk()) {
          far = system;
          break;
        }
      }
      assertNotNull(far, "the galaxy must have a system out of range");
      holder[0].SelectedSystemId(far.Id());
      presenter.updateAll();
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.TradeTargetOutOfRange), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void leavesTheTargetColumnsBlankWhenThereIsNoTarget() throws IOException {
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

      holder[0].SelectedSystemId(StarSystemId.NA);
      presenter.updateAll();
      gui.updateScreen();
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('c', false, false));
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains(Strings.TradeNoTarget), text);
      assertTrue(text.contains(Strings.TradeTarget), text);
      assertFalse(text.contains(Strings.CargoTargetPriceUnknown), "no dashes:\n" + text);
      assertFalse(text.contains(Strings.CargoTargetDiffUnknown), "no dashes:\n" + text);
      assertFalse(text.contains(Strings.CargoTargetPctUnknown), "no dashes:\n" + text);
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

      // The real flow: select in the chart and open the trade panel (no manual refresh).
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
  void theNavigationPanelShowsThePricesOfTheSelectedSystem() throws IOException {
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

      int[] targetSell = TradeCalculator.CalculateStandardSellPrices(target,
          holder[0].Commander().getPoliceRecordScore());
      int[] targetBuy = TradeCalculator.CalculateBuyPrices(target, targetSell,
          holder[0].Commander().getPoliceRecordScore(), holder[0].Commander().getShip().Trader());
      int price = Consts.TradeItems.get(0).StandardPrice(target);
      int localBuy = holder[0].PriceCargoBuy()[0];
      int diff = price - localBuy;
      String pct = localBuy > 0 ? (diff > 0 ? "+" : "") + Functions.FormatNumber(100 * diff / localBuy) + "%"
          : Strings.CargoTargetPctUnknown;
      String expected = String.format("%-10s %10s %10s %5s", Consts.TradeItems.get(0).Name(),
          targetSell[0] > 0 ? Functions.FormatMoney(targetSell[0]) : Strings.NoTrade,
          targetBuy[0] > 0 ? Functions.FormatMoney(targetBuy[0]) : Strings.NotSold, pct);
      assertTrue(screenText(screen).contains(expected), screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
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
      assertTrue(chartHasTarget(screen), "the selected system must be visible:\n" + screenText(screen));
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

      assertTrue(chartHasTarget(screen), "the chart must follow the selection:\n" + screenText(screen));
      int[] after = findInverted(screen);
      assertTrue(after == null || after[0] != before[0] || after[1] != before[1],
          "the chart must have scrolled:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void tracksAndUntracksTheCurrentSystemWhenNothingIsSelectedYet() throws IOException {
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

      // Pressing T again stops tracking it.
      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('t', false, false));
      gui.updateScreen();
      assertEquals(StarSystemId.NA.CastToInt(), holder[0].getTrackedSystemId().CastToInt());
      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.MainUntracking, current.Name())),
          screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void showsTheWormholeDestinationInThePanel() throws IOException {
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
      int[] wormholes = holder[0].Wormholes();
      StarSystem end = null;
      StarSystem pair = null;
      for(int i = 0; i + 1 < wormholes.length; i += 2) {
        if(holder[0].Universe()[wormholes[i]] != current) {
          end = holder[0].Universe()[wormholes[i]];
          pair = holder[0].Universe()[wormholes[i + 1]];
          break;
        }
      }
      assertNotNull(end, "the galaxy must have a wormhole outside the current system");

      holder[0].SelectedSystemId(end.Id());
      presenter.updateAll();
      gui.updateScreen();

      assertTrue(screenText(screen).contains(Functions.StringVars(Strings.MainWormhole, pair.Name())),
          screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void putsTheProgramMenuAfterTheContextualActions() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
      holder[0].setCanSuperWarp(true);
      MainPresenter presenter = new MainPresenter(() -> holder[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains("[G] jump"), text);
      assertTrue(text.contains("[F] fuel"), text);
      assertTrue(text.contains("[R] repairs"), text);
      assertTrue(text.contains("[F10] menu"), text);
      assertTrue(text.indexOf("[F10] menu") > text.indexOf("[G] jump"),
          "the program menu goes after the contextual actions:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void onlyTheChosenQuietAlertsSpeakUnderTheRival() {
    List<AlertType> chosen = List.of(AlertType.EncounterPoliceFine, AlertType.EncounterPoliceBribeCant,
        AlertType.EncounterMarieCelesteNoBribe, AlertType.EncounterSurrenderRefused);
    for(AlertType type : chosen) {
      assertTrue(LanternaMainWindow.speaksUnderTheRival(type), type + " speaks under the rival");
    }
    assertFalse(LanternaMainWindow.speaksUnderTheRival(AlertType.EncounterPoliceNothingFound),
        "the other outcomes keep their log line");
    assertFalse(LanternaMainWindow.speaksUnderTheRival(AlertType.EncounterEscaped));
    assertFalse(LanternaMainWindow.speaksUnderTheRival(AlertType.JailConvicted));
    // No fifth alert may sneak into the speech of the window: the arrest and the
    // pardon are said by the presenter (speechAndWait), not routed here.
    Set<AlertType> speaking = EnumSet.noneOf(AlertType.class);
    for(AlertType type : AlertType.values()) {
      if(LanternaMainWindow.speaksUnderTheRival(type)) {
        speaking.add(type);
      }
    }
    assertEquals(Set.copyOf(chosen), speaking, "only the four action outcomes speak through the window");
  }

  @Test
  void theQuietAlertsGoToTheMainLogWhenNoEncounterIsOpen() throws IOException {
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

      window.alertLog(AlertType.EncounterPoliceFine, "Caught. Fine 1,500 cr.");
      gui.updateScreen();

      assertTrue(screenText(screen).contains("Caught. Fine 1,500 cr."),
          "with no encounter the outcome falls to the main log:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theChosenAlertSpeaksUnderTheRivalWhileTheRestStayInTheEncounterLog() throws Exception {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      Game[] holder = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> holder[0], gui);
      LanternaDialogService dialogs = new LanternaDialogService(new LanternaAlertDialogHost(gui));
      holder[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      dialogs.quietTo(window::alertLog);
      StarSystem noBribe = null;
      for(StarSystem system : holder[0].Universe()) {
        if(system.PoliticalSystem().BribeLevel() <= 0) {
          noBribe = system;
          break;
        }
      }
      assertNotNull(noBribe, "the galaxy must have an unbribable system");
      holder[0].SelectedSystemId(noBribe.Id());
      holder[0].encounter().setEncounterType(EncounterType.PoliceInspect);
      gui.addWindow(window.asWindow());
      gui.updateScreen();

      String demandHead = Strings.EncounterSaysPolice.substring(0, 12);
      String bubbleHead = Alerts.get(AlertType.EncounterPoliceBribeCant).message().substring(0, 20);
      String keptHead = Alerts.get(AlertType.EncounterPoliceNothingFound).message().substring(0, 20);
      List<String> problems = new ArrayList<>();
      int[] bubbleAt = {0, 0, -1};
      int[] keptAt = {0, 0, -1};
      TextColor[] bubbleColor = {null};
      TextColor[] keptColor = {null};
      Thread player = new Thread(() -> {
        try {
          Window encounter = waitForDialog(gui, window.asWindow());
          if(!waitForText(screen, demandHead, 3000)) {
            problems.add("the encounter did not open:\n" + screenText(screen));
            return;
          }
          // The chosen alert: bribing where they take none speaks under the ship.
          encounter.getFocusedInteractable().handleInput(new KeyStroke('b', false, false));
          if(!waitForText(screen, bubbleHead, 3000)) {
            problems.add("the bubble did not arrive:\n" + screenText(screen));
            return;
          }
          int[] bubble = find(screen, bubbleHead);
          bubbleAt[0] = bubble[0];
          bubbleAt[1] = bubble[1];
          bubbleAt[2] = 1;
          bubbleColor[0] = foregroundAt(screen, bubble[0], bubble[1]);
          // The intro key submits: the other quiet alert keeps its log line.
          encounter.getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
          if(!waitForText(screen, keptHead, 3000)) {
            problems.add("the log line did not arrive:\n" + screenText(screen));
            return;
          }
          int[] kept = find(screen, keptHead);
          keptAt[0] = kept[0];
          keptAt[1] = kept[1];
          keptAt[2] = 1;
          keptColor[0] = foregroundAt(screen, kept[0], kept[1]);
          // The waiting scene leaves.
          encounter.getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
        } catch(Throwable t) {
          problems.add(String.valueOf(t));
        }
      });
      player.setDaemon(true);
      player.start();

      EncounterResult result = window.showEncounter();

      player.join(5000);
      assertTrue(problems.isEmpty(), problems.toString());
      assertEquals(EncounterResult.Normal, result);
      assertEquals(1, bubbleAt[2], "the bubble was found");
      assertEquals(1, keptAt[2], "the log line was found");
      assertEquals(TextColor.ANSI.YELLOW_BRIGHT, bubbleColor[0],
          "the chosen alert is the speech under the rival");
      assertEquals(UiPalette.ACCENT, keptColor[0], "the other quiet alert keeps its log line");
      assertFalse(screenText(screen).contains(bubbleHead), "the bubble went with the encounter");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** Waits until a text is painted on the screen (the pumping thread repaints it). */
  private static boolean waitForText(Screen screen, String needle, long millis) throws InterruptedException {
    long deadline = System.currentTimeMillis() + millis;
    while(System.currentTimeMillis() < deadline) {
      if(screenText(screen).contains(needle)) {
        return true;
      }
      Thread.sleep(20);
    }
    return screenText(screen).contains(needle);
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, new QuietHost(), DialogService.NONE);
  }

  /** A system whose wormhole leads to its pair. */
  private static StarSystem systemWithWormhole(Game game) {
    for(StarSystem system : game.Universe()) {
      if(Functions.WormholeTarget(system.Id().CastToInt()) != null) {
        return system;
      }
    }
    fail("the galaxy must have a wormhole");
    return null;
  }

  /**
   * A system with a wormhole whose far end is not the one a bug that always jumped
   * through the first slot of the wormhole map would pick. The wormhole map is a
   * random permutation, so the first system of the galaxy is not a stable stand-in.
   */
  private static StarSystem systemWithWormholeAwayFromTheFirstSlot(Game game) {
    StarSystem first = game.Universe()[game.Wormholes()[0]];
    StarSystem firstPair = Functions.WormholeTarget(first.Id().CastToInt());
    for(StarSystem system : game.Universe()) {
      if(Functions.WormholeTarget(system.Id().CastToInt()) != null
          && system != first && system != firstPair) {
        return system;
      }
    }
    fail("the galaxy must have a wormhole away from the first slot");
    return null;
  }

  /** A system without a wormhole, so SPACE there keeps the plain warp. */
  private static StarSystem systemWithoutWormhole(Game game) {
    for(StarSystem system : game.Universe()) {
      if(Functions.WormholeTarget(system.Id().CastToInt()) == null) {
        return system;
      }
    }
    fail("the galaxy must have a system without a wormhole");
    return null;
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

  /** The selected system is marked with parentheses. */
  private static boolean chartHasTarget(Screen screen) {
    for(int y = 4; y < 27; y++) {
      for(int x = 1; x < 64; x++) {
        char character = screen.getBackCharacter(x, y).getCharacter();
        if(character == '(' || character == ')') {
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

  /** A prefix of the system name that matches no other system. */
  private static String uniqueName(Game game, StarSystem system) {
    String name = system.Name();
    for(int length = 1; length <= name.length(); length++) {
      if(matches(game, name.substring(0, length)).size() == 1) {
        return name.substring(0, length);
      }
    }
    return name;
  }

  private static List<StarSystem> matches(Game game, String text) {
    List<StarSystem> found = new ArrayList<>();
    for(StarSystem system : game.Universe()) {
      if(system.Name().toLowerCase().startsWith(text.toLowerCase())) {
        found.add(system);
      }
    }
    return found;
  }

  private static void type(Window dialog, String text) {
    for(char character : text.toCharArray()) {
      dialog.handleInput(new KeyStroke(character, false, false));
    }
  }

  private static Window waitForDialog(MultiWindowTextGUI gui, Window main) throws InterruptedException {
    for(int i = 0; i < 500; i++) {
      for(Window window : gui.getWindows()) {
        if(window != main) {
          return window;
        }
      }
      Thread.sleep(10);
    }
    fail("the dialog was not shown");
    return null;
  }

  /** A hold with every product and a recorded average price. */
  private static void fillTheHold(Game game) {
    for(int i = 0; i < Consts.TradeItems.size(); i++) {
      game.Commander().getShip().Cargo()[i] = i + 1;
      game.Commander().PriceCargo()[i] = (i + 1) * 100;
    }
  }

  /** The fixed destinations of the quests the tests can activate. */
  private static final StarSystemId[] QUEST_DESTINATIONS = {
      StarSystemId.Gemulon, StarSystemId.Daled, StarSystemId.Nix, StarSystemId.Acamar,
      StarSystemId.Japori, StarSystemId.Baratas, StarSystemId.Melina, StarSystemId.Regulas,
      StarSystemId.Zalkon, StarSystemId.Centauri, StarSystemId.Inthara, StarSystemId.Qonos,
      StarSystemId.Endor, StarSystemId.Utopia};

  /** Opens one quest whose destination is that fixed system. */
  private static void activateQuest(Game game, StarSystemId id) {
    switch(id) {
      case Gemulon -> game.setQuestStatusGemulon(SpecialEvent.StatusGemulonStarted);
      case Daled -> game.setQuestStatusExperiment(SpecialEvent.StatusExperimentStarted);
      case Nix -> game.setQuestStatusReactor(SpecialEvent.StatusReactorDelivered);
      case Acamar -> game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
      case Japori -> game.setQuestStatusJapori(SpecialEvent.StatusJaporiInTransit);
      case Baratas -> game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyBaratas);
      case Melina -> game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyMelina);
      case Regulas -> game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyRegulas);
      case Zalkon -> game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyZalkon);
      case Centauri -> game.setQuestStatusPrincess(SpecialEvent.StatusPrincessFlyCentauri);
      case Inthara -> game.setQuestStatusPrincess(SpecialEvent.StatusPrincessFlyInthara);
      case Qonos -> game.setQuestStatusPrincess(SpecialEvent.StatusPrincessFlyQonos);
      case Endor -> game.setQuestStatusSculpture(SpecialEvent.StatusSculptureDelivered);
      case Utopia -> game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);
      default -> throw new IllegalArgumentException("no quest points at " + id);
    }
  }

  /** The first painted row of the right-hand panel with a selected (cyan) row, or -1. */
  private static int selectedRow(Screen screen) {
    int columns = screen.getTerminalSize().getColumns();
    int from = Math.max(0, columns - 60);
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      for(int x = from; x < columns; x++) {
        if(screen.getBackCharacter(x, y).getBackgroundColor() == UiPalette.SELECTED_BG) {
          return y;
        }
      }
    }
    return -1;
  }

  /** Whether the quests panel (the last 60 columns) paints a selected row. */
  private static boolean questPanelHasSelection(Screen screen) {
    int columns = screen.getTerminalSize().getColumns();
    int from = Math.max(0, columns - 60);
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      for(int x = from; x < columns; x++) {
        if(screen.getBackCharacter(x, y).getBackgroundColor() == UiPalette.SELECTED_BG) {
          return true;
        }
      }
    }
    return false;
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
