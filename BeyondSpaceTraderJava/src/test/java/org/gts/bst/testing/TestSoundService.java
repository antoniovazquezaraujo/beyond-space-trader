/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.testing;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.ports.SoundEffect;
import org.gts.bst.ports.SoundService;


/**
 * A sound service that records the effects the game asks for, so the tests can
 * assert what would have sounded without opening any audio device.
 */
public final class TestSoundService implements SoundService {
  private final List<SoundEffect> effects = new ArrayList<>();

  @Override
  public void play(SoundEffect effect) {
    effects.add(effect);
  }

  /** The effects played so far, in order. */
  public List<SoundEffect> played() {
    return effects;
  }

  /** Forgets what was played (a test that checks one event at a time). */
  public void clear() {
    effects.clear();
  }
}
