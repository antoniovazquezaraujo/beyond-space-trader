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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.junit.jupiter.api.Test;
import spacetrader.GameOptions;


/**
 * The playback side of {@link SynthesizedSound}, without a device: the factory
 * fallback and the mute gate are exercised over a recording line, never over a
 * real audio line (the suite runs headless).
 */
class SynthesizedSoundServiceTest {
  private static final long WAIT_MILLIS = 5000;

  @Test
  void aMachineWithoutALineGetsTheSilentService() {
    assertSame(SoundService.NONE, SynthesizedSound.create(() -> true, format -> {
      throw new LineUnavailableException("the device is busy");
    }), "LineUnavailableException falls back to silence");
    assertSame(SoundService.NONE, SynthesizedSound.create(() -> true, format -> {
      throw new IllegalArgumentException("no line matches the format");
    }), "IllegalArgumentException falls back to silence");
  }

  @Test
  void theSoundOptionGatesThePlaybackAsItChanges() throws InterruptedException {
    GameOptions options = new GameOptions(false);
    RecordingLine line = new RecordingLine();
    SoundService sound = SynthesizedSound.create(options::getSound, line::open);

    // The move of a muted game never reaches the line; the warp of the game with
    // the sound on is the first buffer that does (the queue is played in order).
    options.setSound(false);
    sound.play(SoundEffect.MENU_MOVE);
    options.setSound(true);
    sound.play(SoundEffect.WARP);

    assertTrue(line.written.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "the enabled effect reaches the line");
    assertArrayEquals(SynthesizedSound.pcm(SoundEffect.WARP), line.firstWrite(),
        "the muted effect never reached the line before the warp");
  }

  @Test
  void theRivalLaserToneFollowsThePilot() throws InterruptedException {
    RecordingLine easy = new RecordingLine();
    SynthesizedSound.create(() -> true, easy::open).playRivalLaser(true);
    assertTrue(easy.written.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "the easy rival fires");
    assertArrayEquals(SynthesizedSound.laserPcm(true), easy.firstWrite(), "a weak pilot gets the low tone");

    RecordingLine normal = new RecordingLine();
    SynthesizedSound.create(() -> true, normal::open).playRivalLaser(false);
    assertTrue(normal.written.await(WAIT_MILLIS, TimeUnit.MILLISECONDS), "the good pilot fires");
    assertArrayEquals(SynthesizedSound.pcm(SoundEffect.LASER), normal.firstWrite(), "and gets the normal tone");
  }

  /**
   * A {@link SourceDataLine} proxy that records the first buffer written and
   * counts it down: no device, no native library.
   */
  private static final class RecordingLine {
    private final AtomicReference<byte[]> first = new AtomicReference<>();
    private final CountDownLatch written = new CountDownLatch(1);

    SourceDataLine open(AudioFormat format) {
      return (SourceDataLine)Proxy.newProxyInstance(SynthesizedSoundServiceTest.class.getClassLoader(),
          new Class<?>[] {SourceDataLine.class}, (proxy, method, args) -> {
            if("write".equals(method.getName()) && args != null && args.length == 3) {
              if(first.compareAndSet(null, Arrays.copyOf((byte[])args[0], (int)args[2]))) {
                written.countDown();
              }
              return null;
            }
            return primitiveDefault(method.getReturnType());
          });
    }

    byte[] firstWrite() {
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
