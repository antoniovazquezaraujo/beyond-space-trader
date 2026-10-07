/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.equip.Equipment;
import org.junit.jupiter.api.Test;
import spacetrader.util.Hashtable;


class GameSerializationTest {
  @Test
  void savedGameCanBeLoaded() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.NewsEvents().add(7);
    game.VeryRareEncounters().remove(VeryRareEncounter.CaptainAhab);

    Game loaded = new Game(game.Serialize(), null, new TestDialogService());

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

  @Test
  void serializesTheNewsWithTheKeysAndTypesOfTheOldFormat() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.NewsAddEvent(NewsEvent.Japori);
    game.setPaidForNewspaper(true);

    Hashtable hash = game.Serialize();

    assertInstanceOf(Integer[].class, hash.get("_newsEvents"));
    assertArrayEquals(new Integer[] {NewsEvent.Japori.CastToInt()}, (Integer[])hash.get("_newsEvents"));
    assertInstanceOf(Boolean.class, hash.get("_paidForNewspaper"));
    assertEquals(Boolean.TRUE, hash.get("_paidForNewspaper"));
  }

  @Test
  void loadsTheNewsFromASaveWrittenBeforeTheRefactor() throws Exception {
    Game fresh = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    Hashtable save = fresh.Serialize();
    // Keys and types exactly as the old Game wrote them to the save file.
    save.add("_newsEvents", new Integer[] {NewsEvent.Japori.CastToInt(), NewsEvent.WildArrested.CastToInt()});
    save.add("_paidForNewspaper", true);

    Game loaded = new Game(writeAndReadBack(save), null, new TestDialogService());

    assertEquals(List.of(NewsEvent.Japori.CastToInt(), NewsEvent.WildArrested.CastToInt()), loaded.NewsEvents());
    assertTrue(loaded.getPaidForNewspaper());
  }

  private static Hashtable writeAndReadBack(Hashtable save) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try(ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(save);
    }
    try(ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return (Hashtable)in.readObject();
    }
  }

  private static String[] equipmentNames(Equipment[] equipment) {
    String[] names = new String[equipment.length];
    for(int i = 0; i < equipment.length; i++) {
      names[i] = equipment[i] == null ? null : equipment[i].Name();
    }
    return names;
  }
}

