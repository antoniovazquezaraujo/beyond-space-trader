/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;


/**
 * The loader over the WAV fixtures of {@code src/test/resources/sounds/}: the
 * classpath probing works inside the jar, so the tests exercise it as the game
 * does. The library is hermetic ({@link HermeticSounds}), so the real samples of
 * {@code src/main/resources/sounds/} never leak into the expectations.
 */
class SampleLibraryTest {
  private static final float WAV_TOLERANCE = 0.01f;

  @Test
  void theVariantsOfAKeyAreChosenAtRandom() {
    SampleLibrary library = HermeticSounds.library();

    Set<Float> heard = new HashSet<>();
    for(int i = 0; i < 60; i++) {
      float[] sample = library.sample("combat/hit");
      assertNotNull(sample, "the key has two fixtures");
      assertEquals(220, sample.length);
      heard.add(sample[0]);
    }

    assertTrue(heard.contains(0.25f) && heard.contains(0.5f), "both variants come out: " + heard);
  }

  @Test
  void aMissingKeyIsSilence() {
    SampleLibrary library = HermeticSounds.library();

    assertNull(library.sample("combat/nothing"), "a key without resources is silence, not an error");
    assertNull(library.sample("ui/menu-move"), "and so is a key with no variants");
  }

  @Test
  void aStrayFormatIsConvertedToTheCanonicalOne() {
    // The fixture travel/warp-1.wav is 22.05 kHz: it must arrive resampled to 44.1.
    float[] sample = HermeticSounds.library().sample("travel/warp");

    assertNotNull(sample, "the fixture is loaded through the converter");
    assertTrue(sample.length >= 4300 && sample.length <= 4500,
        "the duration is kept at 44.1 kHz (" + sample.length + " samples)");
    float peak = 0;
    for(float value : sample) {
      peak = Math.max(peak, Math.abs(value));
    }
    assertEquals(0.5f, peak, WAV_TOLERANCE, "the values survive the conversion");
  }

  @Test
  void theEngineFallbackGoesFromTypeToSizeToDefault() {
    SampleLibrary library = HermeticSounds.library();

    // Gnat (small): its own type fixture exists and wins.
    List<String> gnat = SampledSound.engineKeys(ShipType.Gnat);
    assertEquals(List.of("ships/gnat", "ships/small", "ships/default"), gnat);
    assertNotNull(library.sample(gnat.get(0)), "the type sample wins when it exists");

    // Bumblebee (medium) has no type fixture: the size one answers.
    List<String> bumblebee = SampledSound.engineKeys(ShipType.Bumblebee);
    assertEquals("ships/bumblebee", bumblebee.get(0));
    assertNull(library.sample(bumblebee.get(0)), "no sample of the type");
    assertNotNull(library.sample(bumblebee.get(1)), "the size sample answers");

    // Scorpion (huge) has neither: the shared default closes the chain.
    List<String> scorpion = SampledSound.engineKeys(ShipType.Scorpion);
    assertEquals("ships/huge", scorpion.get(1));
    assertNull(library.sample(scorpion.get(0)));
    assertNull(library.sample(scorpion.get(1)));
    assertNotNull(library.sample(scorpion.get(2)), "the default sample answers last");
  }
}
