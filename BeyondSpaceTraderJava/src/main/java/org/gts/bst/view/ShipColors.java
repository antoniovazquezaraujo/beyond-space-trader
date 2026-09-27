/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import com.googlecode.lanterna.TextColor;


/**
 * The colours of the art files: an English name, a #rrggbb value or a palette
 * number (0-255). Unknown names fall back to white.
 */
public final class ShipColors {
  private ShipColors() {
  }

  public static TextColor color(String value) {
    String name = value == null ? "" : value.strip().toLowerCase();
    if(name.startsWith("#") && name.length() == 7) {
      try {
        return new TextColor.RGB(Integer.parseInt(name.substring(1, 3), 16), Integer.parseInt(name.substring(3, 5), 16),
            Integer.parseInt(name.substring(5, 7), 16));
      } catch(NumberFormatException e) {
        return TextColor.ANSI.WHITE;
      }
    }
    try {
      int index = Integer.parseInt(name);
      return index >= 0 && index <= 255 ? new TextColor.Indexed(index) : TextColor.ANSI.WHITE;
    } catch(NumberFormatException e) {
      return byName(name);
    }
  }

  private static TextColor byName(String name) {
    switch(name) {
      case "black":
      case "negro":
        return TextColor.ANSI.BLACK;
      case "red":
      case "rojo":
        return TextColor.ANSI.RED;
      case "green":
      case "verde":
        return TextColor.ANSI.GREEN;
      case "yellow":
      case "amarillo":
        return TextColor.ANSI.YELLOW;
      case "blue":
      case "azul":
        return TextColor.ANSI.BLUE;
      case "magenta":
        return TextColor.ANSI.MAGENTA;
      case "cyan":
      case "cian":
        return TextColor.ANSI.CYAN;
      case "grey":
      case "gray":
      case "gris":
        return TextColor.ANSI.WHITE;
      case "brightwhite":
        return TextColor.ANSI.WHITE_BRIGHT;
      case "brightred":
        return TextColor.ANSI.RED_BRIGHT;
      case "brightgreen":
        return TextColor.ANSI.GREEN_BRIGHT;
      case "brightyellow":
        return TextColor.ANSI.YELLOW_BRIGHT;
      case "brightblue":
        return TextColor.ANSI.BLUE_BRIGHT;
      case "brightmagenta":
        return TextColor.ANSI.MAGENTA_BRIGHT;
      case "brightcyan":
        return TextColor.ANSI.CYAN_BRIGHT;
      case "orange":
        return new TextColor.Indexed(208);
      case "purple":
        return new TextColor.Indexed(93);
      case "pink":
        return new TextColor.Indexed(218);
      case "brown":
        return new TextColor.Indexed(130);
      case "darkgrey":
      case "darkgray":
        return new TextColor.Indexed(238);
      case "lightgrey":
      case "lightgray":
        return new TextColor.Indexed(250);
      case "gold":
        return new TextColor.Indexed(220);
      case "navy":
        return new TextColor.Indexed(17);
      case "teal":
        return new TextColor.Indexed(30);
      case "olive":
        return new TextColor.Indexed(58);
      case "maroon":
        return new TextColor.Indexed(88);
      case "lime":
        return new TextColor.Indexed(118);
      case "skyblue":
        return new TextColor.Indexed(117);
      default:
        return TextColor.ANSI.WHITE;
    }
  }
}
