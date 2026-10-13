/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;


/**
 * Translates the localised names of the ship equipment (the ones the player
 * reads) to the English canonical names of the art pieces ({@code pieces.txt}),
 * which is what {@link ShipSites}, {@link ShipRenderer} and {@link ShipCatalog}
 * look up. The labels shown do not change: the view models keep their localised
 * names and this class is only the bridge to the art.
 *
 * <p>The map is built once from the bundle of the default locale (the game
 * chooses it at startup). The cargo-bay gadgets lose their amount prefix
 * ("5 Extra Cargo Bays" becomes "Extra Cargo Bays") so they match the pieces.
 */
public final class ShipArtNames {
  /** The bundle of the game texts. */
  private static final String BUNDLE = "spacetrader.Strings";
  private static final Map<String, String> ALIASES = aliases(Locale.getDefault());

  private ShipArtNames() {
  }

  /**
   * The English canonical name of a piece, or the name itself when it is unknown
   * (the fixed parts, the roles and the chassis are not in the bundle).
   */
  public static String canonical(String name) {
    if(name == null) {
      return null;
    }
    String canonical = ALIASES.get(name.strip().toLowerCase(Locale.ROOT));
    return canonical == null ? name : canonical;
  }

  /**
   * The alias map of a locale: every localised equipment name (stripped and
   * lowercased) to its English canonical name. The English names are aliased
   * too, so {@code canonical("5 Extra Cargo Bays")} works whatever the locale
   * of the machine. Package-private so the tests can build the map of a language
   * without changing the locale of the machine.
   */
  static Map<String, String> aliases(Locale locale) {
    ResourceBundle localized = ResourceBundle.getBundle(BUNDLE, locale);
    // The English texts are the base bundle: ask for Locale.ROOT, because asking
    // for Locale.ENGLISH under another default locale falls back to that one.
    ResourceBundle english = ResourceBundle.getBundle(BUNDLE, Locale.ROOT);
    Map<String, String> aliases = new HashMap<>();
    for(WeaponType type : WeaponType.values()) {
      String key = "WeaponType." + type.name() + ".name";
      String canonical = english.getString(key);
      add(aliases, localized.getString(key), canonical, canonical);
    }
    for(ShieldType type : ShieldType.values()) {
      String key = "ShieldNames." + type.id;
      String canonical = english.getString(key);
      add(aliases, localized.getString(key), canonical, canonical);
    }
    for(GadgetType type : GadgetType.values()) {
      String key = "GadgetNames." + type.asInteger();
      String englishName = english.getString(key);
      add(aliases, localized.getString(key), englishName, withoutAmount(englishName));
    }
    return aliases;
  }

  /** "5 Extra Cargo Bays" to "Extra Cargo Bays": the art has no amount. */
  static String withoutAmount(String name) {
    int space = name.indexOf(' ');
    if(space > 0 && name.substring(0, space).chars().allMatch(Character::isDigit)) {
      return name.substring(space + 1);
    }
    return name;
  }

  private static void add(Map<String, String> aliases, String localizedName, String englishName, String canonical) {
    aliases.put(normalize(localizedName), canonical);
    aliases.put(normalize(englishName), canonical);
  }

  private static String normalize(String name) {
    return name.strip().toLowerCase(Locale.ROOT);
  }
}
