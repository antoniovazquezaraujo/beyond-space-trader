package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.HighScoresView;
import org.gts.bst.view.HighScoresViewModel;
import org.gts.bst.view.HighScoresViewModel.Row;
import spacetrader.Functions;
import spacetrader.HighScoreRecord;
import spacetrader.Strings;


/**
 * Fills the high scores screen from the stored records. No front-end types involved.
 */
public class HighScoresPresenter {
  private static final int ROWS = 3;

  private final HighScoreRecord[] highScores;
  private final HighScoresView view;

  public HighScoresPresenter(HighScoreRecord[] highScores, HighScoresView view) {
    this.highScores = highScores;
    this.view = view;
  }

  public void update() {
    List<Row> rows = new ArrayList<>(ROWS);
    for(int i = 0; i < ROWS; i++) {
      rows.add(new Row(false, "", "", ""));
    }
    for(int i = highScores.length - 1; i >= 0 && highScores[i] != null; i--) {
      int row = 2 - i;
      if(row >= 0 && row < ROWS) {
        rows.set(row, row(highScores[i]));
      }
    }
    view.render(new HighScoresViewModel(rows));
  }

  private static Row row(HighScoreRecord record) {
    return new Row(true, record.Name(),
        Functions.FormatNumber(record.Score() / 10) + "." + record.Score() % 10,
        Functions.StringVars(Strings.HighScoreStatus, new String[] {
            Strings.GameCompletionTypes[record.Type().CastToInt()],
            Functions.Multiples(record.Days(), Strings.TimeUnit),
            Functions.Multiples(record.Worth(), Strings.MoneyUnit),
            Strings.DifficultyLevels[record.Difficulty().CastToInt()].toLowerCase()
        }));
  }
}
