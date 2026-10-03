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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.gts.bst.view.QuestsViewModel;
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

  @Test
  void movesTheQuestSelectionAndReadsItsDestination() {
    MainTextComponent component = new MainTextComponent(() -> null, key -> false);
    component.quests(new QuestsViewModel(List.of(
        new QuestsViewModel.Entry("Deliver the reactor to Nix.", "Nix"),
        new QuestsViewModel.Entry("Get rid of those pesky tribbles.", null))));

    assertEquals(2, component.questCount());
    assertEquals(0, component.questIndex());
    assertEquals("Nix", component.selectedQuestSystem());

    component.moveQuestSelection(1);
    assertEquals(1, component.questIndex());
    assertNull(component.selectedQuestSystem());

    component.moveQuestSelection(1);
    assertEquals(0, component.questIndex(), "the selection wraps around");
  }

  @Test
  void scrollToKeepsTheViewStillUntilTheSelectionReachesTheMargin() {
    // First time: the view is centred on the selection.
    assertEquals(30, MainTextComponent.scrollTo(-1, 60, 60, 154, 6));
    // Moving inside the still area does not move the view.
    assertEquals(30, MainTextComponent.scrollTo(30, 40, 60, 154, 6));
    assertEquals(30, MainTextComponent.scrollTo(30, 80, 60, 154, 6));
    // Close to the left edge the view scrolls just enough to keep the margin.
    assertEquals(28, MainTextComponent.scrollTo(30, 34, 60, 154, 6));
    // And the same on the right edge.
    assertEquals(37, MainTextComponent.scrollTo(30, 90, 60, 154, 6));
    // The view never leaves the galaxy.
    assertEquals(0, MainTextComponent.scrollTo(30, 3, 60, 154, 6));
    assertEquals(94, MainTextComponent.scrollTo(30, 153, 60, 154, 6));
    // A galaxy smaller than the chart keeps the view at the origin.
    assertEquals(0, MainTextComponent.scrollTo(0, 10, 60, 40, 6));
  }
}
