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
import org.gts.bst.ship.equip.Equipment;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.ship.equip.Gadget;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.Shield;
import org.gts.bst.ship.equip.Weapon;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.view.EquipmentInfoViewModel;
import org.gts.bst.view.EquipmentView;
import org.gts.bst.view.EquipmentViewModel;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.Strings;
import spacetrader.enums.AlertType;


/**
 * Fills the equipment screen and mediates the buy/sell actions. No front-end types
 * involved: the view forwards the selected list and index.
 */
public class EquipmentPresenter {
  private static final List<Equipment> FOR_SALE = Consts.EquipmentForSale;

  private final Game game;
  private final Commander cmdr;
  private final Ship ship;
  private final EquipmentView view;
  private Equipment selected = null;
  private boolean sellSideSelected = false;
  private int selectedSlot = -1;

  public EquipmentPresenter(Game game, EquipmentView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.ship = cmdr.getShip();
    this.view = view;
  }

  public void update() {
    sellSideSelected = false;
    selected = null;
    selectedSlot = -1;
    view.render(model());
    view.renderInfo(info());
  }

  public void select(EquipmentType type, boolean sellSide, int index) {
    sellSideSelected = sellSide;
    selected = null;
    selectedSlot = -1;
    if(index >= 0) {
      if(sellSide) {
        Equipment[] equipment = ship.EquipmentByType(type);
        if(index < equipment.length) {
          selected = equipment[index];
          selectedSlot = index;
        }
      } else {
        List<Equipment> forSale = forSale(type);
        if(index < forSale.size()) {
          selected = forSale.get(index);
        }
      }
    }
    view.renderInfo(info());
  }

  /**
   * Returns true when the item was bought.
   */
  public boolean buy() {
    if(selected == null || sellSideSelected) {
      return false;
    }
    EquipmentType baseType = selected.EquipmentType();
    if(baseType == EquipmentType.Gadget && ship.HasGadget(((Gadget)selected).Type())
        && ((Gadget)selected).Type() != GadgetType.ExtraCargoBays) {
      game.Dialogs().alert(AlertType.EquipmentAlreadyOwn);
      return false;
    }
    if(cmdr.getDebt() > 0) {
      game.Dialogs().alert(AlertType.DebtNoBuy);
      return false;
    }
    if(selected.Price() > cmdr.CashToSpend()) {
      game.Dialogs().alert(AlertType.EquipmentIF);
      return false;
    }
    if((baseType == EquipmentType.Weapon && ship.FreeSlotsWeapon() == 0)
        || (baseType == EquipmentType.Shield && ship.FreeSlotsShield() == 0)
        || (baseType == EquipmentType.Gadget && ship.FreeSlotsGadget() == 0)) {
      game.Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
      return false;
    }
    if(game.Dialogs().alert(AlertType.EquipmentBuy, selected.Name(),
        Functions.FormatNumber(selected.Price())) != DialogResult.Yes) {
      return false;
    }
    ship.AddEquipment(selected);
    cmdr.setCash(cmdr.getCash() - selected.Price());
    update();
    return true;
  }

  /**
   * Returns true when the item was sold. The slot is the index of the selected item in
   * its equipment list.
   */
  public boolean sell() {
    if(selected == null || !sellSideSelected) {
      return false;
    }
    if(game.Dialogs().alert(AlertType.EquipmentSell) != DialogResult.Yes) {
      return false;
    }
    if(selected.EquipmentType() == EquipmentType.Gadget
        && (((Gadget)selected).Type() == GadgetType.ExtraCargoBays || ((Gadget)selected).Type() == GadgetType.HiddenCargoBays)
        && ship.FreeCargoBays() < 5) {
      game.Dialogs().alert(AlertType.EquipmentExtraBaysInUse);
      return false;
    }
    cmdr.setCash(cmdr.getCash() + selected.SellPrice());
    ship.RemoveEquipment(selected.EquipmentType(), selectedSlot);
    update();
    return true;
  }

  private EquipmentViewModel model() {
    return new EquipmentViewModel(
        names(forSale(EquipmentType.Weapon)),
        names(forSale(EquipmentType.Shield)),
        names(forSale(EquipmentType.Gadget)),
        equipmentNames(ship.EquipmentByType(EquipmentType.Weapon)),
        equipmentNames(ship.EquipmentByType(EquipmentType.Shield)),
        equipmentNames(ship.EquipmentByType(EquipmentType.Gadget)));
  }

  private EquipmentInfoViewModel info() {
    if(selected == null) {
      return new EquipmentInfoViewModel(false, "", "", "", "", "", "", "", null, false, false);
    }
    String power;
    String charge;
    switch(selected.EquipmentType()) {
      case Weapon:
        power = "" + ((Weapon)selected).Power();
        charge = Strings.NA;
        break;
      case Shield:
        power = "" + ((Shield)selected).Power();
        charge = sellSideSelected ? "" + ((Shield)selected).getCharge() : Strings.NA;
        break;
      default:
        power = Strings.NA;
        charge = Strings.NA;
        break;
    }
    return new EquipmentInfoViewModel(
        true,
        selected.Name(),
        Strings.EquipmentTypes.get(selected.EquipmentType().CastToInt()),
        Strings.EquipmentDescriptions.get(selected.EquipmentType().CastToInt()).get(selected.SubType().asInteger()),
        Functions.FormatMoney(selected.Price()),
        Functions.FormatMoney(selected.SellPrice()),
        power,
        charge,
        imageIndex(selected),
        !sellSideSelected && selected.Price() > 0,
        sellSideSelected);
  }

  private List<Equipment> forSale(EquipmentType type) {
    List<Equipment> items = new ArrayList<>();
    for(Equipment equipment : FOR_SALE) {
      if(equipment.Price() > 0 && equipment.EquipmentType() == type) {
        items.add(equipment);
      }
    }
    return items;
  }

  private static List<String> names(List<Equipment> equipment) {
    List<String> names = new ArrayList<>(equipment.size());
    for(Equipment item : equipment) {
      names.add(item.Name());
    }
    return names;
  }

  private static List<String> equipmentNames(Equipment[] equipment) {
    List<String> names = new ArrayList<>(equipment.length);
    for(Equipment item : equipment) {
      names.add(item == null ? Strings.EquipmentFreeSlot : item.Name());
    }
    return names;
  }

  private static int imageIndex(Equipment equipment) {
    int base;
    switch(equipment.EquipmentType()) {
      case Shield:
        base = Strings.WeaponNames.size();
        break;
      case Gadget:
        base = Strings.WeaponNames.size() + Strings.ShieldNames.size();
        break;
      default:
        base = 0;
        break;
    }
    return base + equipment.SubType().asInteger();
  }
}

