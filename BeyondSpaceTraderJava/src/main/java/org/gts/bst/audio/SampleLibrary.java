/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * The samples of the game, bundled in the jar as classpath resources (see ADR
 * 0007): {@code /sounds/<key>-<n>.wav}, converted to 16-bit 44.1 kHz mono, or
 * silence when nothing matches. A key can have several numbered variants
 * ({@code combat/hit-1.wav}, {@code -2}, ...) and one of them is chosen at
 * random every time the key is asked for.
 *
 * <p>The variants are probed on the classpath ({@code -1} first, then {@code -2}
 * and so on, up to {@link #MAX_VARIANTS}) instead of listing folders, so the
 * loader also works inside the jar. Everything is optional: a missing key is not
 * an error, it is silence.
 */
final class SampleLibrary {
  /** The rate of the canonical format of the mixer. */
  static final float SAMPLE_RATE = 44100f;
  /** The variants of a key are probed from 1 to this number. */
  static final int MAX_VARIANTS = 9;
  /** The classpath folder of the samples, inside the jar. */
  static final String RESOURCE_ROOT = "/sounds/";
  private static final AudioFormat CANONICAL = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

  private final Map<String, List<float[]>> cache = new HashMap<>();
  private final Set<String> missing = new HashSet<>();

  static AudioFormat canonicalFormat() {
    return CANONICAL;
  }

  /**
   * The samples of a key (one variant at random), or {@code null} when no
   * resource matches: the callers treat null as silence. The variants are loaded
   * once and the choice is made on every call, so two shots do not sound the
   * same.
   */
  synchronized float[] sample(String key) {
    if(key == null || missing.contains(key)) {
      return null;
    }
    List<float[]> variants = cache.get(key);
    if(variants == null) {
      variants = load(key);
      if(variants.isEmpty()) {
        missing.add(key);
        return null;
      }
      cache.put(key, variants);
    }
    return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
  }

  /** Probes {@code key-1.wav}, {@code key-2.wav}... while they appear. */
  private List<float[]> load(String key) {
    List<float[]> variants = new ArrayList<>();
    for(int variant = 1; variant <= MAX_VARIANTS; variant++) {
      float[] samples = read(RESOURCE_ROOT + key + "-" + variant + ".wav");
      if(samples == null) {
        // The variants are numbered without gaps: the first one that is missing
        // ends the run.
        break;
      }
      variants.add(samples);
    }
    return variants;
  }

  /** Reads a WAV resource and converts it to the canonical format; null when missing or broken. */
  private static float[] read(String resource) {
    InputStream raw = SampleLibrary.class.getResourceAsStream(resource);
    if(raw == null) {
      return null;
    }
    // Inside the jar the resource stream (JarURLInputStream) does not support
    // mark/reset and the audio system needs it; on a directory classpath the
    // stream is already buffered. Wrap it when needed, keeping the close order.
    try(InputStream stream = raw.markSupported() ? raw : new BufferedInputStream(raw);
        AudioInputStream fileIn = AudioSystem.getAudioInputStream(stream)) {
      AudioInputStream pcm = fileIn.getFormat().matches(CANONICAL)
          ? fileIn : AudioSystem.getAudioInputStream(CANONICAL, fileIn);
      byte[] bytes = pcm.readAllBytes();
      float[] samples = new float[bytes.length / 2];
      for(int i = 0; i < samples.length; i++) {
        short value = (short)((bytes[2 * i] & 0xFF) | (bytes[2 * i + 1] << 8));
        samples[i] = value / 32768f;
      }
      return samples;
    } catch(UnsupportedAudioFileException | IOException | IllegalArgumentException e) {
      warn("cannot load " + resource + " (" + e.getMessage() + ")");
      return null;
    }
  }

  /** A warning to the console: a broken or unsupported file is ignored, not fatal. */
  private static void warn(String message) {
    System.err.println("sound: " + message);
  }
}
