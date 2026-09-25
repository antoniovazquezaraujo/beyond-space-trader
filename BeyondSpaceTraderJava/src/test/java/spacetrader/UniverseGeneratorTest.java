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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ship.ShipSize;
import org.junit.jupiter.api.Test;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.ShipyardId;
import spacetrader.enums.SpecialResource;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.SystemPressure;
import spacetrader.enums.TechLevel;


class UniverseGeneratorTest {
  @Test
  void generatesTheSystemsAndTheWormholes() {
    newGame();

    UniverseGenerator.Generated generated = UniverseGenerator.Generate(Strings.SystemNames.size(), 6);

    assertEquals(Strings.SystemNames.size(), generated.systems().length);
    assertEquals(6, generated.wormholes().length);
    for(int i = 0; i < generated.systems().length; i++) {
      StarSystem system = generated.systems()[i];
      assertNotNull(system);
      assertEquals(i, system.Id().CastToInt());
      assertTrue(system.X() >= 0 && system.X() <= UniverseGenerator.GalaxyWidth, "X out of range: " + system.X());
      assertTrue(system.Y() >= 0 && system.Y() <= UniverseGenerator.GalaxyHeight, "Y out of range: " + system.Y());
    }
    for(int wormhole : generated.wormholes()) {
      assertTrue(wormhole >= 0 && wormhole < generated.systems().length, "wormhole out of range: " + wormhole);
    }
  }

  @Test
  void placesTheShipyardsInHighTechSystems() {
    newGame();
    StarSystem[] universe = systems(20, TechLevel.t7);

    assertTrue(UniverseGenerator.PlaceShipyards(universe));

    int placed = 0;
    for(StarSystem system : universe) {
      if(system.ShipyardId() != ShipyardId.NA) {
        placed++;
        assertTrue(system.ShipyardId().CastToInt() >= 0
            && system.ShipyardId().CastToInt() < Consts.Shipyards.size());
      }
    }
    assertTrue(placed > 0, "no shipyard was placed");
  }

  @Test
  void rejectsUniversesWithoutEnoughHighTechSystems() {
    newGame();

    assertFalse(UniverseGenerator.PlaceShipyards(systems(20, TechLevel.t4)));
  }

  @Test
  void placesTheSpecialEvents() {
    newGame();
    UniverseGenerator.Generated generated = null;
    boolean placed = false;
    for(int attempt = 0; attempt < 50 && !placed; attempt++) {
      generated = UniverseGenerator.Generate(Strings.SystemNames.size(), 6);
      placed = UniverseGenerator.PlaceSpecialEvents(generated.systems(), generated.wormholes())
          && UniverseGenerator.PlaceShipyards(generated.systems());
    }

    assertTrue(placed, "no universe placed the special events in 50 attempts");
    assertEquals(SpecialEventType.Princess, generated.systems()[StarSystemId.Galvon.CastToInt()].SpecialEventType());
    assertEquals(SpecialEventType.SpaceMonsterKilled, generated.systems()[StarSystemId.Acamar.CastToInt()].SpecialEventType());
    assertEquals(SpecialEventType.DragonflyBaratas, generated.systems()[StarSystemId.Baratas.CastToInt()].SpecialEventType());
  }

  private static StarSystem[] systems(int count, TechLevel tech) {
    StarSystem[] universe = new StarSystem[count];
    for(int i = 0; i < count; i++) {
      universe[i] = new StarSystem(StarSystemId.FromInt(i), i, i, ShipSize.Small, tech,
          PoliticalSystemType.Anarchy, SystemPressure.None, SpecialResource.Nothing);
    }
    return universe;
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }
}
