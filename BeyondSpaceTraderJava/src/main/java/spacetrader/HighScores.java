/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.gts.bst.view.DialogService;
import spacetrader.util.Hashtable;


/**
 * The high score table: the three best games, stored sorted from worst to best like
 * the original (index 0 is the worst, the last one the best).
 */
public final class HighScores {
  public static final int COUNT = 3;

  private HighScores() {
  }

  public static HighScoreRecord[] Load(String fileName, DialogService dialogs) {
    HighScoreRecord[] scores = new HighScoreRecord[COUNT];
    Object obj = Functions.LoadFile(fileName, true, dialogs);
    if(obj != null) {
      @SuppressWarnings("unchecked")
      HighScoreRecord[] stored = (HighScoreRecord[])STSerializableObject.ArrayListToArray(
          (ArrayList<Hashtable>)obj, "HighScoreRecord");
      System.arraycopy(stored, 0, scores, 0, Math.min(stored.length, COUNT));
    }
    return scores;
  }

  /**
   * Returns true when the candidate beats the worst score of the table.
   */
  public static boolean Qualifies(HighScoreRecord candidate, HighScoreRecord[] scores) {
    return candidate.CompareTo(scores[0]) > 0;
  }

  /**
   * Inserts the candidate in the table (when it qualifies) and saves it.
   */
  public static void Add(String fileName, HighScoreRecord candidate, DialogService dialogs) {
    HighScoreRecord[] scores = Load(fileName, dialogs);
    scores[0] = candidate;
    Arrays.sort(scores, Comparator.nullsFirst(Comparator.naturalOrder()));
    Functions.SaveFile(fileName, STSerializableObject.ArrayToArrayList(scores), dialogs);
  }
}
