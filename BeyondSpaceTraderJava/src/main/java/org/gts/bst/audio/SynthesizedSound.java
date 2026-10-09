/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;


/**
 * The sound effects of the game, synthesised in code: no audio files travel in
 * the package (see ADR 0006). The PCM is 16-bit mono at {@value #SAMPLE_RATE} Hz,
 * every effect is rendered once at startup and a daemon thread drains a small
 * queue onto the {@link SourceDataLine}, so {@link #play} never blocks the game.
 *
 * <p>The timbre is the beeper of a ZX Spectrum: the wave is crushed to one bit
 * (every sample at the full level, positive or negative), the notes are pulse
 * waves, the sweeps are arpeggios by steps and the noise is switched on and off
 * in bursts. The only softness is a ramp of a millisecond or two at the edges
 * (the DC click) and a soft clip that keeps the high master level from
 * saturating; everything between the edges is hard and square.
 *
 * <p>{@link #create} returns {@link SoundService#NONE} when the machine has no
 * usable audio line (headless servers, containers), and {@link #pcm} renders an
 * effect without touching any device, which is what the tests use.
 */
public final class SynthesizedSound implements SoundService {
  /** The sample rate of every rendered effect. */
  public static final float SAMPLE_RATE = 44100f;
  private static final int BITS = 16;
  private static final int CHANNELS = 1;
  /** The full level of the one-bit output, as a fraction of full scale. */
  private static final double AMPLITUDE = 0.85;
  /** The attack ramp that keeps the DC click away; the beeper edges are hard. */
  private static final double ATTACK_SECONDS = 0.0015;
  /** A little longer at the end, so the buffer falls to silence without a pop. */
  private static final double RELEASE_SECONDS = 0.004;
  /** The playback queue drops effects when the game fires faster than they play. */
  private static final int QUEUE_SIZE = 8;
  /** The vibrato of the laser: an unsmoothed jump of this ratio every few ms. */
  private static final double LASER_VIBRATO = 1.08;
  private static final double VIBRATO_SECONDS = 0.007;

  private final BooleanSupplier enabled;
  private final SourceDataLine line;
  private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
  private final Map<SoundEffect, byte[]> buffers = new EnumMap<>(SoundEffect.class);
  private final byte[] lowLaser;

  private SynthesizedSound(BooleanSupplier enabled, SourceDataLine line) {
    this.enabled = enabled;
    this.line = line;
    for(SoundEffect effect : SoundEffect.values()) {
      buffers.put(effect, pcm(effect));
    }
    lowLaser = laserPcm(true);
  }

  /** Warms up the playback thread (called by the factory, never from the constructor). */
  private void startPlayer() {
    Thread player = new Thread(this::drain, "synth-sound");
    player.setDaemon(true);
    player.start();
  }

  /** Opens the line of a format; the tests replace it to run with no device. */
  @FunctionalInterface
  interface LineOpener {
    SourceDataLine open(AudioFormat format) throws LineUnavailableException;
  }

  /**
   * A service over the default audio device, or {@link SoundService#NONE} when
   * there is none. {@code enabled} is read on every call, so the player can turn
   * the sound off (and on) without rebuilding the service.
   */
  public static SoundService create(BooleanSupplier enabled) {
    return create(enabled,
        format -> (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format)));
  }

  /**
   * The same, with the line source injected: the tests exercise the fallback and
   * the mute gate with this, without opening a real audio device.
   */
  static SoundService create(BooleanSupplier enabled, LineOpener opener) {
    AudioFormat format = format();
    try {
      SourceDataLine line = opener.open(format);
      line.open(format);
      line.start();
      SynthesizedSound sound = new SynthesizedSound(enabled == null ? () -> true : enabled, line);
      sound.startPlayer();
      return sound;
    } catch(LineUnavailableException | IllegalArgumentException e) {
      // No audio backend at all: the game runs in silence instead of crashing.
      return SoundService.NONE;
    }
  }

  /**
   * The PCM of an effect (16-bit little-endian mono), generated in code without
   * opening any audio line: the tests and the playback buffers use this.
   */
  public static byte[] pcm(SoundEffect effect) {
    switch(effect) {
      case MENU_MOVE:
        // A short high blip: a thin pulse, gone at once.
        return toPcm(shaped(pulse(2000, 0.25, 0.05)));
      case MENU_SELECT:
        // Two quick notes going up: 660 then 990.
        return toPcm(shaped(concat(pulse(660, 0.5, 0.055), rest(0.01), pulse(990, 0.5, 0.09))));
      case ALERT:
        // Three high pulses, one after the other.
        return toPcm(shaped(concat(pulse(1200, 0.25, 0.055), rest(0.04), pulse(1200, 0.25, 0.055),
            rest(0.04), pulse(1200, 0.25, 0.055))));
      case WARNING:
        // A grave/acute alternation: low, high, low.
        return toPcm(shaped(concat(pulse(330, 0.5, 0.09), pulse(660, 0.5, 0.09), pulse(330, 0.5, 0.12))));
      case LASER:
        return laserPcm(false);
      case HIT:
        // Short, strong gated noise: two bursts and a tail.
        return toPcm(shaped(concat(noise(0x51ED270BL, 0.05), rest(0.012), noise(0x2545F491L, 0.04),
            rest(0.012), noise(0x9E3779B9L, 0.03))));
      case EXPLOSION:
        // Gated noise that thins out as it fades (bursts shorter, silences longer).
        return toPcm(shaped(gatedExplosion(0x1BADB002L, 10)));
      case WARP:
        // A climb by steps, alternating the timbre of the pulses.
        return toPcm(shaped(arpeggio(
            new double[] {220, 277, 330, 392, 440, 523, 659, 784, 988, 1319, 1568},
            0.055, 0.5, 0.25)));
      default:
        return new byte[0];
    }
  }

  /** The laser of the rival: the same stepped pew, lower and longer for an easy opponent. */
  static byte[] laserPcm(boolean low) {
    double[] steps = low
        ? new double[] {900, 760, 640, 540, 450, 380, 320, 270}
        : new double[] {1800, 1520, 1280, 1080, 900, 760, 640, 540};
    return toPcm(shaped(steppedLaser(steps, low ? 0.034 : 0.028)));
  }

  @Override
  public void play(SoundEffect effect) {
    if(effect == null || !enabled.getAsBoolean()) {
      return;
    }
    offer(buffers.get(effect));
  }

  @Override
  public void playRivalLaser(boolean easy) {
    if(!enabled.getAsBoolean()) {
      return;
    }
    offer(easy ? lowLaser : buffers.get(SoundEffect.LASER));
  }

  /** Queues a buffer; when the queue is full the effect is dropped, never waited for. */
  private void offer(byte[] buffer) {
    if(buffer != null && !queue.offer(buffer)) {
      // The queue is full: the effect drops instead of blocking the game.
      return;
    }
  }

  /** Writes the queued effects to the line, one after the other. */
  private void drain() {
    try {
      while(!Thread.currentThread().isInterrupted()) {
        byte[] buffer = queue.take();
        line.write(buffer, 0, buffer.length);
      }
    } catch(InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static AudioFormat format() {
    return new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
  }

  /** A pulse (duty 0..1) held for a time: the raw voice of the beeper. */
  private static double[] pulse(double frequency, double duty, double seconds) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    double period = SAMPLE_RATE / frequency;
    double on = period * duty;
    for(int i = 0; i < total; i++) {
      out[i] = i % period < on ? 1 : -1;
    }
    return out;
  }

  /** An arpeggio: pulse steps one after the other, alternating two duties. */
  private static double[] arpeggio(double[] frequencies, double secondsEach, double dutyEven, double dutyOdd) {
    double[][] cells = new double[frequencies.length][];
    for(int i = 0; i < frequencies.length; i++) {
      cells[i] = pulse(frequencies[i], i % 2 == 0 ? dutyEven : dutyOdd, secondsEach);
    }
    return concat(cells);
  }

  /**
   * A "pew" by steps with an unsmoothed vibrato: the frequency jumps between the
   * step and a slightly higher one every few milliseconds, never glides.
   */
  private static double[] steppedLaser(double[] steps, double secondsEach) {
    int perStep = sampleCount(secondsEach);
    int vibrato = Math.max(1, sampleCount(VIBRATO_SECONDS));
    double[] out = new double[perStep * steps.length];
    for(int i = 0; i < out.length; i++) {
      int step = Math.min(steps.length - 1, i / perStep);
      double frequency = steps[step] * ((i / vibrato) % 2 == 0 ? 1 : LASER_VIBRATO);
      double period = SAMPLE_RATE / frequency;
      out[i] = i % period < period / 2 ? 1 : -1;
    }
    return out;
  }

  /** One-bit white noise: the hard hiss of the beeper. */
  private static double[] noise(long seed, double seconds) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    long state = seed == 0 ? 1 : seed;
    for(int i = 0; i < total; i++) {
      state ^= state << 13;
      state ^= state >>> 7;
      state ^= state << 17;
      out[i] = state < 0 ? -1 : 1;
    }
    return out;
  }

  /**
   * The explosion: noise switched on and off, with the bursts shorter and the
   * silences longer as it decays, like a beeper cutting in and out.
   */
  private static double[] gatedExplosion(long seed, int bursts) {
    double[] out = new double[0];
    for(int i = 0; i < bursts; i++) {
      out = concat(out, noise(seed + i, 0.05 - 0.003 * i));
      if(i < bursts - 1) {
        out = concat(out, rest(0.01 + 0.005 * i));
      }
    }
    return out;
  }

  /** Silence of a length: the gaps between the blips. */
  private static double[] rest(double seconds) {
    return new double[sampleCount(seconds)];
  }

  /**
   * The hard envelope of the beeper: a ramp of a millisecond or two keeps the DC
   * click away, but everything between the edges keeps the full one-bit level,
   * so the blips and the clicks stay sharp.
   */
  private static double[] shaped(double[] wave) {
    int attack = sampleCount(ATTACK_SECONDS);
    int release = sampleCount(RELEASE_SECONDS);
    double[] out = wave.clone();
    for(int i = 0; i < attack && i < out.length; i++) {
      out[i] *= i / (double)attack;
    }
    for(int i = 0; i < release && i < out.length; i++) {
      out[out.length - 1 - i] *= i / (double)release;
    }
    return out;
  }

  private static int sampleCount(double seconds) {
    return Math.max(1, (int)Math.round(SAMPLE_RATE * seconds));
  }

  private static double[] concat(double[]... parts) {
    int length = 0;
    for(double[] part : parts) {
      length += part.length;
    }
    double[] out = new double[length];
    int at = 0;
    for(double[] part : parts) {
      System.arraycopy(part, 0, out, at, part.length);
      at += part.length;
    }
    return out;
  }

  /** The waveform as 16-bit little-endian PCM, through the soft clip of the master level. */
  private static byte[] toPcm(double[] samples) {
    byte[] bytes = new byte[samples.length * 2];
    for(int i = 0; i < samples.length; i++) {
      double value = softClip(samples[i]);
      int sample = (int)Math.round(value * Short.MAX_VALUE);
      bytes[2 * i] = (byte)(sample & 0xFF);
      bytes[2 * i + 1] = (byte)((sample >> 8) & 0xFF);
    }
    return bytes;
  }

  /** A cubic soft clip: full at the one-bit edges, gentle with the ramp values. */
  private static double softClip(double value) {
    double limited = Math.max(-1, Math.min(1, value));
    return AMPLITUDE * (1.5 * limited - 0.5 * limited * limited * limited);
  }
}
