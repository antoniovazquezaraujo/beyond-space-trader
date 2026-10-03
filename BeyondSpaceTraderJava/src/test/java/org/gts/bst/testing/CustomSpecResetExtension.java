/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.testing;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import spacetrader.Consts;
import spacetrader.ShipSpec;


/**
 * Puts the shared custom ship spec back to the one Consts declares before and
 * after every test. The shipyard designer mutates it in place
 * ({@code Shipyard.ShipSpec()} is the object of {@code Consts}), so a test that
 * opens the designer and changes the size would otherwise change the budget of
 * {@code Custom} for the rest of the JVM: other tests, like the data rails of
 * ships.txt, read {@code ShipSites.budgetOf}, which uses that shared spec, and
 * would become order dependent. The extension is registered through the
 * ServiceLoader of JUnit Jupiter (autodetection is enabled in
 * junit-platform.properties).
 */
public class CustomSpecResetExtension implements BeforeEachCallback, AfterEachCallback {
  /** The custom spec as declared, copied when Jupiter loads this extension. */
  private static final ShipSpec DECLARED = new ShipSpec(
      Consts.ShipSpecs.get(org.gts.bst.ship.ShipType.Custom.CastToInt()).Serialize());

  @Override
  public void beforeEach(ExtensionContext context) {
    restore();
  }

  @Override
  public void afterEach(ExtensionContext context) {
    restore();
  }

  private static void restore() {
    // The hashtable constructor also puts the declared custom ship name back.
    Consts.SetCustomShipSpec(new ShipSpec(DECLARED.Serialize()));
  }
}
