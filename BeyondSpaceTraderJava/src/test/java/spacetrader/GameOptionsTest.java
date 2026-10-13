/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import spacetrader.util.Hashtable;


class GameOptionsTest {
  @Test
  void theSoundIsOnByDefault() {
    assertTrue(new GameOptions(false).getSound(), "a new game starts with the sound on");
  }

  @Test
  void theSoundRoundTripsThroughTheSaveKey() {
    GameOptions options = new GameOptions(false);
    options.setSound(false);

    Hashtable hash = options.Serialize();

    assertEquals(Boolean.FALSE, hash.get("_sound"), "the save carries the sound key");
    assertFalse(new GameOptions(hash).getSound(), "and the loaded options read it back");
  }

  @Test
  void anOldSaveWithoutTheSoundKeyKeepsTheSoundOn() {
    Hashtable hash = new GameOptions(false).Serialize();
    hash.remove("_sound");

    assertTrue(new GameOptions(hash).getSound(), "an old save without the key defaults to sound on");
  }

  @Test
  void copyValuesCarriesTheSound() {
    GameOptions source = new GameOptions(false);
    source.setSound(false);
    GameOptions target = new GameOptions(false);

    target.CopyValues(source);

    assertFalse(target.getSound(), "the defaults must carry the sound off too");
  }
}
