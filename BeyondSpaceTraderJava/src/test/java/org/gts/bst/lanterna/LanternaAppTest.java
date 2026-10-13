/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class LanternaAppTest {
  @Test
  void aSkillDialogNeverCancelsTheNewGame() {
    assertEquals(0, LanternaApp.skillPoints(null, 9), "a cancelled dialog is zero points");
    assertEquals(0, LanternaApp.skillPoints("", 9), "an empty value too");
    assertEquals(0, LanternaApp.skillPoints("nonsense", 9), "and nonsense counts as zero");
    assertEquals(3, LanternaApp.skillPoints("3", 9));
    assertEquals(9, LanternaApp.skillPoints("10", 9), "an amount over the maximum is capped");
    assertEquals(0, LanternaApp.skillPoints("-4", 9), "and a negative one is zero");
  }

  @Test
  void theMuteArgumentsAreParsed() {
    assertFalse(LanternaApp.soundMuted(new String[0]), "the sound is on by default");
    assertFalse(LanternaApp.soundMuted(new String[] {"--lang", "es"}));
    assertTrue(LanternaApp.soundMuted(new String[] {"--mute"}));
    assertTrue(LanternaApp.soundMuted(new String[] {"--no-sound"}));
    assertTrue(LanternaApp.soundMuted(new String[] {"--lang=es", "--mute"}),
        "the mute flag combines with the other arguments");
  }
}
