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
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListView;
import org.gts.bst.view.ShipCatalog;
import org.gts.bst.view.ShipListViewModel;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.ShipSpec;
import spacetrader.SpecialEvent;
import spacetrader.Strings;
import spacetrader.enums.AlertType;


/**
 * Fills the ship list and mediates the purchase. No front-end types involved.
 *
 * <p>The ship and the prices are read from the commander at the moment of use:
 * caching the ship would keep the old one paying after a trade, and buying the
 * same type again would mint money. The yard only sells the ships with an
 * occurrence (the ten normal ones); the special ships are not offered.
 */
public class ShipListPresenter {
  /** The spec indexes the yard sells: the special ships (occurrence zero) are not for sale. */
  private static final List<Integer> FOR_SALE_IDS = forSaleIds();

  private final Game game;
  private final Commander cmdr;
  private final ShipListView view;

  public ShipListPresenter(Game game, ShipListView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.view = view;
  }

  private static List<Integer> forSaleIds() {
    List<Integer> ids = new ArrayList<>();
    for(int i = 0; i < Consts.ShipSpecs.size(); i++) {
      if(Consts.ShipSpecs.get(i).Occurrence() > 0) {
        ids.add(i);
      }
    }
    return List.copyOf(ids);
  }

  public void update() {
    Ship ship = cmdr.getShip();
    List<ShipListViewModel.Row> rows = new ArrayList<>(FOR_SALE_IDS.size());
    for(int id : FOR_SALE_IDS) {
      ShipSpec spec = Consts.ShipSpecs.get(id);
      boolean buyVisible = false;
      String price;
      if(spec.MinimumTechLevel().ordinal() > cmdr.CurrentSystem().TechLevel().ordinal()) {
        price = Strings.NotSold;
      } else if(spec.Type() == ship.Type()) {
        price = Strings.ShipBuyGotOne;
      } else {
        buyVisible = true;
        price = Functions.FormatMoney(priceOf(spec, ship));
      }
      rows.add(new ShipListViewModel.Row(spec.Name(), price, buyVisible));
    }
    view.render(new ShipListViewModel(rows));
  }

  public void selectCurrentShip() {
    int row = FOR_SALE_IDS.indexOf(cmdr.getShip().Type().CastToInt());
    if(row >= 0) {
      select(row);
    }
  }

  public void select(int row) {
    ShipSpec spec = specOf(row);
    if(spec == null) {
      return;
    }
    view.renderInfo(new ShipInfoViewModel(
        spec.Name(),
        Strings.Sizes.get(spec.getSize().CastToInt()),
        Functions.FormatNumber(spec.CargoBays()),
        Functions.Multiples(spec.FuelTanks(), Strings.DistanceUnit),
        Functions.FormatNumber(spec.HullStrength()),
        Functions.FormatNumber(spec.getWeaponSlots()),
        Functions.FormatNumber(spec.getShieldSlots()),
        Functions.FormatNumber(spec.getGadgetSlots()),
        Functions.FormatNumber(spec.getCrewQuarters()),
        spec.ImageIndex(),
        spec.Type(),
        ShipCatalog.shared().picture(spec.Type(), ShipArtItems.fixed(spec.Type()), spec.CargoBays())));
  }

  public void notifyTribblesTradeInIfNeeded() {
    if(cmdr.getShip().getTribbles() > 0 && !game.getTribbleMessage()) {
      game.Dialogs().alert(AlertType.TribblesTradeIn);
      game.setTribbleMessage(true);
    }
  }

  /**
   * Returns true when the ship was bought. The price is computed again with the
   * ship in flight and only the ships the list offers can be bought, so asking
   * for the type already owned is rejected instead of paying for it again.
   */
  public boolean buy(int row) {
    ShipSpec spec = specOf(row);
    if(spec == null) {
      return false;
    }
    select(row);
    Ship ship = cmdr.getShip();
    if(spec.MinimumTechLevel().ordinal() > cmdr.CurrentSystem().TechLevel().ordinal()
        || spec.Type() == ship.Type()) {
      return false;
    }
    if(cmdr.TradeShip(spec, priceOf(spec, ship))) {
      if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDone) {
        game.setQuestStatusScarab(SpecialEvent.StatusScarabNotStarted);
      }
      update();
      return true;
    }
    return false;
  }

  /** The price of a spec against the ship traded in. */
  private static int priceOf(ShipSpec spec, Ship ship) {
    return spec.getPrice() - ship.Worth(false);
  }

  /** The spec of a listed row, or null when the row does not exist. */
  private static ShipSpec specOf(int row) {
    return row < 0 || row >= FOR_SALE_IDS.size() ? null : Consts.ShipSpecs.get(FOR_SALE_IDS.get(row));
  }
}
