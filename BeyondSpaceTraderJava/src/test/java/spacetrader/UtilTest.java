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

import org.junit.jupiter.api.Test;
import spacetrader.util.Util;


class UtilTest {
  @Test
  void stringsJoinPutsTheSeparatorBetweenElements() {
    assertEquals("a, b, c", Util.StringsJoin(", ", new String[]{"a", "b", "c"}));
    assertEquals("a", Util.StringsJoin(", ", new String[]{"a"}));
    assertEquals("", Util.StringsJoin(", ", new String[0]));
  }
}
