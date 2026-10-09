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

import java.util.Arrays;
import org.junit.jupiter.api.Test;


class SampleMixerTest {
  private static float[] constant(float value, int samples) {
    float[] out = new float[samples];
    Arrays.fill(out, value);
    return out;
  }

  @Test
  void theVoicesAreMixedWithTheirChannelVolumeAndTheMuteSilencesThem() {
    boolean[] enabled = {true};
    SampleMixer mixer = new SampleMixer(() -> enabled[0]);
    mixer.play(constant(0.5f, 44100), 1f);

    // A one-shot at full gain: 0.5 x the effects volume (0.8), after the fade-in.
    float[] out = new float[8820];
    mixer.render(out, out.length);
    assertEquals(0.4f, out[8000], 1e-3f, "the one-shot is mixed with the effects volume");

    // The channel volume is mutable and applies at once.
    mixer.setVolume(SoundChannel.EFFECTS, 0.5f);
    float[] quiet = new float[1000];
    mixer.render(quiet, quiet.length);
    assertEquals(0.25f, quiet[0], 1e-3f, "the new volume is heard at once");

    // With the sound off the block is silence, even with voices alive.
    enabled[0] = false;
    float[] muted = new float[1000];
    mixer.render(muted, muted.length);
    for(float value : muted) {
      assertEquals(0f, value, 1e-6f, "a muted mixer renders silence");
    }
  }

  @Test
  void anAmbienceChangeCrossfadesFromOneLoopToTheOther() {
    SampleMixer mixer = new SampleMixer(() -> true);
    mixer.startAmbience(constant(1f, 2 * 44100));

    float[] first = new float[44100];
    mixer.render(first, first.length);
    assertEquals(0.55f, first[40000], 1e-3f, "the first ambience is fully on");

    // The new loop fades in while the old one fades out (0.25 s): midday both
    // are at half gain, so the opposites cancel; at the end only the new one is.
    mixer.startAmbience(constant(-1f, 2 * 44100));
    float[] change = new float[44100];
    mixer.render(change, change.length);
    assertEquals(0.55f, change[0], 0.02f, "the old ambience still sounds at the start");
    assertEquals(0f, change[5512], 0.02f, "the crossfade crosses at half");
    assertEquals(-0.55f, change[40000], 1e-3f, "and the new one is alone at the end");
  }

  @Test
  void theLoopsCanBeStoppedAndFadeOut() {
    SampleMixer mixer = new SampleMixer(() -> true);
    mixer.startEngine(constant(1f, 44100), true);

    float[] playing = new float[44100];
    mixer.render(playing, playing.length);
    assertEquals(0.5f, playing[40000], 1e-3f, "the engine loop sounds at its channel volume");

    mixer.stopEngine(true);
    float[] stopping = new float[44100];
    mixer.render(stopping, stopping.length);
    assertEquals(0.5f, stopping[0], 1e-3f, "the loop starts fading from its full gain");
    assertEquals(0f, stopping[stopping.length - 1], 1e-3f, "and it is gone at the end of the fade");
  }
}
