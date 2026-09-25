package spacetrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.equip.Equipment;
import org.junit.jupiter.api.Test;


class GameSerializationTest {
  @Test
  void savedGameCanBeLoaded() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null);
    game.NewsEvents().add(7);
    game.VeryRareEncounters().remove(VeryRareEncounter.CaptainAhab);

    Game loaded = new Game(game.Serialize(), null);

    assertEquals(game.Commander().getCash(), loaded.Commander().getCash());
    assertEquals(game.Commander().CurrentSystem().Id(), loaded.Commander().CurrentSystem().Id());
    assertEquals(game.Universe().length, loaded.Universe().length);
    assertEquals(game.Difficulty(), loaded.Difficulty());
    assertEquals(game.NewsEvents(), loaded.NewsEvents());
    assertEquals(game.VeryRareEncounters(), loaded.VeryRareEncounters());
    assertArrayEquals(equipmentNames(game.Commander().getShip().Weapons()),
        equipmentNames(loaded.Commander().getShip().Weapons()));
    assertArrayEquals(equipmentNames(game.Commander().getShip().Shields()),
        equipmentNames(loaded.Commander().getShip().Shields()));
    assertArrayEquals(equipmentNames(game.Commander().getShip().Gadgets()),
        equipmentNames(loaded.Commander().getShip().Gadgets()));
  }

  private static String[] equipmentNames(Equipment[] equipment) {
    String[] names = new String[equipment.length];
    for(int i = 0; i < equipment.length; i++) {
      names[i] = equipment[i] == null ? null : equipment[i].Name();
    }
    return names;
  }
}
