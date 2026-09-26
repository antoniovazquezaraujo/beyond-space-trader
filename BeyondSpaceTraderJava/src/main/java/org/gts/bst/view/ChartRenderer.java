/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;


/**
 * Draws the galactic and the short-range chart on a character grid.
 *
 * <p>Markers: {@code +} the current system, {@code o}/{@code *} an unvisited or
 * visited system, {@code @} the warp target, {@code X} the tracked system and
 * {@code ~} a wormhole next to its system. Systems within the fuel range are green;
 * the selected one uses {@link ChartColor#SELECTED}.
 *
 * <p>The galactic chart scales the whole galaxy down so that it always fits in the
 * chart area: it is a situation map with no scrolling (and no names, which would not
 * fit). The short-range chart is a 1:1 map (one sector per character) with names and
 * its own viewport; its fuel range is drawn as a green braille ring, and when the
 * current or the tracked system falls outside the view an arrow at the edge points
 * to it.
 */
public final class ChartRenderer {
  static final char CURRENT = '+';
  static final char WARP = '@';
  static final char TRACKED = 'X';
  static final char VISITED = '*';
  static final char UNVISITED = 'o';
  static final char WORMHOLE = '~';
  static final char BRAILLE_BASE = '\u2800';
  private static final int[] LEFT_DOTS = {0x01, 0x02, 0x04, 0x40};
  private static final int[] RIGHT_DOTS = {0x08, 0x10, 0x20, 0x80};

  private ChartRenderer() {
  }

  public static void render(ChartCanvas canvas, ChartViewModel model) {
    switch(model.type()) {
      case GALACTIC:
        renderGalactic(canvas, model);
        break;
      case SHORT_RANGE:
        renderShortRange(canvas, model);
        break;
      default:
        break;
    }
  }

  private static void renderGalactic(ChartCanvas canvas, ChartViewModel model) {
    double scale = galacticScale(canvas, model);
    int mapWidth = (int)Math.round(model.galaxyWidth() / scale);
    int mapHeight = (int)Math.round(model.galaxyHeight() / scale);
    int left = Math.max(0, (canvas.width() - mapWidth) / 2);
    int top = Math.max(0, (canvas.height() - mapHeight) / 2);
    drawRangeRing(canvas, left + (int)Math.round(model.currentX() / scale),
        top + (int)Math.round(model.currentY() / scale), model.fuel() / scale);
    // The special systems (current, target, tracked, selected) are drawn last, so a
    // close neighbour cannot paint over them.
    drawGalacticSystems(canvas, model, left, top, scale, false);
    drawGalacticSystems(canvas, model, left, top, scale, true);
  }

  private static double galacticScale(ChartCanvas canvas, ChartViewModel model) {
    int width = Math.max(1, canvas.width() - 1);
    int height = Math.max(1, canvas.height() - 1);
    return Math.max((double)model.galaxyWidth() / width, (double)model.galaxyHeight() / height);
  }

  private static void drawGalacticSystems(ChartCanvas canvas, ChartViewModel model, int left, int top,
      double scale, boolean special) {
    for(ChartSystem system : model.systems()) {
      if(isSpecial(system, model) != special) {
        continue;
      }
      int x = left + (int)Math.round(system.x() / scale);
      int y = top + (int)Math.round(system.y() / scale);
      if(!inside(canvas, x, y)) {
        continue;
      }
      canvas.put(x, y, marker(system, model), color(system, model));
      drawWormhole(canvas, system, x, y);
    }
  }

  private static boolean isSpecial(ChartSystem system, ChartViewModel model) {
    return isCurrent(system, model) || system.warp() || system.tracked() || system.selected();
  }

  private static void renderShortRange(ChartCanvas canvas, ChartViewModel model) {
    int currentX = model.currentX() - model.viewX();
    int currentY = model.currentY() - model.viewY();
    drawRangeRing(canvas, currentX, currentY, model.fuel());
    // First the names, then the systems: the markers stop the names from hiding them.
    for(int pass = 0; pass < 2; pass++) {
      for(ChartSystem system : model.systems()) {
        int x = system.x() - model.viewX();
        int y = system.y() - model.viewY();
        if(pass == 0) {
          drawName(canvas, system.name(), x, y);
        } else if(inside(canvas, x, y)) {
          canvas.put(x, y, marker(system, model), color(system, model));
          drawWormhole(canvas, system, x, y);
        }
      }
    }
    if(inside(canvas, currentX, currentY)) {
      canvas.put(currentX, currentY, CURRENT, ChartColor.CYAN);
    }
    drawEdgeArrow(canvas, currentX, currentY, ChartColor.CYAN);
    drawTrackingArrow(canvas, model);
    if(model.trackedRangeText() != null) {
      drawText(canvas, 0, canvas.height() - 1, model.trackedRangeText(), ChartColor.DEFAULT);
    }
  }

  /**
   * Draws the fuel range as a braille ring: each cell holds up to eight sub-cell dots,
   * so the circle is a continuous curve instead of a chain of character dots. One
   * sector counts as one cell, that is, two dots across and four dots down. The ring
   * is drawn first, so names, systems and arrows paint over it.
   */
  private static void drawRangeRing(ChartCanvas canvas, int centerX, int centerY, double radius) {
    if(radius < 0.5) {
      return;
    }
    int steps = Math.max(360, (int)Math.round(radius * 24));
    int[][] masks = new int[canvas.height()][canvas.width()];
    for(int i = 0; i < steps; i++) {
      double angle = 2 * Math.PI * i / steps;
      int px = centerX * 2 + (int)Math.round(Math.cos(angle) * radius * 2);
      int py = centerY * 4 + (int)Math.round(Math.sin(angle) * radius * 4);
      int x = Math.floorDiv(px, 2);
      int y = Math.floorDiv(py, 4);
      if(inside(canvas, x, y)) {
        masks[y][x] |= dotBit(px, py);
      }
    }
    for(int y = 0; y < masks.length; y++) {
      for(int x = 0; x < masks[y].length; x++) {
        if(masks[y][x] != 0) {
          canvas.put(x, y, (char)(BRAILLE_BASE + masks[y][x]), ChartColor.GREEN);
        }
      }
    }
  }

  private static int dotBit(int px, int py) {
    int dy = Math.floorMod(py, 4);
    return Math.floorMod(px, 2) == 0 ? LEFT_DOTS[dy] : RIGHT_DOTS[dy];
  }

  private static char marker(ChartSystem system, ChartViewModel model) {
    if(isCurrent(system, model)) {
      return CURRENT;
    }
    if(system.warp()) {
      return WARP;
    }
    if(system.tracked()) {
      return TRACKED;
    }
    return system.visited() ? VISITED : UNVISITED;
  }

  private static ChartColor color(ChartSystem system, ChartViewModel model) {
    if(isCurrent(system, model)) {
      return ChartColor.CYAN;
    }
    if(system.selected()) {
      return ChartColor.SELECTED;
    }
    if(system.warp()) {
      return ChartColor.YELLOW;
    }
    if(system.tracked()) {
      return ChartColor.RED;
    }
    return reachable(system, model) ? ChartColor.GREEN : ChartColor.DEFAULT;
  }

  private static boolean isCurrent(ChartSystem system, ChartViewModel model) {
    return system.x() == model.currentX() && system.y() == model.currentY();
  }

  private static boolean reachable(ChartSystem system, ChartViewModel model) {
    int dx = system.x() - model.currentX();
    int dy = system.y() - model.currentY();
    return (int)Math.floor(Math.sqrt(dx * dx + dy * dy)) <= model.fuel();
  }

  private static void drawWormhole(ChartCanvas canvas, ChartSystem system, int x, int y) {
    if(system.wormhole() && x + 1 < canvas.width()) {
      canvas.put(x + 1, y, WORMHOLE, ChartColor.MAGENTA);
    }
  }

  private static void drawName(ChartCanvas canvas, String name, int centerX, int y) {
    if(name == null || name.isEmpty()) {
      return;
    }
    drawText(canvas, centerX - name.length() / 2, y, name, ChartColor.DEFAULT);
  }

  private static void drawText(ChartCanvas canvas, int startX, int y, String text, ChartColor color) {
    if(y < 0 || y >= canvas.height()) {
      return;
    }
    for(int i = 0; i < text.length(); i++) {
      int x = startX + i;
      if(x >= 0 && x < canvas.width()) {
        canvas.put(x, y, text.charAt(i), color);
      }
    }
  }

  private static void drawTrackingArrow(ChartCanvas canvas, ChartViewModel model) {
    ChartSystem tracked = null;
    for(ChartSystem system : model.systems()) {
      if(system.tracked()) {
        tracked = system;
        break;
      }
    }
    if(tracked != null && !isCurrent(tracked, model)) {
      drawEdgeArrow(canvas, tracked.x() - model.viewX(), tracked.y() - model.viewY(), ChartColor.RED);
    }
  }

  /**
   * When a point falls outside the view, an arrow at the edge of the chart points to
   * it (nothing is drawn when the point is inside).
   */
  private static void drawEdgeArrow(ChartCanvas canvas, int x, int y, ChartColor color) {
    if(inside(canvas, x, y)) {
      return;
    }
    int centerX = canvas.width() / 2;
    int centerY = canvas.height() / 2;
    int clampedY = Math.max(0, Math.min(y, canvas.height() - 1));
    int clampedX = Math.max(0, Math.min(x, canvas.width() - 1));
    char arrow;
    if(Math.abs(x - centerX) >= Math.abs(y - centerY)) {
      arrow = x >= centerX ? '>' : '<';
      clampedX = x >= centerX ? canvas.width() - 1 : 0;
    } else {
      arrow = y >= centerY ? 'v' : '^';
      clampedY = y >= centerY ? canvas.height() - 1 : 0;
    }
    canvas.put(clampedX, clampedY, arrow, color);
  }

  private static boolean inside(ChartCanvas canvas, int x, int y) {
    return x >= 0 && y >= 0 && x < canvas.width() && y < canvas.height();
  }
}
