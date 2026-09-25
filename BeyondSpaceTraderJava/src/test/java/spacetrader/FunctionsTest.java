/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spacetrader.util.Hashtable;


class FunctionsTest {
  @Test
  void stringVarsReplacesEveryPlaceholder() {
    assertEquals("Buy 10 units of water",
        Functions.StringVars("Buy ^1 ^2 of ^3", new String[] {"10", "units", "water"}));
  }

  @Test
  void stringVarsHandlesSingleVariableOverload() {
    assertEquals("Hello, Antonio", Functions.StringVars("Hello, ^1", "Antonio"));
  }

  @Test
  void getRandomStaysWithinBounds() {
    for(int i = 0; i < 1000; i++) {
      int value = Functions.GetRandom(7);
      assertTrue(value >= 0 && value < 7, "value out of range: " + value);
    }
  }

  @Test
  void getRandomWithLowerBoundStaysWithinBounds() {
    for(int i = 0; i < 1000; i++) {
      int value = Functions.GetRandom(3, 9);
      assertTrue(value >= 3 && value < 9, "value out of range: " + value);
    }
  }

  @Test
  void saveFileRoundTripsThroughTheFileSystem(@TempDir Path dir) {
    TestDialogService dialogs = new TestDialogService();
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    String fileName = dir.resolve("save.bin").toString();

    assertTrue(Functions.SaveFile(fileName, game.Serialize(), dialogs));

    Object loaded = Functions.LoadFile(fileName, false, dialogs);
    Game loadedGame = new Game((Hashtable)loaded, null, dialogs);
    assertEquals(game.Commander().getCash(), loadedGame.Commander().getCash());
    assertEquals(game.Commander().CurrentSystem().Id(), loadedGame.Commander().CurrentSystem().Id());
    assertEquals(game.Universe().length, loadedGame.Universe().length);
  }
}

