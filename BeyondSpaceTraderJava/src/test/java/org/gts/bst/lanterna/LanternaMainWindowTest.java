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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.GameWindow;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
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

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('s', false, false));
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

      assertEquals(SpecialEvent.StatusMoonBought, holder[0].getQuestStatusMoon(),
          "the moon offer must be accepted");
    } finally {
      screen.stopScreen();
      screen.close();
    }
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
      assertTrue(dialogs.alerts().contains(AlertType.SpecialIF), dialogs.alerts().toString());
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
