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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import spacetrader.Consts;
import spacetrader.ShipSpec;


/**
 * The sites of a ship design: the letters reserved in the chassis art (C cockpit,
 * M engines, D fuel, B cargo gauge, R role, A weapons, E shields, G gadgets and
 * P escape pod) and the numbers the ship type allows, read from the game specs.
 * The panel and the warnings are pure text so they can be tested without a UI.
 */
public final class ShipSites {
  public static final char COCKPIT = 'C';
  public static final char ENGINES = 'M';
  public static final char FUEL = 'D';
  public static final char CARGO = 'B';
  public static final char ROLE = 'R';
  public static final char WEAPON = 'A';
  public static final char SHIELD = 'E';
  public static final char GADGET = 'G';
  public static final char POD = 'P';
  /** The site letters, in the order the panel shows them. */
  static final char[] ALL = {COCKPIT, ENGINES, FUEL, CARGO, ROLE, WEAPON, SHIELD, GADGET, POD};
  private static final String[] NAMES = {"cabina", "motor", "deposito", "bodega", "rol", "arma", "escudo",
      "artilugio", "capsula"};
  /** The dots the braille gauge fills first (the owner's order). */
  private static final int[] DOT_ORDER = {7, 8, 3, 6, 2, 5, 1, 4};

  private ShipSites() {
  }

  /** The name of a site, or "?" when the letter is not one. */
  public static String name(char site) {
    for(int i = 0; i < ALL.length; i++) {
      if(ALL[i] == site) {
        return NAMES[i];
      }
    }
    return "?";
  }

  /** True when the code point is one of the site letters. */
  public static boolean isSite(int codePoint) {
    for(char site : ALL) {
      if(codePoint == site) {
        return true;
      }
    }
    return false;
  }

  /** One site drawn in the chassis: a horizontal run of the same letter. */
  public record Site(char site, int x, int y, int length) {
  }

  /** What a ship type allows: the maximum the game may fill of each thing. */
  public record Budget(String ship, ShipSize size, int cargoBays, int cargoCells, int cockpit, int engines, int fuel,
      int role, int weapons, int shields, int gadgets, int pod) {
  }

  /** The budget of a ship type by name, or null when the name is not a type. */
  public static Budget budgetOf(String ship) {
    if(ship == null) {
      return null;
    }
    ShipType type;
    try {
      type = ShipType.valueOf(ship);
    } catch(IllegalArgumentException e) {
      return null;
    }
    ShipSpec spec = Consts.ShipSpecs.get(type.CastToInt());
    int engines = spec.getSize() == ShipSize.Medium || spec.getSize() == ShipSize.Large ? 2
        : spec.getSize() == ShipSize.Huge ? 3 : 1;
    int fuel = spec.getSize() == ShipSize.Large || spec.getSize() == ShipSize.Huge ? 2 : 1;
    int cargoCells = (spec.CargoBays() + 5 * spec.getGadgetSlots() + 7) / 8;
    return new Budget(ship, spec.getSize(), spec.CargoBays(), cargoCells, 1, engines, fuel, 1, spec.getWeaponSlots(),
        spec.getShieldSlots(), spec.getGadgetSlots(), spec.Occurrence() > 0 ? 1 : 0);
  }

  /** The maximum of a site kind, or -1 when the ship has none. */
  public static int maxOf(Budget budget, char site) {
    switch(site) {
      case COCKPIT:
        return budget.cockpit();
      case ENGINES:
        return budget.engines();
      case FUEL:
        return budget.fuel();
      case CARGO:
        return budget.cargoCells();
      case ROLE:
        return budget.role();
      case WEAPON:
        return budget.weapons();
      case SHIELD:
        return budget.shields();
      case GADGET:
        return budget.gadgets();
      case POD:
        return budget.pod();
      default:
        return -1;
    }
  }

  /** The site runs of a chassis, left to right and top to bottom. */
  public static List<Site> sitesOf(ShipArtFile chassis) {
    List<Site> sites = new ArrayList<>();
    for(int row = 0; row < chassis.height(); row++) {
      int column = 0;
      while(column < chassis.width()) {
        int codePoint = chassis.at(row, column);
        if(!isSite(codePoint)) {
          column++;
          continue;
        }
        int length = 1;
        while(column + length < chassis.width() && chassis.at(row, column + length) == codePoint) {
          length++;
        }
        sites.add(new Site((char) codePoint, column, row, length));
        column += length;
      }
    }
    return sites;
  }

  /** How many cells of each kind the chassis draws. */
  public static Map<Character, Integer> count(List<Site> sites) {
    Map<Character, Integer> count = emptyCounts();
    for(Site site : sites) {
      count.merge(site.site(), site.length(), Integer::sum);
    }
    return count;
  }

  /** The longest run of each kind (what the cargo gauge needs). */
  public static Map<Character, Integer> longestRun(List<Site> sites) {
    Map<Character, Integer> longest = emptyCounts();
    for(Site site : sites) {
      longest.merge(site.site(), site.length(), Math::max);
    }
    return longest;
  }

  private static Map<Character, Integer> emptyCounts() {
    Map<Character, Integer> counts = new LinkedHashMap<>();
    for(char site : ALL) {
      counts.put(site, 0);
    }
    return counts;
  }

  /** The panel lines: the ship type and the placed/maximum of every site. */
  public static List<String> panel(String ship, Map<Character, Integer> placed, Map<Character, Integer> runs) {
    List<String> lines = new ArrayList<>();
    Budget budget = budgetOf(ship);
    lines.add(budget == null ? "sin ficha de tipo" : ship + " (" + budget.size() + ")");
    lines.add("sitio       hay/max");
    for(int i = 0; i < ALL.length; i++) {
      char site = ALL[i];
      int max = budget == null ? -1 : maxOf(budget, site);
      int there = site == CARGO ? runs.getOrDefault(site, 0) : placed.getOrDefault(site, 0);
      lines.add(String.format("%-9s %c %4d/%-3s %s", NAMES[i], site, there, max < 0 ? "?" : max,
          mark(there, max, site)));
    }
    return lines;
  }

  private static String mark(int there, int max, char site) {
    if(max < 0) {
      return "";
    }
    if(site == ROLE || site == POD) {
      return there == 0 ? "·" : there <= max ? "ok" : "sobran";
    }
    if(there < max) {
      return "faltan " + (max - there);
    }
    return there == max ? "ok" : "sobran " + (there - max);
  }

  /** The warnings of a chassis against its type: empty when everything fits. */
  public static List<String> warnings(String ship, Map<Character, Integer> placed, Map<Character, Integer> runs) {
    List<String> warnings = new ArrayList<>();
    Budget budget = budgetOf(ship);
    if(budget == null) {
      return warnings;
    }
    boolean custom = "Custom".equals(ship);
    for(int i = 0; i < ALL.length; i++) {
      char site = ALL[i];
      int max = maxOf(budget, site);
      int there = site == CARGO ? runs.getOrDefault(site, 0) : placed.getOrDefault(site, 0);
      if(site == CARGO && max == 0) {
        continue;
      }
      if(site == ROLE || site == POD) {
        if(there > max) {
          warnings.add("sobran " + NAMES[i]);
        }
      } else if(there < max) {
        warnings.add(site == CARGO ? "faltan celdas en la bodega (" + (max - there) + ")"
            : "faltan sitios de " + NAMES[i] + " (" + (max - there) + ")");
      } else if(there > max && !custom) {
        warnings.add("sobran sitios de " + NAMES[i] + " (" + (there - max) + ")");
      }
    }
    return warnings;
  }

  /** The braille gauge of a capacity: one dot per bay, in the owner's order. */
  public static String gauge(int bays) {
    StringBuilder gauge = new StringBuilder();
    for(int left = bays; left > 0; left -= Math.min(8, left)) {
      int dots = Math.min(8, left);
      int pattern = 0;
      for(int i = 0; i < dots; i++) {
        pattern |= 1 << (DOT_ORDER[i] - 1);
      }
      gauge.appendCodePoint(0x2800 + pattern);
    }
    return gauge.toString();
  }
}
