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
import org.gts.bst.cargo.CargoBuyOffer;
import org.gts.bst.cargo.CargoBuyOp;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.EncounterAction;
import org.gts.bst.view.EncounterViewModel;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipCatalog;
import org.gts.bst.view.ShipPicture;
import org.junit.jupiter.api.Test;


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

      // Advancing is a dash: six cells on the first frame (double of the vertical glide).
      content.move(1, 0);
      content.tick();
      gui.updateScreen();
      assertEquals(before + 6, columnOf(screen, 'x'), "the dash goes at double speed");

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
}
