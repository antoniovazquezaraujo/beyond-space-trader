/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.gts.bst.ports.SoundEffect;
import org.junit.jupiter.api.Test;


class SynthesizedSoundTest {
  private static final int BYTES_PER_SAMPLE = 2;
  /** Effects are short: the longest one stays under this many seconds. */
  private static final double MAX_SECONDS = 0.7;

  @Test
  void everyEffectIsShortAudibleAndWithinAModerateAmplitude() {
    for(SoundEffect effect : SoundEffect.values()) {
      byte[] pcm = SynthesizedSound.pcm(effect);
      double seconds = pcm.length / (double)BYTES_PER_SAMPLE / SynthesizedSound.SAMPLE_RATE;
      assertTrue(pcm.length >= 2 * BYTES_PER_SAMPLE, effect + " has samples");
      assertTrue(seconds >= 0.02 && seconds <= MAX_SECONDS,
          effect + " lasts " + seconds + " s");
      int peak = 0;
      long energy = 0;
      for(int i = 0; i + 1 < pcm.length; i += BYTES_PER_SAMPLE) {
        int sample = sample(pcm, i / BYTES_PER_SAMPLE);
        peak = Math.max(peak, Math.abs(sample));
        energy += Math.abs(sample);
      }
      assertTrue(energy > 0, effect + " is not silent");
      assertTrue(peak > Short.MAX_VALUE * 0.2, effect + " is heard (peak " + peak + ")");
      assertTrue(peak <= Short.MAX_VALUE * 0.6, effect + " stays at a moderate amplitude (peak " + peak + ")");
    }
  }

  @Test
  void theSameEffectRendersTheSameSamples() {
    assertArrayEquals(SynthesizedSound.pcm(SoundEffect.EXPLOSION), SynthesizedSound.pcm(SoundEffect.EXPLOSION),
        "the synthesis is deterministic, so the tests and the playback agree");
  }

  @Test
  void theWarpSweepsUpwards() {
    byte[] pcm = SynthesizedSound.pcm(SoundEffect.WARP);
    int firstHalf = crossings(pcm, 0, pcm.length / 2);
    int secondHalf = crossings(pcm, pcm.length / 2, pcm.length);
    assertTrue(secondHalf > firstHalf,
        "the warp climbs: " + firstHalf + " crossings fall before " + secondHalf);
  }

  @Test
  void theEasyOpponentLaserHasItsOwnLowerTone() {
    byte[] normal = SynthesizedSound.pcm(SoundEffect.LASER);
    byte[] low = SynthesizedSound.laserPcm(true);
    assertFalse(Arrays.equals(normal, low), "the laser of an easy rival is a different tone");
    assertTrue(low.length > normal.length, "the low tone is a little longer");
  }

  /** The signed 16-bit little-endian sample at a position. */
  private static int sample(byte[] pcm, int index) {
    return (short)((pcm[2 * index] & 0xFF) | (pcm[2 * index + 1] << 8));
  }

  /** The sign changes of the samples between two positions: a pitch yardstick. */
  private static int crossings(byte[] pcm, int fromSample, int toSample) {
    int crossings = 0;
    int previous = sample(pcm, fromSample / BYTES_PER_SAMPLE);
    for(int i = fromSample / BYTES_PER_SAMPLE + 1; i < toSample / BYTES_PER_SAMPLE; i++) {
      int current = sample(pcm, i);
      if(previous != 0 && current != 0 && (previous > 0) != (current > 0)) {
        crossings++;
      }
      previous = current;
    }
    return crossings;
  }
}
