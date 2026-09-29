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
 * Draws a ship with the art files: first the chassis (every ink cell with the
 * colour of the zone that covers it, or the hull's own), and then the pieces of
 * the items on board, one per cell of their site. The cargo gauge needs no
 * piece: it is drawn in braille, one dot per bay. The colours stay as names, so
 * the front-end resolves them; the game can draw the same ship anywhere.
 */
public final class ShipRenderer {
  private ShipRenderer() {
  }

  /**
   * Draws a design over its chassis. The items are the names of the pieces on
   * board (weapons, shields, gadgets, cockpit, engines, fuel tanks, role, pod);
   * they go on the sites of their kind, in order, and whatever does not fit or
   * has no art yet leaves the chassis showing through.
   */
  public static ShipPicture draw(ShipDesign design, ShipArtFile chassis, List<ShipArtFile> pieces, List<String> items,
      int cargoBays) {
    ShipPicture picture = new ShipPicture(chassis.width(), chassis.height());
    paintChassis(picture, chassis);
    for(ShipDesign.LetterGroup group : design.groups()) {
      paintSite(picture, pieces, items, cargoBays, group);
    }
    return picture;
  }

  /** Paints the chassis: every ink cell with the colour of its zone, or the hull's own. */
  private static void paintChassis(ShipPicture picture, ShipArtFile chassis) {
    for(int row = 0; row < chassis.height(); row++) {
      for(int column = 0; column < chassis.width(); column++) {
        int codePoint = chassis.at(row, column);
        if(codePoint == ' ' || codePoint == ShipArtFile.CONTINUATION) {
          continue;
        }
        ShipArtFile.ColorLetter zone = zoneLetter(chassis, column, row);
        if(zone == null) {
          picture.put(column, row, codePoint, chassis.color(), chassis.bgColor(), chassis.blink());
        } else {
          picture.put(column, row, codePoint, zone.color(), zone.bgColor(), zone.blink());
        }
      }
    }
  }

  /** The colour letter of the zone that covers a cell, or null when the cell has no zone. */
  private static ShipArtFile.ColorLetter zoneLetter(ShipArtFile chassis, int x, int y) {
    for(ShipArtFile.Zone zone : chassis.zones()) {
      if(x >= zone.x() && x < zone.x() + Math.max(1, zone.w()) && y >= zone.y()
          && y < zone.y() + Math.max(1, zone.h())) {
        for(ShipArtFile.ColorLetter letter : chassis.letters()) {
          if(letter.letter() == zone.letter()) {
            return letter;
          }
        }
      }
    }
    return null;
  }

  /** Paints a site: the gauge when it is the cargo one, or a piece per cell for the items of its kind. */
  private static void paintSite(ShipPicture picture, List<ShipArtFile> pieces, List<String> items, int cargoBays,
      ShipDesign.LetterGroup group) {
    ShipSites.Kind kind = ShipSites.kindOfLetter(group.letter(), pieces);
    if(kind == ShipSites.Kind.CARGO) {
      paintGauge(picture, group, cargoBays);
      return;
    }
    if(kind == ShipSites.Kind.PART) {
      return;
    }
    List<String> ofKind = itemsOfKind(items, kind);
    for(int i = 0; i < Math.max(1, group.n()) && i < ofKind.size(); i++) {
      ShipArtFile piece = pieceNamed(pieces, ofKind.get(i));
      if(piece != null) {
        paintPiece(picture, piece, group.x() + i, group.y());
      }
    }
  }

  /** The items on board that are of a kind, in the order they were given. */
  private static List<String> itemsOfKind(List<String> items, ShipSites.Kind kind) {
    List<String> ofKind = new ArrayList<>();
    for(String item : items) {
      if(ShipSites.kindOf(item) == kind) {
        ofKind.add(item);
      }
    }
    return ofKind;
  }

  /** The art of an item: the piece with that name, or null when it has no art yet. */
  private static ShipArtFile pieceNamed(List<ShipArtFile> pieces, String item) {
    for(ShipArtFile piece : pieces) {
      if(piece.name().equalsIgnoreCase(item)) {
        return piece;
      }
    }
    return null;
  }

  /** Paints a piece over a site cell: its glyphs, with the hull showing through its spaces. */
  private static void paintPiece(ShipPicture picture, ShipArtFile piece, int x, int y) {
    for(int row = 0; row < piece.height(); row++) {
      for(int column = 0; column < piece.width(); column++) {
        int codePoint = piece.at(row, column);
        if(codePoint == ' ' || codePoint == ShipArtFile.CONTINUATION) {
          continue;
        }
        picture.put(x + column, y + row, codePoint, piece.color(), piece.bgColor(), piece.blink());
      }
    }
  }

  /** Draws the cargo gauge of a site: braille, one dot per bay, up to the cells of its run. */
  private static void paintGauge(ShipPicture picture, ShipDesign.LetterGroup group, int cargoBays) {
    String gauge = ShipSites.gauge(cargoBays);
    for(int i = 0; i < gauge.length() && i < Math.max(1, group.n()); i++) {
      picture.put(group.x() + i, group.y(), gauge.codePointAt(i), "green", "", false);
    }
  }
}
