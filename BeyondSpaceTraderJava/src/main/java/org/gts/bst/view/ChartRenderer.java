/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Draws the galactic and the short-range chart on a character grid.
 *
 * <p>Every system is a star whose glyph grows with its size ({@code · • ◦ ✧ ✦ ✶} for
 * Tiny to Gargantuan) and whose colour is decorative: bright while it has not been
 * visited and dim afterwards. The state markers are a pair of horizontal symbols:
 * the current system is drawn inverted (its colour becomes the cell background), the
 * selected/target system carries parentheses, the tracked system brackets (outside
 * the parentheses when both apply, {@code [(·)]}) and a system with a wormhole is
 * drawn as a circled dot ({@code ◉}) instead of its size glyph. The short-range chart also draws the names just under
 * their stars, skipping (or truncating with an ellipsis) the ones that do not fit.
 *
 * <p>The galactic chart scales the whole galaxy down so that it always fits in the
 * chart area: it never scrolls and every system is always visible. The short-range
 * chart is a 1:1 map (one sector per character) with its own viewport and a green
 * braille ring at the current fuel distance; when the current or the tracked system
 * falls outside the view, an arrow at the edge points to it.
 */
public final class ChartRenderer {
  static final char WORMHOLE = '◉';
  static final char BRAILLE_BASE = '\u2800';
  static final char TARGET_OPEN = '(';
  static final char TARGET_CLOSE = ')';
  static final char TRACK_OPEN = '[';
  static final char TRACK_CLOSE = ']';
  static final char ELLIPSIS = '…';
  static final char WORMHOLE_HORIZONTAL = '─';
  static final char WORMHOLE_VERTICAL = '│';
  /** Shortest truncated name worth drawing (letters plus the ellipsis). */
  private static final int MIN_TRUNCATED_NAME = 5;
  private static final char[] SIZE_GLYPHS = {'·', '•', '◦', '✧', '✦', '✶'};
  private static final int[] LEFT_DOTS = {0x01, 0x02, 0x04, 0x40};
  private static final int[] RIGHT_DOTS = {0x08, 0x10, 0x20, 0x80};
  private static final ChartColor TARGET_COLOR = ChartColor.YELLOW;
  private static final ChartColor TRACK_COLOR = ChartColor.WHITE;
  private static final ChartColor CURRENT_ARROW_COLOR = ChartColor.CYAN;
  private static final ChartColor TRACKED_ARROW_COLOR = ChartColor.RED;
  private static final ChartColor WORMHOLE_COLOR = ChartColor.MAGENTA;

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
    int columns = Math.max(1, model.galacticColumns());
    int cellsWide = Math.max(1, (canvas.width() + 1) / columns);
    int cellsTall = Math.max(1, canvas.height());
    double scale = Math.max((double)model.galaxyWidth() / Math.max(1, cellsWide - 1),
        (double)model.galaxyHeight() / Math.max(1, cellsTall - 1));
    int usedCellsWide = (int)Math.round(model.galaxyWidth() / scale) + 1;
    int usedCellsTall = (int)Math.round(model.galaxyHeight() / scale) + 1;
    int leftCells = Math.max(0, (cellsWide - usedCellsWide) / 2);
    int top = Math.max(0, (cellsTall - usedCellsTall) / 2);
    boolean[][] used = new boolean[canvas.height()][canvas.width()];
    int currentX = (leftCells + (int)Math.round(model.currentX() / scale)) * columns;
    int currentY = top + (int)Math.round(model.currentY() / scale);
    drawRangeRing(canvas, currentX, currentY, model.fuel() / scale, columns * 2);
    for(ChartSystem system : model.systems()) {
      if(system.selected() && system.wormholeLinked()) {
        drawWormholeLine(canvas, system, systemColumn(model, system.x(), scale, leftCells, columns),
            systemRow(model, system.y(), scale, top),
            systemColumn(model, system.wormholeToX(), scale, leftCells, columns),
            systemRow(model, system.wormholeToY(), scale, top), used);
        break;
      }
    }
    // The stars first (the special ones last), then, over everything, the state
    // decorations; nothing paints over a star except the state marks.
    for(ChartSystem system : model.systems()) {
      if(!isSpecial(system, model)) {
        drawStar(canvas, model, system, systemColumn(model, system.x(), scale, leftCells, columns),
            systemRow(model, system.y(), scale, top), used);
      }
    }
    for(ChartSystem system : model.systems()) {
      if(isSpecial(system, model)) {
        drawStar(canvas, model, system, systemColumn(model, system.x(), scale, leftCells, columns),
            systemRow(model, system.y(), scale, top), used);
      }
    }
    for(ChartSystem system : model.systems()) {
      drawDecoration(canvas, system, systemColumn(model, system.x(), scale, leftCells, columns),
          systemRow(model, system.y(), scale, top), used, currentX, currentY);
    }
  }

  private static int systemColumn(ChartViewModel model, int x, double scale, int leftCells, int columns) {
    return (leftCells + (int)Math.round(x / scale)) * columns;
  }

  private static int systemRow(ChartViewModel model, int y, double scale, int top) {
    return top + (int)Math.round(y / scale);
  }

  private static void renderShortRange(ChartCanvas canvas, ChartViewModel model) {
    boolean[][] used = new boolean[canvas.height()][canvas.width()];
    int currentX = model.currentX() - model.viewX();
    int currentY = model.currentY() - model.viewY();
    drawRangeRing(canvas, currentX, currentY, model.fuel(), 2);
    for(ChartSystem system : model.systems()) {
      if(system.selected() && system.wormholeLinked()) {
        drawWormholeLine(canvas, system,
            system.x() - model.viewX(), system.y() - model.viewY(),
            system.wormholeToX() - model.viewX(), system.wormholeToY() - model.viewY(), used);
        break;
      }
    }
    for(ChartSystem system : model.systems()) {
      if(!isSpecial(system, model)) {
        drawStar(canvas, model, system, system.x() - model.viewX(), system.y() - model.viewY(), used);
      }
    }
    for(ChartSystem system : model.systems()) {
      if(isSpecial(system, model)) {
        drawStar(canvas, model, system, system.x() - model.viewX(), system.y() - model.viewY(), used);
      }
    }
    for(ChartSystem system : model.systems()) {
      drawDecoration(canvas, system, system.x() - model.viewX(), system.y() - model.viewY(), used,
          currentX, currentY);
    }
    drawEdgeArrow(canvas, currentX, currentY, CURRENT_ARROW_COLOR);
    drawTrackingArrow(canvas, model);
    drawNames(canvas, model, used);
    if(model.trackedRangeText() != null) {
      drawText(canvas, 0, canvas.height() - 1, model.trackedRangeText(), ChartColor.DEFAULT);
    }
  }

  /**
   * Draws a system: its marker glyph (by size, bright/dim by visited) and, for the
   * current system, the inverted cell that highlights it.
   */
  private static void drawStar(ChartCanvas canvas, ChartViewModel model, ChartSystem system, int x, int y,
      boolean[][] used) {
    if(!inside(canvas, x, y)) {
      return;
    }
    char glyph = system.wormhole() ? WORMHOLE : sizeGlyph(system);
    if(isCurrent(system, model)) {
      canvas.putInverted(x, y, glyph, system.color());
    } else {
      canvas.put(x, y, glyph, system.color());
    }
    mark(used, x, y);
  }

  /** Writes a decoration: only into a free cell, never over a star. */
  private static void draw(ChartCanvas canvas, boolean[][] used, int x, int y, char character, ChartColor color) {
    if(inside(canvas, x, y) && !used[y][x]) {
      canvas.put(x, y, character, color);
      mark(used, x, y);
    }
  }

  /**
   * The state symbols around a system: the parentheses of the selected/target system
   * and the brackets of the tracked one. When both apply they nest ({@code [(·)]}),
   * so they combine without hiding each other.
   */
  private static void drawDecoration(ChartCanvas canvas, ChartSystem system, int x, int y, boolean[][] used,
      int blockedX, int blockedY) {
    if(!inside(canvas, x, y)) {
      return;
    }
    if(system.selected()) {
      drawMark(canvas, used, x - 1, y, -1, TARGET_OPEN, TARGET_COLOR, blockedX, blockedY);
      drawMark(canvas, used, x + 1, y, 1, TARGET_CLOSE, TARGET_COLOR, blockedX, blockedY);
    }
    if(system.tracked()) {
      int offset = system.selected() ? 2 : 1;
      drawMark(canvas, used, x - offset, y, -1, TRACK_OPEN, TRACK_COLOR, blockedX, blockedY);
      drawMark(canvas, used, x + offset, y, 1, TRACK_CLOSE, TRACK_COLOR, blockedX, blockedY);
    }
  }

  /**
   * Writes a state mark over whatever is on the cell (the marks win over the stars),
   * except on the current system's cell: there it moves one cell further out, so the
   * mark is never lost and the player's own position is never hidden.
   */
  private static void drawMark(ChartCanvas canvas, boolean[][] used, int x, int y, int direction, char character,
      ChartColor color, int blockedX, int blockedY) {
    int cell = x == blockedX && y == blockedY ? x + direction : x;
    if(inside(canvas, cell, y)) {
      canvas.put(cell, y, character, color);
      mark(used, cell, y);
    }
  }

  /**
   * Draws the link of a wormhole when its system is selected: a horizontal and a
   * vertical segment (an L) in magenta, starting after the state marks and under the
   * stars, so the player sees where it leads before travelling. The names avoid it.
   */
  private static void drawWormholeLine(ChartCanvas canvas, ChartSystem source, int x1, int y1, int x2, int y2,
      boolean[][] used) {
    int stepX = x1 <= x2 ? 1 : -1;
    int stepY = y1 <= y2 ? 1 : -1;
    int offset = source.tracked() ? 3 : 2;
    boolean horizontal = (x2 - (x1 + stepX * offset)) * stepX > 0;
    boolean vertical = (y2 - (y1 + stepY)) * stepY > 0;
    if(horizontal) {
      int x = x1 + stepX * offset;
      while((x2 - x) * stepX > 0) {
        draw(canvas, used, x, y1, WORMHOLE_HORIZONTAL, WORMHOLE_COLOR);
        x += stepX;
      }
    }
    // The corner is drawn even when the vertical segment is empty (the pair is one
    // row away): it sits right above or below it and closes the link.
    if(horizontal && y1 != y2) {
      draw(canvas, used, x2, y1, corner(stepX, stepY), WORMHOLE_COLOR);
    }
    if(vertical) {
      int y = y1 + stepY;
      while((y2 - y) * stepY > 0) {
        draw(canvas, used, x2, y, WORMHOLE_VERTICAL, WORMHOLE_COLOR);
        y += stepY;
      }
    }
  }

  private static char corner(int stepX, int stepY) {
    if(stepX > 0) {
      // The horizontal segment arrives from the left.
      return stepY > 0 ? '┐' : '┘';
    }
    // The horizontal segment arrives from the right.
    return stepY > 0 ? '┌' : '└';
  }

  /**
   * Draws the names under their stars. A name is only drawn when all its cells are
   * free; the current, selected and tracked systems come first and may fall back to a
   * truncated version with an ellipsis, the rest are skipped when they do not fit.
   */
  private static void drawNames(ChartCanvas canvas, ChartViewModel model, boolean[][] used) {
    List<ChartSystem> systems = new ArrayList<>(model.systems());
    systems.sort(Comparator.comparingInt(system -> namePriority(system, model)));
    for(ChartSystem system : systems) {
      if(system.name() == null || system.name().isEmpty()) {
        continue;
      }
      int starY = system.y() - model.viewY();
      int y = starY + 1;
      if(!inside(canvas, system.x() - model.viewX(), starY) || y < 0 || y >= canvas.height()) {
        continue;
      }
      int start = Math.max(0, system.x() - model.viewX() - system.name().length() / 2);
      placeName(canvas, used, start, y, system.name(), namePriority(system, model) < 0);
    }
  }

  private static int namePriority(ChartSystem system, ChartViewModel model) {
    int distance = Math.abs(system.x() - model.currentX()) + Math.abs(system.y() - model.currentY());
    if(isCurrent(system, model) || system.selected() || system.tracked()) {
      return -1_000_000 + distance;
    }
    return distance;
  }

  private static void placeName(ChartCanvas canvas, boolean[][] used, int start, int y, String name,
      boolean priority) {
    int run = 0;
    while(start + run < canvas.width() && !used[y][start + run]) {
      run++;
    }
    if(run >= name.length()) {
      writeName(canvas, used, start, y, name);
    } else if(priority && run >= MIN_TRUNCATED_NAME) {
      writeName(canvas, used, start, y, name.substring(0, run - 1) + ELLIPSIS);
    }
  }

  private static void writeName(ChartCanvas canvas, boolean[][] used, int start, int y, String text) {
    for(int i = 0; i < text.length(); i++) {
      canvas.put(start + i, y, text.charAt(i), ChartColor.DEFAULT);
      mark(used, start + i, y);
    }
  }

  private static void mark(boolean[][] used, int x, int y) {
    if(used != null) {
      used[y][x] = true;
    }
  }

  static char sizeGlyph(ChartSystem system) {
    int size = system.size().CastToInt();
    return SIZE_GLYPHS[Math.max(0, Math.min(size, SIZE_GLYPHS.length - 1))];
  }

  /**
   * Draws the fuel range as a braille ring: each cell holds up to eight sub-cell dots,
   * so the circle is a continuous curve instead of a chain of character dots. One
   * sector counts as one cell, that is, two dots across and four dots down. The ring
   * is drawn first, so stars, names and arrows paint over it.
   */
  private static void drawRangeRing(ChartCanvas canvas, int centerX, int centerY, double radius,
      int horizontalDotsPerCell) {
    if(radius < 0.5) {
      return;
    }
    int steps = Math.max(360, (int)Math.round(radius * 24));
    int[][] masks = new int[canvas.height()][canvas.width()];
    for(int i = 0; i < steps; i++) {
      double angle = 2 * Math.PI * i / steps;
      int px = centerX * 2 + (int)Math.round(Math.cos(angle) * radius * horizontalDotsPerCell);
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

  private static boolean isSpecial(ChartSystem system, ChartViewModel model) {
    return isCurrent(system, model) || system.warp() || system.tracked() || system.selected();
  }

  private static boolean isCurrent(ChartSystem system, ChartViewModel model) {
    return system.x() == model.currentX() && system.y() == model.currentY();
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
      drawEdgeArrow(canvas, tracked.x() - model.viewX(), tracked.y() - model.viewY(), TRACKED_ARROW_COLOR);
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
