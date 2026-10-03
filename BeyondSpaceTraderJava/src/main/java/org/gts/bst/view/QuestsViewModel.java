/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;


/**
 * The quests screen: one {@link Entry} per open quest, in the order the presenter
 * lists them. Every entry carries its text (already formatted) and the name of the
 * destination system the quest points at ({@code null} when it has no destination).
 * The front-end selects an entry and sets its destination as the map target.
 */
public record QuestsViewModel(List<Entry> quests) {
  /**
   * A quest line and the system it points at, when it has one. {@code text} never
   * contains the system link markers of the old front-ends.
   */
  public record Entry(String text, String systemName) {
    public boolean hasSystem() {
      return systemName != null && !systemName.isEmpty();
    }
  }

  /** Whether any entry points at a system, so the front-end shows a selection. */
  public boolean hasDestinations() {
    for(Entry entry : quests) {
      if(entry.hasSystem()) {
        return true;
      }
    }
    return false;
  }
}
