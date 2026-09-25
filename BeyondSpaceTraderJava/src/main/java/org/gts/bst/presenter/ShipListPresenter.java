package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.ShipInfoViewModel;
import org.gts.bst.view.ShipListView;
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
 */
public class ShipListPresenter {
  private final Game game;
  private final Commander cmdr;
  private final Ship ship;
  private final ShipListView view;
  private final int[] prices = new int[Consts.ShipSpecs.length];

  public ShipListPresenter(Game game, ShipListView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.ship = cmdr.getShip();
    this.view = view;
  }

  public void update() {
    List<ShipListViewModel.Row> rows = new ArrayList<>(Consts.ShipSpecs.length);
    for(int i = 0; i < Consts.ShipSpecs.length; i++) {
      ShipSpec spec = Consts.ShipSpecs[i];
      boolean buyVisible = false;
      String price;
      if(spec.MinimumTechLevel().ordinal() > cmdr.CurrentSystem().TechLevel().ordinal()) {
        price = "not sold";
      } else if(spec.Type() == ship.Type()) {
        price = Strings.ShipBuyGotOne;
      } else {
        buyVisible = true;
        prices[i] = spec.getPrice() - ship.Worth(false);
        price = Functions.FormatMoney(prices[i]);
      }
      rows.add(new ShipListViewModel.Row(spec.Name(), price, buyVisible));
    }
    view.render(new ShipListViewModel(rows));
  }

  public void selectCurrentShip() {
    select(ship.Type().CastToInt());
  }

  public void select(int id) {
    ShipSpec spec = Consts.ShipSpecs[id];
    view.renderInfo(new ShipInfoViewModel(
        spec.Name(),
        Strings.Sizes[spec.getSize().CastToInt()],
        Functions.FormatNumber(spec.CargoBays()),
        Functions.Multiples(spec.FuelTanks(), Strings.DistanceUnit),
        Functions.FormatNumber(spec.HullStrength()),
        Functions.FormatNumber(spec.getWeaponSlots()),
        Functions.FormatNumber(spec.getShieldSlots()),
        Functions.FormatNumber(spec.getGadgetSlots()),
        Functions.FormatNumber(spec.getCrewQuarters()),
        spec.ImageIndex()));
  }

  public void notifyTribblesTradeInIfNeeded() {
    if(ship.getTribbles() > 0 && !game.getTribbleMessage()) {
      game.Dialogs().alert(AlertType.TribblesTradeIn);
      game.setTribbleMessage(true);
    }
  }

  /**
   * Returns true when the ship was bought.
   */
  public boolean buy(int id) {
    select(id);
    if(cmdr.TradeShip(Consts.ShipSpecs[id], prices[id])) {
      if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDone) {
        game.setQuestStatusScarab(SpecialEvent.StatusScarabNotStarted);
      }
      update();
      return true;
    }
    return false;
  }
}
