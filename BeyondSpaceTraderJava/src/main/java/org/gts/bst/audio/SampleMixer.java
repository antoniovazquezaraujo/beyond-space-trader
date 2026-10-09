/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;


/**
 * Mixes the active voices into a float buffer: the one-shots (effects, shots),
 * the ambience loop of the screen, the music loop and the two engine loops (the
 * player and the rival). Each voice carries its gain and a short fade in/out,
 * and each {@link SoundChannel} has its own volume.
 *
 * <p>It has no thread and no device: {@link SampledSound} calls {@link #render}
 * from its playback thread, and the tests call it directly, so the mixing is
 * deterministic.
 */
final class SampleMixer {
  /** The crossfade (and default fade) of a loop, in seconds. */
  private static final double LOOP_FADE_SECONDS = 0.3;
  /** A one-shot fades in and out this fast, so no voice ever clicks. */
  private static final double SHOT_FADE_IN_SECONDS = 0.004;
  private static final double SHOT_FADE_OUT_SECONDS = 0.012;

  private final BooleanSupplier enabled;
  private final List<Voice> voices = new ArrayList<>();
  private final Map<SoundChannel, Float> volumes = new EnumMap<>(SoundChannel.class);
  private Voice ambience;
  private Voice music;
  private final Voice[] engines = new Voice[2];

  SampleMixer(BooleanSupplier enabled) {
    this.enabled = enabled;
    // Sensible defaults with headroom: the effects over the loops, the music back.
    volumes.put(SoundChannel.EFFECTS, 0.8f);
    volumes.put(SoundChannel.AMBIENCE, 0.55f);
    volumes.put(SoundChannel.MUSIC, 0.5f);
    volumes.put(SoundChannel.ENGINES, 0.5f);
  }

  void setVolume(SoundChannel channel, float volume) {
    volumes.put(channel, Math.max(0f, Math.min(1f, volume)));
  }

  /** Starts a one-shot (an effect) at the given gain. */
  void play(float[] sample, float gain) {
    if(sample != null && sample.length > 0) {
      voices.add(new Voice(sample, SoundChannel.EFFECTS, false, Math.max(0f, gain)));
    }
  }

  /** Starts the ambience loop, crossfading out the previous one. */
  void startAmbience(float[] sample) {
    ambience = startLoop(ambience, sample, SoundChannel.AMBIENCE);
  }

  void stopAmbience() {
    stopAmbience(LOOP_FADE_SECONDS);
  }

  void stopAmbience(double fadeSeconds) {
    stopLoop(ambience, fadeSeconds);
    ambience = null;
  }

  /** Starts the music loop, crossfading out the previous mood. */
  void startMusic(float[] sample) {
    music = startLoop(music, sample, SoundChannel.MUSIC);
  }

  void stopMusic() {
    stopMusic(LOOP_FADE_SECONDS);
  }

  void stopMusic(double fadeSeconds) {
    stopLoop(music, fadeSeconds);
    music = null;
  }

  /** Starts or replaces the loop of an engine (the player or the rival). */
  void startEngine(float[] sample, boolean player) {
    int slot = player ? 0 : 1;
    engines[slot] = startLoop(engines[slot], sample, SoundChannel.ENGINES);
  }

  void stopEngine(boolean player) {
    stopEngine(player, LOOP_FADE_SECONDS);
  }

  /** Stops an engine with a fade of its own: short on destruction, long on a march. */
  void stopEngine(boolean player, double fadeSeconds) {
    int slot = player ? 0 : 1;
    stopLoop(engines[slot], fadeSeconds);
    engines[slot] = null;
  }

  /**
   * Mixes the next block into {@code out}, adding the voices to the buffer; the
   * caller passes a buffer of {@code frames} samples. With the sound off the
   * block is silence (the loops wait, they do not advance).
   */
  void render(float[] out, int frames) {
    Arrays.fill(out, 0, frames, 0f);
    if(!enabled.getAsBoolean()) {
      return;
    }
    for(Iterator<Voice> iterator = voices.iterator(); iterator.hasNext();) {
      Voice voice = iterator.next();
      mix(out, frames, voice);
      if(voice.isFinished()) {
        iterator.remove();
      }
    }
  }

  private Voice startLoop(Voice old, float[] sample, SoundChannel channel) {
    stopLoop(old);
    if(sample == null || sample.length == 0) {
      return null;
    }
    Voice voice = new Voice(sample, channel, true, 1f);
    voice.gain = 0f;
    voice.target = 1f;
    voices.add(voice);
    return voice;
  }

  private static void stopLoop(Voice voice) {
    stopLoop(voice, LOOP_FADE_SECONDS);
  }

  private static void stopLoop(Voice voice, double fadeSeconds) {
    if(voice != null) {
      voice.target = 0f;
      voice.fadeOut(fadeSeconds);
    }
  }

  private void mix(float[] out, int frames, Voice voice) {
    float volume = volumes.get(voice.channel);
    int fadeIn = (int)(SHOT_FADE_IN_SECONDS * SampleLibrary.SAMPLE_RATE);
    int fadeOut = (int)(SHOT_FADE_OUT_SECONDS * SampleLibrary.SAMPLE_RATE);
    for(int i = 0; i < frames && !voice.isFinished(); i++) {
      out[i] += voice.data[voice.position] * voice.gainAt(fadeIn, fadeOut) * volume;
      voice.advance();
    }
  }

  /** A voice: a sample being played, with its position, its gain and its fades. */
  private static final class Voice {
    private final float[] data;
    private final SoundChannel channel;
    private final boolean loop;
    private final float level;
    private int position;
    private float gain;
    private float target;
    private float fadeStep;

    Voice(float[] data, SoundChannel channel, boolean loop, float level) {
      this.data = data;
      this.channel = channel;
      this.loop = loop;
      this.level = level;
      this.gain = level;
      this.target = level;
      this.fadeStep = fadeStep(LOOP_FADE_SECONDS);
    }

    void fadeOut(double seconds) {
      fadeStep = fadeStep(seconds);
    }

    private static float fadeStep(double seconds) {
      return (float)(1.0 / (Math.max(0.02, seconds) * SampleLibrary.SAMPLE_RATE));
    }

    float gainAt(int fadeIn, int fadeOut) {
      if(loop) {
        return gain;
      }
      float shaped = Math.min(1f, position / (float)fadeIn);
      shaped = Math.min(shaped, (data.length - position) / (float)fadeOut);
      return level * shaped;
    }

    void advance() {
      position++;
      if(loop && position >= data.length) {
        position = 0;
      }
      if(target > gain) {
        gain = Math.min(target, gain + fadeStep);
      } else if(target < gain) {
        gain = Math.max(target, gain - fadeStep);
      }
    }

    boolean isFinished() {
      if(loop) {
        return target <= 0f && gain <= 0f;
      }
      return position >= data.length;
    }
  }
}
