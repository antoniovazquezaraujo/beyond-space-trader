/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.AmbienceKey;
import org.gts.bst.ports.MusicTheme;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.WeaponType;


/**
 * The sample engine of the game: it plays the WAV files of the {@code sounds/}
 * folder (see ADR 0007), all of them optional. The samples are read and
 * converted by {@link SampleLibrary} and mixed by {@link SampleMixer} in a
 * daemon thread that writes to the {@link SourceDataLine}, so the calls of the
 * port never block the game.
 *
 * <p>The keys are {@code ui/menu-move}, {@code combat/laser-pulse},
 * {@code travel/warp}... for the effects, {@code ambient/<key>} for the
 * ambience, {@code ships/<type>} for the engines and {@code music/<theme>} for
 * the music; a key with variants ({@code combat/hit-1.wav}, {@code -2}...) gets
 * one of them at random. A missing key is silence, never an error.
 *
 * <p>{@link #create} returns {@link SoundService#NONE} when the machine has no
 * usable audio line (headless servers, containers); the injected
 * {@link LineOpener} lets the tests exercise the fallback without a device.
 */
public final class SampledSound implements SoundService {
  /** The rate of the canonical samples and of the line. */
  public static final float SAMPLE_RATE = SampleLibrary.SAMPLE_RATE;
  /** The frames of every writing block (about 23 ms at 44.1 kHz). */
  private static final int FRAMES = 1024;

  private final BooleanSupplier enabled;
  private final SourceDataLine line;
  private final SampleLibrary library;
  private final SampleMixer mixer;
  private AmbienceKey currentAmbience;
  private MusicTheme currentMusic;

  private SampledSound(BooleanSupplier enabled, SourceDataLine line, SampleLibrary library) {
    this.enabled = enabled;
    this.line = line;
    this.library = library;
    this.mixer = new SampleMixer(enabled);
  }

  /**
   * A service over the default audio device and the usual {@code sounds/}
   * folders, or {@link SoundService#NONE} when there is none. {@code enabled} is
   * read on every call, so the player can turn the sound off (and on) without
   * rebuilding the service.
   */
  public static SoundService create(BooleanSupplier enabled) {
    return create(enabled,
        format -> (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format)),
        new SampleLibrary());
  }

  /** The same, with the line source injected: the tests exercise the fallback. */
  static SoundService create(BooleanSupplier enabled, LineOpener opener) {
    return create(enabled, opener, new SampleLibrary());
  }

  /** The same, with the line and the sample folders injected (tests). */
  static SoundService create(BooleanSupplier enabled, LineOpener opener, SampleLibrary library) {
    AudioFormat format = SampleLibrary.canonicalFormat();
    try {
      SourceDataLine line = opener.open(format);
      line.open(format);
      line.start();
      SampledSound sound = new SampledSound(enabled == null ? () -> true : enabled, line, library);
      sound.startPlayer();
      return sound;
    } catch(LineUnavailableException | IllegalArgumentException e) {
      // No audio backend at all: the game runs in silence instead of crashing.
      return SoundService.NONE;
    }
  }

  @Override
  public void play(SoundEffect effect) {
    if(effect == null || !enabled.getAsBoolean()) {
      return;
    }
    playKey(keyOf(effect));
  }

  @Override
  public void playRivalLaser(boolean easy) {
    if(!enabled.getAsBoolean()) {
      return;
    }
    // The tone of an easy rival needs its own sample: phase B maps the weapons.
    playKey(keyOf(SoundEffect.LASER));
  }

  @Override
  public void playWeapon(WeaponType type) {
    if(type == null || !enabled.getAsBoolean()) {
      return;
    }
    playKey("combat/laser-" + weaponSuffix(type));
  }

  @Override
  public void engine(ShipType type, boolean player) {
    if(type == null) {
      return;
    }
    // The loops start even with the sound off: turning it on resumes them.
    float[] sample = library.sample(engineKey(type));
    if(sample == null) {
      mixer.stopEngine(player);
    } else {
      mixer.startEngine(sample, player);
    }
  }

  @Override
  public void engineStop(boolean player) {
    mixer.stopEngine(player);
  }

  @Override
  public void ambience(AmbienceKey key) {
    if(key == null || key == currentAmbience) {
      return;
    }
    currentAmbience = key;
    float[] sample = library.sample(ambienceKey(key));
    if(sample == null) {
      // The key has no sample: silence, and the previous loop fades out.
      mixer.stopAmbience();
    } else {
      mixer.startAmbience(sample);
    }
  }

  @Override
  public void music(MusicTheme theme) {
    if(theme == null || theme == currentMusic) {
      return;
    }
    currentMusic = theme;
    float[] sample = theme == MusicTheme.NONE ? null : library.sample(musicKey(theme));
    if(sample == null) {
      mixer.stopMusic();
    } else {
      mixer.startMusic(sample);
    }
  }

  /** Changes the volume of a channel (phase B will expose them in the options). */
  public void setVolume(SoundChannel channel, float volume) {
    mixer.setVolume(channel, volume);
  }

  /** Plays the samples of a key as a one-shot (null sample is silence). */
  private void playKey(String key) {
    mixer.play(library.sample(key), 1f);
  }

  /** The key of an effect: the map of the README of {@code sounds/}. */
  static String keyOf(SoundEffect effect) {
    switch(effect) {
      case MENU_MOVE:
        return "ui/menu-move";
      case MENU_SELECT:
        return "ui/menu-select";
      case ALERT:
        return "alerts/alert";
      case WARNING:
        return "alerts/warning";
      case LASER:
        return "combat/laser-pulse";
      case HIT:
        return "combat/hit";
      case EXPLOSION:
        return "combat/explosion";
      case WARP:
        return "travel/warp";
      default:
        return "";
    }
  }

  /** The suffix of the weapon samples: {@code combat/laser-<suffix>}. */
  static String weaponSuffix(WeaponType type) {
    switch(type) {
      case PulseLaser:
        return "pulse";
      case BeamLaser:
        return "beam";
      case MilitaryLaser:
        return "military";
      case MorgansLaser:
        return "morgan";
      case PhotonDisruptor:
        return "photon";
      case QuantumDistruptor:
        return "quantum";
      default:
        return "pulse";
    }
  }

  private static String ambienceKey(AmbienceKey key) {
    return "ambient/" + key.name().toLowerCase(Locale.ROOT);
  }

  private static String musicKey(MusicTheme theme) {
    return "music/" + theme.name().toLowerCase(Locale.ROOT);
  }

  private static String engineKey(ShipType type) {
    return "ships/" + type.name().toLowerCase(Locale.ROOT);
  }

  /** Warms up the playback thread (called by the factory, never from the constructor). */
  private void startPlayer() {
    Thread player = new Thread(this::run, "sample-sound");
    player.setDaemon(true);
    player.start();
  }

  /** Renders and writes blocks until the program ends. */
  private void run() {
    float[] mix = new float[FRAMES];
    byte[] out = new byte[FRAMES * 2];
    while(!Thread.currentThread().isInterrupted()) {
      mixer.render(mix, FRAMES);
      toPcm(mix, out);
      try {
        line.write(out, 0, out.length);
      } catch(IllegalStateException e) {
        // The line was closed under us: stop the thread, the game goes on.
        return;
      }
    }
  }

  /** The mix as 16-bit little-endian samples, clipped to full scale. */
  private static void toPcm(float[] mix, byte[] out) {
    for(int i = 0; i < mix.length; i++) {
      double value = Math.max(-1, Math.min(1, mix[i]));
      int sample = (int)Math.round(value * Short.MAX_VALUE);
      out[2 * i] = (byte)(sample & 0xFF);
      out[2 * i + 1] = (byte)((sample >> 8) & 0xFF);
    }
  }
}
