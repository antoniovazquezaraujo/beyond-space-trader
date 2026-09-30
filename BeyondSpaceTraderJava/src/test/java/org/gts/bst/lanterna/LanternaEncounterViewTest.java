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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
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
          false, 0, "Flea", "Hull at 100%", "Shields at 100%",
          "Pirate", "Hull at 100%", "Shields at 100%",
          "The pirate attacks.", "Choose an action.",
          ShipType.Flea, ShipType.Scorpion, true, false, 5, 0, you, opponent));
      gui.addWindow(view.asWindow());
      gui.updateScreen();

      String text = screenText(screen);
      assertTrue(text.contains("Flea"), text);
      assertTrue(text.contains("xxxxx"), "the ship of the player is painted: " + text);
      assertTrue(text.contains("yyyyy"), "and the opponent too: " + text);
      assertFalse(text.contains("| o o >"), "the old sprite is gone: " + text);
      assertTrue(text.contains("The pirate attacks."), text);
      assertTrue(text.contains("[A]Attack"), text);
      assertTrue(text.contains("[F]Flee"), text);
      assertTrue(text.contains("[S]Surrender"), text);

      boolean keyYellow = false;
      boolean frameCyan = false;
      for(int y = 0; y < screen.getTerminalSize().getRows(); y++) {
        for(int x = 0; x < screen.getTerminalSize().getColumns(); x++) {
          TextCharacter character = screen.getBackCharacter(x, y);
          if(character.getCharacter() == '[' && character.getForegroundColor() == TextColor.ANSI.YELLOW) {
            keyYellow = true;
          }
          if("┌┐└┘".indexOf(character.getCharacter()) >= 0
              && character.getForegroundColor() == TextColor.ANSI.CYAN) {
            frameCyan = true;
          }
        }
      }
      assertTrue(keyYellow, "the action keys must be yellow:\n" + text);
      assertTrue(frameCyan, "the window frame must be cyan:\n" + text);

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('i', false, false));
      assertTrue(executed.isEmpty(), "an unavailable action must be ignored");

      view.asWindow().getFocusedInteractable().handleInput(new KeyStroke('f', false, false));
      assertEquals(List.of(EncounterAction.Flee), executed);
    } finally {
      screen.stopScreen();
      screen.close();
    }
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
