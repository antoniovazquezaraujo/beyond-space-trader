/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.audio;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.gts.bst.ports.Alarm;
import org.gts.bst.ports.AmbienceKey;
import org.gts.bst.ports.MusicTheme;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.WeaponType;
import spacetrader.Consts;


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
  /**
   * The buffer of the output line, in bytes: four writing blocks (about 93 ms at
   * 44.1 kHz). The default buffer of some devices is 500 ms, which queues every
   * effect far behind the picture; if the device rejects this size, the factory
   * falls back to its default.
   */
  static final int LINE_BUFFER_BYTES = FRAMES * 4 * 2;
  /** The music fades to nothing fast when the scene closes. */
  private static final double MUSIC_STOP_FADE_SECONDS = 0.15;
  /** The workshop loop of the repair scene fades out fast when the scene closes. */
  private static final double REPAIR_FADE_SECONDS = 0.12;

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
      openLine(line, format);
      line.start();
      SampledSound sound = new SampledSound(enabled == null ? () -> true : enabled, line, library);
      sound.startPlayer();
      return sound;
    } catch(LineUnavailableException | IllegalArgumentException e) {
      // No audio backend at all: the game runs in silence instead of crashing.
      return SoundService.NONE;
    }
  }

  /**
   * Opens the line with the bounded buffer; when the device only takes its own
   * sizes, the default (longer) latency is better than no sound at all.
   */
  private static void openLine(SourceDataLine line, AudioFormat format) throws LineUnavailableException {
    try {
      line.open(format, LINE_BUFFER_BYTES);
    } catch(IllegalArgumentException e) {
      line.open(format);
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
    // The loops start even with the sound off: turning it on resumes them. The
    // sample falls back from the type to its size and then to the shared one.
    float[] sample = null;
    for(String key : engineKeys(type)) {
      sample = library.sample(key);
      if(sample != null) {
        break;
      }
    }
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
  public void engineStop(boolean player, double fadeSeconds) {
    mixer.stopEngine(player, fadeSeconds);
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
    if(theme == MusicTheme.NONE) {
      // The scene is closing: the music goes away quickly.
      mixer.stopMusic(MUSIC_STOP_FADE_SECONDS);
      return;
    }
    float[] sample = library.sample(musicKey(theme));
    if(sample == null) {
      mixer.stopMusic(MUSIC_STOP_FADE_SECONDS);
    } else {
      mixer.startMusic(sample);
    }
  }

  /** Changes the volume of a channel (phase B will expose them in the options). */
  public void setVolume(SoundChannel channel, float volume) {
    mixer.setVolume(channel, volume);
  }

  @Override
  public void alarm(Alarm alarm, boolean on) {
    if(alarm == null) {
      return;
    }
    if(!on) {
      mixer.stopAlarm(alarm);
      return;
    }
    // Like the engines, the loops start even with the sound off: turning it on
    // resumes them. A missing sample means no alarm at all.
    float[] sample = library.sample(alarmKey(alarm));
    if(sample == null) {
      mixer.stopAlarm(alarm);
    } else {
      mixer.startAlarm(alarm, sample);
    }
  }

  /** Plays the samples of a key as a one-shot (null sample is silence). */
  private void playKey(String key) {
    mixer.play(library.sample(key), 1f);
  }

  @Override
  public void repairLoop(boolean on) {
    // Like the other loops, it starts even with the sound off: turning it on
    // resumes it. It does not touch the ambience of the screen: the map keeps
    // sounding under the scene. A missing sample is silence.
    float[] sample = on ? library.sample("repair/workshop") : null;
    if(sample == null) {
      mixer.stopRepair(REPAIR_FADE_SECONDS);
    } else {
      mixer.startRepair(sample);
    }
  }

  /** The key of an effect: the map of the README of {@code sounds/}. */
  static String keyOf(SoundEffect effect) {
    switch(effect) {
      case MENU_MOVE:
        return "ui/menu-move";
      case MENU_SELECT:
        return "ui/menu-select";
      case CHART_MOVE:
        return "ui/chart-move";
      case CHART_SELECT:
        return "ui/chart-select";
      case ALERT:
        return "alerts/alert";
      case POLICE:
        return "alerts/police";
      case POLICE_SCAN:
        return "alerts/police-scan";
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
      case ESCAPE:
        return "travel/escape";
      case REPAIR_DONE:
        return "repair/done";
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

  /** The sample of an alarm loop. */
  static String alarmKey(Alarm alarm) {
    return alarm == Alarm.ATTACK ? "alerts/attack" : "alerts/danger";
  }

  private static String musicKey(MusicTheme theme) {
    return "music/" + theme.name().toLowerCase(Locale.ROOT);
  }

  /**
   * The engine samples of a ship, in order: its own type, its size
   * (tiny/small/medium/large/huge/gargantuan) and the shared default. The first
   * one that exists wins; when none does, the engine is silence.
   */
  static List<String> engineKeys(ShipType type) {
    return List.of("ships/" + type.name().toLowerCase(Locale.ROOT),
        "ships/" + Consts.ShipSpecs.get(type.CastToInt()).getSize().name().toLowerCase(Locale.ROOT),
        "ships/default");
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
