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

import java.util.List;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;


class LanternaDialogServiceTest {
  @Test
  void showsTheAlertWithTheResolvedPlaceholders() {
    FakeHost host = new FakeHost();

    DialogResult result = new LanternaDialogService(host).alert(AlertType.CargoNoneToSell, "Jettison");

    assertEquals(DialogResult.OK, result);
    assertEquals("None To Jettison", host.title);
    assertEquals("You have none of these goods in your cargo bays.", host.message);
    assertEquals(1, host.buttons.size());
    assertEquals("Ok", host.buttons.get(0).text());
    assertEquals(DialogResult.OK, host.buttons.get(0).result());
  }

  @Test
  void showsBothButtonsWhenTheAlertHasThem() {
    FakeHost host = new FakeHost();

    new LanternaDialogService(host).alert(AlertType.WildWontStayAboardLaser, "Acamar");

    assertEquals(2, host.buttons.size());
    assertEquals("Say Goodbye to Wild", host.buttons.get(0).text());
    assertEquals("Cancel", host.buttons.get(1).text());
    assertEquals(DialogResult.Cancel, host.buttons.get(1).result());
  }

  @Test
  void showsTheImageAlertsWithAnOkButton() {
    FakeHost host = new FakeHost();

    assertEquals(DialogResult.OK, new LanternaDialogService(host).alert(AlertType.GameEndKilled));

    assertEquals("You Are Dead", host.title);
    assertEquals(1, host.buttons.size());
    assertEquals("Ok", host.buttons.get(0).text());
  }

  private static class FakeHost implements AlertDialogHost {
    private String title;
    private String message;
    private List<AlertButton> buttons;

    @Override
    public DialogResult show(String title, String message, List<AlertButton> buttons) {
      this.title = title;
      this.message = message;
      this.buttons = buttons;
      return buttons.get(0).result();
    }
  }
}
