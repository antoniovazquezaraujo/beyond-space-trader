/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import java.util.ArrayList;
import java.util.List;
import spacetrader.Commander;
import spacetrader.Functions;
import spacetrader.PoliceRecord;
import spacetrader.Ship;
import spacetrader.Strings;


/**
 * The header of the game: name, day, cash, debt, fuel, hull, shields, cargo and
 * police record of the commander, with a separating line under them. The main
 * screen draws it at the top, and the encounter scene keeps it visible so the
 * values of the fight (hull, shields, ...) are always at hand.
 */
public final class HeaderBar {
  private HeaderBar() {
  }

  /**
   * The rows the header takes: one when every field fits in a line, two when the
   * commander data and the ship data need one line each. With no commander (no
   * game) the empty line is always one row.
   */
  public static int height(int width, Commander cmdr) {
    if(cmdr == null) {
      return 1;
    }
    return oneLine(fields(cmdr), width) ? 1 : 2;
  }

  /**
   * Draws the header and returns its height: the fields on the first rows and
   * the separating line on the row after them. With no commander it draws the
   * "no game" line instead.
   */
  public static int draw(TextGUIGraphics graphics, int width, Commander cmdr) {
    if(cmdr == null) {
      UiPalette.line(graphics, 1, 0, Strings.MainNoGame, width - 2);
      UiPalette.reset(graphics);
      graphics.drawLine(0, 1, width - 1, 1, '─');
      return 1;
    }
    Fields fields = fields(cmdr);
    boolean oneLine = oneLine(fields, width);
    if(oneLine) {
      List<HeaderField> all = new ArrayList<>(fields.commander());
      all.addAll(fields.ship());
      drawFields(graphics, 1, 0, all, width - 2);
    } else {
      drawFields(graphics, 1, 0, fields.commander(), width - 2);
      drawFields(graphics, 1, 1, fields.ship(), width - 2);
    }
    UiPalette.reset(graphics);
    int headerHeight = oneLine ? 1 : 2;
    graphics.drawLine(0, headerHeight, width - 1, headerHeight, '─');
    return headerHeight;
  }

  /** The fields of the header, built from the live values of the commander. */
  private static Fields fields(Commander cmdr) {
    Ship ship = cmdr.getShip();
    List<HeaderField> commanderFields = List.of(
        new HeaderField(cmdr.Name(), UiPalette.TEXT),
        new HeaderField(Functions.StringVars(Strings.MainDay, "" + cmdr.getDays()), UiPalette.ACCENT),
        new HeaderField(Functions.FormatMoney(cmdr.getCash()), UiPalette.MONEY),
        new HeaderField(Functions.StringVars(Strings.MainDebt, Functions.FormatMoney(cmdr.getDebt())),
            cmdr.getDebt() > 0 ? UiPalette.BAD : UiPalette.TEXT));
    List<HeaderField> shipFields = List.of(
        new HeaderField(Functions.StringVars(Strings.MainFuel, "" + ship.getFuel(), "" + ship.FuelTanks()),
            UiPalette.statusColor(ship.getFuel(), ship.FuelTanks())),
        new HeaderField(Functions.StringVars(Strings.MainHull, "" + ship.getHull(), "" + ship.HullStrength()),
            UiPalette.statusColor(ship.getHull(), ship.HullStrength())),
        new HeaderField(Functions.StringVars(Strings.MainShields, "" + ship.ShieldCharge(),
            "" + ship.ShieldStrength()), UiPalette.statusColor(ship.ShieldCharge(), ship.ShieldStrength())),
        new HeaderField(Functions.StringVars(Strings.MainCargo, "" + ship.FilledCargoBays(),
            "" + ship.CargoBays()), UiPalette.TEXT),
        new HeaderField(Functions.StringVars(Strings.MainPolice,
            PoliceRecord.GetPoliceRecordFromScore(cmdr.getPoliceRecordScore()).Name()),
            policeColor(cmdr.getPoliceRecordScore())));
    return new Fields(commanderFields, shipFields);
  }

  /** True when the commander and the ship fields fit together on one line. */
  private static boolean oneLine(Fields fields, int width) {
    return fieldsWidth(fields.commander()) + 3 + fieldsWidth(fields.ship()) <= width - 2;
  }

  private static int fieldsWidth(List<HeaderField> fields) {
    int width = 0;
    for(HeaderField field : fields) {
      width += field.text().length() + 3;
    }
    return Math.max(0, width - 3);
  }

  private static void drawFields(TextGUIGraphics graphics, int x, int row, List<HeaderField> fields, int maxX) {
    boolean first = true;
    for(HeaderField field : fields) {
      if(!first) {
        x = UiPalette.draw(graphics, x, row, " · ", UiPalette.TEXT, maxX);
      }
      x = UiPalette.draw(graphics, x, row, field.text(), field.color(), maxX);
      first = false;
    }
  }

  private static TextColor policeColor(int score) {
    switch(PoliceRecord.GetPoliceRecordFromScore(score).Type()) {
      case Clean:
      case Lawful:
      case Trusted:
      case Liked:
      case Hero:
        return UiPalette.GOOD;
      case Crook:
      case Dubious:
        return UiPalette.WARN;
      default:
        return UiPalette.BAD;
    }
  }

  /** The fields of the header: the commander data and the ship data. */
  private record Fields(List<HeaderField> commander, List<HeaderField> ship) {
  }

  /** One field of the header: its text and its colour. */
  private record HeaderField(String text, TextColor color) {
  }
}
