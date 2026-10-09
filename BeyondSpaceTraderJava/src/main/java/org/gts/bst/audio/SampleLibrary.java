/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * The samples of the game, read from the {@code sounds/} folder (see ADR 0007):
 * the first WAV that matches a key, converted to 16-bit 44.1 kHz mono, or
 * silence when nothing matches. A key can have variants (the files ending in
 * {@code -<n>.wav}, as in {@code combat/hit-1.wav}) and one of them is chosen at
 * random every time the key is asked for.
 *
 * <p>The files are read from disk on the first use and kept in memory; dropping
 * a WAV into {@code sounds/} needs no recompilation, only a restart (or asking
 * for a key that was not cached yet). Everything is optional: a missing key is
 * not an error, it is silence.
 */
final class SampleLibrary {
  /** The rate of the canonical format of the mixer. */
  static final float SAMPLE_RATE = 44100f;
  private static final AudioFormat CANONICAL = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

  private final List<Path> directories;
  private final Map<String, List<float[]>> cache = new HashMap<>();
  private final Set<String> missing = new HashSet<>();

  SampleLibrary(List<Path> directories) {
    this.directories = List.copyOf(directories);
  }

  /** The folders next to the working directory, as {@code ShipArtFile} does. */
  static List<Path> defaultDirectories() {
    return List.of(Path.of("sounds"), Path.of("../sounds"), Path.of("BeyondSpaceTraderJava/sounds"));
  }

  static AudioFormat canonicalFormat() {
    return CANONICAL;
  }

  /**
   * The samples of a key (one variant at random), or {@code null} when no file
   * matches: the callers treat null as silence. The variants are loaded once
   * and the choice is made on every call, so two shots do not sound the same.
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

  /** True for a file that is the key itself or one of its numbered variants. */
  static boolean matches(String key, String fileName) {
    if(!fileName.toLowerCase(Locale.ROOT).endsWith(".wav")) {
      return false;
    }
    String stem = fileName.substring(0, fileName.length() - 4);
    if(stem.equals(key)) {
      return true;
    }
    String prefix = key + "-";
    if(!stem.startsWith(prefix)) {
      return false;
    }
    String suffix = stem.substring(prefix.length());
    return !suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit);
  }

  private List<float[]> load(String key) {
    List<Path> matches = new ArrayList<>();
    for(Path directory : directories) {
      addVariants(directory, key, matches);
    }
    List<float[]> variants = new ArrayList<>(matches.size());
    for(Path file : matches) {
      float[] samples = read(file);
      if(samples != null) {
        variants.add(samples);
      }
    }
    return variants;
  }

  private static void addVariants(Path directory, String key, List<Path> matches) {
    int slash = key.lastIndexOf('/');
    Path folder = slash < 0 ? directory : directory.resolve(key.substring(0, slash));
    String base = slash < 0 ? key : key.substring(slash + 1);
    if(!Files.isDirectory(folder)) {
      return;
    }
    try(Stream<Path> files = Files.list(folder)) {
      files.filter(Files::isRegularFile)
          .sorted()
          .forEach(file -> {
            Path name = file.getFileName();
            if(name != null && matches(base, name.toString())) {
              matches.add(file);
            }
          });
    } catch(IOException e) {
      warn("cannot list " + folder + " (" + e.getMessage() + ")");
    }
  }

  /** Reads a WAV and converts it to the canonical format; null when it cannot. */
  private static float[] read(Path file) {
    try {
      AudioInputStream source = AudioSystem.getAudioInputStream(file.toFile());
      try(AudioInputStream pcm = source.getFormat().matches(CANONICAL)
          ? source : AudioSystem.getAudioInputStream(CANONICAL, source)) {
        byte[] bytes = pcm.readAllBytes();
        float[] samples = new float[bytes.length / 2];
        for(int i = 0; i < samples.length; i++) {
          short value = (short)((bytes[2 * i] & 0xFF) | (bytes[2 * i + 1] << 8));
          samples[i] = value / 32768f;
        }
        return samples;
      }
    } catch(UnsupportedAudioFileException | IOException | IllegalArgumentException e) {
      warn("cannot load " + file + " (" + e.getMessage() + ")");
      return null;
    }
  }

  /** A warning to the console: a broken or unsupported file is ignored, not fatal. */
  private static void warn(String message) {
    System.err.println("sound: " + message);
  }
}
