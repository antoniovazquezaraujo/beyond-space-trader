/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.view.ShipView;
import org.gts.bst.view.ShipViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.SpecialEvent;
import spacetrader.Strings;
import spacetrader.stub.ArrayList;
import spacetrader.util.Util;


/**
 * Fills the current-ship screen from the model. No front-end types involved.
 */
public class ShipPresenter {
  private final Game game;
  private final ShipView view;

  public ShipPresenter(Game game, ShipView view) {
    this.game = game;
    this.view = view;
  }

  public void update() {
    Ship ship = game.Commander().getShip();
    EquipmentText equipment = equipmentText(ship);
    view.render(new ShipViewModel(ship.Name(), equipment.labels(), equipment.values(), specialCargo(ship)));
  }

  private EquipmentText equipmentText(Ship ship) {
    StringBuilder labels = new StringBuilder();
    StringBuilder values = new StringBuilder();
    boolean equipPrinted = false;
    if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDone) {
      labels.append(Strings.ShipHullLabel).append(Strings.newline).append(Strings.newline);
      values.append(Strings.ShipHullHardened).append(Strings.newline).append(Strings.newline);
    }
    for(int i = 0; i < Consts.WeapObjs.length; i++) {
      int count = countWeapons(ship, i);
      if(count > 0) {
        labels.append(equipPrinted ? Strings.newline : Strings.ShipEquipmentLabel + Strings.newline);
        values.append(Functions.Multiples(count, Consts.WeapObjs[i].Name())).append(Strings.newline);
        equipPrinted = true;
      }
    }
    for(int i = 0; i < Consts.Shields.length; i++) {
      int count = countShields(ship, i);
      if(count > 0) {
        labels.append(equipPrinted ? Strings.newline : Strings.ShipEquipmentLabel + Strings.newline);
        values.append(Functions.Multiples(count, Consts.Shields[i].Name())).append(Strings.newline);
        equipPrinted = true;
      }
    }
    for(int i = 0; i < Consts.Gadgets.length; i++) {
      int count = countGadgets(ship, i);
      if(count > 0) {
        labels.append(equipPrinted ? Strings.newline : Strings.ShipEquipmentLabel + Strings.newline);
        if(i == GadgetType.ExtraCargoBays.asInteger() || i == GadgetType.HiddenCargoBays.asInteger()) {
          values.append(Functions.FormatNumber(count * 5)).append(Consts.Gadgets[i].Name().substring(1)).append(Strings.newline);
        } else {
          values.append(Functions.Multiples(count, Consts.Gadgets[i].Name())).append(Strings.newline);
        }
        equipPrinted = true;
      }
    }
    if(ship.getEscapePod()) {
      labels.append(equipPrinted ? Strings.newline : Strings.ShipEquipmentLabel + Strings.newline);
      values.append("1 ").append(Strings.ShipInfoEscapePod).append(Strings.newline);
      equipPrinted = true;
    }
    if(ship.FreeSlots() > 0) {
      labels.append(equipPrinted ? Strings.newline : "").append(Strings.ShipUnfilledLabel);
      values.append(equipPrinted ? Strings.newline : "");
      if(ship.FreeSlotsWeapon() > 0) {
        values.append(Functions.Multiples(ship.FreeSlotsWeapon(), Strings.ShipWeaponSlot)).append(Strings.newline);
      }
      if(ship.FreeSlotsShield() > 0) {
        values.append(Functions.Multiples(ship.FreeSlotsShield(), Strings.ShipShieldSlot)).append(Strings.newline);
      }
      if(ship.FreeSlotsGadget() > 0) {
        values.append(Functions.Multiples(ship.FreeSlotsGadget(), Strings.ShipGadgetSlot)).append(Strings.newline);
      }
    }
    return new EquipmentText(labels.toString(), values.toString());
  }

  private static int countWeapons(Ship ship, int index) {
    int count = 0;
    for(int j = 0; j < ship.Weapons().length; j++) {
      if(ship.Weapons()[j] != null && ship.Weapons()[j].Type() == Consts.WeapObjs[index].Type()) {
        count++;
      }
    }
    return count;
  }

  private static int countShields(Ship ship, int index) {
    int count = 0;
    for(int j = 0; j < ship.Shields().length; j++) {
      if(ship.Shields()[j] != null && ship.Shields()[j].Type() == Consts.Shields[index].Type()) {
        count++;
      }
    }
    return count;
  }

  private static int countGadgets(Ship ship, int index) {
    int count = 0;
    for(int j = 0; j < ship.Gadgets().length; j++) {
      if(ship.Gadgets()[j] != null && ship.Gadgets()[j].Type() == Consts.Gadgets[index].Type()) {
        count++;
      }
    }
    return count;
  }

  private String specialCargo(Ship ship) {
    ArrayList<String> specialCargo = new ArrayList<>(12);
    if(ship.getTribbles() > 0) {
      if(ship.getTribbles() == Consts.MaxTribbles) {
        specialCargo.add(Strings.SpecialCargoTribblesInfest);
      } else {
        specialCargo.add(Functions.Multiples(ship.getTribbles(), Strings.SpecialCargoTribblesCute) + ".");
      }
    }
    if(game.getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
      specialCargo.add(Strings.SpecialCargoJapori);
    }
    if(ship.ArtifactOnBoard()) {
      specialCargo.add(Strings.SpecialCargoArtifact);
    }
    if(game.getQuestStatusJarek() == SpecialEvent.StatusJarekDone) {
      specialCargo.add(Strings.SpecialCargoJarek);
    }
    if(ship.ReactorOnBoard()) {
      specialCargo.add(Strings.SpecialCargoReactor);
      specialCargo.add(Functions.Multiples(10 - ((game.getQuestStatusReactor() - 1) / 2), Strings.ShipBayUnit) + Strings.SpecialCargoReactorBays);
    }
    if(ship.SculptureOnBoard()) {
      specialCargo.add(Strings.SpecialCargoSculpture);
    }
    if(game.getCanSuperWarp()) {
      specialCargo.add(Strings.SpecialCargoExperiment);
    }
    return specialCargo.size() == 0
        ? Strings.SpecialCargoNone
        : Util.StringsJoin(Strings.newline + Strings.newline, Functions.ArrayListtoStringArray(specialCargo));
  }

  private record EquipmentText(String labels, String values) {
  }
}

