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


/**
 * Everything the shipyard designer displays, already formatted. {@code percentLevel}
 * tells the front-end how to highlight the used-units percentage (0 normal, 1 first
 * penalty, 2 second penalty, 3 over the maximum).
 */
public record ShipyardDesignerViewModel(
    String title,
    int logoIndex,
    String welcome,
    String sizeSpecialty,
    String skill,
    String skillDescription,
    String warning,
    List<String> sizes,
    int sizeIndex,
    List<String> templates,
    int templateIndex,
    String name,
    List<Numeric> numerics,
    String unitsUsed,
    String percent,
    int percentLevel,
    String shipCost,
    String designFee,
    String penalty,
    String tradeIn,
    String totalCost,
    boolean constructEnabled,
    boolean saveEnabled,
    int imageIndex,
    boolean customImage,
    String imageName) {

  public record Numeric(int value, Integer min, Integer max, Integer increment) {
  }
}
