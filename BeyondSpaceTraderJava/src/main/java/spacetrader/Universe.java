/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;


/**
 * The universe of a game: the star systems of the galaxy and the wormhole map.
 * It owns the generation (with its retry until the special events and the
 * shipyards can be placed) and the state; the game only delegates.
 */
public final class Universe {
  private static final int WORMHOLE_COUNT = 6;

  private final StarSystem[] _systems;
  private final int[] _wormholes;

  private Universe(StarSystem[] systems, int[] wormholes) {
    _systems = systems;
    _wormholes = wormholes;
  }

  /**
   * Generates a new universe, retrying with a fresh layout until every special
   * event and shipyard can be placed.
   */
  public static Universe generate() {
    StarSystem[] systems;
    int[] wormholes;
    do {
      UniverseGenerator.Generated generated = UniverseGenerator.Generate(Strings.SystemNames.size(), WORMHOLE_COUNT);
      systems = generated.systems();
      wormholes = generated.wormholes();
    } while(!(UniverseGenerator.PlaceSpecialEvents(systems, wormholes) && UniverseGenerator.PlaceShipyards(systems)));
    return new Universe(systems, wormholes);
  }

  /** Restores the universe of a saved game: the systems and the wormhole map as they were stored. */
  public static Universe from(StarSystem[] systems, int[] wormholes) {
    return new Universe(systems, wormholes);
  }

  /** The live array of the star systems of the galaxy. */
  public StarSystem[] systems() {
    return _systems;
  }

  /** The live map of wormholes: the system index of every wormhole endpoint. */
  public int[] wormholes() {
    return _wormholes;
  }
}
