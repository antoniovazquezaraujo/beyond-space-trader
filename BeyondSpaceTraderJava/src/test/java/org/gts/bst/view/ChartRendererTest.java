/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;


class ChartRendererTest {
  @Test
  void galacticDrawsTheSystemsAroundTheCurrentSystem() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        new ChartSystem(10, 5, "Current", true, false, false, false),
        new ChartSystem(6, 4, "Unvisited", false, false, false, false),
        new ChartSystem(14, 6, "Visited", true, false, false, false),
        new ChartSystem(0, 0, "Outside", false, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 10, 5, 0, 20, 10));

    // The viewport is centred on (10, 5): the top-left corner is (5, 3).
    assertEquals('+', canvas.at(5, 2));
    assertEquals(ChartColor.CYAN, canvas.colorAt(5, 2));
    assertEquals('o', canvas.at(1, 1));
    assertEquals('*', canvas.at(9, 3));
    assertEquals(' ', canvas.at(0, 0));
  }

  @Test
  void galacticMarksWarpTrackedWormholeReachabilityAndSelection() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        new ChartSystem(10, 5, "Current", true, false, false, false),
        new ChartSystem(12, 5, "Warp", false, false, true, false),
        new ChartSystem(8, 5, "Tracked", false, false, false, true),
        new ChartSystem(10, 7, "Worm", true, true, false, false),
        new ChartSystem(13, 5, "Reachable", false, false, false, false),
        new ChartSystem(10, 3, "Selected", false, false, false, false, true));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 10, 5, 3, 20, 10));

    assertEquals('@', canvas.at(7, 2));
    assertEquals(ChartColor.YELLOW, canvas.colorAt(7, 2));
    assertEquals('X', canvas.at(3, 2));
    assertEquals(ChartColor.RED, canvas.colorAt(3, 2));
    assertEquals('*', canvas.at(5, 4));
    assertEquals('~', canvas.at(6, 4));
    assertEquals(ChartColor.MAGENTA, canvas.colorAt(6, 4));
    assertEquals('o', canvas.at(8, 2));
    assertEquals(ChartColor.GREEN, canvas.colorAt(8, 2));
    assertEquals('o', canvas.at(5, 0));
    assertEquals(ChartColor.SELECTED, canvas.colorAt(5, 0));
  }

  @Test
  void galacticClampsTheViewportToTheGalaxy() {
    TestChartCanvas topLeft = new TestChartCanvas(11, 5);
    ChartRenderer.render(topLeft,
        ChartViewModel.galactic(List.of(new ChartSystem(0, 0, "Top", false, false, false, false)),
            0, 0, 0, 0, 0, 20, 10));
    assertEquals('+', topLeft.at(0, 0));

    TestChartCanvas bottomRight = new TestChartCanvas(11, 5);
    ChartRenderer.render(bottomRight,
        ChartViewModel.galactic(List.of(new ChartSystem(19, 9, "Bottom", false, false, false, false)),
            19, 9, 19, 9, 0, 20, 10));
    // pan = (min(19 - 5, 20 - 11), min(9 - 2, 10 - 5)) = (9, 5)
    assertEquals('+', bottomRight.at(10, 4));
  }

  @Test
  void shortRangeCentersTheCurrentSystemAndDrawsNames() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "Here", true, false, false, false),
        new ChartSystem(3, 0, "Sol", false, false, false, false),
        new ChartSystem(0, 3, "Up", false, false, false, false),
        new ChartSystem(25, 0, "Far", false, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 5, 20, "3 parsecs to Sol."));

    assertEquals('+', canvas.at(10, 4));
    assertEquals('o', canvas.at(13, 4));
    assertEquals('S', canvas.at(12, 4));
    assertEquals('l', canvas.at(14, 4));
    assertEquals(ChartColor.GREEN, canvas.colorAt(13, 4));
    assertEquals('o', canvas.at(10, 7));
    assertEquals(' ', canvas.at(20, 4));
    assertTrue(canvas.line(8).startsWith("3 parsecs to Sol."), canvas.line(8));
  }

  @Test
  void shortRangeDrawsTheFuelRangeRing() {
    TestChartCanvas canvas = new TestChartCanvas(21, 13);
    List<ChartSystem> systems = List.of(new ChartSystem(0, 0, "Here", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 4, 20, null));

    // The ring is drawn with braille cells at the fuel distance; inside and outside
    // stay free.
    assertRing(canvas, 14, 6);
    assertRing(canvas, 6, 6);
    assertRing(canvas, 10, 10);
    assertRing(canvas, 10, 2);
    assertEquals(' ', canvas.at(12, 6));
    assertEquals(' ', canvas.at(16, 6));
  }

  @Test
  void shortRangeScalesTheRangeRingWithTheChart() {
    TestChartCanvas canvas = new TestChartCanvas(41, 25);
    List<ChartSystem> systems = List.of(new ChartSystem(0, 0, "Here", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 5, 5, null));

    // delta = 25 / (5 * 2) = 2, so the ring is drawn 10 cells to the right.
    assertRing(canvas, 30, 12);
    assertEquals('+', canvas.at(20, 12));
  }

  private static void assertRing(TestChartCanvas canvas, int x, int y) {
    char character = canvas.at(x, y);
    assertTrue(character >= ChartRenderer.BRAILLE_BASE
        && character <= (char)(ChartRenderer.BRAILLE_BASE + 0xFF),
        "no ring at (" + x + ", " + y + "): '" + character + "'");
    assertEquals(ChartColor.GREEN, canvas.colorAt(x, y));
  }

  @Test
  void shortRangeDrawsTheTrackingArrowWhenTheSystemIsOutOfView() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "Here", true, false, false, false),
        new ChartSystem(0, 10, "Tracked", false, false, false, true));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 5, 20, null));

    assertEquals('v', canvas.at(10, 8));
    assertEquals(ChartColor.RED, canvas.colorAt(10, 8));
  }
}
