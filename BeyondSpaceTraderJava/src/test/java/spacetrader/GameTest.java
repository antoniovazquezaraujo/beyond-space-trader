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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.gts.bst.cargo.CargoSellOp;
import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class GameTest {
  private final TestDialogService dialogs = new TestDialogService();

  private Game newGame(Difficulty difficulty) {
    return new Game("Test", difficulty, 4, 4, 4, 4, null, dialogs);
  }

  @Test
  void newGameStartsWithDefaultCash() {
    Game game = newGame(Difficulty.Normal);

    assertEquals(1000, game.Commander().getCash());
  }

  @Test
  void currentGameIsAssignedOnConstruction() {
    Game game = newGame(Difficulty.Normal);

    assertSame(game, Game.CurrentGame());
  }

  @Test
  void newGameIsNotInCheatMode() {
    Game game = newGame(Difficulty.Normal);

    assertFalse(game.getCheatEnabled());
    assertFalse(game.getEasyEncounters());
    assertFalse(game.getCanSuperWarp());
  }

  @Test
  void universeContainsEveryNamedSystem() {
    Game game = newGame(Difficulty.Normal);

    assertEquals(Strings.SystemNames.size(), game.Universe().length);
  }

  @Test
  void commanderStartsInAValidSystem() {
    Game game = newGame(Difficulty.Normal);

    assertNotNull(game.Commander().CurrentSystem());
  }

  @Test
  void difficultyIsKept() {
    Game game = newGame(Difficulty.Hard);

    assertEquals(Difficulty.Hard, game.Difficulty());
  }

  @Test
  void dialogServiceIsInjected() {
    Game game = newGame(Difficulty.Normal);

    assertSame(dialogs, game.Dialogs());
  }

  @Test
  void gameAlertsGoThroughTheDialogService() {
    Game game = newGame(Difficulty.Normal);

    game.CargoSellOffer(0, CargoSellOp.SellSystem);

    assertEquals(List.of(AlertType.CargoNoneToSell), dialogs.alerts());
  }
}

