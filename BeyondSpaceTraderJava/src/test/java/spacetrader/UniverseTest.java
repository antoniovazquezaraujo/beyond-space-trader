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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.enums.ShipyardId;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;
import spacetrader.util.Hashtable;


class UniverseTest {
  /**
   * The first layout drawn with this seed has fewer Hi-Tech systems than
   * shipyards, so {@link Universe#generate()} must discard it and retry. The
   * seed makes the retry deterministic: without it the first layout fails only
   * once in tens of thousands of draws.
   */
  private static final int RETRY_SEED = 6161;

  /**
   * The trade items of a system are initialized from the difficulty of the
   * current game, so generation needs one even when it is called directly.
   */
  @BeforeAll
  static void keepGenerationIndependentFromTheTestOrder() {
    newGame();
  }

  @Test
  void generatesEveryNamedSystemAndSixWormholes() {
    Universe universe = Universe.generate();

    assertEquals(Strings.SystemNames.size(), universe.systems().length);
    assertEquals(6, universe.wormholes().length);
    for(int i = 0; i < universe.systems().length; i++) {
      StarSystem system = universe.systems()[i];
      assertNotNull(system);
      assertEquals(i, system.Id().CastToInt());
    }
    for(int wormhole : universe.wormholes()) {
      assertTrue(wormhole >= 0 && wormhole < universe.systems().length, "wormhole out of range: " + wormhole);
    }
  }

  @Test
  void generationPlacesTheSpecialEventsAndTheShipyards() {
    Universe universe = Universe.generate();

    // The fixed events of the quests.
    assertEquals(SpecialEventType.Princess, universe.systems()[StarSystemId.Galvon.CastToInt()].SpecialEventType());
    assertEquals(SpecialEventType.SpaceMonsterKilled, universe.systems()[StarSystemId.Acamar.CastToInt()].SpecialEventType());
    assertEquals(SpecialEventType.DragonflyBaratas, universe.systems()[StarSystemId.Baratas.CastToInt()].SpecialEventType());
    // Exactly one wormhole endpoint hosts the Scarab, or the generation retried.
    assertEquals(1, countScarabEndpoints(universe), "the Scarab needs a wormhole endpoint");
    // The retry guarantees enough Hi-Tech systems for every shipyard to fit.
    assertTrue(countHighTechSystems(universe) >= Consts.Shipyards.size(), "not enough Hi-Tech systems for the shipyards");
    int placed = 0;
    for(StarSystem system : universe.systems()) {
      if(system.ShipyardId() != ShipyardId.NA) {
        placed++;
        assertEquals(TechLevel.t7, system.TechLevel(), "a shipyard outside a Hi-Tech system");
      }
    }
    assertTrue(placed > 0, "no shipyard was placed");
  }

  @Test
  void systemsAndWormholesAreTheLiveArrays() {
    Universe universe = Universe.generate();

    StarSystem[] systems = universe.systems();
    int[] wormholes = universe.wormholes();
    systems[0].Visited(true);
    wormholes[0] = 3;

    assertSame(systems, universe.systems(), "the systems must not be copied");
    assertSame(wormholes, universe.wormholes(), "the wormholes must not be copied");
    assertTrue(systems[0].Visited());
    assertEquals(3, wormholes[0]);
  }

  @Test
  void restoringASaveKeepsItsArrays() {
    Universe generated = Universe.generate();
    StarSystem[] systems = generated.systems();
    int[] wormholes = generated.wormholes();

    Universe restored = Universe.from(systems, wormholes);

    assertSame(systems, restored.systems());
    assertSame(wormholes, restored.wormholes());
  }

  @Test
  void aSavedUniverseSurvivesAHashRoundTrip() {
    Universe generated = Universe.generate();
    generated.systems()[0].Visited(true);
    generated.wormholes()[0] = 3;
    Hashtable save = new Hashtable();

    generated.saveTo(save);
    Universe loaded = Universe.from(save);

    assertInstanceOf(ArrayList.class, save.get("_universe"));
    assertInstanceOf(int[].class, save.get("_wormholes"));
    assertEquals(generated.systems().length, loaded.systems().length);
    assertTrue(loaded.systems()[0].Visited(), "the system state must survive the save");
    assertArrayEquals(generated.wormholes(), loaded.wormholes());
  }

  @Test
  void theFacadeDelegatesToTheComponent() {
    Game game = newGame();

    StarSystem[] systems = game.Universe();
    int[] wormholes = game.Wormholes();

    assertEquals(Strings.SystemNames.size(), systems.length);
    assertEquals(6, wormholes.length);
    assertSame(systems, game.Universe());
    assertSame(wormholes, game.Wormholes());

    // The arrays the facade exposes are the ones the game itself uses.
    game.SelectedSystemId(StarSystemId.FromInt(0));
    assertSame(systems[0], game.SelectedSystem(), "the facade must expose the live systems");
  }

  @Test
  void generationRetriesUntilTheShipyardsFit() throws Exception {
    Random previous = replaceRandom(new Random(RETRY_SEED));
    try {
      // Arrange: the first layout of this seed cannot host all the shipyards.
      UniverseGenerator.Generated firstDraw = UniverseGenerator.Generate(Strings.SystemNames.size(), 6);
      UniverseGenerator.PlaceSpecialEvents(firstDraw.systems(), firstDraw.wormholes());
      assertFalse(UniverseGenerator.PlaceShipyards(firstDraw.systems()),
          "the seed must force the first layout to be rejected");

      // Act: with the same seed, generation must retry with a fresh layout.
      replaceRandom(new Random(RETRY_SEED));
      Universe universe = Universe.generate();

      // Assert: the returned universe is not the rejected one and fits the shipyards.
      assertNotSame(firstDraw.systems()[0], universe.systems()[0], "generate must draw a fresh layout");
      assertTrue(countHighTechSystems(universe) >= Consts.Shipyards.size(),
          "not enough Hi-Tech systems after the retry");
      int placed = 0;
      for(StarSystem system : universe.systems()) {
        if(system.ShipyardId() != ShipyardId.NA) {
          placed++;
        }
      }
      assertTrue(placed > 0, "no shipyard was placed after the retry");
    } finally {
      replaceRandom(previous);
    }
  }

  /**
   * Seeds the shared random source of {@link Functions} so generation is
   * deterministic; returns the source to restore afterwards.
   */
  private static Random replaceRandom(Random random) throws ReflectiveOperationException {
    Field rand = Functions.class.getDeclaredField("rand");
    rand.setAccessible(true);
    Random previous = (Random)rand.get(null);
    rand.set(null, random);
    return previous;
  }

  private static int countScarabEndpoints(Universe universe) {
    int count = 0;
    for(int wormhole : universe.wormholes()) {
      if(universe.systems()[wormhole].SpecialEventType() == SpecialEventType.ScarabDestroyed) {
        count++;
      }
    }
    return count;
  }

  private static int countHighTechSystems(Universe universe) {
    int count = 0;
    for(StarSystem system : universe.systems()) {
      if(system.TechLevel() == TechLevel.t7) {
        count++;
      }
    }
    return count;
  }

  private static Game newGame() {
    return new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }
}
