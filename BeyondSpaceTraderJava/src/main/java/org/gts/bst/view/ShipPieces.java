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
 * The vocabulary of the ship art: every piece with its name, its zone and its
 * pixel footprint. The same piece can be rendered as characters (1x1), block
 * elements (2x2) or braille (2x4), so the catalogue can compare the three.
 */
public final class ShipPieces {
  /** A piece: a small pixel drawing ('#' set) with its zone. */
  public record Piece(String name, char zone, String[] pattern) {
    public int width() {
      return pattern[0].length();
    }

    public int height() {
      return pattern.length;
    }

    /** The piece as a pixel grid (all its cells share its zone). */
    public ShipPixelArt toPixelArt() {
      boolean[][] pixels = new boolean[height()][width()];
      char[][] zones = new char[height() / 4][width() / 2];
      for(int row = 0; row < height(); row++) {
        for(int column = 0; column < width(); column++) {
          pixels[row][column] = pattern[row].charAt(column) == '#';
        }
      }
      for(int row = 0; row < zones.length; row++) {
        for(int column = 0; column < zones[0].length; column++) {
          zones[row][column] = zone;
        }
      }
      return new ShipPixelArt(pixels, zones);
    }

    /** The piece one pixel per character, for the catalogue. */
    public ShipArt asAscii() {
      List<String> lines = new ArrayList<>(height());
      List<String> zones = new ArrayList<>(height());
      for(String line : pattern) {
        lines.add(line.replace('#', '=').replace('.', ' '));
        zones.add(("" + zone).repeat(width()));
      }
      return new ShipArt(lines, zones);
    }
  }

  private static final List<Piece> PIECES = List.of(
      new Piece("motor", ShipArt.ENGINE, new String[] {
          ".##..##.",
          ".##..##.",
          ".##..##.",
          ".##..##.",
          "########",
          "########",
          ".######.",
          "..####..",
      }),
      new Piece("torreta laser", ShipArt.WEAPON, new String[] {
          "...##...",
          "..####..",
          "...##...",
          "########",
          "...##...",
          "..####..",
          "...##...",
          "........",
      }),
      new Piece("torreta disruptor", ShipArt.WEAPON, new String[] {
          ".#####..",
          "##...##.",
          "#.....#.",
          "##...##.",
          ".#####..",
          "...##...",
          "...##...",
          "...##...",
      }),
      new Piece("vaina de carga", ShipArt.CARGO, new String[] {
          "########",
          "#..##..#",
          "#..##..#",
          "#..##..#",
          "#..##..#",
          "########",
          "........",
          "........",
      }),
      new Piece("cabina", ShipArt.COCKPIT, new String[] {
          ".####...",
          "##..##..",
          "#....#..",
          "######..",
          "........",
          "........",
          "........",
          "........",
      }),
      new Piece("deposito", ShipArt.HULL, new String[] {
          ".####...",
          "######..",
          "#....#..",
          "#....#..",
          "#....#..",
          "#....#..",
          "######..",
          ".####...",
      }),
      new Piece("mastil de sensores", ShipArt.HULL, new String[] {
          ".##.....",
          ".##.....",
          ".##.....",
          ".##.....",
          ".##.....",
          ".##.....",
          ".##.....",
          ".##.....",
      }),
      new Piece("placa de casco", ShipArt.HULL, new String[] {
          "########",
          "#......#",
          "#.####.#",
          "#......#",
          "########",
          "........",
          "........",
          "........",
      }),
      new Piece("greeble (tipo)", ShipArt.HULL, new String[] {
          ".##.....",
          "####....",
          ".##.....",
          "........",
          "........",
          "........",
          "........",
          "........",
      }));

  private ShipPieces() {
  }

  public static List<Piece> all() {
    return PIECES;
  }
}
