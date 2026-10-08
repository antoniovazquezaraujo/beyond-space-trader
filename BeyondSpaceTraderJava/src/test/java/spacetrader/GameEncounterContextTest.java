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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterType;
import org.junit.jupiter.api.Test;
import spacetrader.enums.StarSystemId;


/**
 * Pins the wiring of the narrow context: the real game, implemented as the
 * {@link EncounterContext} the {@link EncounterGenerator} asks, feeds the
 * generator the trip state of the quest encounter.
 */
class GameEncounterContextTest {
  @Test
  void theGeneratorSeesTheSpaceMonsterQuestThroughTheGameContext() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
    game.SelectedSystemId(StarSystemId.Acamar);
    game.setClicks(1);

    boolean showEncounter = new EncounterGenerator(game).determine();

    assertTrue(showEncounter, "the game state must make the quest monster appear");
    assertSame(game.SpaceMonster(), game.encounter().getOpponent(), "the opponent must come from the game context");
    assertEquals(EncounterType.SpaceMonsterAttack, game.encounter().getEncounterType(),
        "the uncloaked commander must be attacked");
  }
}
