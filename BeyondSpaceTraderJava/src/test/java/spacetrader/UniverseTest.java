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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.junit.jupiter.api.Test;
import spacetrader.enums.ShipyardId;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;


class UniverseTest {
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

    universe.systems()[0].Visited(true);
    universe.wormholes()[0] = 3;

    assertTrue(universe.systems()[0].Visited());
    assertEquals(3, universe.wormholes()[0]);
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
  void theFacadeDelegatesToTheComponent() {
    Game game = newGame();

    StarSystem[] systems = game.Universe();
    int[] wormholes = game.Wormholes();

    assertEquals(Strings.SystemNames.size(), systems.length);
    assertEquals(6, wormholes.length);
    assertSame(systems, game.Universe());
    assertSame(wormholes, game.Wormholes());
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
