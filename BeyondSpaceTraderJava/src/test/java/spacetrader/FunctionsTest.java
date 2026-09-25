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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class FunctionsTest {
  @Test
  void stringVarsReplacesEveryPlaceholder() {
    assertEquals("Buy 10 units of water",
        Functions.StringVars("Buy ^1 ^2 of ^3", new String[] {"10", "units", "water"}));
  }

  @Test
  void stringVarsHandlesSingleVariableOverload() {
    assertEquals("Hello, Antonio", Functions.StringVars("Hello, ^1", "Antonio"));
  }

  @Test
  void getRandomStaysWithinBounds() {
    for(int i = 0; i < 1000; i++) {
      int value = Functions.GetRandom(7);
      assertTrue(value >= 0 && value < 7, "value out of range: " + value);
    }
  }

  @Test
  void getRandomWithLowerBoundStaysWithinBounds() {
    for(int i = 0; i < 1000; i++) {
      int value = Functions.GetRandom(3, 9);
      assertTrue(value >= 3 && value < 9, "value out of range: " + value);
    }
  }
}

