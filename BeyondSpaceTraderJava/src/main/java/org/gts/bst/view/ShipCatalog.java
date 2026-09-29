/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.List;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;


/**
 * The ship art of the game: the chassis, the pieces and the assemblies, loaded
 * from the art files and drawn with {@link ShipRenderer}. A ship type with no
 * assembly of its own falls back to a chassis of its size, so every ship can be
 * drawn; the files are read once and the catalogue is shared.
 */
public final class ShipCatalog {
  private static final ShipCatalog SHARED = load();

  private final List<ShipDesign> designs;
  private final List<ShipArtFile> chassis;
  private final List<ShipArtFile> pieces;

  public ShipCatalog(List<ShipDesign> designs, List<ShipArtFile> chassis, List<ShipArtFile> pieces) {
    this.designs = List.copyOf(designs);
    this.chassis = List.copyOf(chassis);
    this.pieces = List.copyOf(pieces);
  }

  /** The art of the game, read once from the ships files. */
  public static ShipCatalog shared() {
    return SHARED;
  }

  private static ShipCatalog load() {
    try {
      return new ShipCatalog(ShipDesign.load(ShipArtFile.resolve("ships.txt").toString()),
          ShipArtFile.load("chassis.txt"), ShipArtFile.load("pieces.txt"));
    } catch(java.io.IOException e) {
      // The art files are missing or unreadable: the screens keep their old sprites.
      return new ShipCatalog(List.of(), List.of(), List.of());
    }
  }

  /** The picture of a ship: its assembly and chassis, with the pieces of the items on board. */
  public ShipPicture picture(ShipType type, List<String> items, int cargoBays) {
    ShipDesign design = designOf(type);
    ShipArtFile hull = design == null ? null : chassisNamed(design.chassis());
    if(hull == null) {
      // No assembly (or its chassis is missing): a chassis of the size will do, with no sites.
      hull = chassisOfSize(type);
      design = hull == null ? null : new ShipDesign(type.name(), type.name(), hull.name(), List.of());
    }
    if(hull == null) {
      return new ShipPicture(0, 0);
    }
    return ShipRenderer.draw(design, hull, pieces, items, cargoBays);
  }

  /** The assembly of a ship type, or null when the type has none. */
  public ShipDesign designOf(ShipType type) {
    for(ShipDesign design : designs) {
      if(design.type().equalsIgnoreCase(type.name())) {
        return design;
      }
    }
    return null;
  }

  /** The chassis with a name, or null when it is not drawn. */
  public ShipArtFile chassisNamed(String name) {
    for(ShipArtFile hull : chassis) {
      if(hull.name().equalsIgnoreCase(name)) {
        return hull;
      }
    }
    return null;
  }

  /** A chassis for a ship type with no assembly: the one of its size, or the first drawn. */
  private ShipArtFile chassisOfSize(ShipType type) {
    ShipSites.Budget budget = ShipSites.budgetOf(type.name());
    ShipSize size = budget == null ? null : budget.size();
    if(size != null) {
      for(ShipArtFile hull : chassis) {
        if(size.toString().equalsIgnoreCase(hull.size())) {
          return hull;
        }
      }
      for(ShipArtFile hull : chassis) {
        if(ShipSites.sizeFits(hull.size(), size)) {
          return hull;
        }
      }
    }
    return chassis.isEmpty() ? null : chassis.get(0);
  }
}
