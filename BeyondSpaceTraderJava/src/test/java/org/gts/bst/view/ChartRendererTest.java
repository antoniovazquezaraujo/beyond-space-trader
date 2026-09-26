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
  void galacticFitsTheWholeGalaxyInTheCanvas() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "TopLeft", true, false, false, false),
        new ChartSystem(10, 5, "Current", true, false, false, false),
        new ChartSystem(20, 10, "BottomRight", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 0, 20, 10));

    // scale = max(20 / 10, 10 / 4) = 2.5; the map is 8 x 4 characters at (1, 0).
    assertEquals('*', canvas.at(1, 0));
    assertEquals('+', canvas.at(5, 2));
    assertEquals(ChartColor.CYAN, canvas.colorAt(5, 2));
    assertEquals('*', canvas.at(9, 4));
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
        new ChartSystem(10, 3, "Reachable", false, false, false, false),
        new ChartSystem(11, 5, "OverCurrent", false, false, false, false),
        new ChartSystem(10, 9, "Selected", false, false, false, false, true),
        new ChartSystem(13, 9, "Normal", false, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 3, 20, 10));

    // The special systems win over their neighbours: the current one is drawn last.
    assertEquals('+', canvas.at(5, 2));
    assertEquals(ChartColor.CYAN, canvas.colorAt(5, 2));
    assertEquals('@', canvas.at(6, 2));
    assertEquals(ChartColor.YELLOW, canvas.colorAt(6, 2));
    assertEquals('X', canvas.at(4, 2));
    assertEquals(ChartColor.RED, canvas.colorAt(4, 2));
    assertEquals('*', canvas.at(5, 3));
    assertEquals('~', canvas.at(6, 3));
    assertEquals(ChartColor.MAGENTA, canvas.colorAt(6, 3));
    assertEquals('o', canvas.at(5, 1));
    assertEquals(ChartColor.GREEN, canvas.colorAt(5, 1));
    assertEquals('o', canvas.at(5, 4));
    assertEquals(ChartColor.SELECTED, canvas.colorAt(5, 4));
    assertEquals('o', canvas.at(6, 4));
    assertEquals(ChartColor.DEFAULT, canvas.colorAt(6, 4));
  }

  @Test
  void galacticFitsTheGalaxyEvenOnASmallCanvas() {
    TestChartCanvas canvas = new TestChartCanvas(6, 3);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "TopLeft", true, false, false, false),
        new ChartSystem(153, 109, "BottomRight", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 0, 0, 0, 154, 110));

    // scale = max(154 / 5, 110 / 2) = 55; the map is 3 x 2 characters at (1, 0).
    assertEquals('+', canvas.at(1, 0));
    assertEquals(ChartColor.CYAN, canvas.colorAt(1, 0));
    assertEquals('*', canvas.at(4, 2));
  }

  @Test
  void galacticDrawsTheFuelCircleScaledWithTheMap() {
    TestChartCanvas canvas = new TestChartCanvas(21, 11);
    List<ChartSystem> systems = List.of(new ChartSystem(10, 5, "Here", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 5, 20, 10));

    // Here scale is 1, so the circle has a radius of five cells.
    assertRing(canvas, 15, 5);
  }

  @Test
  void shortRangeUsesTheViewOriginAndDrawsNames() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "Here", true, false, false, false),
        new ChartSystem(3, 0, "Sol", false, false, false, false),
        new ChartSystem(0, 3, "Up", false, false, false, false),
        new ChartSystem(25, 0, "Far", false, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -4, 5, "3 parsecs to Sol."));

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
  void shortRangePointsToTheCurrentSystemWhenItIsOutOfView() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(new ChartSystem(0, 0, "Here", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 30, 0, 5, null));

    // The current system is to the left of the view: a cyan arrow at the left edge.
    assertEquals('<', canvas.at(0, 0));
    assertEquals(ChartColor.CYAN, canvas.colorAt(0, 0));
  }

  @Test
  void shortRangeDrawsTheTrackingArrowWhenTheSystemIsOutOfView() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        new ChartSystem(0, 0, "Here", true, false, false, false),
        new ChartSystem(0, 10, "Tracked", false, false, false, true));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -4, 5, null));

    assertEquals('v', canvas.at(10, 8));
    assertEquals(ChartColor.RED, canvas.colorAt(10, 8));
  }

  @Test
  void shortRangeDrawsTheFuelRangeRing() {
    TestChartCanvas canvas = new TestChartCanvas(21, 13);
    List<ChartSystem> systems = List.of(new ChartSystem(0, 0, "Here", true, false, false, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -6, 4, null));

    // The ring is drawn with braille cells at the fuel distance; inside and outside
    // stay free.
    assertRing(canvas, 14, 6);
    assertRing(canvas, 6, 6);
    assertRing(canvas, 10, 10);
    assertRing(canvas, 10, 2);
    assertEquals(' ', canvas.at(12, 6));
    assertEquals(' ', canvas.at(16, 6));
  }

  private static void assertRing(TestChartCanvas canvas, int x, int y) {
    char character = canvas.at(x, y);
    assertTrue(character >= ChartRenderer.BRAILLE_BASE
        && character <= (char)(ChartRenderer.BRAILLE_BASE + 0xFF),
        "no ring at (" + x + ", " + y + "): '" + character + "'");
    assertEquals(ChartColor.GREEN, canvas.colorAt(x, y));
  }
}
