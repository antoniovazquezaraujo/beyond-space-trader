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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.Alerts;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterViewModel;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipCatalog;
import org.gts.bst.view.ShipPicture;
import org.junit.jupiter.api.Test;
import spacetrader.Game;
import spacetrader.Ship;


class LanternaEncounterViewTest {
  @Test
  void rendersTheEncounterAndForwardsTheActions() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 0, ""));
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      String text = screenText(screen);
      assertFalse(text.contains("xxxxx"), "the encounter opens with the empty sky: " + text);
      assertFalse(text.contains("yyyyy"), "and the other ship is still coming in: " + text);
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      text = screenText(screen);
      assertTrue(text.contains("Flea"), text);
      assertTrue(text.contains("casco ████████"), "the hull bar of the player: " + text);
      assertTrue(text.contains("casco ████░░░░"), "half hull for the opponent: " + text);
      assertTrue(text.contains("xxxxx"), "the ship of the player is painted: " + text);
      assertTrue(text.contains("yyyyy"), "and the opponent too: " + text);
      assertFalse(text.contains("| o o >"), "the old sprite is gone: " + text);
      assertTrue(text.contains("The pirate attacks."), text);
      assertFalse(text.contains("[A]Attack"), "no buttons in the scene: " + text);
      assertFalse(text.contains("[F]Flee"), "no buttons in the scene: " + text);

      assertTrue(text.contains(spacetrader.Strings.EncounterLegend),
          "the legend of the pieces is on the right:\n" + text);

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('i', false, false));
      assertTrue(executed.isEmpty(), "an unavailable action must be ignored");

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      assertEquals(List.of(EncounterAction.Flee), executed);

      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      assertEquals(List.of(EncounterAction.Attack), executed, "space fires");

      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      assertTrue(executed.isEmpty(), "the left arrow moves the ship, it does not flee by itself");

      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('k', false, false));
      assertTrue(executed.isEmpty(), "the vim keys move the ship too");

      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      assertTrue(executed.isEmpty(), "the up arrow only moves the ship");

      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      assertEquals(List.of(EncounterAction.Surrender), executed, "the intro key gives up");

      // A new round leaves the other ship about to answer: our shot has to wait.
      content.resetPosition();
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
          false, 1, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 1, ""));
      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      assertTrue(executed.isEmpty(), "while the other ship is answering, our shot waits");
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      assertEquals(List.of(EncounterAction.Attack), executed, "and after the reply it fires again");

      // With a trader, the intro key deals.
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Trade),
          false, 2, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Trader", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The trader offers to deal.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false, false, 5, false, 2, "La policia te ordena someterte a una inspeccion."));
      content.resetPosition();
      executed.clear();
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      assertEquals(List.of(EncounterAction.Trade), executed, "the intro key deals with the trader");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSceneShowsTheKeysOfTheAvailableActions() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      String text = screenText(screen);
      String attack = "[A] " + spacetrader.Strings.EncounterActionAttack;
      assertTrue(text.contains(attack), "the attack key is shown:\n" + text);
      assertTrue(text.contains("[F] " + spacetrader.Strings.EncounterActionFlee), text);
      assertTrue(text.contains("[S] " + spacetrader.Strings.EncounterActionSurrender), text);
      assertEquals(screen.getTerminalSize().getRows() - 1, rowOfText(screen, attack),
          "the keys sit on the last row of the scene:\n" + text);
      assertFalse(text.contains("[T]"), "the trader key is not offered in a fight:\n" + text);
      assertFalse(text.contains("[I]"), "nor the ignore key:\n" + text);
      assertFalse(text.contains("[ENTER] continue"), "the scene is not waiting:\n" + text);

      // A new model with other actions refreshes the bar.
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Ignore, EncounterAction.Trade),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Trader", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The trader offers to deal.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 1, ""));
      gui.updateScreen();
      text = screenText(screen);
      assertTrue(text.contains("[T] " + spacetrader.Strings.EncounterActionTrade),
          "the trader key replaces the old ones:\n" + text);
      assertFalse(text.contains("[F]"), "the fight keys are gone:\n" + text);
      assertFalse(text.contains("[S]"), "and no surrender with a trader:\n" + text);

      // With no actions at all there is no bar.
      content.model(new EncounterViewModel(
          EnumSet.noneOf(EncounterAction.class),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Trader", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The trader is gone.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 2, ""));
      gui.updateScreen();
      text = screenText(screen);
      assertFalse(text.contains("["), "an empty set of actions draws no bar:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theWaitingSceneKeepsTheContinueKeyInsteadOfTheActionKeys() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();

      content.awaitLeave();
      gui.updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains("[ENTER] continue"), "the leave key is shown:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 1, rowOfText(screen, "[ENTER] continue"),
          "on the last row:\n" + text);
      assertFalse(text.contains("[A] " + spacetrader.Strings.EncounterActionAttack),
          "the bar is gone while the scene waits:\n" + text);
      assertFalse(text.contains("[S]"), "and so are its keys:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void everyActionOfTheEncounterHasItsClassicKey() {
    // Arrange: the keys of the classic text UI, the ones the player knows.
    Map<EncounterAction, Character> classic = Map.ofEntries(
        Map.entry(EncounterAction.Attack, 'a'),
        Map.entry(EncounterAction.Board, 'o'),
        Map.entry(EncounterAction.Bribe, 'b'),
        Map.entry(EncounterAction.Drink, 'd'),
        Map.entry(EncounterAction.Flee, 'f'),
        Map.entry(EncounterAction.Ignore, 'i'),
        Map.entry(EncounterAction.Interrupt, 'x'),
        Map.entry(EncounterAction.Meet, 'm'),
        Map.entry(EncounterAction.Plunder, 'p'),
        Map.entry(EncounterAction.Submit, 'u'),
        Map.entry(EncounterAction.Surrender, 's'),
        Map.entry(EncounterAction.Trade, 't'),
        Map.entry(EncounterAction.Yield, 'y'));

    // Act & Assert: the single table of the view covers the whole enum, so the
    // scene can always print the key of an action and the key always works.
    for(EncounterAction action : EncounterAction.values()) {
      assertEquals(classic.get(action), LanternaEncounterView.keyOf(action),
          action + " keeps its classic key");
    }
  }

  @Test
  void thePoliceKeysAreShownWholeAndInTheOrderOfTheEnum() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the inspection offers attack, flee, submit and bribe.
      content.model(police(you, opponent, 0));

      // Act: the last row of the scene is painted.
      gui.updateScreen();
      String row = lastScreenRow(screen);

      // Assert: one whole entry per action, in the order of the enum (bribe is
      // the second action of the encounter, so it goes before flee and submit).
      String attack = "[A] " + spacetrader.Strings.EncounterActionAttack;
      String bribe = "[B] " + spacetrader.Strings.EncounterActionBribe;
      String flee = "[F] " + spacetrader.Strings.EncounterActionFlee;
      String submit = "[U] " + spacetrader.Strings.EncounterActionSubmit;
      assertTrue(row.contains(attack), "the attack key:\n" + row);
      assertTrue(row.contains(bribe), "the bribe key:\n" + row);
      assertTrue(row.contains(flee), "the flee key:\n" + row);
      assertTrue(row.contains(submit), "the submit key:\n" + row);
      assertTrue(row.indexOf(attack) < row.indexOf(bribe), "attack before bribe:\n" + row);
      assertTrue(row.indexOf(bribe) < row.indexOf(flee), "bribe before flee:\n" + row);
      assertTrue(row.indexOf(flee) < row.indexOf(submit), "flee before submit:\n" + row);
      assertEquals(4, row.chars().filter(character -> character == '[').count(),
          "one entry per available action:\n" + row);
      assertFalse(row.contains("[T]"), "an action that is not offered is not shown:\n" + row);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theKeyPrintedOnTheBarRunsItsAction() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the inspection offers attack, flee, submit and bribe.
      view.render(police(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      // Act & Assert: pressing the key the bar prints runs that action. The key
      // is read from the scene, so the test does not repeat the mapping.
      assertPrintedKeyRuns(view, screen, executed, spacetrader.Strings.EncounterActionFlee,
          EncounterAction.Flee);
      assertPrintedKeyRuns(view, screen, executed, spacetrader.Strings.EncounterActionSubmit,
          EncounterAction.Submit);
      assertPrintedKeyRuns(view, screen, executed, spacetrader.Strings.EncounterActionBribe,
          EncounterAction.Bribe);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theInterruptKeyIsShownAndRunsWhenTheAutomaticFightIsOn() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(120, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the automatic fight adds the interrupt (the way out).
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Ignore,
              EncounterAction.Interrupt, EncounterAction.Trade),
          true, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      // Act & Assert: the bar prints the interrupt and its key stops the automatic.
      assertPrintedKeyRuns(view, screen, executed, spacetrader.Strings.EncounterActionInterrupt,
          EncounterAction.Interrupt);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aNarrowTerminalDropsWholeEntriesAndKeepsThemOffTheLog() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(64, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(64, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: four actions for the space of three, and a full log.
      content.model(police(you, opponent, 0));
      content.log(List.of("First line.", "Second line.", "Third line.", "Fourth line."));

      // Act: the scene is painted.
      gui.updateScreen();
      String row = lastScreenRow(screen);

      // Assert: the entry that does not fit whole is left out (no half entry),
      // the log keeps its own rows and nothing steps on the legend. With four
      // actions for the space of three, submit is the one left out whole.
      String bar = "[A] " + spacetrader.Strings.EncounterActionAttack + "  [B] "
          + spacetrader.Strings.EncounterActionBribe + "  [F] " + spacetrader.Strings.EncounterActionFlee;
      assertEquals(bar, row.substring(0, 40).strip(),
          "only the whole entries that fit are offered:\n" + row);
      assertFalse(row.substring(0, 40).contains("[U]"), "the submit entry is not begun:\n" + row);
      assertFalse(row.substring(40).contains("["), "the bar never steps on the legend:\n" + row);
      assertEquals(22, rowOfText(screen, "Fourth line."), "the log keeps its own rows:\n" + row);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theDecisionsAreOfferedBeforeTheInterruptWhenTheBarRunsOutOfRoom() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(70, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(70, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the automatic fight adds the interrupt to the decisions of a
      // police inspection, in a bar where the five entries do not fit (terminal
      // 70x24: scene width 46, 45 cells for the bar).
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Bribe, EncounterAction.Flee,
              EncounterAction.Interrupt, EncounterAction.Submit),
          true, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Police", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The police requests to inspect.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 0, ""));

      // Act: the last row of the scene is painted.
      gui.updateScreen();
      String row = lastScreenRow(screen);

      // Assert: the decisions keep the order of the enum and are all offered; the
      // interrupt waits at the end and, with no room left, it is the entry that
      // falls (never a decision).
      String attack = "[A] " + spacetrader.Strings.EncounterActionAttack;
      String bribe = "[B] " + spacetrader.Strings.EncounterActionBribe;
      String flee = "[F] " + spacetrader.Strings.EncounterActionFlee;
      String submit = "[U] " + spacetrader.Strings.EncounterActionSubmit;
      assertTrue(row.contains(attack), "the attack key:\n" + row);
      assertTrue(row.contains(bribe), "the bribe key:\n" + row);
      assertTrue(row.contains(flee), "the flee key:\n" + row);
      assertTrue(row.contains(submit), "the decision is not the one left out:\n" + row);
      assertTrue(row.indexOf(attack) < row.indexOf(bribe), "attack before bribe:\n" + row);
      assertTrue(row.indexOf(bribe) < row.indexOf(flee), "bribe before flee:\n" + row);
      assertTrue(row.indexOf(flee) < row.indexOf(submit), "flee before submit:\n" + row);
      assertFalse(row.substring(0, 46).contains("[X]"), "the interrupt falls first:\n" + row);
      assertEquals(4, row.substring(0, 46).chars().filter(character -> character == '[').count(),
          "one whole entry per decision, with none half-drawn:\n" + row);
      assertFalse(row.substring(46).contains("["), "the bar never steps on the legend:\n" + row);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theReactionOfTheOtherShipGoesWithItsPilot() {
    assertEquals(5, EncounterSceneComponent.reactionFrames(1), "a poor pilot is slow to react");
    assertEquals(3, EncounterSceneComponent.reactionFrames(5));
    assertEquals(1, EncounterSceneComponent.reactionFrames(9), "a good pilot reacts at once");
  }

  @Test
  void theShipTurnsAroundWhenItWithdraws() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      // A ship with its only cell at the left: normal it points right, turned around it does not.
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx..\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\n.\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(EnumSet.of(EncounterAction.Attack), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int before = columnOf(screen, 'x');

      content.move(-1, 0);
      gui.updateScreen();

      assertEquals(before + 2, columnOf(screen, 'x'), "the drawing is mirrored in place");

      // The next press away sends it gliding, still facing away (the stars sell it).
      content.move(-1, 0);
      content.tick();
      gui.updateScreen();
      assertEquals(before - 4, columnOf(screen, 'x'), "advancing away it glides, facing away");

      // A press against the glide brakes it; the next one faces it forward again.
      content.move(1, 0);
      content.tick();
      gui.updateScreen();
      assertEquals(before - 4, columnOf(screen, 'x'), "the press against the glide only stops it");

      content.move(1, 0);
      gui.updateScreen();
      assertEquals(before - 6, columnOf(screen, 'x'), "and now it faces the other one again");

      // Advancing forward is a dash: six cells on the first frame.
      content.move(1, 0);
      content.tick();
      gui.updateScreen();
      assertEquals(before, columnOf(screen, 'x'), "the dash goes forward");

      // Withdrawing all the way: the camera follows us, so the ship slides to
      // the left edge and stays there, visible, instead of leaving the scene.
      content.resetPosition();
      content.move(-1, 0);
      content.move(-1, 0);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(3, columnOf(screen, 'x'), "at the left edge the ship stays visible, facing away");
      assertFalse(content.exitedRight(), "it reached the left edge");
      for(int i = 0; i < 10; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(3, columnOf(screen, 'x'), "and it never leaves the scene");

      // A ship that flees with the other one behind also points away, even standing still.
      content.resetPosition();
      content.model(new EncounterViewModel(EnumSet.of(EncounterAction.Attack), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      gui.updateScreen();
      int facing = columnOf(screen, 'x');
      content.model(new EncounterViewModel(EnumSet.of(EncounterAction.Attack), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false,
          false, 5, true, 0, ""));
      gui.updateScreen();
      assertEquals(facing + 2, columnOf(screen, 'x'), "fleeing, the ship faces away");

      // Taking up the fight again turns the ship back to face the other one.
      content.model(new EncounterViewModel(EnumSet.of(EncounterAction.Attack), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      gui.updateScreen();
      assertEquals(facing, columnOf(screen, 'x'), "attacking again, the ship faces the other one");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theKeyThatFleesTurnsTheShipAway() throws Exception {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The game resolves the escape: we got away, and the encounter closes
      // (the close waits for the animation of the departure).
      view[0] = new LanternaEncounterView(gui, action -> {
        if(action == EncounterAction.Flee) {
          view[0].escaped();
          view[0].close();
        }
      }, () -> { }, plunder -> { });
      // An asymmetric ship, so the turn is seen: the ink lives at its left cell.
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx..\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int facing = columnOf(screen, 'x');

      // The key that flees: the game settles the escape and the ship turns
      // away where it stands (the other one, ahead, is left behind).
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      assertTrue(content.facingAway(), "the ship faces away with the flee key");
      gui.updateScreen();
      assertEquals(facing + 2, columnOf(screen, 'x'), "the drawing is mirrored in place");

      // The one that lost us is the one that leaves, through our back (the
      // right), while the camera keeps us in the scene, facing away.
      int behind = firstColumnOf(screen, 'y');
      for(int i = 0; i < 3; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("yyyyy"), "the other ship is on its way out");
      assertTrue(firstColumnOf(screen, 'y') > behind,
          "it leaves through the right (" + firstColumnOf(screen, 'y') + " > " + behind + ")");
      int out = 0;
      while(out < 40 && screenText(screen).contains("yyyyy")) {
        content.tick();
        gui.updateScreen();
        out++;
      }
      assertFalse(screenText(screen).contains("yyyyy"), "and all the way out");
      assertEquals(facing + 2, columnOf(screen, 'x'), "while we stay in the scene, facing away");

      // The window closes when the departure has been played.
      for(int i = 0; i < 60 && gui.getWindows().contains(view[0].asWindow()); i++) {
        Thread.sleep(100);
        gui.updateScreen();
        gui.getGUIThread().processEventsAndUpdate();
      }
      assertFalse(gui.getWindows().contains(view[0].asWindow()), "the window closes after the animation");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipKeepsInsideTheSceneMovingUpAndDown() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\n.\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(EnumSet.of(EncounterAction.Attack), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      content.log(List.of("The pirate attacks.", "Choose an action."));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int rest = rowOf(screen, 'x');

      content.move(0, -1);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      int top = rowOf(screen, 'x');
      assertTrue(top < rest, "the ship rises above its resting row (" + top + " < " + rest + ")");

      content.move(0, -1);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(top, rowOf(screen, 'x'), "a wall stops it at the top");

      content.move(0, 1);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      int bottom = rowOf(screen, 'x');
      int logRow = rowOfText(screen, "The pirate attacks.");
      assertTrue(bottom > top, "and then it goes down again");
      assertTrue(bottom < logRow, "and stops over the log (" + bottom + " < " + logRow + ")");

      // The vertical brake, the same as the horizontal one.
      content.move(0, -1);
      for(int i = 0; i < 2; i++) {
        content.tick();
      }
      gui.updateScreen();
      int rising = rowOf(screen, 'x');
      content.move(0, 1);
      content.tick();
      gui.updateScreen();
      assertEquals(rising, rowOf(screen, 'x'), "the press against the rise only stops it");

      content.move(0, 1);
      content.tick();
      gui.updateScreen();
      assertTrue(rowOf(screen, 'x') > rising, "and the next one sends it down");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The row where a text appears on the screen, or -1. */
  private static int rowOfText(Screen screen, String needle) {
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      StringBuilder line = new StringBuilder();
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        line.append(screen.getBackCharacter(column, row).getCharacterString());
      }
      if(line.toString().contains(needle)) {
        return row;
      }
    }
    return -1;
  }

  /** The row of the first cell with a glyph. */
  private static int rowOf(Screen screen, char glyph) {
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          return row;
        }
      }
    }
    return -1;
  }

  /** The column of the first cell with a glyph. */
  private static int columnOf(Screen screen, char glyph) {
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          return column;
        }
      }
    }
    return -1;
  }

  /** The top and bottom rows of a glyph in a column: {top, bottom}, or null. */
  private static int[] rowsOf(Screen screen, int column, char glyph) {
    int top = Integer.MAX_VALUE;
    int bottom = -1;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
        top = Math.min(top, row);
        bottom = Math.max(bottom, row);
      }
    }
    return bottom < 0 ? null : new int[] {top, bottom};
  }

  @Test
  void theInspectionScansAndThenTheCatwalkGoesOut() {
    EncounterSceneComponent content = new EncounterSceneComponent(key -> false);

    content.inspection(true);
    assertTrue(content.animating(), "the scanner runs over the ship");

    for(int i = 0; i < 16; i++) {
      content.tick();
    }
    assertTrue(content.animating(), "and then the catwalk, because they took cargo");

    for(int i = 0; i < 40; i++) {
      content.tick();
    }
    assertFalse(content.animating(), "the scene is over");

    content.inspection(false);
    for(int i = 0; i < 16; i++) {
      content.tick();
    }
    assertFalse(content.animating(), "with nothing taken there is no catwalk");
  }

  @Test
  void theCatwalkOfTheTradeGoesOutWaitsCrossesAndComesBack() {
    EncounterSceneComponent content = new EncounterSceneComponent(key -> false);

    content.catwalk();
    assertFalse(content.catwalkOut(), "the catwalk is still on its way out");
    for(int i = 0; i < 12; i++) {
      content.tick();
    }
    assertTrue(content.catwalkOut(), "out, it waits for the question");

    content.haul();
    assertFalse(content.catwalkOut(), "the boxes cross with the goods");
    for(int i = 0; i < 12; i++) {
      content.tick();
    }
    assertTrue(content.catwalkOut(), "the crossing is over and the catwalk waits again");

    content.retract();
    assertTrue(content.animating(), "the catwalk is going back in");
    for(int i = 0; i < 8; i++) {
      content.tick();
    }
    assertFalse(content.animating(), "the scene of the trade is over");
  }

  @Test
  void theCatwalkBendsDownWhenTheRivalFliesLower() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      // We climb before the catwalk (the rival does not get to mirror us): the
      // bridge has to bend down to the middle of the other ship.
      scene.content().move(0, -1);
      for(int i = 0; i < 3; i++) {
        scene.content().tick();
      }
      scene.content().catwalk();
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
      }
      scene.gui().updateScreen();

      String text = screenText(screen);
      int yourRow = rowOfText(screen, "xxxxx");
      int opponentRow = rowOfText(screen, "yyyyy");
      int cornerColumn = firstColumnOf(screen, 'y') - 1;
      assertTrue(yourRow < opponentRow, "we fly above the rival:\n" + text);
      assertTrue(text.contains("═"), "the horizontal run leaves our nose:\n" + text);
      int[] corner = boxOf(screen, '╗');
      assertNotNull(corner, "the corner where the bridge bends down:\n" + text);
      assertEquals(cornerColumn, corner[0], "the corner sits just before the hull of the rival");
      assertEquals(yourRow, corner[1], "and on our line");
      int[] leg = rowsOf(screen, cornerColumn, '║');
      assertNotNull(leg, "the vertical leg goes down to the rival:\n" + text);
      assertEquals(yourRow + 1, leg[0], "it starts under our line");
      assertEquals(opponentRow, leg[1], "and reaches the middle of the rival");

      // The boxes cross the whole path: some frame catches one on the leg.
      scene.content().haul();
      boolean onTheLeg = false;
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
        scene.gui().updateScreen();
        for(int row = leg[0]; row <= leg[1]; row++) {
          onTheLeg |= screen.getBackCharacter(cornerColumn, row).getCharacter() == '■';
        }
      }
      assertTrue(onTheLeg, "a box crosses the vertical leg:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCatwalkBendsUpWhenTheRivalFliesHigher() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      // We dive: now the rival is above and the bridge has to climb to it.
      scene.content().move(0, 1);
      for(int i = 0; i < 3; i++) {
        scene.content().tick();
      }
      scene.content().catwalk();
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
      }
      scene.gui().updateScreen();

      String text = screenText(screen);
      int yourRow = rowOfText(screen, "xxxxx");
      int opponentRow = rowOfText(screen, "yyyyy");
      int cornerColumn = firstColumnOf(screen, 'y') - 1;
      assertTrue(yourRow > opponentRow, "we fly below the rival:\n" + text);
      int[] corner = boxOf(screen, '╝');
      assertNotNull(corner, "the corner where the bridge bends up:\n" + text);
      assertEquals(cornerColumn, corner[0], "the corner sits just before the hull of the rival");
      assertEquals(yourRow, corner[1], "and on our line");
      int[] leg = rowsOf(screen, cornerColumn, '║');
      assertNotNull(leg, "the vertical leg climbs to the rival:\n" + text);
      assertEquals(opponentRow, leg[0], "it starts at the middle of the rival");
      assertEquals(yourRow - 1, leg[1], "and reaches our line");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCatwalkStaysStraightWhenBothShipsShareTheHeight() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      scene.content().catwalk();
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
      }
      scene.gui().updateScreen();

      String text = screenText(screen);
      int yourRow = rowOfText(screen, "xxxxx");
      assertTrue(text.contains("═"), "the straight bridge goes out:\n" + text);
      assertEquals(yourRow, rowOfText(screen, "═"), "at our middle");
      assertFalse(text.contains("╗") || text.contains("╝") || text.contains("║"),
          "with no bend when both fly level:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCatwalkBendsASingleStepWhenTheRivalIsOneRowAway() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 11)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      // The rival rests one row above our lane and the bridge is asked for
      // before it can mirror us (the catwalk freezes it): the L is a single
      // step, and the corner has to be exact.
      scene.content().move(0, 1);
      scene.content().catwalk();
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
      }
      scene.gui().updateScreen();

      String text = screenText(screen);
      int yourRow = rowOfText(screen, "xxxxx");
      int opponentRow = rowOfText(screen, "yyyyy");
      assertEquals(1, yourRow - opponentRow, "the ships are one row apart:\n" + text);
      int cornerColumn = firstColumnOf(screen, 'y') - 1;
      int[] corner = boxOf(screen, '╝');
      assertNotNull(corner, "the corner of the single step:\n" + text);
      assertEquals(cornerColumn, corner[0], "the corner sits just before the hull of the rival");
      assertEquals(yourRow, corner[1], "and on our line");
      int[] leg = rowsOf(screen, cornerColumn, '║');
      assertNotNull(leg, "the leg of a single cell:\n" + text);
      assertEquals(opponentRow, leg[0], "it starts at the middle of the rival");
      assertEquals(opponentRow, leg[1], "and it is a single cell high");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void thePoliceConfiscationBendsTheCatwalkToTheRival() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      // We climb before the inspection: the scanner runs while the rival
      // mirrors us, but it cannot reach our lane before the catwalk freezes it.
      scene.content().move(0, -1);
      for(int i = 0; i < 3; i++) {
        scene.content().tick();
      }
      scene.content().inspection(true);

      boolean bent = false;
      boolean onTheLeg = false;
      // The scanner runs first and the automatic catwalk takes 32 frames more:
      // the fixed loop covers it all (the catwalk starts after the scan, so
      // "gone" is not a valid stop for the first frames).
      for(int i = 0; i < 80; i++) {
        scene.content().tick();
        scene.gui().updateScreen();
        int[] corner = cornerOf(screen);
        if(corner == null) {
          continue;
        }
        bent = true;
        // The leg runs from our line to the rival's; a box over any of its
        // cells proves the automatic haul crosses it.
        int opponentRow = rowOfText(screen, "yyyyy");
        for(int row = Math.min(corner[1], opponentRow); row <= Math.max(corner[1], opponentRow); row++) {
          if(row != corner[1] && screen.getBackCharacter(corner[0], row).getCharacter() == '■') {
            onTheLeg = true;
          }
        }
      }
      assertTrue(bent, "the confiscation bends the catwalk when the rival flies at another height");
      assertTrue(onTheLeg, "a box crosses the vertical leg of the confiscation");
      assertTrue(scene.content().catwalkGone(), "the automatic catwalk goes back in");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void thePirateLootingBendsTheCatwalkOnItsOwn() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      scene.content().move(0, -1);
      for(int i = 0; i < 3; i++) {
        scene.content().tick();
      }
      scene.content().loot();
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
      }
      scene.gui().updateScreen();

      String text = screenText(screen);
      int yourRow = rowOfText(screen, "xxxxx");
      int opponentRow = rowOfText(screen, "yyyyy");
      int cornerColumn = firstColumnOf(screen, 'y') - 1;
      assertTrue(yourRow < opponentRow, "we fly above the rival:\n" + text);
      int[] corner = boxOf(screen, '╗');
      assertNotNull(corner, "the looting bends down:\n" + text);
      assertEquals(cornerColumn, corner[0], "the corner sits just before the hull");
      assertEquals(yourRow, corner[1], "and on our line");
      int[] leg = rowsOf(screen, cornerColumn, '║');
      assertNotNull(leg, "the vertical leg goes down to the rival:\n" + text);
      assertEquals(yourRow + 1, leg[0], "it starts under our line");
      assertEquals(opponentRow - 1, leg[1], "the far end is already the box of the haul");
      assertEquals('■', screen.getBackCharacter(cornerColumn, opponentRow).getCharacter(),
          "the box reaches the middle of the rival:\n" + text);

      // The boxes cross the whole path (horizontal and vertical) by themselves.
      boolean onTheLeg = false;
      for(int i = 0; i < 12; i++) {
        scene.content().tick();
        scene.gui().updateScreen();
        for(int row = yourRow + 1; row <= opponentRow; row++) {
          onTheLeg |= screen.getBackCharacter(cornerColumn, row).getCharacter() == '■';
        }
      }
      assertTrue(onTheLeg, "a box crosses the vertical leg:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The smallest box around one of the corners of the L, or null. */
  private static int[] cornerOf(Screen screen) {
    int[] down = boxOf(screen, '╗');
    return down != null ? down : boxOf(screen, '╝');
  }

  @Test
  void theBentCatwalkGrowsFromTheNoseAndShrinksFromTheFarEnd() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      ReadyScene scene = sceneWithTwoShips(screen);
      scene.content().move(0, -1);
      for(int i = 0; i < 3; i++) {
        scene.content().tick();
      }
      scene.content().catwalk();
      scene.content().tick();
      scene.gui().updateScreen();
      String text = screenText(screen);
      assertTrue(text.contains("═"), "the run leaves our nose first:\n" + text);
      assertFalse(text.contains("╗") || text.contains("╝") || text.contains("║"),
          "the leg waits for the full run:\n" + text);

      // The path only adds cells at its far end: the leg never grows without its
      // corner over it, cell by cell.
      for(int i = 0; i < 11; i++) {
        scene.content().tick();
        scene.gui().updateScreen();
        text = screenText(screen);
        int[] corner = cornerOf(screen);
        if(text.contains("║")) {
          assertNotNull(corner, "the leg grows from the corner:\n" + text);
          int[] leg = rowsOf(screen, corner[0], '║');
          assertNotNull(leg, "the leg keeps its column:\n" + text);
          assertEquals(corner[1] + 1, leg[0], "and starts right under the corner:\n" + text);
        }
      }
      assertTrue(scene.content().catwalkOut(), "the full path waits there");

      // Coming back: the far end goes first; the corner never returns once gone.
      scene.content().retract();
      scene.content().tick();
      scene.gui().updateScreen();
      assertNotNull(cornerOf(screen), "the far end is still there on the way back");
      boolean cornerGone = false;
      for(int i = 0; i < 7; i++) {
        scene.content().tick();
        scene.gui().updateScreen();
        text = screenText(screen);
        boolean cornerThere = cornerOf(screen) != null;
        if(cornerGone) {
          assertFalse(cornerThere, "the corner never comes back:\n" + text);
          assertFalse(text.contains("║"), "nor the leg:\n" + text);
        }
        cornerGone |= !cornerThere;
      }
      assertTrue(scene.content().catwalkGone(), "the path is gone");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** A scene with our ship and a rival coming in, ready for the catwalk. */
  private record ReadyScene(MultiWindowTextGUI gui, EncounterSceneComponent content) {
  }

  private static ReadyScene sceneWithTwoShips(Screen screen) throws IOException {
    MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
    gui.setTheme(LanternaTheme.create());
    EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
    content.setPreferredSize(screen.getTerminalSize());
    BasicWindow window = new BasicWindow();
    window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
    window.setComponent(content);
    gui.addWindow(window);
    ShipPicture you = new ShipCatalog(List.of(),
        ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
        .picture(ShipType.Flea, List.of(), 0);
    ShipPicture opponent = new ShipCatalog(List.of(),
        ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
        .picture(ShipType.Scorpion, List.of(), 0);
    content.model(fight(you, opponent, 0, false, false, false, 0, 0));
    // The first paint sets the width of the scene: only then do the ships come
    // in on the ticks.
    gui.updateScreen();
    for(int i = 0; i < 14; i++) {
      content.tick();
    }
    gui.updateScreen();
    return new ReadyScene(gui, content);
  }

  @Test
  void theTradeQuestionSitsAtTheBottom() throws Exception {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      BasicWindow scene = new BasicWindow();
      scene.setHints(Set.of(Window.Hint.FULL_SCREEN));
      gui.addWindow(scene);
      gui.updateScreen();

      Thread asking = new Thread(() -> LanternaDialogs.askAmountAtBottom(gui, "Comprar armas",
          "Cuántas unidades", 5));
      asking.setDaemon(true);
      asking.start();
      for(int i = 0; i < 40 && gui.getWindows().size() < 2; i++) {
        Thread.sleep(50);
      }
      gui.updateScreen();
      String text = screenText(screen);
      int row = rowOf(text, "Comprar armas");
      assertTrue(row >= 24 / 2, "the question waits at the bottom (row " + row + "):\n" + text);

      if(gui.getActiveWindow() != null) {
        gui.getActiveWindow().close();
      }
      asking.join(2000);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The row where a text appears on the screen, or -1. */
  private static int rowOf(String text, String needle) {
    int at = text.indexOf(needle);
    if(at < 0) {
      return -1;
    }
    return text.substring(0, at).split("\n", -1).length - 1;
  }

  @Test
  void thePoliceSayAllIsInOrderAfterTheScan() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(new EncounterViewModel(EnumSet.of(EncounterAction.Submit), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The police requests to inspect.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false, false, 5, false, 0,
          spacetrader.Strings.EncounterSaysPolice));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      view.inspection(false);
      // The ships come in (14 frames) and then the scan runs (16 frames).
      for(int i = 0; i < 14 + 16; i++) {
        ((EncounterSceneComponent) view.asWindow().getComponent()).tick();
      }
      gui.updateScreen();

      // The speech is wrapped: the first words have to be on one line.
      String said = spacetrader.Strings.EncounterSaysPoliceAllClear.substring(0, 20);
      assertTrue(screenText(screen).contains(said), "the police say the outcome:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aFailedEscapeKeepsBothShipsInTheScene() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(chase(you, opponent, 0, true));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int enemyBefore = firstColumnOf(screen, 'y');

      // Running away to the left: the camera follows us, so the ship reaches
      // the edge and stays there, visible (no wrap, no turn of the scene).
      content.move(-1, 0);
      content.move(-1, 0);
      for(int i = 0; i < 40; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("x"), "the ship stays in the scene, at the edge");
      assertFalse(content.exitedRight(), "it reached the left edge");

      // The game keeps the chase: the scene does not loop; the other ship
      // closes in and both keep going in the same scene.
      content.model(chase(you, opponent, 1, true));
      gui.updateScreen();
      assertTrue(screenText(screen).contains("x"), "we are still in the scene");
      assertTrue(screenText(screen).contains("y"), "and so is the other ship");
      assertTrue(firstColumnOf(screen, 'y') < enemyBefore,
          "the chase brings it closer (" + firstColumnOf(screen, 'y') + " < " + enemyBefore + ")");
      assertTrue(firstColumnOf(screen, 'x') < 20,
          "we stay at the left edge, not coming back in from the right");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSkyFollowsTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // A star in the middle of the sky, so it does not wrap in one step.
      java.util.List<org.gts.bst.view.Starfield.Star> sorted =
          new java.util.ArrayList<>(content.sky().stars());
      sorted.sort(java.util.Comparator.comparingDouble(org.gts.bst.view.Starfield.Star::x));
      int index = content.sky().stars().indexOf(sorted.get(sorted.size() / 2));

      org.gts.bst.view.Starfield.Star before = content.sky().stars().get(index);
      content.tick();
      assertEquals(before.x() - before.speed(), content.sky().stars().get(index).x(), 1e-9,
          "going forward, the sky drifts left");

      content.turnAway();
      before = content.sky().stars().get(index);
      content.tick();
      assertEquals(before.x() + before.speed(), content.sky().stars().get(index).x(), 1e-9,
          "and turning back, it drags the other way");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void slippingPastTheOtherShipAsksForTheEscape() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      // A slow pilot (1) does not close the gap in time to stop the dodge.
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 1, false, 0, ""));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, to slip past the other one, and then advancing forward: the ship
      // dodges it, reaches the right edge and that is an escape attempt. The
      // camera follows us, so it stays in the scene.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      for(int i = 0; i < 30; i++) {
        content.tick();
      }
      gui.updateScreen();

      assertEquals(List.of(EncounterAction.Flee), executed, "slipping past is an escape attempt");
      assertTrue(content.exitedRight(), "it reached the right edge");
      assertTrue(screenText(screen).contains("x"), "and the ship stays in the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void advancingAwayIsAnEscapeAttempt() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx....\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int shipBefore = columnOf(screen, 'x');

      // The first press away is only the half turn: the ship holds its place.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      gui.updateScreen();
      assertTrue(executed.isEmpty(), "the half turn asks nothing of the game");
      assertEquals(shipBefore + 4, columnOf(screen, 'x'), "the ship turns away where it stands");

      // The next one sends it away: reaching the edge is an escape attempt.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      for(int i = 0; i < 40; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(List.of(EncounterAction.Flee), executed, "reaching the edge asks to flee");
      assertTrue(screenText(screen).contains("x"), "and the camera keeps us in the scene");
      assertFalse(content.exitedRight(), "we reached the left edge");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void reachingTheEdgeAsksForTheEscapeOnce() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> exits = new ArrayList<>();
      content.onExit(() -> exits.add("edge"));
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int before = firstColumnOf(screen, 'x');

      // The half turn and the advance: reaching the left edge asks once, and
      // the ship waits there, visible.
      content.move(-1, 0);
      content.move(-1, 0);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(1, exits.size(), "reaching the edge asks for the escape once");
      assertTrue(screenText(screen).contains("x"), "and the ship stays in the scene");

      // It does not ask again on the ticks that follow.
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      assertEquals(1, exits.size(), "the notice does not repeat while the ship waits at the edge");

      // Back inside the scene the notice is re-armed: a new withdrawal can ask
      // for another escape (the first press brakes the dash, the next turns).
      content.move(1, 0);
      content.move(1, 0);
      for(int i = 0; i < 2; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(firstColumnOf(screen, 'x') > before,
          "the dash goes back into the scene (" + firstColumnOf(screen, 'x') + " > " + before + ")");
      content.move(-1, 0);
      content.move(-1, 0);
      content.move(-1, 0);
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      assertEquals(2, exits.size(), "back inside, a new withdrawal asks for another escape");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aSuccessfulEscapeKeepsUsInTheSceneAndSeesTheEnemyOff() throws Exception {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The game resolves the escape: we got away, and the encounter closes
      // (the close waits for the animation of the departure).
      view[0] = new LanternaEncounterView(gui, action -> {
        if(action == EncounterAction.Flee) {
          view[0].escaped();
          view[0].close();
        }
      }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(chase(you, opponent, 0, true));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The half turn and the advance away: at the left edge the escape is
      // resolved and the other ship, the one behind us, is the one that leaves.
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      int frames = 0;
      while(frames < 40 && screenText(screen).contains("yyyyy")) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertFalse(screenText(screen).contains("yyyyy"), "the other ship is gone");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      int ourColumn = firstColumnOf(screen, 'x');
      assertTrue(ourColumn < 20, "at the left edge (" + ourColumn + ")");

      // The window closes when the departure has been played.
      for(int i = 0; i < 60 && gui.getWindows().contains(view[0].asWindow()); i++) {
        Thread.sleep(100);
        gui.updateScreen();
        gui.getGUIThread().processEventsAndUpdate();
      }
      assertFalse(gui.getWindows().contains(view[0].asWindow()), "the window closes after the animation");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aShipTooWideForItsHalfDoesNotAskToFleeFromTheSpawn() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      // A ship wider than half the scene (the real Flea at 80x24) sits at the
      // left edge from the spawn: parked there it must not ask for anything.
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\n" + "x".repeat(44) + "\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      // The bug fired on the first tick after the entry: give it a few more.
      for(int i = 0; i < 5; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(executed.isEmpty(), "a wide ship parked at the edge does not ask to flee by itself");
      assertTrue(screenText(screen).contains("x"), "and it is still in the scene");

      // The half turn and then the dash (to the edge it is already on): now the
      // escape is asked, and the camera keeps the ship in the scene.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      for(int i = 0; i < 3; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(List.of(EncounterAction.Flee), executed, "the dash to the edge asks to flee");
      assertTrue(screenText(screen).contains("x"), "and the ship stays in the scene");
      assertFalse(content.exitedRight(), "it is the left edge");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theRealWideShipsDoNotAskToFleeFromTheSpawn() throws IOException {
    // The original repro: real art at 80x24, where the cropped Flea (44) and
    // Wasp (54) are wider than their half (27) and spawn parked on the left edge.
    for(ShipType type : List.of(ShipType.Flea, ShipType.Wasp)) {
      Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
      screen.startScreen();
      try {
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        gui.setTheme(LanternaTheme.create());
        List<EncounterAction> executed = new ArrayList<>();
        LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
        ShipPicture you = ShipCatalog.shared().picture(type, List.of(), 0);
        ShipPicture opponent = new ShipCatalog(List.of(),
            ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
            .picture(ShipType.Scorpion, List.of(), 0);
        assertTrue(you.cropped().width() > 27, type + " is wider than its half at 80x24");
        view.render(fight(you, opponent, 0, false, false, false, 0, 0));
        gui.addWindow(view.asWindow());
        gui.updateScreen();
        EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
        for(int i = 0; i < 14 + 5; i++) {
          content.tick();
        }
        gui.updateScreen();
        assertTrue(executed.isEmpty(),
            type + " parked at the edge does not ask to flee by itself: " + executed);

        // The real dash (the half turn and away) does ask, at the edge it is on.
        view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
        view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
        for(int i = 0; i < 3; i++) {
          content.tick();
        }
        gui.updateScreen();
        assertEquals(List.of(EncounterAction.Flee), executed, type + " asks to flee on the real dash");
        assertFalse(content.exitedRight(), type + " dashes to its left edge");
      } finally {
        screen.stopScreen();
        screen.close();
      }
    }
  }

  @Test
  void aBlockedDashDoesNotChangeTheSideOfTheEscape() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Forward, into the other ship (parked in the middle): the dash bumps and
      // stops before the right edge, so it must not settle the side of anything.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("yyyyy"), "the other ship blocks the way");
      assertFalse(content.exitedRight(), "a blocked dash never reached the right edge");
      int ourColumn = firstColumnOf(screen, 'x');

      // The escape is resolved with no dash of its own (the key): the other
      // ship is behind us, to the right, and that is the side it must leave.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      assertEquals(List.of(EncounterAction.Flee), executed, "the key asks to flee");
      view.escaped();
      int behind = firstColumnOf(screen, 'y');
      for(int i = 0; i < 3; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("yyyyy"), "the other ship is on its way out");
      assertTrue(firstColumnOf(screen, 'y') > behind,
          "it leaves through the right (" + firstColumnOf(screen, 'y') + " > " + behind + ")");
      int out = 0;
      while(out < 40 && screenText(screen).contains("yyyyy")) {
        content.tick();
        gui.updateScreen();
        out++;
      }
      assertFalse(screenText(screen).contains("yyyyy"), "and all the way out");
      assertEquals(ourColumn, firstColumnOf(screen, 'x'), "while we stay in the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aFailedEscapeBringsTheOtherShipCloser() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx....\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(chase(you, opponent, 0, false));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int enemyBefore = firstColumnOf(screen, 'y');
      int facing = columnOf(screen, 'x');

      // A failed escape (the game keeps the chase): the other one closes in and
      // the ship faces away with it (the same half turn as running away).
      content.model(chase(you, opponent, 1, true));
      gui.updateScreen();
      assertTrue(content.facingAway(), "the failed flee leaves the ship facing away");
      assertEquals(facing + 4, columnOf(screen, 'x'), "and the drawing is mirrored in place");
      int closer = firstColumnOf(screen, 'y');
      assertTrue(closer < enemyBefore, "the other ship gains ground (" + closer + " < " + enemyBefore + ")");

      content.model(chase(you, opponent, 2, true));
      gui.updateScreen();
      assertTrue(firstColumnOf(screen, 'y') < closer, "and closer again with the next round");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aSuccessfulEscapePastTheOtherShipSeesItOffToTheLeft() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The game resolves the escape: we got away (the close is proved elsewhere).
      view[0] = new LanternaEncounterView(gui, action -> {
        if(action == EncounterAction.Flee) {
          view[0].escaped();
        }
      }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(chase(you, opponent, 0, true));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, to slip past the other one, and then advancing: the ship reaches
      // the right edge and the game resolves the escape.
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      int frames = 0;
      while(frames < 40 && !content.exitedRight()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertTrue(content.exitedRight(), "we reached the right edge");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      assertTrue(screenText(screen).contains("yyyyy"), "with the other ship still behind us");

      // It is the other ship that leaves now, through our back (the left).
      int behind = firstColumnOf(screen, 'y');
      for(int i = 0; i < 3; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("yyyyy"), "it is on its way out");
      int leaving = firstColumnOf(screen, 'y');
      assertTrue(leaving < behind,
          "it goes away through the left (" + leaving + " < " + behind + ")");
      int out = 0;
      while(out < 40 && screenText(screen).contains("yyyyy")) {
        content.tick();
        gui.updateScreen();
        out++;
      }
      assertFalse(screenText(screen).contains("yyyyy"), "and all the way out");
      assertTrue(screenText(screen).contains("x"), "while we never leave the scene");
      assertTrue(firstColumnOf(screen, 'x') > 40,
          "waiting at the right edge (" + firstColumnOf(screen, 'x') + ")");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theEscapePastTheOtherShipDoesNotTurnTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      // An asymmetric ship, so a turn would be seen: the ink lives at its left cell.
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx....\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, to slip past the other one, and then advancing: the ship reaches
      // the right edge with the other one now behind it.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      int frames = 0;
      while(frames < 40 && !content.exitedRight()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertTrue(content.exitedRight(), "we reached the right edge");
      assertEquals(List.of(EncounterAction.Flee), executed, "the edge asks to flee");
      assertFalse(content.facingAway(), "the ship already faced the way it runs");
      int atTheEdge = columnOf(screen, 'x');

      // The flee key settles the escape (the game gives it for good): the ship
      // goes on facing the way it runs. It must not turn: the other one is
      // behind, and turning would show it heading back into the chase.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      view.escaped();
      gui.updateScreen();
      assertFalse(content.facingAway(), "the escape past the other one must not turn the ship");
      assertEquals(atTheEdge, columnOf(screen, 'x'), "the drawing is not mirrored in place");

      // The one that lost us leaves through our back (the left).
      int behind = firstColumnOf(screen, 'y');
      for(int i = 0; i < 3; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(firstColumnOf(screen, 'y') < behind,
          "it leaves through the left (" + firstColumnOf(screen, 'y') + " < " + behind + ")");
      int out = 0;
      while(out < 40 && screenText(screen).contains("yyyyy")) {
        content.tick();
        gui.updateScreen();
        out++;
      }
      assertFalse(screenText(screen).contains("yyyyy"), "and all the way out");
      assertEquals(atTheEdge, columnOf(screen, 'x'), "while we stay in the scene, facing forward");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aFailedEscapePastTheOtherShipKeepsEveryoneInTheScene() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(chase(you, opponent, 0, true));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, to slip past the other one, and then advancing to the right edge.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      int frames = 0;
      while(frames < 40 && executed.isEmpty()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(List.of(EncounterAction.Flee), executed, "reaching the right edge asks to flee");
      assertTrue(content.exitedRight(), "we are at the right edge");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      assertTrue(screenText(screen).contains("yyyyy"), "with the other ship behind us");

      // A failed escape (the game keeps the chase): the scene does not loop and
      // the other one closes in for the next round.
      int enemyBefore = firstColumnOf(screen, 'y');
      content.model(chase(you, opponent, 1, true));
      gui.updateScreen();
      assertTrue(firstColumnOf(screen, 'y') > enemyBefore,
          "the other ship gains ground behind us (" + firstColumnOf(screen, 'y') + " > " + enemyBefore + ")");
      assertTrue(screenText(screen).contains("x"), "we stay in the scene, at the edge");
      assertTrue(screenText(screen).contains("yyyyy"), "and so does the other one");

      int edge = firstColumnOf(screen, 'x');
      for(int i = 0; i < 30; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("x"), "we keep waiting at the edge, visible");
      assertTrue(screenText(screen).contains("yyyyy"), "with the other one closing in");
      assertEquals(edge, firstColumnOf(screen, 'x'), "and we never come back in from the other side");
      assertEquals(List.of(EncounterAction.Flee), executed, "the edge asks once, not every frame");
      assertTheShipsNeverTouch(screen);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void pressingForwardAtTheRightEdgeDoesNotMoveTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> exits = new ArrayList<>();
      content.onExit(() -> exits.add("edge"));
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, and then forward to the right edge while the other one crosses.
      content.move(0, -1);
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      int frames = 0;
      while(frames < 60 && exits.isEmpty()) {
        content.move(1, 0);
        for(int i = 0; i < 10 && exits.isEmpty(); i++) {
          content.tick();
          frames++;
        }
        gui.updateScreen();
      }
      assertEquals(1, exits.size(), "reaching the right edge asks for the escape");
      assertTrue(content.exitedRight(), "we are at the right edge");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      int edge = lastColumnOf(screen, 'x');
      assertTrue(edge > 40, "the ship is at the right edge (" + edge + ")");

      // Pressing forward again, glued to the edge, neither moves us nor asks again.
      content.move(1, 0);
      for(int i = 0; i < 3; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(edge, lastColumnOf(screen, 'x'), "pressing forward at the edge does not move the ship");
      assertEquals(1, exits.size(), "and does not ask for the escape again");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void reachingTheRightEdgeAsksForTheEscapeAgainAfterGoingBackInside() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> exits = new ArrayList<>();
      content.onExit(() -> exits.add("edge"));
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Up, and then forward to the right edge while the other one crosses.
      content.move(0, -1);
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      int frames = 0;
      while(frames < 60 && exits.isEmpty()) {
        content.move(1, 0);
        for(int i = 0; i < 10 && exits.isEmpty(); i++) {
          content.tick();
          frames++;
        }
        gui.updateScreen();
      }
      assertEquals(1, exits.size(), "reaching the right edge asks for the escape");
      int edge = lastColumnOf(screen, 'x');

      // Turn away and glide back inside: the notice is re-armed there.
      content.move(-1, 0);
      content.move(-1, 0);
      int inside = 0;
      while(inside < 20 && lastColumnOf(screen, 'x') >= edge) {
        content.tick();
        gui.updateScreen();
        inside++;
      }
      assertTrue(lastColumnOf(screen, 'x') < edge, "we are back inside the scene");

      // Brake, face forward and dash to the right edge again.
      content.move(1, 0);
      content.move(1, 0);
      content.move(1, 0);
      int again = 0;
      while(again < 60 && exits.size() < 2) {
        content.move(1, 0);
        for(int i = 0; i < 10 && exits.size() < 2; i++) {
          content.tick();
          again++;
        }
        gui.updateScreen();
      }
      assertEquals(2, exits.size(), "back inside, the right edge asks for another escape");
      assertTrue(content.exitedRight(), "we are at the right edge again");
      assertTrue(screenText(screen).contains("x"), "and still in the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void fleeingPastTheShipThatIgnoresUsIgnoresItAndSeesItOff() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(160, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(ignoring(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the other ship is crossing");

      // Up, and then forward until the right edge, while the other one crosses.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowUp));
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowRight));
      int frames = 0;
      while(frames < 80 && executed.isEmpty()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(List.of(EncounterAction.Ignore), executed, "reaching the edge ignores the encounter");
      assertTrue(content.exitedRight(), "we reached the right edge");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      assertTrue(screenText(screen).contains("y"), "while the other ship was still crossing");

      // It is the other ship that leaves, and the crossing must not ignore the
      // encounter a second time when it finishes.
      int out = 0;
      while(out < 80 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        out++;
      }
      assertFalse(screenText(screen).contains("y"), "the other ship leaves the scene");
      assertTrue(screenText(screen).contains("x"), "and we stay visible");
      for(int i = 0; i < 20; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertEquals(List.of(EncounterAction.Ignore), executed, "the crossing does not ignore it twice");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void pressingBackAtTheLeftEdgeDoesNotMoveTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> exits = new ArrayList<>();
      content.onExit(() -> exits.add("edge"));
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The half turn and the advance away: the ship reaches the left edge.
      content.move(-1, 0);
      content.move(-1, 0);
      int frames = 0;
      while(frames < 30 && exits.isEmpty()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(1, exits.size(), "reaching the left edge asks for the escape");
      assertFalse(content.exitedRight(), "we are at the left edge");
      assertTrue(screenText(screen).contains("x"), "and we stay in the scene");
      int edge = firstColumnOf(screen, 'x');

      // Pressing back again, glued to the edge, neither moves us nor asks again.
      content.move(-1, 0);
      for(int i = 0; i < 3; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(edge, firstColumnOf(screen, 'x'), "pressing back at the edge does not move the ship");
      assertEquals(1, exits.size(), "and does not ask for the escape again");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSceneWaitingForThePlayerNeverLosesTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> exits = new ArrayList<>();
      content.onExit(() -> exits.add("edge"));
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The scene shows a result and waits for the player; the manoeuvres keep
      // working. Withdrawing to the edge must not send the ship off the screen.
      content.awaitLeave();
      content.move(-1, 0);
      content.move(-1, 0);
      int frames = 0;
      while(frames < 30 && exits.isEmpty()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(1, exits.size(), "reaching the left edge asks for the escape");
      assertFalse(content.exitedRight(), "we are at the left edge");
      assertTrue(screenText(screen).contains("x"), "the ship waits at the edge, visible");
      int edge = firstColumnOf(screen, 'x');
      for(int i = 0; i < 10; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertEquals(edge, firstColumnOf(screen, 'x'), "and it never slides off the screen");
      assertEquals(1, exits.size(), "nor asks for the escape again");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** A round of a chase (or of the fight) over the ships of the test. */
  private static EncounterViewModel chase(ShipPicture you, ShipPicture opponent, int round, boolean fleeing) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
        false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
        "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
        "The pirate attacks.", "Choose an action.",
        ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        false, 5, fleeing, round, "");
  }

  @Test
  void theOtherShipLeavesTheSceneWhenItLosesYou() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      // An asymmetric ship, so the turn is seen: the ink lives at its left cell.
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx...\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("yyyyy"), "the other ship is in the scene");
      int facing = columnOf(screen, 'x');

      content.turnAway();
      content.opponentLeaves();
      assertTrue(content.animating(), "and now it is leaving");
      gui.updateScreen();
      assertEquals(facing + 3, columnOf(screen, 'x'),
          "our ship goes on facing away while the other falls behind");

      for(int i = 0; i < 12; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertFalse(content.animating(), "it is gone");
      assertFalse(screenText(screen).contains("yyyyy"), "out through its side:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipThatIgnoresUsSlipsOutOfOurWay() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Ignore), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police ignores.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          true, 5, false, 0, ""));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int enemyRow = rowOf(screen, 'y');

      // We go above it and advance: it slips away downwards and crosses without
      // stopping, while we slip past it without bumping.
      content.move(0, -1);
      content.move(1, 0);
      int frames = 0;
      boolean dodged = false;
      while(frames < 40 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        if(screenText(screen).contains("y")) {
          dodged |= rowOf(screen, 'y') > enemyRow;
        }
        frames++;
      }
      assertTrue(dodged, "the one that ignores us dodges away from us (" + enemyRow + ")");
      assertFalse(screenText(screen).contains("y"), "it crosses the scene and leaves:\n" + screenText(screen));
      assertTrue(frames <= 20, "and it does it quickly (" + frames + " frames)");
      assertTrue(screenText(screen).contains("x"),
          "we slip past it without bumping and stay in the scene:\n" + screenText(screen));
      assertTrue(firstColumnOf(screen, 'x') > 40,
          "the dash crosses the whole scene (" + firstColumnOf(screen, 'x') + ")");

      // If we attack it, it is done with ignoring us: it comes back for us.
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee), false, 1, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 1, ""));
      for(int i = 0; i < 4; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "and it comes back when the fight starts");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCallbackOfTheGoneOpponentFiresOnceAndOnlyForTheCrossing() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> notices = new ArrayList<>();
      content.onOpponentGone(() -> notices.add("gone"));
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the ship comes in and starts crossing");
      assertTrue(notices.isEmpty(), "nothing is announced while it is there");

      int frames = 0;
      while(frames < 40 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        if(screenText(screen).contains("y")) {
          assertTrue(notices.isEmpty(), "still crossing at frame " + frames);
        }
        frames++;
      }
      assertFalse(screenText(screen).contains("y"), "it crosses the scene and leaves:\n" + screenText(screen));
      assertEquals(1, notices.size(), "the scene is told once, when it is gone");

      for(int i = 0; i < 5; i++) {
        content.tick();
      }
      assertEquals(1, notices.size(), "and only once");

      // A ship that loses us also leaves, but that is the close of the chase:
      // the notice of the crossing must not fire for it.
      content.model(fight(you, opponent, 1, false, false, false, 0, 0));
      content.opponentLeaves();
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      assertEquals(1, notices.size(), "losing us is not the crossing of the one that ignores us");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipThatIgnoresUsChangesLaneInsteadOfStalling() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Ignore), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police ignores.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          true, 5, false, 0, ""));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int rest = rowOf(screen, 'y');

      // We rest on the same band and it picks its lane; then we take that lane:
      // instead of stalling in front of us, it veers to the opposite band, keeps
      // its way and leaves the scene.
      content.tick();
      content.move(0, 1);
      int frames = 0;
      int lastRow = rest;
      while(frames < 80 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        if(screenText(screen).contains("y")) {
          lastRow = rowOf(screen, 'y');
        }
        frames++;
      }
      assertFalse(screenText(screen).contains("y"),
          "it goes all the way out instead of stalling in front of us:\n" + screenText(screen));
      assertTrue(frames <= 20, "and it does it quickly (" + frames + " frames)");
      assertTrue(lastRow < rest,
          "changing band on the way (" + lastRow + " < " + rest + ")");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipThatIgnoresUsVeersDownWhenWeTakeItsLane() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int rest = rowOf(screen, 'y');

      // We drop first, so it picks the upper band; then we brake, climb to that
      // same band and stay there: the mirror of the other test, it has to veer
      // down and leave instead of stalling in front of us.
      content.move(0, 1);
      content.tick();
      gui.updateScreen();
      content.move(0, -1);
      content.move(0, -1);
      int frames = 0;
      int lastRow = rest;
      int lastColumn = firstColumnOf(screen, 'y');
      int blocked = 0;
      while(frames < 80 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        assertTheShipsNeverTouch(screen);
        if(screenText(screen).contains("y")) {
          lastRow = rowOf(screen, 'y');
          int column = firstColumnOf(screen, 'y');
          if(lastRow == rowOf(screen, 'x') && column >= lastColumn) {
            blocked++;
            assertTrue(blocked <= 3,
                "it never stalls in our row with its way blocked (" + blocked + " frames):\n" + screenText(screen));
          } else {
            blocked = 0;
          }
          lastColumn = column;
        }
        frames++;
      }
      assertFalse(screenText(screen).contains("y"),
          "it goes all the way out instead of stalling in front of us:\n" + screenText(screen));
      assertTrue(frames <= 24, "and it does it quickly (" + frames + " frames)");
      assertTrue(lastRow > rest, "veering to the band below (" + lastRow + " > " + rest + ")");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipThatIgnoresUsCrossesWhileWeStayStill() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int ourColumn = columnOf(screen, 'x');

      // We never move: it picks its band, crosses at speed and leaves.
      int frames = 0;
      while(frames < 40 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        assertTheShipsNeverTouch(screen);
        assertEquals(ourColumn, columnOf(screen, 'x'), "we never move");
        frames++;
      }
      assertFalse(screenText(screen).contains("y"),
          "it crosses the scene and leaves:\n" + screenText(screen));
      assertTrue(frames <= 20, "and it does it quickly (" + frames + " frames)");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aShipThatIgnoresUsLeavesTheOtherWay() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx....\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(chase(you, opponent, 0, false));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int before = firstColumnOf(screen, 'y');

      // It has no interest in us and we dodge it: it leaves through our back.
      content.opponentLeaves(false);
      content.tick();
      gui.updateScreen();
      assertTrue(firstColumnOf(screen, 'y') < before,
          "it goes away through the left (" + firstColumnOf(screen, 'y') + " < " + before + ")");

      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertFalse(screenText(screen).contains("yyyyy"), "all the way out");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theEncounterEndsWhenTheIgnoringShipIsGone() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The presenter closes the encounter as soon as the action is over.
      view[0] = new LanternaEncounterView(gui, action -> {
        executed.add(action);
        view[0].close();
      }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(ignoring(you, opponent, 0));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(executed.isEmpty(), "the ship is still crossing");

      int frames = 0;
      while(frames < 40 && gui.getWindows().contains(view[0].asWindow())) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(List.of(EncounterAction.Ignore), executed,
          "once it is gone, the encounter ignores it by itself");
      assertFalse(gui.getWindows().contains(view[0].asWindow()), "and the window is closed");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aSurrenderingShipStaysAndKeepsTheDecisionOpen() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The presenter closes the encounter as soon as the action is over.
      view[0] = new LanternaEncounterView(gui, action -> {
        executed.add(action);
        view[0].close();
      }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(surrendering(you, opponent, 0));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the surrendered ship is in front of us");
      int waiting = firstColumnOf(screen, 'y');

      // It stays in its half instead of crossing away: the surrender leaves
      // attack/plunder on the table and the scene waits for the player.
      for(int i = 0; i < 80; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("y"),
          "the surrendered ship stays in the scene:\n" + screenText(screen));
      assertEquals(waiting, firstColumnOf(screen, 'y'), "it keeps its place, it does not cross");
      assertTrue(executed.isEmpty(), "a surrender is not ignored by itself");
      assertTrue(gui.getWindows().contains(view[0].asWindow()),
          "the player can still attack or plunder it");

      // The mirror: it copies our manoeuvres at its own pace.
      int atRest = rowOf(screenText(screen), "yyyyy");
      content.move(0, -1);
      for(int i = 0; i < 40; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(rowOf(screenText(screen), "yyyyy") < atRest,
          "the surrendered ship copies our climb:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aPirateLootingShowsTheCatwalkAndThenWaitsForThePlayer() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The pirates take cargo: the catwalk goes out with the boxes crossing,
      // and then the scene waits for the player, even if the presenter closes it.
      view.looted(true);
      view.close();
      gui.updateScreen();
      assertTrue(gui.getWindows().contains(view.asWindow()),
          "the scene waits for the player after the looting");
      assertTrue(screenText(screen).contains("[ENTER] continue"),
          "the leave key is shown:\n" + screenText(screen));

      boolean catwalk = false;
      boolean boxes = false;
      for(int i = 0; i < 40; i++) {
        content.tick();
        gui.updateScreen();
        catwalk |= screenText(screen).contains("═");
        boxes |= screenText(screen).contains("■");
      }
      assertTrue(catwalk, "the catwalk goes out for the looting:\n" + screenText(screen));
      assertTrue(boxes, "and the boxes cross it:\n" + screenText(screen));
      assertTrue(content.catwalkGone(), "the looting is played out");

      // The player leaves with enter (escape and flying away do the same).
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      assertFalse(gui.getWindows().contains(view.asWindow()), "the player leaves the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theLootLineIsLoggedUnderTheShips() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // While the scene waits after the looting, the loot is listed in the log.
      String loot = spacetrader.Functions.StringVars(spacetrader.Strings.EncounterPiratesTake,
          "2 " + spacetrader.Consts.TradeItems.get(0).Name());
      view.looted(true);
      view.log(loot);
      view.close();
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(gui.getWindows().contains(view.asWindow()), "the scene waits for the player");
      assertTrue(text.contains(loot), "the loot is in the log:\n" + text);
      int lootRow = rowOfText(screen, loot);
      int shipRow = rowOfText(screen, "yyyyy");
      assertTrue(lootRow > shipRow, "under the ships (" + lootRow + " > " + shipRow + "):\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2, lootRow,
          "and at the bottom of the scene:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theLootLineStaysInTheLogWhileTheSceneWaits() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      String loot = spacetrader.Functions.StringVars(spacetrader.Strings.EncounterPiratesTake,
          "2 " + spacetrader.Consts.TradeItems.get(0).Name());
      view.looted(true);
      view.log(loot);
      view.close();
      // The catwalk of the looting comes out and back while the scene waits for
      // the player: the line has to survive the whole animation.
      for(int i = 0; i < 40; i++) {
        content.tick();
        gui.updateScreen();
      }

      String text = screenText(screen);
      assertTrue(gui.getWindows().contains(view.asWindow()), "the scene still waits for the player");
      assertTrue(text.contains(loot), "the loot line stays in the log:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2, rowOfText(screen, loot),
          "and keeps its place at the bottom of the scene:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theBlackmailAlertWrapsWholeInTheLog() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      gui.addWindow(view.asWindow());
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();

      // The pirates that find no cargo blackmail us: the alert of the game (the
      // same one quietTo sends to the log of the encounter) is long. The tests
      // run the English bundle; the tail the author saw cut off was
      // "patrimonio actual: <amount>" (here, "current worth").
      String amount = spacetrader.Functions.Multiples(22750, spacetrader.Strings.MoneyUnit);
      String blackmail = spacetrader.Functions.StringVars(
          Alerts.get(spacetrader.enums.AlertType.EncounterPiratesFindNoCargo).message(), amount);
      view.log(blackmail);
      gui.updateScreen();

      String text = screenText(screen);
      int head = rowOfText(screen, "The pirates are very angry");
      int tail = rowOfText(screen, "your current worth - " + amount + ".");
      assertTrue(head >= 0, "the head of the blackmail is in the log:\n" + text);
      assertTrue(tail >= 0, "and its tail is not cut off any more:\n" + text);
      assertTrue(tail >= head + 2,
          "the message falls in several rows (" + head + ".." + tail + "):\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, head).getForegroundColor(),
          "the alert keeps its colour:\n" + text);

      // The last row of the screen belongs to the keys of the actions: the
      // wrapped alert ends right over it.
      assertEquals(screen.getTerminalSize().getRows() - 2, tail, "the tail ends over the key bar:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("current worth"), "the alert never steps on the keys:\n" + bar);

      // And when the scene waits for the player, the leave key keeps the last
      // row while the whole message stays in the log.
      view.looted(false);
      gui.updateScreen();
      text = screenText(screen);
      assertEquals(screen.getTerminalSize().getRows() - 1, rowOfText(screen, "[ENTER] continue"),
          "the leave key keeps the last row:\n" + text);
      assertTrue(text.contains("your current worth - " + amount + "."),
          "the whole blackmail stays in the log:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aLongLogLineWrapsWholeInTheLog() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(police(you, opponent, 0));

      // A long line of the log of the encounter (the text of the round): it is
      // wrapped over the rows, keeps the colour of the log and never steps on
      // the keys of the last row.
      String line = "The police requests to inspect and the pirate decides to run while the scanner sweeps the sky.";
      content.log(List.of(line));
      gui.updateScreen();

      String text = screenText(screen);
      int head = rowOfText(screen, "The police requests to inspect and");
      int tail = rowOfText(screen, "the scanner sweeps the sky.");
      assertTrue(head >= 0, "the head of the line is in the log:\n" + text);
      assertTrue(tail > head, "the line falls in several rows (" + head + ".." + tail + "):\n" + text);
      assertEquals(UiPalette.TEXT, screen.getBackCharacter(1, head).getForegroundColor(),
          "the log keeps its colour:\n" + text);
      assertTrue(tail < screen.getTerminalSize().getRows() - 1, "the line ends over the key bar:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("scanner"), "the log never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aVeryLongWordWrapsByRowsWithoutLosingItsTail() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: a line with no spaces to cut at (the log is 54 wide here:
      // scene 56, the two cells left to the legend), with marked chunks.
      String head = "A".repeat(54);
      String middle = "B".repeat(54);
      String tail = "TAIL";
      content.model(police(you, opponent, 0));
      content.log(List.of(head + middle + tail));

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the word is hard-cut row by row, keeps the colour of the log
      // and never steps on the keys.
      String text = screenText(screen);
      int first = rowOfText(screen, head);
      int second = rowOfText(screen, middle);
      int last = rowOfText(screen, tail);
      assertTrue(first >= 0, "the head of the word is in the log:\n" + text);
      assertEquals(first + 1, second, "the middle falls on the next row:\n" + text);
      assertEquals(second + 1, last, "and the tail is not lost:\n" + text);
      assertEquals(UiPalette.TEXT, screen.getBackCharacter(1, first).getForegroundColor(),
          "the log keeps its colour:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains(tail), "the wrapped word never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theNewestAlertsKeepTheirRowsAndTheOldestFallOut() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: a burst of three long alerts with no log lines, the case of a
      // surrender that pays, hides the princess and has a reactor on board. The
      // first one needs three rows (54 + 54 + 46), the second two and the third
      // two: there are only four rows for all of them.
      String tailOne = "-one";
      String tailTwo = "-two";
      String tailThree = "-three";
      content.model(police(you, opponent, 0));
      content.addAlert("A".repeat(150) + tailOne);
      content.addAlert("B".repeat(60) + tailTwo);
      content.addAlert("C".repeat(60) + tailThree);

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the rows go to the newest alerts (the oldest scroll out whole),
      // and none is half drawn over the keys of the last row.
      String text = screenText(screen);
      assertTrue(rowOfText(screen, "A".repeat(54)) < 0, "the oldest alert scrolls out:\n" + text);
      assertTrue(rowOfText(screen, tailOne) < 0, "with its tail too:\n" + text);
      assertEquals(19, rowOfText(screen, "B".repeat(54)), "the second alert opens the log:\n" + text);
      assertEquals(20, rowOfText(screen, tailTwo), "and ends whole on the next row:\n" + text);
      assertEquals(21, rowOfText(screen, "C".repeat(54)), "the newest alert follows:\n" + text);
      assertEquals(22, rowOfText(screen, tailThree), "and ends whole over the key bar:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains(tailTwo) || bar.contains(tailThree), "no alert steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aLongAlertWrapsWholeOnANarrowTerminal() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(60, 20));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: a quiet alert of 118 cells on a 60x20 terminal: the scene is 40
      // wide there (36 would fit in the screen but the legend is kept), so the
      // alert must fall in four rows of 38, all the rows the log has there.
      String message = "The collector of the port demands a payment of five thousand credits before the sun sets and the hold is almost empty.";
      content.model(police(you, opponent, 0));
      content.addAlert(message);

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: head and tail read on different rows, with the colour of the
      // alerts, and the last row keeps the keys.
      String text = screenText(screen);
      int head = rowOfText(screen, "The collector of the port");
      int tail = rowOfText(screen, "almost empty.");
      assertTrue(head >= 0, "the head of the alert is in the log:\n" + text);
      assertTrue(tail > head, "the alert falls in several rows (" + head + ".." + tail + "):\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, head).getForegroundColor(),
          "the alert keeps its colour:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2, tail, "the tail ends over the key bar:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("almost empty"), "the alert never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSurrenderBlackmailReadsWholeOverTheRoundLog() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      gui.addWindow(view.asWindow());

      // Arrange: the real flow of the surrender, as the presenter runs it. The
      // round is rendered first (three rows of log: the round text, an empty
      // line and the action text) and no render comes between the surrender
      // and its quiet alert.
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      String amount = spacetrader.Functions.Multiples(22750, spacetrader.Strings.MoneyUnit);
      String blackmail = spacetrader.Functions.StringVars(
          Alerts.get(spacetrader.enums.AlertType.EncounterPiratesFindNoCargo).message(), amount);

      // Act: the blackmail of the pirates that find no cargo arrives.
      view.log(blackmail);
      gui.updateScreen();

      // Assert: the newest rows win the room: the log of the round scrolls out
      // and the whole blackmail reads, its tail with the amount included, over
      // the keys.
      String text = screenText(screen);
      int head = rowOfText(screen, "The pirates are very angry");
      int tail = rowOfText(screen, "your current worth - " + amount + ".");
      assertTrue(head >= 0, "the head of the blackmail is in the log:\n" + text);
      assertTrue(tail >= 0, "and its tail with the amount is not cut off any more:\n" + text);
      assertTrue(tail >= head + 2, "the message falls in several rows (" + head + ".." + tail + "):\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, head).getForegroundColor(),
          "the alert keeps its colour:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2, tail, "the tail ends over the key bar:\n" + text);
      assertTrue(rowOfText(screen, "The pirate attacks.") < 0, "the round log scrolls out:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("pirates"), "the blackmail never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSurrenderBlackmailShowsItsAmountOnANarrowTerminal() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      gui.addWindow(view.asWindow());
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      String amount = spacetrader.Functions.Multiples(22750, spacetrader.Strings.MoneyUnit);
      String blackmail = spacetrader.Functions.StringVars(
          Alerts.get(spacetrader.enums.AlertType.EncounterPiratesFindNoCargo).message(), amount);
      view.log(blackmail);

      // Act: the scene is painted on the narrow terminal.
      gui.updateScreen();

      // Assert: the scene is 40 wide there, so the blackmail needs six rows of
      // 38 and only its newest four fit; the head has to scroll out, but the
      // tail the player has to read (the amount) keeps the last rows, whole
      // over the keys.
      String text = screenText(screen);
      String worth = "your current worth - " + amount.split(" ")[0];
      String unit = amount.split(" ")[1] + ".";
      assertTrue(rowOfText(screen, "The pirates are very angry") < 0,
          "the head of the blackmail has to scroll out here:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 3, rowOfText(screen, worth),
          "the worth of the tail reads over the key bar:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2, rowOfText(screen, unit),
          "and the amount is not cut:\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, screen.getTerminalSize().getRows() - 3)
          .getForegroundColor(), "the alert keeps its colour:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains(unit), "the alert never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void anEmptyLineStillTakesItsRowInTheLog() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the blank line the view puts between the round text and the
      // action text.
      content.model(police(you, opponent, 0));
      content.log(List.of("First line.", "", "Third line."));

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the blank row is consumed, not collapsed.
      String text = screenText(screen);
      assertEquals(19, rowOfText(screen, "First line."), "the first line takes the first row:\n" + text);
      assertEquals(21, rowOfText(screen, "Third line."), "the empty line still takes its row:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void anAlertLongerThanTheWholeLogKeepsItsTail() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: one alert of 283 cells, six rows at the 54 of the log: two
      // more than the four rows there are, so the head must scroll out.
      content.model(police(you, opponent, 0));
      content.addAlert("The pirates demand " + "plunder ".repeat(30) + "or your hull goes first.");

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the oldest rows are the ones dropped, so the tail of the alert
      // (the part the player must read) ends whole over the keys and every
      // visible row keeps the colour of the alerts.
      String text = screenText(screen);
      assertTrue(rowOfText(screen, "The pirates demand") < 0, "the head scrolled out:\n" + text);
      assertEquals(screen.getTerminalSize().getRows() - 2,
          rowOfText(screen, "or your hull goes first."), "the tail reads over the key bar:\n" + text);
      for(int row = 19; row <= 22; row++) {
        assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, row).getForegroundColor(),
            "the row " + row + " keeps the colour of the alerts:\n" + text);
      }
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("hull"), "the alert never steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void manyShortAlertsShowOnlyTheNewestOnes() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: six quiet alerts of one row each, more than the four rows of
      // the log.
      content.model(police(you, opponent, 0));
      for(int i = 1; i <= 6; i++) {
        content.addAlert("Notice " + i + ".");
      }

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: only the newest four stay, in order, over the keys.
      String text = screenText(screen);
      assertTrue(rowOfText(screen, "Notice 1.") < 0, "the oldest alert scrolled out:\n" + text);
      assertTrue(rowOfText(screen, "Notice 2.") < 0, "and the second one too:\n" + text);
      assertEquals(19, rowOfText(screen, "Notice 3."), "the third opens the log:\n" + text);
      assertEquals(20, rowOfText(screen, "Notice 4."), "the fourth follows:\n" + text);
      assertEquals(21, rowOfText(screen, "Notice 5."), "the fifth follows:\n" + text);
      assertEquals(22, rowOfText(screen, "Notice 6."), "the newest ends over the key bar:\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, 19).getForegroundColor(),
          "the alerts keep their colour:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
      assertFalse(bar.contains("Notice"), "no alert steps on the keys:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theNewestRowKeepsTheOnlyRowOfATinyScene() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 6)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 6));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: a scene of six rows leaves one single row for the log (row
      // four, over the key bar): the newest line must be the one that stays.
      content.model(police(you, opponent, 0));
      content.log(List.of("First line.", "", "Third line."));

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the last line takes the only row, the oldest is dropped.
      String text = screenText(screen);
      assertEquals(4, rowOfText(screen, "Third line."), "the newest line keeps the row:\n" + text);
      assertTrue(rowOfText(screen, "First line.") < 0, "and the oldest is dropped:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theRoundLogKeepsTheTopWhenEverythingFits() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);

      // Arrange: the three rows of the round and one alert of a single row:
      // four rows, exactly the room of the log, so nothing scrolls.
      content.model(police(you, opponent, 0));
      content.log(List.of("The pirate attacks.", "", "Choose an action."));
      content.addAlert("The cargo is ours.");

      // Act: the scene is painted.
      gui.updateScreen();

      // Assert: the layout does not change when everything fits: the round
      // keeps the top (in the colour of the log) and the alert closes the log
      // (in the colour of the alerts), over the keys.
      String text = screenText(screen);
      assertEquals(19, rowOfText(screen, "The pirate attacks."), "the round opens the log:\n" + text);
      assertEquals(21, rowOfText(screen, "Choose an action."), "and keeps its empty row:\n" + text);
      assertEquals(22, rowOfText(screen, "The cargo is ours."), "the alert closes the log:\n" + text);
      assertEquals(UiPalette.TEXT, screen.getBackCharacter(1, 19).getForegroundColor(),
          "the round keeps the colour of the log:\n" + text);
      assertEquals(UiPalette.ACCENT, screen.getBackCharacter(1, 22).getForegroundColor(),
          "the alert keeps its colour:\n" + text);
      String bar = lastScreenRow(screen);
      assertTrue(bar.contains("[A]"), "the action keys keep the last row:\n" + bar);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aPirateLootingWithNoCargoWaitsWithoutTheCatwalk() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The pirates took nothing (the blackmail): the scene waits for the
      // player, but no catwalk goes out and no box crosses.
      view.looted(false);
      view.close();
      gui.updateScreen();
      assertTrue(gui.getWindows().contains(view.asWindow()), "the scene waits for the player");
      assertTrue(screenText(screen).contains("[ENTER] continue"),
          "the leave key is shown:\n" + screenText(screen));

      // No catwalk goes out during the whole wait (checking every frame: a
      // catwalk that comes out and goes back in is still a catwalk).
      boolean catwalk = false;
      boolean boxes = false;
      for(int i = 0; i < 40; i++) {
        content.tick();
        gui.updateScreen();
        catwalk |= screenText(screen).contains("═");
        boxes |= screenText(screen).contains("■");
      }
      assertFalse(catwalk, "no catwalk without cargo:\n" + screenText(screen));
      assertFalse(boxes, "and no boxes cross it");

      // Escape leaves the scene too.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Escape));
      assertFalse(gui.getWindows().contains(view.asWindow()), "the player leaves the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aPirateLootingWaitsAndFlyingAwayLeavesTheScene() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The scene waits after the looting: the keys of decision no longer fight.
      view.looted(true);
      view.close();
      gui.updateScreen();
      for(int i = 0; i < 60 && !content.catwalkGone(); i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(content.catwalkGone(), "the looting is played out");
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('a', false, false));
      gui.updateScreen();
      assertTrue(gui.getWindows().contains(view.asWindow()), "the scene is still waiting");
      assertTrue(executed.isEmpty(), "the wait does not fight: " + executed);

      // Flying away (dashing to the edge) is the third way out: it leaves
      // without asking the game for another action.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      for(int i = 0; i < 60 && gui.getWindows().contains(view.asWindow()); i++) {
        content.tick();
        gui.updateScreen();
      }
      assertFalse(gui.getWindows().contains(view.asWindow()), "flying away leaves the scene");
      assertTrue(executed.isEmpty(), "and does not fire an action: " + executed);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void thePoliceDemandingSurrenderStaysInFrontWaiting() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(policeSurrendering(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the police is in front of us:\n" + screenText(screen));
      assertTrue(
          screenText(screen).contains(spacetrader.Strings.EncounterSaysPoliceArrest.substring(0, 20)),
          "the police demands our surrender:\n" + screenText(screen));
      int waiting = firstColumnOf(screen, 'y');

      // The demand is not an ignore: the police waits in front for our answer
      // (attack, flee or surrender) instead of crossing away.
      for(int i = 0; i < 80; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertTrue(screenText(screen).contains("y"),
          "the police stays in the scene:\n" + screenText(screen));
      assertEquals(waiting, firstColumnOf(screen, 'y'), "it keeps its place, it does not cross");
      assertTrue(executed.isEmpty(), "the demand is not ignored by itself");
      assertTrue(gui.getWindows().contains(view.asWindow()), "the encounter waits for our decision");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theEdgeInASurrenderedPoliceAsksToFleeInsteadOfIgnoring() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(policeSurrendering(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The demand is not an ignore: slipping away from it is an escape attempt
      // (the flee of the game), not an automatic ignore of the encounter.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      for(int i = 0; i < 40; i++) {
        content.tick();
      }
      gui.updateScreen();

      assertEquals(List.of(EncounterAction.Flee), executed,
          "reaching the edge of the demand asks to flee, not to ignore");
      assertFalse(content.exitedRight(), "we reached the left edge");
      assertTrue(screenText(screen).contains("x"), "and the camera keeps us in the scene");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void aCloakedPlayerKeepsItsDecisionWhenTheOpponentCrossesAway() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      // Cloaked: the other ship does not see us and crosses away, but attack,
      // flee and surrender stay on the table (there is no ignore to fall back on).
      view.render(cloaked(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the ship comes in and starts crossing");

      int frames = 0;
      while(frames < 40 && screenText(screen).contains("y")) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertFalse(screenText(screen).contains("y"),
          "it crosses the scene and leaves:\n" + screenText(screen));
      assertTrue(executed.isEmpty(), "the crossing did not ignore the encounter by itself");
      assertTrue(gui.getWindows().contains(view.asWindow()), "the encounter waits for our decision");

      // The decision is still ours: the attack key runs the attack.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('a', false, false));
      assertEquals(List.of(EncounterAction.Attack), executed, "the attack is still on the table");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void attackingWhileItCrossesKeepsTheEncounterOpen() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(ignoring(you, opponent, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view.asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "it is still crossing");

      // We fire at it: the game resolves the round and it is done ignoring us.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(' ', false, false));
      assertEquals(List.of(EncounterAction.Attack), executed, "space fires at the crossing ship");
      view.render(fight(you, opponent, 1, false, false, false, 0, 0));
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the ship comes back for the fight");
      assertTrue(gui.getWindows().contains(view.asWindow()), "the encounter is still open");
      assertEquals(List.of(EncounterAction.Attack), executed, "no automatic ignore after attacking");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void queuedFramesAfterTheAutomaticCloseDoNotIgnoreAgain() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      int[] closes = {0};
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The presenter closes the encounter as soon as the action is over.
      view[0] = new LanternaEncounterView(gui, action -> {
        executed.add(action);
        view[0].close();
      }, () -> { }, plunder -> { });
      view[0].onClose(() -> closes[0]++);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(ignoring(you, opponent, 0));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      int frames = 0;
      while(frames < 40 && gui.getWindows().contains(view[0].asWindow())) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(List.of(EncounterAction.Ignore), executed, "the crossing ends the encounter");
      assertEquals(1, closes[0], "and the window closes once");
      assertFalse(gui.getWindows().contains(view[0].asWindow()));

      // The star timer can leave frames queued, and the close can be asked
      // again: neither may ignore the encounter twice nor close it again.
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      view[0].close();
      view[0].close();
      gui.updateScreen();
      assertEquals(List.of(EncounterAction.Ignore), executed, "late frames must not ignore it twice");
      assertEquals(1, closes[0], "nor close the window again");
      assertFalse(gui.getWindows().contains(view[0].asWindow()));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void thePlayerLeavingOnItsOwnIgnoresOnlyOnce() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView[] view = new LanternaEncounterView[1];
      // The presenter closes the encounter as soon as the action is over.
      view[0] = new LanternaEncounterView(gui, action -> {
        executed.add(action);
        view[0].close();
      }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view[0].render(ignoring(you, opponent, 0));
      gui.addWindow(view[0].asWindow());
      gui.updateScreen();
      EncounterSceneComponent content = (EncounterSceneComponent) view[0].asWindow().getComponent();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // The half turn and then the advance away: the ship leaves through its
      // left side and the scene asks the game for the escape (an ignore).
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      view[0].asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.ArrowLeft));
      int frames = 0;
      while(frames < 30 && executed.isEmpty()) {
        content.tick();
        gui.updateScreen();
        frames++;
      }
      assertEquals(List.of(EncounterAction.Ignore), executed, "leaving past it ignores the encounter");

      // The ship that ignores us goes on crossing while the scene closes: its
      // departure must not ignore the encounter a second time.
      for(int i = 0; i < 30; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertEquals(List.of(EncounterAction.Ignore), executed, "and it is not ignored twice");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theGoneNoticeDoesNotFireWhileTheShipComesIn() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> notices = new ArrayList<>();
      content.onOpponentGone(() -> notices.add("gone"));
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();

      for(int i = 0; i < 14; i++) {
        content.tick();
        gui.updateScreen();
        assertTrue(notices.isEmpty(), "the ship is still coming in at frame " + i);
      }
      assertTrue(notices.isEmpty(), "coming in is not crossing the scene");
      assertTrue(screenText(screen).contains("yyyyy"), "it is there, facing the crossing");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void attackingBeforeItLeavesCancelsTheGoneNotice() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      List<String> notices = new ArrayList<>();
      content.onOpponentGone(() -> notices.add("gone"));
      content.model(ignoring(you, opponent, 0));
      gui.updateScreen();
      for(int i = 0; i < 20; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "it is crossing");

      // The player fires and the game calls it back: the crossing is over, so
      // the notice must never arrive.
      content.model(fight(you, opponent, 1, true, false, false, 0, 0));
      for(int i = 0; i < 30; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(screenText(screen).contains("y"), "the ship is back for the fight");
      assertTrue(notices.isEmpty(), "the fight cancels the notice of the crossing");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theSpeechLeavesWithTheShip() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nx....\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee), false, 0, "Flea",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 0, "Routine inspection, stop your engines"));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      int before = columnOf(screen, 'R');

      // Losing us, it leaves: the speech goes out with it (no bubble at the edge).
      content.opponentLeaves();
      for(int i = 0; i < 3; i++) {
        content.tick();
      }
      gui.updateScreen();
      int after = columnOf(screen, 'R');
      assertTrue(after > before, "the bubble slides out with the ship (" + before + " -> " + after + ")");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theShipsNeverTouch() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // Advancing: the wall of the other ship stops ours before touching it.
      content.move(1, 0);
      for(int i = 0; i < 40; i++) {
        content.tick();
      }
      gui.updateScreen();

      int lastX = lastColumnOf(screen, 'x');
      int firstY = firstColumnOf(screen, 'y');
      assertTrue(firstY - lastX - 1 >= 2,
          "they stop with a gap between them (" + (firstY - lastX - 1) + " cells)");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The last column with a glyph. */
  private static int lastColumnOf(Screen screen, char glyph) {
    int last = -1;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          last = column;
        }
      }
    }
    return last;
  }

  /** The first column with a glyph. */
  private static int firstColumnOf(Screen screen, char glyph) {
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          return column;
        }
      }
    }
    return -1;
  }

  /** The smallest box around a glyph on the screen: {left, top, right, bottom}, or null. */
  private static int[] boxOf(Screen screen, char glyph) {
    int left = Integer.MAX_VALUE;
    int top = Integer.MAX_VALUE;
    int right = -1;
    int bottom = -1;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          left = Math.min(left, column);
          top = Math.min(top, row);
          right = Math.max(right, column);
          bottom = Math.max(bottom, row);
        }
      }
    }
    return right < 0 ? null : new int[] {left, top, right, bottom};
  }

  /**
   * The drawings never touch: on every frame some axis keeps the gap the scene
   * promises (MIN_GAP = 2), so no cell of one ship lands over the other.
   */
  private static void assertTheShipsNeverTouch(Screen screen) {
    int[] you = boxOf(screen, 'x');
    int[] opponent = boxOf(screen, 'y');
    if(you == null || opponent == null) {
      return;
    }
    int gapX = Math.max(you[0] - (opponent[2] + 1), opponent[0] - (you[2] + 1));
    int gapY = Math.max(you[1] - (opponent[3] + 1), opponent[1] - (you[3] + 1));
    assertTrue(gapX >= 2 || gapY >= 2,
        "the ships never touch (gapX " + gapX + ", gapY " + gapY + "):\n" + screenText(screen));
  }

  @Test
  void theBlinkingPiecesBlinkWithTheClock() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 28)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(100, 28));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      // The badge of the police blinks (red over blue): with the blink off it hides.
      ShipPicture you = ShipCatalog.shared().picture(ShipType.Gnat, List.of("Role Police"), 15);
      ShipPicture opponent = ShipCatalog.shared().picture(ShipType.Wasp, List.of(), 20).mirrored();
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee), false, 0, "Gnat",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police attacks.",
          "Choose an action.", ShipType.Gnat, ShipType.Wasp, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 0, ""));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      // The colours of the badge, as the piece file paints them.
      org.gts.bst.view.ShipArtFile badge = ShipCatalog.shared().piece("Role Police");
      String glyph = new String(Character.toChars(firstGlyph(badge)));
      TextColor shape = org.gts.bst.view.ShipColors.color(badge.color());
      TextColor behind = org.gts.bst.view.ShipColors.color(badge.bgColor());
      assertTrue(badgesOver(screen, glyph, behind) > 0, "the blink is on: the shape over its colour");
      assertEquals(0, badgesOver(screen, glyph, shape), "and nothing swapped yet");

      for(int i = 0; i < 3; i++) {
        content.tick();
      }
      gui.updateScreen();
      assertTrue(badgesOver(screen, glyph, shape) > 0, "the blink is off: shape and background swap");
      assertEquals(0, badgesOver(screen, glyph, behind), "and the other way around");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The glyphs of the screen painted over a background colour. */
  private static int badgesOver(Screen screen, String glyph, TextColor background) {
    int count = 0;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        TextCharacter character = screen.getBackCharacter(column, row);
        if(character.getCharacterString().equals(glyph) && character.getBackgroundColor().equals(background)) {
          count++;
        }
      }
    }
    return count;
  }

  /** The first glyph of the art of a piece. */
  private static int firstGlyph(org.gts.bst.view.ShipArtFile piece) {
    for(int row = 0; row < piece.height(); row++) {
      for(int column = 0; column < piece.width(); column++) {
        int codePoint = piece.at(row, column);
        if(codePoint != ' ' && codePoint != org.gts.bst.view.ShipArtFile.CONTINUATION) {
          return codePoint;
        }
      }
    }
    return '?';
  }

  @Test
  void theLegendListsThePiecesOfBothShips() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 28)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(100, 28));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nyyyyy\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nzzzzz\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee), false, 0, "Gnat",
          new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
          new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate attacks.",
          "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
          false, 5, false, 0, "", List.of("Engine", "Cockpit"), List.of("Pulse Laser", "Energy Shield"), 15, 8));
      gui.updateScreen();
      String text = screenText(screen);

      assertTrue(text.contains("Engine"), "the engine of the player:\n" + text);
      assertTrue(text.contains("Cockpit"), text);
      assertTrue(text.contains("Pulse Laser"), text);
      assertTrue(text.contains("Energy Shield"), text);
      assertTrue(text.contains("\u29ef"), "the glyph of the engine, as it is painted:\n" + text);
      assertTrue(text.contains(spacetrader.Strings.EncounterLegendCargo), text);
      String gauge = org.gts.bst.view.ShipSites.gauge(15).substring(0, 1);
      assertTrue(text.contains(gauge), "the braille of the cargo, one dot per bay:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void myShotsAreGreenTheirsRedAndTheHitsBurst() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.updateScreen();
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();

      // My shot hits: a green beam, the hull of the other ship bursting and cracking.
      content.model(fight(you, opponent, 1, true, true, false, 5, 0));
      gui.updateScreen();
      assertTrue(hasColor(screen, TextColor.ANSI.GREEN_BRIGHT), "my shot is green");
      int pieces = debrisGlyphs(screen);
      assertTrue(pieces >= 2, "the hit bursts into pieces (" + pieces + ")");
      assertTrue(countGlyph(screen, 'y') < 15,
          "the rival cracks (its cells turn into debris): " + countGlyph(screen, 'y'));

      // Their reply lands on me: the beam is red.
      content.model(fight(you, opponent, 2, false, false, true, 0, 3));
      for(int i = 0; i < 5; i++) {
        content.tick();
      }
      gui.updateScreen();
      int red = countColor(screen, TextColor.ANSI.RED_BRIGHT);
      assertTrue(red >= 10, "their shot is red (" + red + " cells)");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static EncounterViewModel fight(ShipPicture you, ShipPicture opponent, int round, boolean youAttacked,
      boolean youHit, boolean oppHit, int youDamage, int oppDamage) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
        false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
        "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
        "The pirate attacks.", "Choose an action.",
        ShipType.Flea, ShipType.Scorpion, youHit, oppHit, youDamage, oppDamage, you, opponent, false, youAttacked,
        false, 5, false, round, "");
  }

  /** A round of a ship that ignores us (it crosses the scene). */
  private static EncounterViewModel ignoring(ShipPicture you, ShipPicture opponent, int round) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Ignore), false, 0, "Flea",
        new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
        new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The police ignores.",
        "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        true, 5, false, round, "");
  }

  /**
   * A round of a ship that surrenders: it stays in front, waiting, and the
   * decision (attack or plunder it) stays open.
   */
  private static EncounterViewModel surrendering(ShipPicture you, ShipPicture opponent, int round) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Plunder), false, 0, "Flea",
        new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Pirate",
        new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100), "The pirate surrenders.",
        "Choose an action.", ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        false, 5, false, round, "");
  }

  /** A round of the police demanding our surrender: it stays in front, waiting. */
  private static EncounterViewModel policeSurrendering(ShipPicture you, ShipPicture opponent, int round) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender), false, 0, "Flea",
        new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
        new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
        "The police demands your surrender.", "Choose an action.",
        ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        false, 5, false, round, spacetrader.Strings.EncounterSaysPoliceArrest);
  }

  /**
   * A round of a cloaked player: the other ship does not see us and crosses away,
   * but the decision (attack, flee or surrender) stays open, with no ignore.
   */
  private static EncounterViewModel cloaked(ShipPicture you, ShipPicture opponent, int round) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
        false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
        "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
        "The pirate attacks.", "Choose an action.",
        ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        true, 5, false, round, "");
  }

  /** A round of a police inspection: attack, flee, submit or bribe. */
  private static EncounterViewModel police(ShipPicture you, ShipPicture opponent, int round) {
    return new EncounterViewModel(
        EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Submit, EncounterAction.Bribe),
        false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100), "Police",
        new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
        "The police requests to inspect.", "Choose an action.",
        ShipType.Flea, ShipType.Scorpion, false, false, 0, 0, you, opponent, false, false,
        false, 5, false, round, "");
  }

  /** How many times a glyph is on the screen. */
  private static int countGlyph(Screen screen, char glyph) {
    int count = 0;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if(screen.getBackCharacter(column, row).getCharacter() == glyph) {
          count++;
        }
      }
    }
    return count;
  }

  /** True when a colour is on the screen. */
  private static boolean hasColor(Screen screen, TextColor color) {
    return countColor(screen, color) > 0;
  }

  private static int countColor(Screen screen, TextColor color) {
    int count = 0;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        TextCharacter character = screen.getBackCharacter(column, row);
        if(character.getCharacter() != ' ' && character.getForegroundColor() == color) {
          count++;
        }
      }
    }
    return count;
  }

  /** The pieces of a burst on the screen. */
  private static int debrisGlyphs(Screen screen) {
    int count = 0;
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        if("*\u00b7+".indexOf(screen.getBackCharacter(column, row).getCharacter()) >= 0) {
          count++;
        }
      }
    }
    return count;
  }

  @Test
  void aKeyDuringTheEntryStopsItAndAnswers() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Surrender),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Pirate", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 0, ""));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      assertFalse(screenText(screen).contains("xxxxx"), "the ships are still coming in");

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      gui.updateScreen();

      assertEquals(List.of(EncounterAction.Flee), executed, "the key answers while they come in");
      assertTrue(screenText(screen).contains("xxxxx"), "the entry stops at once:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theTraderThanksWhenTheDealIsDone() throws Exception {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Trade),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Trader", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The trader offers to deal.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 0, ""));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      // The player answers the question of the trade with 2 units.
      Thread player = new Thread(() -> {
        for(int i = 0; i < 1500 && gui.getWindows().size() < 2; i++) {
          try {
            Thread.sleep(20);
          } catch(InterruptedException e) {
            return;
          }
        }
        Window dialog = gui.getActiveWindow();
        if(dialog != null && dialog != view.asWindow()) {
          if(dialog.getFocusedInteractable() instanceof TextBox) {
            ((TextBox) dialog.getFocusedInteractable()).setText("2");
          }
          dialog.handleInput(new KeyStroke(KeyType.Enter));
        }
      });
      player.setDaemon(true);
      player.start();

      Integer qty = view.askCargoBuyQuantity(new CargoBuyOffer(0, CargoBuyOp.BuyTrader, 120, 3));
      assertEquals(2, qty);
      gui.updateScreen();

      // The trader thanks on its ship (the first words have to be on one line).
      String thanks = spacetrader.Strings.EncounterSaysTradeThanks.substring(0, 12);
      assertTrue(screenText(screen).contains(thanks), "the trader thanks:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theTradeLeavesTheSceneWaitingForThePlayer() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      List<EncounterAction> executed = new ArrayList<>();
      LanternaEncounterView view = new LanternaEncounterView(gui, executed::add, () -> { }, plunder -> { });
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(new EncounterViewModel(
          EnumSet.of(EncounterAction.Attack, EncounterAction.Flee, EncounterAction.Trade),
          false, 0, "Flea", new EncounterViewModel.Bar(100, 100), new EncounterViewModel.Bar(0, 100),
          "Trader", new EncounterViewModel.Bar(50, 100), new EncounterViewModel.Bar(100, 100),
          "The trader offers to deal.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 0, ""));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      assertEquals(List.of(EncounterAction.Trade), executed, "the intro key deals");

      // The presenter closes the encounter when the trade is over: the scene waits.
      view.close();
      gui.updateScreen();
      assertTrue(gui.getWindows().contains(view.asWindow()), "the scene waits for the player");

      // And the player leaves with the intro key when they are ready.
      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(KeyType.Enter));
      gui.updateScreen();
      assertFalse(gui.getWindows().contains(view.asWindow()), "the player leaves with intro");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theCloseWaitsForADialogOfTheEncounter() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      gui.addWindow(view.asWindow());
      BasicWindow dialog = new BasicWindow("Question");
      gui.addWindow(dialog);
      gui.updateScreen();

      view.close();
      gui.updateScreen();

      assertTrue(gui.getWindows().contains(view.asWindow()),
          "the encounter does not close under a dialog of its own");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theStarsOfTheBackgroundMove() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN));
      window.setComponent(content);
      gui.addWindow(window);
      gui.updateScreen();

      String before = braille(screen);
      content.tick();
      gui.updateScreen();
      String after = braille(screen);

      assertFalse(before.isEmpty(), "the sky is drawn in braille");
      assertNotEquals(before, after, "the stars move with the clock");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theGameHeaderStaysVisibleDuringTheEncounter() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      content.commander(game::Commander);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      content.log(List.of("First line.", "Second line.", "Third line.", "Fourth line."));
      gui.updateScreen();
      String text = screenText(screen);

      // The header opens the scene with the values of the commander.
      assertEquals(0, rowOfText(screen, "Antonio"), "the name opens the header:\n" + text);
      String hull = spacetrader.Functions.StringVars(spacetrader.Strings.MainHull,
          "" + game.Commander().getShip().getHull(), "" + game.Commander().getShip().HullStrength());
      int hullRow = rowOfText(screen, hull);
      assertTrue(hullRow > 0, "the live hull of the header is shown (" + hull + "):\n" + text);
      int separator = headerSeparatorRow(screen);
      assertTrue(separator > hullRow, "the separator closes the header:\n" + text);

      // The bars, the legend and the log start under the header; the keys stay at the bottom.
      assertEquals(separator + 1, rowOfText(screen, "casco ████████"),
          "the bars start under the header:\n" + text);
      assertTrue(rowOfText(screen, spacetrader.Strings.EncounterLegend) > separator,
          "the legend starts under the header:\n" + text);
      assertTrue(rowOfText(screen, "First line.") > separator, text);
      assertEquals(screen.getTerminalSize().getRows() - 2, rowOfText(screen, "Fourth line."),
          "the log ends right over the action bar:\n" + text);
      for(int i = 0; i < 14; i++) {
        content.tick();
      }
      gui.updateScreen();
      text = screenText(screen);
      assertTrue(rowOf(screen, 'x') > separator, "the ships start under the header:\n" + text);
      String attack = "[A] " + spacetrader.Strings.EncounterActionAttack;
      assertEquals(screen.getTerminalSize().getRows() - 1, rowOfText(screen, attack),
          "the action bar is still on the last row:\n" + text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theHeaderOfTheSceneShowsTheLiveHull() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(80, 24));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      content.commander(game::Commander);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      Ship ship = game.Commander().getShip();
      ship.setHull(ship.HullStrength());
      gui.updateScreen();
      String full = spacetrader.Functions.StringVars(spacetrader.Strings.MainHull,
          "" + ship.getHull(), "" + ship.HullStrength());
      assertTrue(screenText(screen).contains(full), screenText(screen));

      // A hit in combat lowers the hull: the header of the scene shows it at once.
      ship.setHull(1);
      gui.updateScreen();
      String damaged = spacetrader.Functions.StringVars(spacetrader.Strings.MainHull,
          "1", "" + ship.HullStrength());
      assertTrue(screenText(screen).contains(damaged),
          "the header follows the live hull (" + damaged + "):\n" + screenText(screen));
      assertFalse(screenText(screen).contains(full), "the old value is gone:\n" + screenText(screen));
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theHeaderOfTheSceneClosesRightUnderItsFields() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(80, 24)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaEncounterView view = new LanternaEncounterView(gui, action -> { }, () -> { }, plunder -> { });
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      view.header(game::Commander);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      view.render(fight(you, opponent, 0, false, false, false, 0, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();
      String text = screenText(screen);

      // The commander fields close the first line and the ship ones take the
      // second: the separator goes right under them.
      spacetrader.Commander cmdr = game.Commander();
      assertEquals(0, rowOfText(screen, cmdr.Name()), text);
      assertEquals(0, rowOfText(screen, spacetrader.Functions.StringVars(spacetrader.Strings.MainDay,
          "" + cmdr.getDays())), text);
      assertEquals(0, rowOfText(screen, spacetrader.Functions.FormatMoney(cmdr.getCash())), text);
      String fuel = spacetrader.Functions.StringVars(spacetrader.Strings.MainFuel,
          "" + cmdr.getShip().getFuel(), "" + cmdr.getShip().FuelTanks());
      assertEquals(1, rowOfText(screen, fuel), text);
      int separator = headerSeparatorRow(screen);
      assertEquals(2, separator, "the separator closes the header right under its fields:\n" + text);
      assertEquals(separator + 1, rowOfText(screen, "casco"),
          "the bars start under the separator:\n" + text);

      // The legend starts under the header: it never steps on it.
      int legendColumn = screen.getTerminalSize().getColumns() - 24;
      for(int row = 0; row <= separator; row++) {
        assertNotEquals('│', screen.getBackCharacter(legendColumn, row).getCharacter(),
            "the legend never steps on the header (row " + row + "):\n" + text);
      }
      assertTrue(rowOfText(screen, spacetrader.Strings.EncounterLegend) > separator, text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theHeaderOfTheSceneTakesTwoLinesOnANarrowTerminal() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(60, 20));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      content.commander(game::Commander);
      assertEquals(2, HeaderBar.height(60, game.Commander()),
          "the commander and the ship fields need one line each at 60 columns");
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));
      content.log(List.of("First line.", "Second line.", "Third line.", "Fourth line."));
      gui.updateScreen();
      String text = screenText(screen);

      // The two lines of the header and its separator push the scene down; the
      // log and the action keys stay anchored at the bottom.
      assertEquals(0, rowOfText(screen, "Antonio"), text);
      String fuel = spacetrader.Functions.StringVars(spacetrader.Strings.MainFuel,
          "" + game.Commander().getShip().getFuel(), "" + game.Commander().getShip().FuelTanks());
      assertEquals(1, rowOfText(screen, fuel), text);
      assertEquals(2, headerSeparatorRow(screen), "the separator closes the header:\n" + text);
      assertEquals(3, rowOfText(screen, "casco"), "the bars start under the separator:\n" + text);
      assertTrue(rowOfText(screen, spacetrader.Strings.EncounterLegend) > 2, text);
      assertEquals(screen.getTerminalSize().getRows() - 2, rowOfText(screen, "Fourth line."), text);
      assertEquals(screen.getTerminalSize().getRows() - 1,
          rowOfText(screen, "[A] " + spacetrader.Strings.EncounterActionAttack), text);

      // The legend column starts under the header, so it never steps on it.
      int legendColumn = screen.getTerminalSize().getColumns() - 24;
      for(int row = 0; row <= 2; row++) {
        assertNotEquals('│', screen.getBackCharacter(legendColumn, row).getCharacter(),
            "the legend never steps on the header (row " + row + "):\n" + text);
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void theLegendOfANarrowSceneStartsUnderTheBarsAndKeepsTheRivalBarWhole() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(60, 20)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      EncounterSceneComponent content = new EncounterSceneComponent(key -> false);
      content.setPreferredSize(new TerminalSize(60, 20));
      BasicWindow window = new BasicWindow();
      window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
      window.setComponent(content);
      gui.addWindow(window);
      ShipPicture you = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[uno]\nxxxxx\n")), List.of())
          .picture(ShipType.Flea, List.of(), 0);
      ShipPicture opponent = new ShipCatalog(List.of(),
          ShipArtFile.parse(new StringReader("[dos]\nyyyyy\n")), List.of())
          .picture(ShipType.Scorpion, List.of(), 0);
      content.model(fight(you, opponent, 0, false, false, false, 0, 0));

      // At 60 columns the scene (40) and the legend column (36) overlap: the
      // legend opens two rows under the bars, so it must not erase the right
      // end of the rival bar, which reaches its column.
      gui.updateScreen();
      String text = screenText(screen);
      int legendColumn = screen.getTerminalSize().getColumns() - 24;
      assertEquals(0, rowOfText(screen, "casco"), text);
      assertRivalBarWhole(screen, legendColumn, 0, text);
      assertEquals('│', screen.getBackCharacter(legendColumn, 2).getCharacter(),
          "the legend starts under the bars:\n" + text);
      assertTrue(rowOfText(screen, spacetrader.Strings.EncounterLegend) > 2, text);

      // With the header the rule is the same: two rows under the bars.
      Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, DialogService.NONE);
      content.commander(game::Commander);
      gui.updateScreen();
      text = screenText(screen);
      assertEquals(3, rowOfText(screen, "casco"), text);
      assertRivalBarWhole(screen, legendColumn, 3, text);
      assertEquals('│', screen.getBackCharacter(legendColumn, 5).getCharacter(),
          "the legend starts under the bars and their gap:\n" + text);
      assertTrue(rowOfText(screen, spacetrader.Strings.EncounterLegend) > 3, text);
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The rival bar reaches the legend column with its cells painted. */
  private static void assertRivalBarWhole(Screen screen, int legendColumn, int row, String text) {
    for(int column = legendColumn - 1; column <= legendColumn + 2; column++) {
      assertEquals('█', screen.getBackCharacter(column, row).getCharacter(),
          "the legend must not erase the rival bar (column " + column + "):\n" + text);
    }
    assertNotEquals('│', screen.getBackCharacter(legendColumn, row).getCharacter(), text);
  }

  /** The row of the header separator (a whole row of dashes), or -1. */
  private static int headerSeparatorRow(Screen screen) {
    for(int row = 0; row < 5; row++) {
      StringBuilder line = new StringBuilder();
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        line.append(screen.getBackCharacter(column, row).getCharacter());
      }
      if(line.toString().chars().allMatch(character -> character == '─')) {
        return row;
      }
    }
    return -1;
  }

  /** The braille glyphs of the screen (the stars of the sky). */
  private static String braille(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        char character = screen.getBackCharacter(column, row).getCharacter();
        if(character >= 0x2800 && character <= 0x28FF) {
          text.append(character);
        }
      }
    }
    return text.toString();
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
      for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
        text.append(screen.getBackCharacter(x, y).getCharacter());
      }
      text.append('\n');
    }
    return text.toString();
  }

  /** The last row of the screen (where the keys of the actions live). */
  private static String lastScreenRow(Screen screen) {
    int row = screen.getTerminalSize().getRows() - 1;
    StringBuilder line = new StringBuilder();
    for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
      line.append(screen.getBackCharacter(column, row).getCharacterString());
    }
    return line.toString();
  }

  /** The key the bar prints for an action, looked up by the name of the action. */
  private static char keyShownFor(Screen screen, String name) {
    String row = lastScreenRow(screen);
    int at = row.indexOf("] " + name);
    assertTrue(at > 0, "the bar prints " + name + ":\n" + row);
    assertEquals('[', row.charAt(at - 2), "the key of " + name + " goes in brackets:\n" + row);
    return row.charAt(at - 1);
  }

  /** Presses the key the bar prints for a name and checks it runs that action. */
  private static void assertPrintedKeyRuns(LanternaEncounterView view, Screen screen,
      List<EncounterAction> executed, String name, EncounterAction action) {
    char key = keyShownFor(screen, name);
    executed.clear();
    view.asWindow().getFocusedInteractable().handleInput(new KeyStroke(key, false, false));
    assertEquals(List.of(action), executed,
        "the printed key [" + Character.toUpperCase(key) + "] runs " + name);
  }
}
