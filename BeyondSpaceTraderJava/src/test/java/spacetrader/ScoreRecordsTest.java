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

import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import spacetrader.enums.PoliceRecordType;
import spacetrader.enums.ReputationType;


class ScoreRecordsTest {
  @Test
  void policeRecordUsesTheGivenScore() {
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreHero);

    assertEquals(PoliceRecordType.Psychopath,
        PoliceRecord.GetPoliceRecordFromScore(Consts.PoliceRecordScorePsychopath).Type());
  }

  @Test
  void reputationUsesTheGivenScore() {
    Game game = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.Commander().setReputationScore(Consts.ReputationScoreElite);

    assertEquals(ReputationType.Harmless, Reputation.GetReputationFromScore(Consts.ReputationScoreHarmless).Type());
  }
}
