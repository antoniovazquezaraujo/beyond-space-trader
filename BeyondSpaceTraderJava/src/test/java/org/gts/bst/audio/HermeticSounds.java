/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on SpaceTrader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.io.InputStream;
import java.util.Set;


/**
 * A hermetic {@link SampleLibrary} for the tests: it serves only the WAV
 * fixtures of {@code src/test/resources/sounds/} and nothing else, so the
 * assertions do not depend on the samples the author has (or adds later) in
 * {@code src/main/resources/sounds/}. Any path outside the list is silence.
 */
final class HermeticSounds {
  /** The fixtures of the test resources, exactly the paths the tests use. */
  private static final Set<String> FIXTURES = Set.of(
      "/sounds/combat/hit-1.wav",
      "/sounds/combat/hit-2.wav",
      "/sounds/combat/laser-pulse-1.wav",
      "/sounds/music/calm-1.wav",
      "/sounds/music/tense-1.wav",
      "/sounds/ships/default-1.wav",
      "/sounds/ships/gnat-1.wav",
      "/sounds/ships/medium-1.wav",
      "/sounds/travel/escape-1.wav",
      "/sounds/travel/warp-1.wav");

  private HermeticSounds() {
  }

  /** A library over the fixtures, with everything else silent. */
  static SampleLibrary library() {
    return new SampleLibrary(HermeticSounds::open);
  }

  private static InputStream open(String resource) {
    if(!FIXTURES.contains(resource)) {
      return null;
    }
    return HermeticSounds.class.getResourceAsStream(resource);
  }
}
