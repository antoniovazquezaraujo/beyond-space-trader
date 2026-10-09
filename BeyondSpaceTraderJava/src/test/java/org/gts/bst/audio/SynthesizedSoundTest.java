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
import java.util.List;
import org.gts.bst.ports.SoundEffect;
import org.junit.jupiter.api.Test;


class SynthesizedSoundTest {
  private static final int BYTES_PER_SAMPLE = 2;
  /** Effects are short: the longest one stays under this many seconds. */
  private static final double MAX_SECONDS = 0.7;
  /** The full one-bit level of the beeper, as a fraction of full scale. */
  private static final double MIN_LEVEL = 0.8;
  private static final double MAX_LEVEL = 0.9;
  /** The edges carry the attack and release ramps; the body is hard and one bit. */
  private static final double EDGE_SECONDS = 0.01;
  /** The effects that are one pulse train, with no silence inside. */
  private static final List<SoundEffect> PURE_TONES =
      List.of(SoundEffect.MENU_MOVE, SoundEffect.WARNING, SoundEffect.LASER, SoundEffect.WARP);

  @Test
  void everyEffectIsShortLoudAndOneBitInTheBody() {
    int edge = (int)(SynthesizedSound.SAMPLE_RATE * EDGE_SECONDS);
    for(SoundEffect effect : SoundEffect.values()) {
      byte[] pcm = SynthesizedSound.pcm(effect);
      double seconds = pcm.length / (double)BYTES_PER_SAMPLE / SynthesizedSound.SAMPLE_RATE;
      int samples = pcm.length / BYTES_PER_SAMPLE;
      int peak = peak(pcm);
      assertTrue(pcm.length >= 2 * BYTES_PER_SAMPLE, effect + " has samples");
      assertTrue(seconds >= 0.02 && seconds <= MAX_SECONDS, effect + " lasts " + seconds + " s");
      assertTrue(energy(pcm) > 0, effect + " is not silent");
      assertTrue(peak >= Short.MAX_VALUE * MIN_LEVEL, effect + " is loud (peak " + peak + ")");
      assertTrue(peak <= Short.MAX_VALUE * MAX_LEVEL, effect + " never saturates (peak " + peak + ")");
      // Between the edge ramps every sample is silence or the full level: the
      // one-bit crush of the beeper, with no intermediate amplitudes.
      for(int i = edge; i < samples - edge; i++) {
        int value = sample(pcm, i);
        assertTrue(value == 0 || Math.abs(value) == peak,
            effect + " is one-bit at sample " + i + " (" + value + " of peak " + peak + ")");
        if(PURE_TONES.contains(effect)) {
          assertTrue(value != 0, effect + " never rests in the middle (sample " + i + ")");
        }
      }
    }
  }

  /**
   * The envelope fades every effect in and out: the buffers start and end in
   * silence. Without it the samples jump at the edges and the effects click.
   */
  @Test
  void everyEffectFadesInAndOutFromSilence() {
    for(SoundEffect effect : SoundEffect.values()) {
      byte[] pcm = SynthesizedSound.pcm(effect);
      int samples = pcm.length / BYTES_PER_SAMPLE;
      int peak = peak(pcm);
      int first = sample(pcm, 0);
      int last = sample(pcm, samples - 1);
      assertTrue(Math.abs(first) <= peak / 5,
          effect + " fades in from silence (first sample " + first + " of peak " + peak + ")");
      assertTrue(Math.abs(last) <= peak / 5,
          effect + " fades out to silence (last sample " + last + " of peak " + peak + ")");
    }
  }

  @Test
  void theSameEffectRendersTheSameSamples() {
    assertArrayEquals(SynthesizedSound.pcm(SoundEffect.EXPLOSION), SynthesizedSound.pcm(SoundEffect.EXPLOSION),
        "the synthesis is deterministic, so the tests and the playback agree");
  }

  /** The warp climbs and the laser comes down: the stepped arpeggios of the beeper. */
  @Test
  void theStepsClimbAndTheLaserComesDown() {
    byte[] warp = SynthesizedSound.pcm(SoundEffect.WARP);
    assertTrue(crossings(warp, 0, warp.length / 2) < crossings(warp, warp.length / 2, warp.length),
        "the warp climbs by steps");

    // The two notes of the select: a quarter on each side avoids the silence in
    // the middle and compares the 660 note against the 990 one.
    byte[] select = SynthesizedSound.pcm(SoundEffect.MENU_SELECT);
    int quarter = (select.length / 4) & ~1;
    assertTrue(crossings(select, 0, quarter) < crossings(select, select.length - quarter, select.length),
        "the select goes up (660 -> 990)");

    byte[] laser = SynthesizedSound.pcm(SoundEffect.LASER);
    quarter = (laser.length / 4) & ~1;
    assertTrue(crossings(laser, 0, quarter) > crossings(laser, laser.length - quarter, laser.length),
        "the laser pew comes down by steps");
  }

  @Test
  void theEasyOpponentLaserHasItsOwnLowerTone() {
    byte[] normal = SynthesizedSound.pcm(SoundEffect.LASER);
    byte[] low = SynthesizedSound.laserPcm(true);
    assertFalse(Arrays.equals(normal, low), "the laser of an easy rival is a different tone");
    assertTrue(low.length > normal.length, "the low tone is a little longer");
    assertTrue(crossings(low, 0, low.length) < crossings(normal, 0, normal.length),
        "and it sounds lower for a weak pilot");
  }

  /** The signed 16-bit little-endian sample at a position. */
  private static int sample(byte[] pcm, int index) {
    return (short)((pcm[2 * index] & 0xFF) | (pcm[2 * index + 1] << 8));
  }

  private static int peak(byte[] pcm) {
    int peak = 0;
    for(int i = 0; i < pcm.length / BYTES_PER_SAMPLE; i++) {
      peak = Math.max(peak, Math.abs(sample(pcm, i)));
    }
    return peak;
  }

  private static long energy(byte[] pcm) {
    long energy = 0;
    for(int i = 0; i < pcm.length / BYTES_PER_SAMPLE; i++) {
      energy += Math.abs(sample(pcm, i));
    }
    return energy;
  }

  /** The sign changes of the samples between two byte offsets: a pitch yardstick. */
  private static int crossings(byte[] pcm, int fromByte, int toByte) {
    int crossings = 0;
    int previous = sample(pcm, fromByte / BYTES_PER_SAMPLE);
    for(int i = fromByte / BYTES_PER_SAMPLE + 1; i < toByte / BYTES_PER_SAMPLE; i++) {
      int current = sample(pcm, i);
      if(previous != 0 && current != 0 && (previous > 0) != (current > 0)) {
        crossings++;
      }
      previous = current;
    }
    return crossings;
  }
}
