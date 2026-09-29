/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.Gadget;
import org.gts.bst.ship.equip.Shield;
import org.gts.bst.ship.equip.Weapon;
import org.gts.bst.view.ShipSites;
import spacetrader.Ship;


/**
 * The pieces a ship shows in its drawing: the game equipment by its name and the
 * fixed parts. The names must match the pieces of the art files (`Engine`,
 * `Fuel Tank`, `Cockpit`, `Escape Pod` and the equipment names of the game).
 */
public final class ShipArtItems {
  private static final String COCKPIT = "Cockpit";
  private static final String ENGINE = "Engine";
  private static final String FUEL_TANK = "Fuel Tank";
  private static final String ESCAPE_POD = "Escape Pod";

  private ShipArtItems() {
  }

  /** The pieces of a ship: its equipment and its fixed parts (cockpit, engines, fuel, pod). */
  public static List<String> of(Ship ship) {
    List<String> items = new ArrayList<>();
    for(Weapon weapon : ship.Weapons()) {
      items.add(weapon.Name());
    }
    for(Shield shield : ship.Shields()) {
      items.add(shield.Name());
    }
    for(Gadget gadget : ship.Gadgets()) {
      items.add(gadget.Name());
    }
    items.addAll(fixed(ship.Type()));
    if(ship.getEscapePod()) {
      items.add(ESCAPE_POD);
    }
    return items;
  }

  /** The pieces of a ship nobody has equipped yet: its cockpit, engines and fuel tanks. */
  public static List<String> fixed(ShipType type) {
    List<String> items = new ArrayList<>();
    ShipSites.Budget budget = ShipSites.budgetOf(type.name());
    if(budget != null) {
      for(int i = 0; i < budget.engines(); i++) {
        items.add(ENGINE);
      }
      for(int i = 0; i < budget.fuel(); i++) {
        items.add(FUEL_TANK);
      }
    }
    items.add(COCKPIT);
    return items;
  }
}
