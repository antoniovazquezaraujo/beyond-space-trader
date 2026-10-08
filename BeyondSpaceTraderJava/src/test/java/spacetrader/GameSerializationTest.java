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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.equip.Equipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spacetrader.enums.StarSystemId;
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
  void serializesThePricesWithTheKeysAndTypesOfTheOldFormat() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.PriceCargoBuy()[0] = 123;
    game.PriceCargoSell()[1] = 45;

    Hashtable hash = game.Serialize();

    assertInstanceOf(int[].class, hash.get("_priceCargoBuy"));
    assertEquals(123, ((int[])hash.get("_priceCargoBuy"))[0]);
    assertInstanceOf(int[].class, hash.get("_priceCargoSell"));
    assertEquals(45, ((int[])hash.get("_priceCargoSell"))[1]);
  }

  @Test
  void loadsThePricesFromASaveWrittenBeforeTheRefactor() throws Exception {
    Game fresh = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    Hashtable save = fresh.Serialize();
    // Keys and types exactly as the old Game wrote them to the save file.
    save.add("_priceCargoBuy", new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0});
    save.add("_priceCargoSell", new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0});

    Game loaded = new Game(writeAndReadBack(save), null, new TestDialogService());

    assertArrayEquals(new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0}, loaded.PriceCargoBuy());
    assertArrayEquals(new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0}, loaded.PriceCargoSell());
  }

  @Test
  void loadsThePricesFromAnOldSaveFile(@TempDir Path dir) {
    TestDialogService dialogs = new TestDialogService();
    Game fresh = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    Hashtable save = fresh.Serialize();
    // Keys and types exactly as the old Game wrote them to the save file.
    save.add("_priceCargoBuy", new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0});
    save.add("_priceCargoSell", new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0});
    String fileName = dir.resolve("old-save.bin").toString();

    assertTrue(Functions.SaveFile(fileName, save, dialogs));

    Game loaded = new Game((Hashtable)Functions.LoadFile(fileName, false, dialogs), null, dialogs);

    assertArrayEquals(new int[] {11, 12, 13, 0, 0, 0, 0, 0, 0, 0}, loaded.PriceCargoBuy());
    assertArrayEquals(new int[] {1, 2, 3, 0, 0, 0, 0, 0, 0, 0}, loaded.PriceCargoSell());
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

  @Test
  void serializesTheUniverseWithTheKeysAndTypesOfTheOldFormat() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());

    Hashtable hash = game.Serialize();

    Object systems = hash.get("_universe");
    assertInstanceOf(ArrayList.class, systems);
    assertEquals(game.Universe().length, ((ArrayList<?>)systems).size());
    assertInstanceOf(Hashtable.class, ((ArrayList<?>)systems).get(0));
    assertInstanceOf(int[].class, hash.get("_wormholes"));
    assertEquals(6, ((int[])hash.get("_wormholes")).length);
    assertArrayEquals(game.Wormholes(), (int[])hash.get("_wormholes"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void loadsTheUniverseFromASaveWrittenBeforeTheRefactor() throws Exception {
    Game fresh = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    Hashtable save = fresh.Serialize();
    // Keys and types exactly as the old Game wrote them to the save file.
    ArrayList<Hashtable> systems = (ArrayList<Hashtable>)save.get("_universe");
    systems.get(0).add("_x", -1);
    save.add("_wormholes", new int[] {3, 1, 4, 1, 5, 9});

    Game loaded = new Game(writeAndReadBack(save), null, new TestDialogService());

    assertEquals(-1, loaded.Universe()[0].X());
    assertArrayEquals(new int[] {3, 1, 4, 1, 5, 9}, loaded.Wormholes());

    // The arrays of the loaded save are the live ones the game uses, not copies.
    StarSystem[] loadedSystems = loaded.Universe();
    int[] loadedWormholes = loaded.Wormholes();
    loadedSystems[0].Visited(true);
    loadedWormholes[0] = 42;
    loaded.SelectedSystemId(StarSystemId.FromInt(0));
    Hashtable reSaved = loaded.Serialize();
    assertSame(loadedSystems[0], loaded.SelectedSystem(), "SelectedSystem must be the loaded live system");
    assertEquals(Boolean.TRUE, ((Hashtable)((ArrayList<?>)reSaved.get("_universe")).get(0)).get("_visited"));
    assertEquals(42, ((int[])reSaved.get("_wormholes"))[0]);
  }

  @Test
  void serializesTheQuestsWithTheKeysAndTypesOfTheOldFormat() {
    Game game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    game.setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
    game.setQuestStatusDragonfly(SpecialEvent.StatusDragonflyFlyBaratas);
    game.setQuestStatusExperiment(SpecialEvent.StatusExperimentStarted);
    game.setQuestStatusGemulon(SpecialEvent.StatusGemulonStarted);
    game.setQuestStatusJapori(SpecialEvent.StatusJaporiInTransit);
    game.setQuestStatusJarek(SpecialEvent.StatusJarekStarted);
    game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);
    game.setQuestStatusPrincess(SpecialEvent.StatusPrincessRescued);
    game.setQuestStatusReactor(SpecialEvent.StatusReactorFuelOk);
    game.setQuestStatusScarab(SpecialEvent.StatusScarabHunting);
    game.setQuestStatusSculpture(SpecialEvent.StatusSculptureInTransit);
    game.setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
    game.setQuestStatusWild(SpecialEvent.StatusWildStarted);
    game.setFabricRipProbability(7);
    game.setCanSuperWarp(true);

    Hashtable hash = game.Serialize();

    // Keys and types exactly as the old Game wrote them to the save file.
    assertInstanceOf(Integer.class, hash.get("_questStatusArtifact"));
    assertEquals(SpecialEvent.StatusArtifactOnBoard, hash.get("_questStatusArtifact"));
    assertInstanceOf(Integer.class, hash.get("_questStatusDragonfly"));
    assertEquals(SpecialEvent.StatusDragonflyFlyBaratas, hash.get("_questStatusDragonfly"));
    assertInstanceOf(Integer.class, hash.get("_questStatusExperiment"));
    assertEquals(SpecialEvent.StatusExperimentStarted, hash.get("_questStatusExperiment"));
    assertInstanceOf(Integer.class, hash.get("_questStatusGemulon"));
    assertEquals(SpecialEvent.StatusGemulonStarted, hash.get("_questStatusGemulon"));
    assertInstanceOf(Integer.class, hash.get("_questStatusJapori"));
    assertEquals(SpecialEvent.StatusJaporiInTransit, hash.get("_questStatusJapori"));
    assertInstanceOf(Integer.class, hash.get("_questStatusJarek"));
    assertEquals(SpecialEvent.StatusJarekStarted, hash.get("_questStatusJarek"));
    assertInstanceOf(Integer.class, hash.get("_questStatusMoon"));
    assertEquals(SpecialEvent.StatusMoonBought, hash.get("_questStatusMoon"));
    assertInstanceOf(Integer.class, hash.get("_questStatusPrincess"));
    assertEquals(SpecialEvent.StatusPrincessRescued, hash.get("_questStatusPrincess"));
    assertInstanceOf(Integer.class, hash.get("_questStatusReactor"));
    assertEquals(SpecialEvent.StatusReactorFuelOk, hash.get("_questStatusReactor"));
    assertInstanceOf(Integer.class, hash.get("_questStatusScarab"));
    assertEquals(SpecialEvent.StatusScarabHunting, hash.get("_questStatusScarab"));
    assertInstanceOf(Integer.class, hash.get("_questStatusSculpture"));
    assertEquals(SpecialEvent.StatusSculptureInTransit, hash.get("_questStatusSculpture"));
    assertInstanceOf(Integer.class, hash.get("_questStatusSpaceMonster"));
    assertEquals(SpecialEvent.StatusSpaceMonsterAtAcamar, hash.get("_questStatusSpaceMonster"));
    assertInstanceOf(Integer.class, hash.get("_questStatusWild"));
    assertEquals(SpecialEvent.StatusWildStarted, hash.get("_questStatusWild"));
    assertInstanceOf(Integer.class, hash.get("_fabricRipProbability"));
    assertEquals(7, hash.get("_fabricRipProbability"));
    assertInstanceOf(Boolean.class, hash.get("_canSuperWarp"));
    assertEquals(Boolean.TRUE, hash.get("_canSuperWarp"));
  }

  @Test
  void loadsTheQuestsFromASaveWrittenBeforeTheRefactor() throws Exception {
    Game fresh = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
    Hashtable save = fresh.Serialize();
    // Keys and types exactly as the old Game wrote them to the save file.
    save.add("_questStatusArtifact", SpecialEvent.StatusArtifactDone);
    save.add("_questStatusDragonfly", SpecialEvent.StatusDragonflyDone);
    save.add("_questStatusExperiment", SpecialEvent.StatusExperimentCancelled);
    save.add("_questStatusGemulon", SpecialEvent.StatusGemulonDone);
    save.add("_questStatusJapori", SpecialEvent.StatusJaporiDone);
    save.add("_questStatusJarek", SpecialEvent.StatusJarekDone);
    save.add("_questStatusMoon", SpecialEvent.StatusMoonDone);
    save.add("_questStatusPrincess", SpecialEvent.StatusPrincessDone);
    save.add("_questStatusReactor", SpecialEvent.StatusReactorDone);
    save.add("_questStatusScarab", SpecialEvent.StatusScarabDone);
    save.add("_questStatusSculpture", SpecialEvent.StatusSculptureDone);
    save.add("_questStatusSpaceMonster", SpecialEvent.StatusSpaceMonsterDone);
    save.add("_questStatusWild", SpecialEvent.StatusWildDone);
    save.add("_fabricRipProbability", 7);
    save.add("_canSuperWarp", true);

    Game loaded = new Game(writeAndReadBack(save), null, new TestDialogService());

    assertEquals(SpecialEvent.StatusArtifactDone, loaded.getQuestStatusArtifact());
    assertEquals(SpecialEvent.StatusDragonflyDone, loaded.getQuestStatusDragonfly());
    assertEquals(SpecialEvent.StatusExperimentCancelled, loaded.getQuestStatusExperiment());
    assertEquals(SpecialEvent.StatusGemulonDone, loaded.getQuestStatusGemulon());
    assertEquals(SpecialEvent.StatusJaporiDone, loaded.getQuestStatusJapori());
    assertEquals(SpecialEvent.StatusJarekDone, loaded.getQuestStatusJarek());
    assertEquals(SpecialEvent.StatusMoonDone, loaded.getQuestStatusMoon());
    assertEquals(SpecialEvent.StatusPrincessDone, loaded.getQuestStatusPrincess());
    assertEquals(SpecialEvent.StatusReactorDone, loaded.getQuestStatusReactor());
    assertEquals(SpecialEvent.StatusScarabDone, loaded.getQuestStatusScarab());
    assertEquals(SpecialEvent.StatusSculptureDone, loaded.getQuestStatusSculpture());
    assertEquals(SpecialEvent.StatusSpaceMonsterDone, loaded.getQuestStatusSpaceMonster());
    assertEquals(SpecialEvent.StatusWildDone, loaded.getQuestStatusWild());
    assertEquals(7, loaded.getFabricRipProbability());
    assertTrue(loaded.getCanSuperWarp());
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

