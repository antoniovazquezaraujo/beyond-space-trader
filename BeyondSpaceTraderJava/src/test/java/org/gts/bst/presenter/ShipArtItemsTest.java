/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Ship;


class ShipArtItemsTest {
  @Test
  void listsTheEquipmentAndTheFixedParts() {
    Ship ship = new Ship(ShipType.Firefly);
    ship.AddEquipment(Consts.WeapObjs.get(WeaponType.PulseLaser.id));
    ship.AddEquipment(Consts.Shields.get(ShieldType.Energy.id));
    ship.AddEquipment(Consts.Gadgets.get(GadgetType.NavigatingSystem.asInteger()));
    ship.setEscapePod(true);

    List<String> items = ShipArtItems.of(ship);

    assertTrue(items.contains("Pulse Laser"), items.toString());
    assertTrue(items.contains("Energy Shield"), items.toString());
    assertTrue(items.contains("Navigating System"), items.toString());
    assertTrue(items.contains("Cockpit"), items.toString());
    assertTrue(items.contains("Engine"), items.toString());
    assertTrue(items.contains("Fuel Tank"), items.toString());
    assertTrue(items.contains("Escape Pod"), items.toString());
  }

  @Test
  void aShipForSaleOnlyShowsItsFixedParts() {
    List<String> items = ShipArtItems.fixed(ShipType.Wasp);

    assertEquals(List.of("Engine", "Engine", "Engine", "Fuel Tank", "Fuel Tank", "Cockpit"), items);
  }
}
