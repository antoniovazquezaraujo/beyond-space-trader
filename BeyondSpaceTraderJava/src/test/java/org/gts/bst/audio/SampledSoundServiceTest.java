/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * The playback side of {@link SampledSound}, without a device: the factory
 * fallback and the mute gate are exercised over a recording line, never over a
 * real audio line (the suite runs headless).
 */
class SampledSoundServiceTest {
  private static final long WAIT_MILLIS = 5000;

  @Test
  void theFactoryFallsBackWithoutALineAndPlaysThroughTheInjectedOne(@TempDir Path dir)
      throws IOException, InterruptedException {
    Path file = dir.resolve("combat/laser-pulse.wav");
    Files.createDirectories(file.getParent());
    writeWav(file, 0.5f, 4410);
    SampleLibrary library = new SampleLibrary(List.of(dir));

    assertSame(SoundService.NONE, SampledSound.create(() -> true, format -> {
      throw new LineUnavailableException("the device is busy");
    }, library), "LineUnavailableException falls back to silence");
    assertSame(SoundService.NONE, SampledSound.create(() -> true, format -> {
      throw new IllegalArgumentException("no line matches the format");
    }, library), "IllegalArgumentException falls back to silence");

    // The laser sample reaches the line as sound.
    RecordingLine line = new RecordingLine();
    SoundService sound = SampledSound.create(() -> true, line::open, library);
    sound.play(SoundEffect.LASER);
    assertTrue(line.sounded.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "the laser sample reaches the line");
    assertTrue(hasSound(line.firstNonSilent()), "the written block is not silence");

    // The mute gate: the call with the sound off never reaches the line.
    boolean[] enabled = {false};
    RecordingLine muted = new RecordingLine();
    SoundService gated = SampledSound.create(() -> enabled[0], muted::open, library);
    gated.play(SoundEffect.LASER);
    assertNull(muted.firstNonSilent(), "a muted call never reaches the line");
    enabled[0] = true;
    gated.play(SoundEffect.LASER);
    assertTrue(muted.sounded.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "and it sounds again when enabled");
  }

  private static void writeWav(Path file, float value, int samples) throws IOException {
    byte[] bytes = new byte[samples * 2];
    int sample = Math.round(value * Short.MAX_VALUE);
    for(int i = 0; i < samples; i++) {
      bytes[2 * i] = (byte)(sample & 0xFF);
      bytes[2 * i + 1] = (byte)((sample >> 8) & 0xFF);
    }
    try(AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(bytes),
        SampleLibrary.canonicalFormat(), samples)) {
      AudioSystem.write(in, AudioFileFormat.Type.WAVE, file.toFile());
    }
  }

  private static boolean hasSound(byte[] block) {
    if(block == null) {
      return false;
    }
    for(byte value : block) {
      if(value != 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * A {@link SourceDataLine} proxy that records the first non-silent block and
   * counts it down: no device, no native library. Every write sleeps a moment,
   * as a real line does, so the playback thread is paced.
   */
  private static final class RecordingLine {
    private final AtomicReference<byte[]> first = new AtomicReference<>();
    private final CountDownLatch sounded = new CountDownLatch(1);

    SourceDataLine open(AudioFormat format) {
      return (SourceDataLine)Proxy.newProxyInstance(SampledSoundServiceTest.class.getClassLoader(),
          new Class<?>[] {SourceDataLine.class}, (proxy, method, args) -> {
            if("write".equals(method.getName()) && args != null && args.length == 3) {
              byte[] block = Arrays.copyOf((byte[])args[0], (int)args[2]);
              if(hasSound(block) && first.compareAndSet(null, block)) {
                sounded.countDown();
              }
              Thread.sleep(20);
              return (int)args[2];
            }
            return primitiveDefault(method.getReturnType());
          });
    }

    byte[] firstNonSilent() {
      return first.get();
    }
  }

  private static Object primitiveDefault(Class<?> type) {
    if(!type.isPrimitive() || type == void.class) {
      return null;
    }
    if(type == boolean.class) {
      return false;
    }
    if(type == byte.class) {
      return (byte)0;
    }
    if(type == short.class) {
      return (short)0;
    }
    if(type == char.class) {
      return (char)0;
    }
    if(type == int.class) {
      return 0;
    }
    if(type == long.class) {
      return 0L;
    }
    if(type == float.class) {
      return 0f;
    }
    return 0d;
  }
}
