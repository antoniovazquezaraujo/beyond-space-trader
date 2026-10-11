/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on SpaceTrader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import org.junit.jupiter.api.Test;


class DialogPlacementTest {
  @Test
  void theDialogLandsCentredAtTheBottom() {
    assertEquals(new TerminalPosition(20, 17),
        DialogPlacement.bottomCenter(new TerminalSize(60, 20), new TerminalSize(20, 2)),
        "centred in the line, one row over the edge");
    assertEquals(new TerminalPosition(0, 0),
        DialogPlacement.bottomCenter(new TerminalSize(10, 6), new TerminalSize(30, 9)),
        "a window that does not fit clamps to the top-left corner");
    assertEquals(new TerminalPosition(1, 0),
        DialogPlacement.bottomCenter(new TerminalSize(10, 4), new TerminalSize(8, 4)),
        "no room below: the window sits on the top edge");
    assertEquals(new TerminalPosition(0, 0),
        DialogPlacement.bottomCenter(new TerminalSize(1, 1), new TerminalSize(1, 1)),
        "a one-cell screen still clamps");
  }
}
