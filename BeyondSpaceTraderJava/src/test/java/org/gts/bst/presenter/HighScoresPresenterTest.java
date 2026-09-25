/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.HighScoresView;
import org.gts.bst.view.HighScoresViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.HighScoreRecord;
import spacetrader.enums.GameEndType;


class HighScoresPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheThreeStoredRecords() {
    HighScoreRecord[] scores = {
        record("Third", 3000, Difficulty.Impossible, 300, 5000),
        record("Second", 2000, Difficulty.Hard, 200, 100000),
        record("First", 1000, Difficulty.Normal, 100, 250000)
    };
    RecordingView view = new RecordingView();

    new HighScoresPresenter(scores, view).update();

    assertEquals(3, view.model.rows().size());
    assertEquals("First", view.model.rows().get(0).name());
    assertEquals("100.0", view.model.rows().get(0).score());
    assertEquals("Retired in 100 days, worth 250,000 credits on normal level.", view.model.rows().get(0).status());
    assertTrue(view.model.rows().get(0).filled());
    assertEquals("Second", view.model.rows().get(1).name());
    assertEquals("Third", view.model.rows().get(2).name());
  }

  @Test
  void leavesTheRowsEmptyWhenThereAreNoRecords() {
    RecordingView view = new RecordingView();

    new HighScoresPresenter(new HighScoreRecord[3], view).update();

    assertEquals(3, view.model.rows().size());
    for(HighScoresViewModel.Row row : view.model.rows()) {
      assertFalse(row.filled());
    }
  }

  private static HighScoreRecord record(String name, int score, Difficulty difficulty, int days, int worth) {
    return new HighScoreRecord(name, score, GameEndType.Retired, days, worth, difficulty);
  }

  private static class RecordingView implements HighScoresView {
    private HighScoresViewModel model;

    @Override
    public void render(HighScoresViewModel model) {
      this.model = model;
    }
  }
}

