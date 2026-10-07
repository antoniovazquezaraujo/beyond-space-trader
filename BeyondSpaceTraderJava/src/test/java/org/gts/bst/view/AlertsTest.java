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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.gts.bst.ports.DialogResult;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class AlertsTest {
  private static final Set<AlertType> IMAGE_ALERTS = Set.of(
      AlertType.AppStart, AlertType.GameEndBoughtMoon, AlertType.GameEndBoughtMoonGirl,
      AlertType.GameEndKilled, AlertType.GameEndRetired);

  @Test
  void definesTheStandardAlerts() {
    AlertDefinition definition = Alerts.get(AlertType.CargoIF);

    assertEquals("Not Enough Money", definition.title());
    assertEquals("You don't have enough money to spend on any of these goods.", definition.message());
    assertEquals("Ok", definition.button1());
    assertEquals(DialogResult.OK, definition.result1());
    assertNull(definition.button2());
    assertEquals(DialogResult.None, definition.result2());
  }

  @Test
  void definesTheTwoButtonAlerts() {
    AlertDefinition definition = Alerts.get(AlertType.WildWontStayAboardLaser);

    assertEquals("Say Goodbye to Wild", definition.button1());
    assertEquals(DialogResult.OK, definition.result1());
    assertEquals("Cancel", definition.button2());
    assertEquals(DialogResult.Cancel, definition.result2());
    assertTrue(definition.message().endsWith("\n"), definition.message());
  }

  @Test
  void theImageAlertsOnlyHaveATitle() {
    assertNull(Alerts.get(AlertType.AppStart));
    assertEquals("Space Trader for Windows", Alerts.title(AlertType.AppStart));
  }

  @Test
  void everyAlertTypeIsCovered() {
    for(AlertType type : AlertType.values()) {
      assertTrue(Alerts.get(type) != null || IMAGE_ALERTS.contains(type), "no definition for " + type);
    }
  }
}
