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
import java.util.List;


/**
 * A ship drawn with the art files: a fixed grid of cells, each with a glyph and
 * the names of its colours, so every front-end can paint it (the names are the
 * ones of the art files, and the front-end resolves them). Built by
 * {@link ShipRenderer}.
 */
public final class ShipPicture {
  /** One cell of the drawing: a code point and the names of its colours. */
  public record Cell(int codePoint, String color, String background, boolean blink) {
    /** True when the cell is the right half of a wide glyph: the front-end must not draw it. */
    public boolean continuation() {
      return codePoint == ShipArtFile.CONTINUATION;
    }
  }

  private final int width;
  private final int height;
  private final Cell[] cells;

  ShipPicture(int width, int height) {
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
    this.cells = new Cell[this.width * this.height];
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  /** The cell of a position, or null when the position is empty or out of the grid. */
  public Cell at(int x, int y) {
    if(x < 0 || y < 0 || x >= width || y >= height) {
      return null;
    }
    return cells[y * width + x];
  }

  /** The picture seen from the other side: the opponent in an encounter faces the player. */
  public ShipPicture mirrored() {
    ShipPicture mirrored = new ShipPicture(width, height);
    for(int y = 0; y < height; y++) {
      for(int x = 0; x < width; x++) {
        Cell cell = at(x, y);
        if(cell == null || cell.continuation()) {
          continue;
        }
        int target = width - 1 - x - (ShipArtFile.isWide(cell.codePoint()) ? 1 : 0);
        mirrored.put(Math.max(0, target), y, cell.codePoint(), cell.color(), cell.background(), cell.blink());
      }
    }
    return mirrored;
  }

  /** The cells of a row, left to right (empty cells are null). */
  public List<Cell> row(int y) {
    List<Cell> row = new ArrayList<>(width);
    for(int x = 0; x < width; x++) {
      row.add(at(x, y));
    }
    return row;
  }

  /** Paints a glyph: a space does nothing, and a wide glyph takes its two cells. */
  void put(int x, int y, int codePoint, String color, String background, boolean blink) {
    if(x < 0 || y < 0 || x >= width || y >= height || codePoint == ' ') {
      return;
    }
    cells[y * width + x] = new Cell(codePoint, color, background, blink);
    if(ShipArtFile.isWide(codePoint) && x + 1 < width) {
      cells[y * width + x + 1] = new Cell(ShipArtFile.CONTINUATION, color, background, blink);
    }
  }
}
