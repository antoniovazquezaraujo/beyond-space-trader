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
 * <p>The galactic chart is a viewport of the whole galaxy centred on
 * {@code viewX/viewY}; the short-range chart is centred on the current system and
 * scales the distances to fit.
 */
public final class ChartRenderer {
  static final char CURRENT = '+';
  static final char WARP = '@';
  static final char TRACKED = 'X';
  static final char VISITED = '*';
  static final char UNVISITED = 'o';
  static final char WORMHOLE = '~';

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
    int panX = pan(model.viewX(), canvas.width(), model.galaxyWidth());
    int panY = pan(model.viewY(), canvas.height(), model.galaxyHeight());
    for(ChartSystem system : model.systems()) {
      int x = system.x() - panX;
      int y = system.y() - panY;
      if(!inside(canvas, x, y)) {
        continue;
      }
      canvas.put(x, y, marker(system, model), color(system, model));
      drawWormhole(canvas, system, x, y);
    }
  }

  private static void renderShortRange(ChartCanvas canvas, ChartViewModel model) {
    int centerX = canvas.width() / 2;
    int centerY = canvas.height() / 2;
    int delta = Math.max(1, canvas.height() / (model.maxRange() * 2));
    // First the names, then the systems: the markers stop the names from hiding them.
    for(int pass = 0; pass < 2; pass++) {
      for(ChartSystem system : model.systems()) {
        int x = centerX + (system.x() - model.currentX()) * delta;
        int y = centerY + (system.y() - model.currentY()) * delta;
        if(pass == 0) {
          drawName(canvas, system.name(), x, y);
        } else if(inside(canvas, x, y)) {
          canvas.put(x, y, marker(system, model), color(system, model));
          drawWormhole(canvas, system, x, y);
        }
      }
    }
    canvas.put(centerX, centerY, CURRENT, ChartColor.CYAN);
    drawTrackingArrow(canvas, model, centerX, centerY, delta);
    if(model.trackedRangeText() != null) {
      drawText(canvas, 0, canvas.height() - 1, model.trackedRangeText(), ChartColor.DEFAULT);
    }
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

  private static void drawTrackingArrow(ChartCanvas canvas, ChartViewModel model, int centerX, int centerY, int delta) {
    ChartSystem tracked = null;
    for(ChartSystem system : model.systems()) {
      if(system.tracked()) {
        tracked = system;
        break;
      }
    }
    if(tracked == null || isCurrent(tracked, model)) {
      return;
    }
    int x = centerX + (tracked.x() - model.currentX()) * delta;
    int y = centerY + (tracked.y() - model.currentY()) * delta;
    if(inside(canvas, x, y)) {
      return;
    }
    int clampedX = Math.max(0, Math.min(x, canvas.width() - 1));
    int clampedY = Math.max(0, Math.min(y, canvas.height() - 1));
    char arrow;
    if(Math.abs(x - centerX) >= Math.abs(y - centerY)) {
      arrow = x >= centerX ? '>' : '<';
    } else {
      arrow = y >= centerY ? 'v' : '^';
    }
    canvas.put(clampedX, clampedY, arrow, ChartColor.RED);
  }

  private static int pan(int center, int canvasSize, int galaxySize) {
    if(canvasSize >= galaxySize) {
      return 0;
    }
    return Math.max(0, Math.min(center - canvasSize / 2, galaxySize - canvasSize));
  }

  private static boolean inside(ChartCanvas canvas, int x, int y) {
    return x >= 0 && y >= 0 && x < canvas.width() && y < canvas.height();
  }
}
