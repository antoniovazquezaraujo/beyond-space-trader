/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ship.ShipSize;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.ShipyardId;
import spacetrader.enums.SpecialResource;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.SystemPressure;
import spacetrader.enums.TechLevel;
import spacetrader.util.Util;


/**
 * Creates the universe of a new game: the star systems and wormholes, then places
 * the shipyards and the special events. It is stateless: the generated data is
 * returned and the caller keeps it (the save format is unchanged).
 */
public final class UniverseGenerator {
  public static final int GalaxyHeight = 110;
  public static final int GalaxyWidth = 154;
  public static final int MinDistance = 7;

  private UniverseGenerator() {
  }

  /**
   * The generated star systems and the wormhole map (system index per wormhole).
   */
  public record Generated(StarSystem[] systems, int[] wormholes) {
  }

  public static Generated Generate(int systemCount, int wormholeCount) {
    StarSystem[] universe = new StarSystem[systemCount];
    int[] wormholes = new int[wormholeCount];
    int i, j;
    for(i = 0; i < universe.length; i++) {
      StarSystemId id = (StarSystemId.FromInt(i));
      SystemPressure pressure = SystemPressure.None;
      SpecialResource specRes = SpecialResource.Nothing;
      ShipSize size = ShipSize.FromInt(Functions.GetRandom(ShipSize.Huge.CastToInt() + 1));
      PoliticalSystem polSys = Consts.PoliticalSystems.get(Functions.GetRandom(Consts.PoliticalSystems.size()));
      TechLevel tech = TechLevel.FromInt(Functions.GetRandom(polSys.MinimumTechLevel().ordinal(), polSys.MaximumTechLevel().ordinal() + 1));
      // Galvon must be a Monarchy.
      if(id == StarSystemId.Galvon) {
        size = ShipSize.Large;
        polSys = Consts.PoliticalSystems.get(PoliticalSystemType.Monarchy.CastToInt());
        tech = TechLevel.t7;
      }
      if(Functions.GetRandom(100) < 15) {
        pressure = SystemPressure.FromInt(Functions.GetRandom(SystemPressure.War.CastToInt(), SystemPressure.Employment.CastToInt() + 1));
      }
      if(Functions.GetRandom(5) >= 3) {
        specRes = SpecialResource.FromInt(Functions.GetRandom(SpecialResource.MineralRich.CastToInt(), SpecialResource.Warlike.CastToInt() + 1));
      }
      int x = 0;
      int y = 0;
      if(i < wormholes.length) {
        // Place the first systems somewhere in the center.
        x = ((GalaxyWidth * (1 + 2 * (i % 3))) / 6) - Functions.GetRandom(-Consts.CloseDistance + 1, Consts.CloseDistance);
        y = ((GalaxyHeight * (i < 3 ? 1 : 3)) / 4) - Functions.GetRandom(-Consts.CloseDistance + 1, Consts.CloseDistance);
        wormholes[i] = i;
      } else {
        boolean ok = false;
        while(!ok) {
          x = Functions.GetRandom(1, GalaxyWidth);
          y = Functions.GetRandom(1, GalaxyHeight);
          boolean closeFound = false;
          boolean tooClose = false;
          for(j = 0; j < i && !tooClose; j++) {
            // Minimum distance between any two systems not to be accepted.
            if(Functions.Distance(universe[j], x, y) < MinDistance) {
              tooClose = true;
            }
            // There should be at least one system which is close enough.
            if(Functions.Distance(universe[j], x, y) < Consts.CloseDistance) {
              closeFound = true;
            }
          }
          ok = (closeFound && !tooClose);
        }
      }
      universe[i] = new StarSystem(id, x, y, size, tech, polSys.Type(), pressure, specRes);
    }
    // Randomize the system locations a bit more, otherwise the systems with the first names in the alphabet are all in the center.
    for(i = 0; i < universe.length; i++) {
      j = Functions.GetRandom(universe.length);
      if(Util.BruteSeek(wormholes, j) < 0) {
        int x = universe[i].X();
        int y = universe[i].Y();
        universe[i].X(universe[j].X());
        universe[i].Y(universe[j].Y());
        universe[j].X(x);
        universe[j].Y(y);
        int w = Util.BruteSeek(wormholes, i);
        if(w >= 0) {
          wormholes[w] = j;
        }
      }
    }
    // Randomize wormhole order
    for(i = 0; i < wormholes.length; i++) {
      j = Functions.GetRandom(wormholes.length);
      int w = wormholes[i];
      wormholes[i] = wormholes[j];
      wormholes[j] = w;
    }
    return new Generated(universe, wormholes);
  }

  public static boolean PlaceSpecialEvents(StarSystem[] universe, int[] wormholes) {
    boolean goodUniverse = true;
    int system;
    universe[StarSystemId.Baratas.CastToInt()].SpecialEventType(SpecialEventType.DragonflyBaratas);
    universe[StarSystemId.Melina.CastToInt()].SpecialEventType(SpecialEventType.DragonflyMelina);
    universe[StarSystemId.Regulas.CastToInt()].SpecialEventType(SpecialEventType.DragonflyRegulas);
    universe[StarSystemId.Zalkon.CastToInt()].SpecialEventType(SpecialEventType.DragonflyDestroyed);
    universe[StarSystemId.Daled.CastToInt()].SpecialEventType(SpecialEventType.ExperimentStopped);
    universe[StarSystemId.Gemulon.CastToInt()].SpecialEventType(SpecialEventType.GemulonRescued);
    universe[StarSystemId.Japori.CastToInt()].SpecialEventType(SpecialEventType.JaporiDelivery);
    universe[StarSystemId.Devidia.CastToInt()].SpecialEventType(SpecialEventType.JarekGetsOut);
    universe[StarSystemId.Utopia.CastToInt()].SpecialEventType(SpecialEventType.MoonRetirement);
    universe[StarSystemId.Nix.CastToInt()].SpecialEventType(SpecialEventType.ReactorDelivered);
    universe[StarSystemId.Acamar.CastToInt()].SpecialEventType(SpecialEventType.SpaceMonsterKilled);
    universe[StarSystemId.Kravat.CastToInt()].SpecialEventType(SpecialEventType.WildGetsOut);
    universe[StarSystemId.Endor.CastToInt()].SpecialEventType(SpecialEventType.SculptureDelivered);
    universe[StarSystemId.Galvon.CastToInt()].SpecialEventType(SpecialEventType.Princess);
    universe[StarSystemId.Centauri.CastToInt()].SpecialEventType(SpecialEventType.PrincessCentauri);
    universe[StarSystemId.Inthara.CastToInt()].SpecialEventType(SpecialEventType.PrincessInthara);
    universe[StarSystemId.Qonos.CastToInt()].SpecialEventType(SpecialEventType.PrincessQonos);
    // Assign a wormhole location endpoint for the Scarab.
    for(system = 0; system < wormholes.length && universe[wormholes[system]].SpecialEventType() != SpecialEventType.NA; system++) {
    }
    if(system < wormholes.length) {
      universe[wormholes[system]].SpecialEventType(SpecialEventType.ScarabDestroyed);
    } else {
      goodUniverse = false;
    }
    // Find a Hi-Tech system without a special event.
    if(goodUniverse) {
      for(system = 0; system < universe.length && !(universe[system].SpecialEventType() == SpecialEventType.NA && universe[system].TechLevel() == TechLevel.t7); system++) {
      }
      if(system < universe.length) {
        universe[system].SpecialEventType(SpecialEventType.ArtifactDelivery);
      } else {
        goodUniverse = false;
      }
    }
    // Find the closest system at least 70 parsecs away from Nix that doesn't already have a special event.
    if(goodUniverse && !FindDistantSystem(universe, StarSystemId.Nix, SpecialEventType.Reactor)) {
      goodUniverse = false;
    }
    // Find the closest system at least 70 parsecs away from Gemulon that doesn't already have a special event.
    if(goodUniverse && !FindDistantSystem(universe, StarSystemId.Gemulon, SpecialEventType.Gemulon)) {
      goodUniverse = false;
    }
    // Find the closest system at least 70 parsecs away from Daled that doesn't already have a special event.
    if(goodUniverse && !FindDistantSystem(universe, StarSystemId.Daled, SpecialEventType.Experiment)) {
      goodUniverse = false;
    }
    // Find the closest system at least 70 parsecs away from Endor that doesn't already have a special event.
    if(goodUniverse && !FindDistantSystem(universe, StarSystemId.Endor, SpecialEventType.Sculpture)) {
      goodUniverse = false;
    }
    // Assign the rest of the events randomly.
    if(goodUniverse) {
      for(int i = 0; i < Consts.SpecialEvents.size(); i++) {
        for(int j = 0; j < Consts.SpecialEvents.get(i).Occurrence(); j++) {
          do {
            system = Functions.GetRandom(universe.length);
          } while(universe[system].SpecialEventType() != SpecialEventType.NA);
          universe[system].SpecialEventType(Consts.SpecialEvents.get(i).Type());
        }
      }
    }
    return goodUniverse;
  }

  public static boolean PlaceShipyards(StarSystem[] universe) {
    boolean goodUniverse = true;
    ArrayList<Integer> systemIdList = new ArrayList<>();
    for(int system = 0; system < universe.length; system++) {
      if(universe[system].TechLevel() == TechLevel.t7) {
        systemIdList.add(system);
      }
    }
    if(systemIdList.size() < Consts.Shipyards.size()) {
      goodUniverse = false;
    } else {
      // Assign the shipyards to High-Tech systems.
      for(int shipyard = 0; shipyard < Consts.Shipyards.size(); shipyard++) {
        universe[systemIdList.get(Functions.GetRandom(systemIdList.size()))].ShipyardId(ShipyardId.FromInt(shipyard));
      }
    }
    return goodUniverse;
  }

  private static boolean FindDistantSystem(StarSystem[] universe, StarSystemId baseSystem, SpecialEventType specEvent) {
    int bestDistance = 999;
    int system = -1;
    for(int i = 0; i < universe.length; i++) {
      int distance = Functions.Distance(universe[baseSystem.CastToInt()], universe[i]);
      if(distance >= 70 && distance < bestDistance && universe[i].SpecialEventType() == SpecialEventType.NA) {
        system = i;
        bestDistance = distance;
      }
    }
    if(system >= 0) {
      universe[system].SpecialEventType(specEvent);
    }
    return (system >= 0);
  }
}
