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
import org.gts.bst.ship.ShipSize;
import org.junit.jupiter.api.Test;


class ChartRendererTest {
  @Test
  void galacticFitsTheWholeGalaxyInTheCanvas() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        system(0, 0, "TopLeft", ShipSize.Tiny, ChartColor.GREEN_DIM, false),
        system(10, 5, "Current", ShipSize.Large, ChartColor.CYAN, false),
        system(20, 10, "BottomRight", ShipSize.Gargantuan, ChartColor.RED, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 0, 20, 10));

    // scale = max(20 / 10, 10 / 4) = 2.5; the map is 8 x 4 characters at (1, 0).
    assertEquals('·', canvas.at(1, 0));
    assertEquals(ChartColor.GREEN_DIM, canvas.colorAt(1, 0));
    assertEquals('✧', canvas.at(5, 2));
    assertTrue(canvas.invertedAt(5, 2), "the current system must be inverted");
    assertEquals('✶', canvas.at(9, 4));
    assertEquals(' ', canvas.at(0, 0));
  }

  @Test
  void galacticMarksTheStatesAsShapes() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        system(10, 5, "Current", ShipSize.Medium, ChartColor.CYAN, false),
        system(12, 5, "Selected", ShipSize.Medium, ChartColor.WHITE, false, false, false, true),
        system(6, 5, "Tracked", ShipSize.Medium, ChartColor.WHITE, false, false, true, false),
        system(10, 9, "Worm", ShipSize.Medium, ChartColor.WHITE, true, true, false, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 3, 20, 10));

    // The current system keeps its inverted cell: the parentheses cannot paint over it.
    assertEquals('◦', canvas.at(5, 2));
    assertTrue(canvas.invertedAt(5, 2), canvas.line(2));
    // The parentheses of the selected system (its left one falls on the current).
    assertEquals('◦', canvas.at(6, 2));
    assertEquals(')', canvas.at(7, 2));
    assertEquals(ChartColor.YELLOW, canvas.colorAt(7, 2));
    // The brackets of the tracked system.
    assertEquals('[', canvas.at(2, 2));
    assertEquals(']', canvas.at(4, 2));
    assertEquals(ChartColor.WHITE, canvas.colorAt(2, 2));
    // A wormhole system is drawn as a circled dot.
    assertEquals('◉', canvas.at(5, 4));
    assertEquals(ChartColor.WHITE, canvas.colorAt(5, 4));
    assertEquals(' ', canvas.at(6, 4));
  }

  @Test
  void galacticFitsTheGalaxyEvenOnASmallCanvas() {
    TestChartCanvas canvas = new TestChartCanvas(6, 3);
    List<ChartSystem> systems = List.of(
        system(0, 0, "TopLeft", ShipSize.Tiny, ChartColor.WHITE, false),
        system(153, 109, "BottomRight", ShipSize.Gargantuan, ChartColor.WHITE, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 0, 0, 0, 154, 110));

    // scale = max(154 / 5, 110 / 2) = 55; the map is 3 x 2 characters at (1, 0).
    assertEquals('·', canvas.at(1, 0));
    assertTrue(canvas.invertedAt(1, 0));
    assertEquals('✶', canvas.at(4, 2));
  }

  @Test
  void galacticDrawsTheFuelCircleScaledWithTheMap() {
    TestChartCanvas canvas = new TestChartCanvas(21, 11);
    List<ChartSystem> systems = List.of(
        system(10, 5, "Here", ShipSize.Medium, ChartColor.WHITE, false));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 10, 5, 5, 20, 10));

    // Here scale is 1, so the circle has a radius of five cells.
    assertRing(canvas, 15, 5);
  }

  @Test
  void shortRangeUsesTheViewOriginAndDrawsNamesUnderTheStars() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        system(3, 0, "Sol", ShipSize.Small, ChartColor.WHITE, false),
        system(0, 2, "Up", ShipSize.Medium, ChartColor.WHITE, false),
        system(25, 0, "Far", ShipSize.Medium, ChartColor.WHITE, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -4, 5, "3 parsecs to Sol."));

    assertEquals('◦', canvas.at(10, 4));
    assertTrue(canvas.invertedAt(10, 4), "the current system must be inverted");
    assertEquals('•', canvas.at(13, 4));
    // The names go under their stars.
    assertEquals('S', canvas.at(12, 5));
    assertEquals('o', canvas.at(13, 5));
    assertEquals('l', canvas.at(14, 5));
    assertEquals('U', canvas.at(9, 7));
    assertEquals('p', canvas.at(10, 7));
    assertEquals(' ', canvas.at(20, 4));
    assertTrue(canvas.line(8).startsWith("3 parsecs to Sol."), canvas.line(8));
  }

  @Test
  void shortRangeCombinesTheTargetParenthesesWithTheTrackedBrackets() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        system(3, 0, "Both", ShipSize.Medium, ChartColor.WHITE, false, false, true, true));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -4, 0, null));

    // The markers nest: the brackets outside, the parentheses inside.
    assertEquals('(', canvas.at(12, 4));
    assertEquals(')', canvas.at(14, 4));
    assertEquals('[', canvas.at(11, 4));
    assertEquals(']', canvas.at(15, 4));
  }

  @Test
  void shortRangeTruncatesOrSkipsTheNamesThatDoNotFit() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        system(10, 4, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        system(1, 1, "Zed", ShipSize.Tiny, ChartColor.WHITE, false),
        system(7, 1, "Narcotics", ShipSize.Small, ChartColor.WHITE, false, false, false, true));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 10, 4, 0, 0, 0, null));

    assertEquals("Zed", canvas.line(2).substring(0, 3));
    // The selected name falls back to a truncation with an ellipsis (the brackets
    // and parentheses leave the row under the star free).
    assertTrue(canvas.line(2).contains("Narcoti…"), canvas.line(2));
  }

  @Test
  void shortRangePointsToTheCurrentSystemWhenItIsOutOfView() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 30, 0, 5, null));

    // The current system is to the left of the view: a cyan arrow at the left edge.
    assertEquals('<', canvas.at(0, 0));
    assertEquals(ChartColor.CYAN, canvas.colorAt(0, 0));
  }

  @Test
  void shortRangeDrawsTheTrackingArrowWhenTheSystemIsOutOfView() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        system(0, 10, "Tracked", ShipSize.Medium, ChartColor.WHITE, false, false, true, false));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, -10, -4, 5, null));

    assertEquals('v', canvas.at(10, 8));
    assertEquals(ChartColor.RED, canvas.colorAt(10, 8));
  }

  @Test
  void shortRangeDrawsTheFuelRangeRing() {
    TestChartCanvas canvas = new TestChartCanvas(21, 13);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false));

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

  @Test
  void shortRangeDrawsTheWormholeLinkOfTheSelectedSystem() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        new ChartSystem(5, 2, "End", false, true, false, false, true, ShipSize.Small, ChartColor.WHITE, 9, 5),
        new ChartSystem(9, 5, "Pair", false, true, false, false, false, ShipSize.Small, ChartColor.WHITE, 5, 2));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 0, 0, 0, null));

    // The link goes right and down: an L starting after the parentheses.
    assertEquals('(', canvas.at(4, 2));
    assertEquals(')', canvas.at(6, 2));
    assertEquals('─', canvas.at(7, 2));
    assertEquals('─', canvas.at(8, 2));
    assertEquals(ChartColor.MAGENTA, canvas.colorAt(7, 2));
    assertEquals('┐', canvas.at(9, 2));
    assertEquals('│', canvas.at(9, 3));
    assertEquals('│', canvas.at(9, 4));
    // The ends of the link are the circled-dot systems.
    assertEquals('◉', canvas.at(5, 2));
    assertEquals('◉', canvas.at(9, 5));
  }

  @Test
  void shortRangeDrawsTheWormholeCornerWhenThePairIsOneRowAway() {
    TestChartCanvas canvas = new TestChartCanvas(21, 9);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        new ChartSystem(5, 2, "End", false, true, false, false, true, ShipSize.Small, ChartColor.WHITE, 9, 3),
        new ChartSystem(9, 3, "Pair", false, true, false, false, false, ShipSize.Small, ChartColor.WHITE, 5, 2));

    ChartRenderer.render(canvas, ChartViewModel.shortRange(systems, 0, 0, 0, 0, 0, null));

    // The pair is one row below: the horizontal still turns at its column (there is
    // no vertical segment to draw).
    assertEquals('─', canvas.at(7, 2));
    assertEquals('─', canvas.at(8, 2));
    assertEquals('┐', canvas.at(9, 2));
    assertEquals('◉', canvas.at(9, 3));
  }

  @Test
  void galacticDrawsTheWormholeLinkOfTheSelectedSystem() {
    TestChartCanvas canvas = new TestChartCanvas(11, 5);
    List<ChartSystem> systems = List.of(
        system(0, 0, "Here", ShipSize.Medium, ChartColor.CYAN, false),
        new ChartSystem(10, 5, "End", false, true, false, false, true, ShipSize.Small, ChartColor.WHITE, 18, 9),
        new ChartSystem(18, 9, "Pair", false, true, false, false, false, ShipSize.Small, ChartColor.WHITE, 10, 5));

    ChartRenderer.render(canvas, ChartViewModel.galactic(systems, 0, 0, 0, 20, 10));

    assertEquals('─', canvas.at(7, 2));
    assertEquals(ChartColor.MAGENTA, canvas.colorAt(7, 2));
    assertEquals('┐', canvas.at(8, 2));
    assertEquals('│', canvas.at(8, 3));
    assertEquals('◉', canvas.at(8, 4));
  }

  @Test
  void sizeGlyphsGrowWithTheSystemSize() {
    assertEquals('·', ChartRenderer.sizeGlyph(system(0, 0, "T", ShipSize.Tiny, ChartColor.WHITE, false)));
    assertEquals('•', ChartRenderer.sizeGlyph(system(0, 0, "S", ShipSize.Small, ChartColor.WHITE, false)));
    assertEquals('◦', ChartRenderer.sizeGlyph(system(0, 0, "M", ShipSize.Medium, ChartColor.WHITE, false)));
    assertEquals('✧', ChartRenderer.sizeGlyph(system(0, 0, "L", ShipSize.Large, ChartColor.WHITE, false)));
    assertEquals('✦', ChartRenderer.sizeGlyph(system(0, 0, "H", ShipSize.Huge, ChartColor.WHITE, false)));
    assertEquals('✶', ChartRenderer.sizeGlyph(system(0, 0, "G", ShipSize.Gargantuan, ChartColor.WHITE, false)));
  }

  private static ChartSystem system(int x, int y, String name, ShipSize size, ChartColor color, boolean wormhole) {
    return new ChartSystem(x, y, name, false, wormhole, false, false, false, size, color);
  }

  private static ChartSystem system(int x, int y, String name, ShipSize size, ChartColor color, boolean wormhole,
      boolean warp, boolean tracked, boolean selected) {
    return new ChartSystem(x, y, name, false, wormhole, warp, tracked, selected, size, color);
  }

  private static void assertRing(TestChartCanvas canvas, int x, int y) {
    char character = canvas.at(x, y);
    assertTrue(character >= ChartRenderer.BRAILLE_BASE
        && character <= (char)(ChartRenderer.BRAILLE_BASE + 0xFF),
        "no ring at (" + x + ", " + y + "): '" + character + "'");
    assertEquals(ChartColor.GREEN, canvas.colorAt(x, y));
  }
}
