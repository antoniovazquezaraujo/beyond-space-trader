/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on SpaceTrader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ship.ShipType;
import org.gts.bst.testing.TestSoundService;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipCatalog;
import org.gts.bst.view.ShipPicture;
import org.junit.jupiter.api.Test;
import spacetrader.Strings;


class RepairSceneComponentTest {
  @Test
  void theBarRisesWithTheHitsAndEndsWithTheDoneSound() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestSoundService sound = new TestSoundService();
      RepairSceneComponent content = new RepairSceneComponent(picture(), 40, 60, 100, sound);
      Window window = show(gui, content);
      gui.updateScreen();

      assertEquals(40, content.hull(), "the bar starts at the hull in flight");
      String text = screenText(screen);
      assertTrue(text.contains(Strings.RepairSceneTitle), text);
      assertTrue(text.contains("40 -> 60 / 100"), "the numbers of the repair: " + text);
      assertTrue(text.contains(Strings.RepairSceneSkip), text);
      int filledAtStart = count(text, '█');

      int ticks = 0;
      while(!content.finished() && ticks < 200) {
        content.tick();
        ticks++;
      }
      gui.updateScreen();

      assertTrue(content.finished(), "the repair ends by itself");
      assertEquals(20, ticks, "one frame per hull point when they fit in the range");
      assertEquals(60, content.hull(), "all the way to the hull asked for");
      assertEquals(1, count(sound.played(), SoundEffect.REPAIR_DONE), "the done sounds once");
      assertEquals(SoundEffect.REPAIR_DONE, sound.played().get(sound.played().size() - 1),
          "and it is the last thing heard");
      assertTrue(count(sound.played(), SoundEffect.REPAIR_HIT) >= 2, "the hammer hits on the way");
      assertTrue(count(screenText(screen), '█') > filledAtStart, "the bar has grown by the end");

      for(int i = 0; i < 6; i++) {
        content.tick();
      }
      assertFalse(gui.getWindows().contains(window), "the scene goes after the done pause");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  @Test
  void anyKeySkipsToTheEndAndTheSceneClosesAfterTheDone() throws IOException {
    Screen screen = new TerminalScreen(new DefaultVirtualTerminal(new TerminalSize(100, 30)));
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      TestSoundService sound = new TestSoundService();
      RepairSceneComponent content = new RepairSceneComponent(picture(), 40, 60, 100, sound);
      Window window = show(gui, content);
      gui.updateScreen();
      content.tick();

      // Any key skips to the end: the repair completes, not just one frame.
      window.getFocusedInteractable().handleInput(new KeyStroke('x', false, false));
      assertTrue(content.finished(), "the key completes the repair");
      assertEquals(60, content.hull(), "all the way to the end");
      assertEquals(SoundEffect.REPAIR_DONE, sound.played().get(sound.played().size() - 1),
          "the done sounds before the close");

      for(int i = 0; i < 6; i++) {
        content.tick();
        gui.updateScreen();
      }
      assertFalse(gui.getWindows().contains(window), "the scene closes after the done");
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static Window show(MultiWindowTextGUI gui, RepairSceneComponent content) {
    BasicWindow window = new BasicWindow();
    window.setHints(Set.of(Window.Hint.FULL_SCREEN));
    window.setComponent(content);
    window.setFocusedInteractable(content);
    content.onDone(window::close);
    gui.addWindow(window);
    return window;
  }

  private static ShipPicture picture() throws IOException {
    return new ShipCatalog(List.of(),
        ShipArtFile.parse(new StringReader("[uno]\nxxxxx\nxxxxx\nxxxxx\n")), List.of())
        .picture(ShipType.Flea, List.of(), 0);
  }

  private static int count(List<SoundEffect> played, SoundEffect effect) {
    int total = 0;
    for(SoundEffect one : played) {
      if(one == effect) {
        total++;
      }
    }
    return total;
  }

  private static int count(String text, char glyph) {
    int total = 0;
    for(int i = 0; i < text.length(); i++) {
      if(text.charAt(i) == glyph) {
        total++;
      }
    }
    return total;
  }

  private static String screenText(Screen screen) {
    StringBuilder text = new StringBuilder();
    for(int row = 0; row < screen.getTerminalSize().getRows(); row++) {
      StringBuilder line = new StringBuilder();
      for(int column = 0; column < screen.getTerminalSize().getColumns(); column++) {
        line.append(screen.getBackCharacter(column, row).getCharacterString());
      }
      text.append(line.toString().stripTrailing()).append('\n');
    }
    return text.toString();
  }
}
