package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.gts.bst.difficulty.Difficulty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


class GameSerializationTest {
  @Test
  @Disabled("Blocked by #15: equipment type enums do not implement CastToInt")
  void savedGameCanBeLoaded() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null);

    Game loaded = new Game(game.Serialize(), null);

    assertEquals(game.Commander().getCash(), loaded.Commander().getCash());
    assertEquals(game.Commander().CurrentSystem().Id(), loaded.Commander().CurrentSystem().Id());
  }
}
