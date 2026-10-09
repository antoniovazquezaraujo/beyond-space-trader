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
 * <p>{@link #create} returns {@link SoundService#NONE} when the machine has no
 * usable audio line (headless servers, containers), and {@link #pcm} renders an
 * effect without touching any device, which is what the tests use.
 */
public final class SynthesizedSound implements SoundService {
  /** The sample rate of every rendered effect. */
  public static final float SAMPLE_RATE = 44100f;
  private static final int BITS = 16;
  private static final int CHANNELS = 1;
  /** Peaks of the rendered effects, as a fraction of full scale: short and gentle. */
  private static final double AMPLITUDE = 0.45;
  /** The playback queue drops effects when the game fires faster than they play. */
  private static final int QUEUE_SIZE = 8;

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

  /**
   * A service over the default audio device, or {@link SoundService#NONE} when
   * there is none. {@code enabled} is read on every call, so the player can turn
   * the sound off (and on) without rebuilding the service.
   */
  public static SoundService create(BooleanSupplier enabled) {
    AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
    try {
      SourceDataLine line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
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
        return toPcm(beep(1600, 0.055, true, 6));
      case MENU_SELECT:
        return toPcm(concat(beep(900, 0.05, true, 3), gap(0.012), beep(1350, 0.085, true, 3)));
      case ALERT:
        return toPcm(concat(beep(660, 0.07, true, 2), gap(0.05), beep(660, 0.07, true, 2), gap(0.05),
            beep(990, 0.12, true, 2)));
      case WARNING:
        return toPcm(concat(beep(520, 0.12, true, 1.5), gap(0.06), beep(370, 0.22, true, 1.5)));
      case LASER:
        return laserPcm(false);
      case HIT:
        return toPcm(noiseBurst(0.16, 0x51ED270BL, 7, 0.45));
      case EXPLOSION:
        return toPcm(rumble(0.6, 0x1BADB002L));
      case WARP:
        return toPcm(glidingTone(180, 1500, 0.62, 0.02));
      default:
        return new byte[0];
    }
  }

  /** The laser of the rival: the same sweep, lower and longer for an easy opponent. */
  static byte[] laserPcm(boolean low) {
    return toPcm(sweep(low ? 750 : 1500, low ? 110 : 230, low ? 0.28 : 0.22, 2.5));
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

  /** A fixed note of {@code seconds}, with a fast decay and its attack and release. */
  private static double[] beep(double frequency, double seconds, boolean square, double decay) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    double phase = 0;
    for(int i = 0; i < total; i++) {
      double t = i / (double)total;
      phase += 2 * Math.PI * frequency / SAMPLE_RATE;
      double wave = square ? (Math.sin(phase) >= 0 ? 1 : -1) : Math.sin(phase);
      out[i] = wave * Math.exp(-decay * t) * envelope(i, total, 0.05, 0.4);
    }
    return out;
  }

  /** A square note that glides from {@code from} to {@code to} (the shot, the hit). */
  private static double[] sweep(double from, double to, double seconds, double decay) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    double phase = 0;
    for(int i = 0; i < total; i++) {
      double t = i / (double)total;
      double frequency = from + (to - from) * t;
      phase += 2 * Math.PI * frequency / SAMPLE_RATE;
      out[i] = (Math.sin(phase) >= 0 ? 1 : -1) * Math.exp(-decay * t) * envelope(i, total, 0.02, 0.35);
    }
    return out;
  }

  /** A sine sweep that climbs, with a soft vibrato: the warp. */
  private static double[] glidingTone(double from, double to, double seconds, double vibrato) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    double phase = 0;
    for(int i = 0; i < total; i++) {
      double t = i / (double)total;
      double frequency = (from + (to - from) * t) * (1 + vibrato * Math.sin(2 * Math.PI * 6 * t));
      phase += 2 * Math.PI * frequency / SAMPLE_RATE;
      out[i] = Math.sin(phase) * envelope(i, total, 0.05, 0.3);
    }
    return out;
  }

  /** A filtered white-noise burst with a sharp attack: the hit. */
  private static double[] noiseBurst(double seconds, long seed, double decay, double smoothing) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    long state = seed == 0 ? 1 : seed;
    double low = 0;
    for(int i = 0; i < total; i++) {
      state ^= state << 13;
      state ^= state >>> 7;
      state ^= state << 17;
      double white = state / (double)Long.MAX_VALUE;
      low += smoothing * (white - low);
      double t = i / (double)total;
      out[i] = low * Math.exp(-decay * t) * envelope(i, total, 0.005, 0.5);
    }
    return out;
  }

  /** The low rumble of an explosion: brown noise with a long decay. */
  private static double[] rumble(double seconds, long seed) {
    int total = sampleCount(seconds);
    double[] out = new double[total];
    long state = seed == 0 ? 1 : seed;
    double low = 0;
    for(int i = 0; i < total; i++) {
      state ^= state << 13;
      state ^= state >>> 7;
      state ^= state << 17;
      double white = state / (double)Long.MAX_VALUE;
      low += 0.08 * (white - low);
      double t = i / (double)total;
      out[i] = (low * 4 + white * 0.2) * Math.exp(-4 * t) * envelope(i, total, 0.01, 0.5);
    }
    return out;
  }

  private static int sampleCount(double seconds) {
    return Math.max(1, (int)Math.round(SAMPLE_RATE * seconds));
  }

  /** A linear attack and release over the buffer, as fractions of its length. */
  private static double envelope(int index, int total, double attack, double release) {
    double t = index / (double)total;
    double gain = Math.min(1, t / attack);
    gain = Math.min(gain, Math.max(0, (1 - t) / release));
    return Math.max(0, gain);
  }

  private static double[] gap(double seconds) {
    return new double[sampleCount(seconds)];
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

  /**
   * Scales the waveform to the moderate amplitude of the effects and turns it into
   * 16-bit signed little-endian samples.
   */
  private static byte[] toPcm(double[] samples) {
    double peak = 0;
    for(double sample : samples) {
      peak = Math.max(peak, Math.abs(sample));
    }
    double scale = peak == 0 ? 0 : AMPLITUDE / peak;
    byte[] bytes = new byte[samples.length * 2];
    for(int i = 0; i < samples.length; i++) {
      double value = Math.max(-1, Math.min(1, samples[i] * scale));
      int sample = (int)Math.round(value * Short.MAX_VALUE);
      bytes[2 * i] = (byte)(sample & 0xFF);
      bytes[2 * i + 1] = (byte)((sample >> 8) & 0xFF);
    }
    return bytes;
  }
}
