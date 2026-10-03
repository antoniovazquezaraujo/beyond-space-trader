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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import spacetrader.Functions;
import spacetrader.Strings;
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
  void sendsTheOutcomesToTheLogWhenTheQuietSinkIsSet() {
    FakeHost host = new FakeHost();
    List<String> log = new ArrayList<>();
    List<AlertType> types = new ArrayList<>();
    LanternaDialogService service = new LanternaDialogService(host);
    service.quietTo((type, line) -> {
      types.add(type);
      log.add(line);
    });

    // An outcome (one button): one more line of the log, with no dialog.
    assertEquals(DialogResult.OK, service.alert(AlertType.EncounterEscaped));

    assertEquals(1, log.size());
    assertEquals("You have managed to escape your opponent.", log.get(0));
    assertEquals(List.of(AlertType.EncounterEscaped), types, "the sink knows which alert it is");
    assertNull(host.title, "an outcome opens no dialog");

    // A question (two buttons) keeps its dialog.
    service.alert(AlertType.WildWontStayAboardLaser, "Acamar");
    assertEquals(2, host.buttons.size());
  }

  @Test
  void resolvesThePlaceholdersOfAnOutcomeBeforeTheSink() {
    List<AlertType> types = new ArrayList<>();
    List<String> texts = new ArrayList<>();
    LanternaDialogService service = new LanternaDialogService(new FakeHost());
    service.quietTo((type, line) -> {
      types.add(type);
      texts.add(line);
    });

    service.alert(AlertType.EncounterPoliceFine, "1,500 cr.");

    assertEquals(List.of(AlertType.EncounterPoliceFine), types);
    assertEquals(List.of(Functions.StringVars(Alerts.get(AlertType.EncounterPoliceFine).message(), "1,500 cr.")),
        texts);
    assertTrue(texts.get(0).contains("1,500 cr."), "the fine is in the bubble: " + texts);
  }

  @Test
  void showsTheImageAlertsWithAnOkButton() {
    FakeHost host = new FakeHost();

    assertEquals(DialogResult.OK, new LanternaDialogService(host).alert(AlertType.GameEndKilled));

    assertEquals("You Are Dead", host.title);
    assertEquals(1, host.buttons.size());
    assertEquals("Ok", host.buttons.get(0).text());
  }

  @Test
  void showsAMessageWithTheOkButton() {
    FakeHost host = new FakeHost();

    DialogResult result = new LanternaDialogService(host).message("Moon For Sale", "Buy the moon?");

    assertEquals(DialogResult.OK, result);
    assertEquals("Moon For Sale", host.title);
    assertEquals("Buy the moon?", host.message);
    assertEquals(1, host.buttons.size());
    assertEquals(Strings.AlertButtonOk, host.buttons.get(0).text());
    assertEquals(DialogResult.OK, host.buttons.get(0).result());
  }

  @Test
  void showsTheYesAndNoButtonsOfAConfirmation() {
    FakeHost host = new FakeHost();

    DialogResult result = new LanternaDialogService(host).confirm("Moon For Sale", "Buy the moon?");

    assertEquals(DialogResult.Yes, result, "the fake presses the first button");
    assertEquals("Moon For Sale", host.title);
    assertEquals("Buy the moon?", host.message);
    assertEquals(2, host.buttons.size());
    assertEquals(Strings.AlertButtonYes, host.buttons.get(0).text());
    assertEquals(DialogResult.Yes, host.buttons.get(0).result());
    assertEquals(Strings.AlertButtonNo, host.buttons.get(1).text());
    assertEquals(DialogResult.No, host.buttons.get(1).result());
  }

  @Test
  void theOfferNeverGoesThroughTheQuietSink() {
    FakeHost host = new FakeHost();
    List<String> log = new ArrayList<>();
    LanternaDialogService service = new LanternaDialogService(host);
    service.quietTo((type, line) -> log.add(line));

    service.message("Title", "story");
    List<AlertButton> messageButtons = host.buttons;
    service.confirm("Title", "question");

    assertEquals(List.of(), log, "the offer must be shown even while the outcomes are logged");
    assertEquals(1, messageButtons.size(), "the message kept its dialog");
    assertEquals(2, host.buttons.size(), "the confirmation kept its dialog");
  }

  private static class FakeHost implements AlertDialogHost {
    private String title;
    private String message;
    private List<AlertButton> buttons;
    private int chosen;

    @Override
    public DialogResult show(String title, String message, List<AlertButton> buttons) {
      this.title = title;
      this.message = message;
      this.buttons = buttons;
      return buttons.get(chosen).result();
    }
  }
}
