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
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.DialogService;
import org.junit.jupiter.api.Test;
import spacetrader.Functions;
import spacetrader.Game;
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
      assertTrue(areaContains(screen, 1, 4, 60, 26, "*o@+"), "no chart markers");
      assertTrue(row(screen, 29).contains("TAB map"), row(screen, 29));
      assertEquals(TextColor.ANSI.BLACK, screen.getBackCharacter(5, 10).getBackgroundColor(),
          "the window background must be black");
      assertTrue(columnContains(screen, 68, 3, 29, holder[0].Commander().CurrentSystem().Name()),
          "current system not in the panel");

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
      assertTrue(trade.contains(Strings.TradeKeys), trade);

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowDown));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("> Furs"), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.TradeTitle));
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
      assertTrue(screenText(screen).contains(Strings.BankKeys), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.BankTitle));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke('q', false, false));
      gui.updateScreen();
      assertTrue(screenText(screen).contains(Strings.QuestsTitle), screenText(screen));

      window.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      gui.updateScreen();
      assertFalse(screenText(screen).contains(Strings.QuestsTitle));
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
      assertFalse(screenText(screen).contains(Strings.EquipmentTitle));
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

  private static boolean columnContains(Screen screen, int x1, int y1, int y2, String text) {
    StringBuilder column = new StringBuilder();
    int lastColumn = Math.min(x1 + 32, screen.getTerminalSize().getColumns());
    for(int y = y1; y <= y2; y++) {
      for(int x = x1; x < lastColumn; x++) {
        column.append(screen.getBackCharacter(x, y).getCharacter());
      }
      column.append('\n');
    }
    return column.toString().contains(text);
  }
}
