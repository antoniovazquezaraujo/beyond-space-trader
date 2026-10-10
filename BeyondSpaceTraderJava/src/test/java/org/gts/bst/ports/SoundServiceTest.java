/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.ports;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;


class SoundServiceTest {
  @Test
  void theNoneServicePlaysNothing() {
    assertNotNull(SoundService.NONE);
    for(SoundEffect effect : SoundEffect.values()) {
      SoundService.NONE.play(effect);
      SoundService.NONE.playRivalLaser(true);
      SoundService.NONE.playRivalLaser(false);
    }
  }
}
