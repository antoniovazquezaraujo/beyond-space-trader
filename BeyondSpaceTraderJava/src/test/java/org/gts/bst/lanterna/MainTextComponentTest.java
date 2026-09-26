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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class MainTextComponentTest {
  @Test
  void wrapsTheNewsAndScrolls() {
    MainTextComponent component = new MainTextComponent(() -> null, key -> false);

    component.news("The Head", "word ".repeat(200) + "\nsecond paragraph");

    assertTrue(component.newsLineCount() > 1, "the news must be wrapped");
    assertEquals(0, component.newsScroll());

    component.moveNewsScroll(1);
    assertEquals(1, component.newsScroll());

    component.moveNewsScroll(-5);
    assertEquals(0, component.newsScroll());

    component.moveNewsScroll(1000);
    assertEquals(component.newsLineCount() - 1, component.newsScroll());
  }
}
