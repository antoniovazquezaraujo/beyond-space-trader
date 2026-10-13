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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;


class ShipArtNamesTest {
  private static final Locale SPANISH = Locale.forLanguageTag("es");

  @Test
  void theEnglishNamesPassThroughUnchanged() {
    assertEquals("Pulse Laser", ShipArtNames.canonical("Pulse Laser"));
    assertEquals("Morgan's Laser", ShipArtNames.canonical("Morgan's Laser"));
    assertEquals("Cockpit", ShipArtNames.canonical("Cockpit"), "a fixed part is not in the map");
    assertEquals("Role Pirate", ShipArtNames.canonical("Role Pirate"));
    assertNull(ShipArtNames.canonical(null));
  }

  @Test
  void theCargoBaysLoseTheirAmountPrefix() {
    assertEquals("Extra Cargo Bays", ShipArtNames.withoutAmount("5 Extra Cargo Bays"));
    assertEquals("Hidden Cargo Bays", ShipArtNames.withoutAmount("5 Hidden Cargo Bays"));
    assertEquals("Auto-Repair System", ShipArtNames.withoutAmount("Auto-Repair System"),
        "without the prefix pattern the name is kept");
    assertEquals("Extra Cargo Bays", ShipArtNames.canonical("5 Extra Cargo Bays"),
        "the English item maps to the piece of the art");
    assertEquals("Hidden Cargo Bays", ShipArtNames.canonical("5 Hidden Cargo Bays"));
  }

  @Test
  void theSpanishEquipmentMapsToItsEnglishCanonicalName() {
    ResourceBundle bundleEs = ResourceBundle.getBundle("spacetrader.Strings", SPANISH);
    // The English texts are the base bundle: see the comment of ShipArtNames.
    ResourceBundle bundleEn = ResourceBundle.getBundle("spacetrader.Strings", Locale.ROOT);
    Map<String, String> aliases = ShipArtNames.aliases(SPANISH);

    String weapon = bundleEs.getString("WeaponType.PulseLaser.name").toLowerCase(Locale.ROOT);
    assertEquals(bundleEn.getString("WeaponType.PulseLaser.name"), aliases.get(weapon));
    String shield = bundleEs.getString("ShieldNames.0").toLowerCase(Locale.ROOT);
    assertEquals(bundleEn.getString("ShieldNames.0"), aliases.get(shield));
    String gadget = bundleEs.getString("GadgetNames.0").toLowerCase(Locale.ROOT);
    assertEquals("Extra Cargo Bays", aliases.get(gadget), "the amount is not part of the piece");
  }

  @Test
  void theSpanishNameFindsItsSiteAndItsArt() {
    ResourceBundle bundleEs = ResourceBundle.getBundle("spacetrader.Strings", SPANISH);
    String spanishWeapon = bundleEs.getString("WeaponType.PulseLaser.name");
    String canonical = ShipArtNames.aliases(SPANISH).get(spanishWeapon.toLowerCase(Locale.ROOT));

    assertEquals("Pulse Laser", canonical);
    assertEquals(ShipSites.Kind.WEAPON, ShipSites.kindOf(canonical), "the site of the piece");
    assertNotNull(ShipCatalog.shared().piece(canonical), "and its art");
  }
}
