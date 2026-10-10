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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.junit.jupiter.api.Test;


/**
 * The playback side of {@link SampledSound}, without a device: the factory
 * fallback and the mute gate are exercised over a recording line, never over a
 * real audio line (the suite runs headless). The sample is the fixture
 * {@code src/test/resources/sounds/combat/laser-pulse-1.wav}.
 */
class SampledSoundServiceTest {
  private static final long WAIT_MILLIS = 5000;

  @Test
  void theFactoryFallsBackWithoutALineAndPlaysThroughTheInjectedOne() throws InterruptedException {
    SampleLibrary library = HermeticSounds.library();

    assertSame(SoundService.NONE, SampledSound.create(() -> true, format -> {
      throw new LineUnavailableException("the device is busy");
    }, library), "LineUnavailableException falls back to silence");
    assertSame(SoundService.NONE, SampledSound.create(() -> true, format -> {
      throw new IllegalArgumentException("no line matches the format");
    }, library), "IllegalArgumentException falls back to silence");

    // The laser fixture reaches the line as sound.
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

  @Test
  void theLineOpensWithABoundedBuffer() {
    RecordingLine line = new RecordingLine();

    SoundService sound = SampledSound.create(() -> true, line::open, HermeticSounds.library());

    assertNotSame(SoundService.NONE, sound, "the device exists");
    assertEquals(List.of(SampledSound.LINE_BUFFER_BYTES), line.opens(),
        "the line opens with the bounded buffer, not the slow device default");
    assertTrue(SampledSound.LINE_BUFFER_BYTES <= 16384, "the latency stays bounded");
  }

  @Test
  void aRejectedBufferSizeFallsBackToTheDeviceDefault() {
    RecordingLine line = new RecordingLine();
    line.rejectSizedOpen = true;

    SoundService sound = SampledSound.create(() -> true, line::open, HermeticSounds.library());

    assertNotSame(SoundService.NONE, sound, "the default open is better than silence");
    assertEquals(List.of(SampledSound.LINE_BUFFER_BYTES, RecordingLine.DEFAULT_OPEN), line.opens(),
        "a rejected size is followed by the default open");
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
   * A {@link SourceDataLine} proxy that records the first non-silent block, the
   * size of every {@code open} and counts the first sound down: no device, no
   * native library. Every write sleeps a moment, as a real line does, so the
   * playback thread is paced. Setting {@link #rejectSizedOpen} makes the sized
   * open fail, as some devices do.
   */
  private static final class RecordingLine {
    /** The marker of an {@code open(format)} with the device default size. */
    static final int DEFAULT_OPEN = -1;

    private final AtomicReference<byte[]> first = new AtomicReference<>();
    private final CountDownLatch sounded = new CountDownLatch(1);
    private final List<Integer> opens = new ArrayList<>();
    private boolean rejectSizedOpen;

    SourceDataLine open(AudioFormat format) {
      return (SourceDataLine)Proxy.newProxyInstance(SampledSoundServiceTest.class.getClassLoader(),
          new Class<?>[] {SourceDataLine.class}, (proxy, method, args) -> {
            if("open".equals(method.getName())) {
              if(args != null && args.length == 2) {
                opens.add((int)args[1]);
                if(rejectSizedOpen) {
                  throw new IllegalArgumentException("the device rejects the buffer size");
                }
              } else {
                opens.add(DEFAULT_OPEN);
              }
              return null;
            }
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

    /** The sizes of the opens, in order; {@link #DEFAULT_OPEN} for the default one. */
    List<Integer> opens() {
      return opens;
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
