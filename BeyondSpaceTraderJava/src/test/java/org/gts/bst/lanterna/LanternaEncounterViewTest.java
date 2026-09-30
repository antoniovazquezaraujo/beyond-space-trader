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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
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
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent, false, false, false, 5, false, 0));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains("Flea"), text);
      assertTrue(text.contains("casco ████████"), "the hull bar of the player: " + text);
      assertTrue(text.contains("casco ████░░░░"), "half hull for the opponent: " + text);
      assertTrue(text.contains("xxxxx"), "the ship of the player is painted: " + text);
      assertTrue(text.contains("yyyyy"), "and the opponent too: " + text);
      assertFalse(text.contains("| o o >"), "the old sprite is gone: " + text);
      assertTrue(text.contains("The pirate attacks."), text);
      assertFalse(text.contains("[A]Attack"), "no buttons in the scene: " + text);
      assertFalse(text.contains("[F]Flee"), "no buttons in the scene: " + text);

      boolean frameCyan = false;
      for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
        for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
          TextCharacter character = screen.getBackCharacter(x, y);
          if("┌┐└┘".indexOf(character.getCharacter()) >= 0
              && character.getForegroundColor() == TextColor.ANSI.CYAN) {
            frameCyan = true;
          }
        }
      }
      assertTrue(frameCyan, "the window frame must be cyan:\n" + text);

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
          false, 5, false, 0));
      gui.updateScreen();
      int before = columnOf(screen, 'x');

      content.move(-1, 0);
      gui.updateScreen();

      assertEquals(before + 1, columnOf(screen, 'x'), "the drawing is mirrored (one cell right and one back)");
    } finally {
      screen.stopScreen();
      screen.close();
    }
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
