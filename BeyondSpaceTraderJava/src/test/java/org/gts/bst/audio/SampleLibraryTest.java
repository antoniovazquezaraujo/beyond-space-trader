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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


class SampleLibraryTest {
  /** A file of samples at the canonical format. */
  private static Path wav(Path dir, String relative, float[] samples) throws IOException {
    Path file = dir.resolve(relative);
    Files.createDirectories(file.getParent());
    writeWav(file, SampleLibrary.canonicalFormat(), samples);
    return file;
  }

  private static void writeWav(Path file, AudioFormat format, float[] samples) throws IOException {
    byte[] bytes = new byte[samples.length * 2];
    for(int i = 0; i < samples.length; i++) {
      int value = Math.round(samples[i] * Short.MAX_VALUE);
      bytes[2 * i] = (byte)(value & 0xFF);
      bytes[2 * i + 1] = (byte)((value >> 8) & 0xFF);
    }
    try(AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(bytes), format, samples.length)) {
      AudioSystem.write(in, AudioFileFormat.Type.WAVE, file.toFile());
    }
  }

  private static float[] constant(float value, int samples) {
    float[] out = new float[samples];
    Arrays.fill(out, value);
    return out;
  }

  @Test
  void theVariantsOfAKeyAreChosenAtRandom(@TempDir Path dir) throws IOException {
    wav(dir, "combat/hit-1.wav", constant(0.25f, 220));
    wav(dir, "combat/hit-2.wav", constant(0.5f, 220));
    SampleLibrary library = new SampleLibrary(List.of(dir));

    Set<Float> heard = new HashSet<>();
    for(int i = 0; i < 60; i++) {
      float[] sample = library.sample("combat/hit");
      assertNotNull(sample, "the key has two variants");
      assertEquals(220, sample.length);
      heard.add(sample[0]);
    }

    assertTrue(heard.contains(0.25f) && heard.contains(0.5f), "both variants come out: " + heard);
  }

  @Test
  void aMissingKeyIsSilenceAndTheFoldersFollowTheShipArtRule() {
    SampleLibrary library = new SampleLibrary(List.of(Path.of("no-such-sounds-folder")));

    assertNull(library.sample("combat/hit"), "a missing key is silence, not an error");
    assertEquals(List.of(Path.of("sounds"), Path.of("../sounds"), Path.of("BeyondSpaceTraderJava/sounds")),
        SampleLibrary.defaultDirectories(), "the candidates mirror ShipArtFile.resolve");

    assertTrue(SampleLibrary.matches("hit", "hit.wav"), "the key itself matches");
    assertTrue(SampleLibrary.matches("hit", "hit-3.wav"), "a numbered variant matches");
    assertTrue(SampleLibrary.matches("hit", "hit-10.WAV"), "the extension is case-insensitive");
    assertTrue(!SampleLibrary.matches("hit", "hit-x.wav"), "a non-numeric suffix does not match");
    assertTrue(!SampleLibrary.matches("hit", "hits-1.wav"), "the prefix must end at the dash");
    assertTrue(!SampleLibrary.matches("hit", "hit-1.ogg"), "only WAV files match");
  }

  @Test
  void aStrayFormatIsConvertedToTheCanonicalOne(@TempDir Path dir) throws IOException,
      UnsupportedAudioFileException {
    // A 0.1 s file at 22050 Hz: the library must resample it to 44.1 kHz mono.
    AudioFormat stray = new AudioFormat(22050f, 16, 1, true, false);
    Path file = dir.resolve("travel/warp.wav");
    Files.createDirectories(file.getParent());
    writeWav(file, stray, constant(0.5f, 2205));
    SampleLibrary library = new SampleLibrary(List.of(dir));

    float[] sample = library.sample("travel/warp");

    assertNotNull(sample, "the file is loaded through the converter");
    assertTrue(sample.length >= 4300 && sample.length <= 4500,
        "the duration is kept at 44.1 kHz (" + sample.length + " samples)");
    float peak = 0;
    for(float value : sample) {
      peak = Math.max(peak, Math.abs(value));
    }
    assertEquals(0.5f, peak, 0.02f, "the values survive the conversion");
  }
}
