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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.DialogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spacetrader.enums.GameEndType;


class HighScoresTest {
  @Test
  void addsAndSortsTheScores(@TempDir Path dir) {
    String file = dir.resolve("scores.bin").toString();

    HighScores.Add(file, record("A", 100), DialogService.NONE);
    HighScores.Add(file, record("B", 300), DialogService.NONE);
    HighScores.Add(file, record("C", 200), DialogService.NONE);

    HighScoreRecord[] scores = HighScores.Load(file, DialogService.NONE);

    assertEquals(HighScores.COUNT, scores.length);
    assertEquals("A", scores[0].Name());
    assertEquals("C", scores[1].Name());
    assertEquals("B", scores[2].Name());
  }

  @Test
  void qualifiesOnlyWhenItBeatsTheWorstScore(@TempDir Path dir) {
    String file = dir.resolve("scores.bin").toString();

    assertTrue(HighScores.Qualifies(record("A", 1), HighScores.Load(file, DialogService.NONE)));

    HighScores.Add(file, record("A", 100), DialogService.NONE);
    HighScores.Add(file, record("B", 300), DialogService.NONE);
    HighScores.Add(file, record("C", 200), DialogService.NONE);

    assertTrue(HighScores.Qualifies(record("D", 101), HighScores.Load(file, DialogService.NONE)));
    assertFalse(HighScores.Qualifies(record("E", 99), HighScores.Load(file, DialogService.NONE)));
  }

  private static HighScoreRecord record(String name, int score) {
    return new HighScoreRecord(name, score, GameEndType.Retired, 10, 1000, Difficulty.Normal);
  }
}
