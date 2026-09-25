/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import org.gts.bst.view.BankView;
import org.gts.bst.view.BankViewModel;
import org.gts.bst.view.DialogResult;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Strings;
import spacetrader.enums.AlertType;


/**
 * Fills the bank screen and mediates its actions. No front-end types involved: the view
 * only renders and asks for the amounts.
 */
public class BankPresenter {
  private final Game game;
  private final Commander cmdr;
  private final BankView view;
  private final int maxLoan;

  public BankPresenter(Game game, BankView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.view = view;
    maxLoan = cmdr.getPoliceRecordScore() >= Consts.PoliceRecordScoreClean
        ? Math.min(25000, Math.max(1000, cmdr.Worth() / 5000 * 500)) : 500;
  }

  public void update() {
    view.render(new BankViewModel(
        Functions.FormatMoney(cmdr.getDebt()),
        Functions.FormatMoney(maxLoan),
        cmdr.getDebt() > 0,
        Functions.FormatMoney(cmdr.getShip().BaseWorth(true)),
        Functions.FormatPercent(cmdr.NoClaim()),
        cmdr.NoClaim() == Consts.MaxNoClaim,
        Functions.StringVars(Strings.MoneyRateSuffix, Functions.FormatMoney(game.InsuranceCosts())),
        Functions.StringVars("^1 Insurance", cmdr.getInsurance() ? "Stop" : "Buy")));
  }

  /**
   * Returns true when the loan was taken.
   */
  public boolean getLoan() {
    if(cmdr.getDebt() >= maxLoan) {
      game.Dialogs().alert(AlertType.DebtTooLargeLoan);
      return false;
    }
    Integer amount = view.askLoanAmount(maxLoan - cmdr.getDebt());
    if(amount == null) {
      return false;
    }
    cmdr.setCash(cmdr.getCash() + amount);
    cmdr.setDebt(cmdr.getDebt() + amount);
    update();
    return true;
  }

  /**
   * Returns true when the debt was paid back.
   */
  public boolean payBack() {
    if(cmdr.getDebt() == 0) {
      game.Dialogs().alert(AlertType.DebtNone);
      return false;
    }
    Integer amount = view.askPayBackAmount();
    if(amount == null) {
      return false;
    }
    cmdr.setCash(cmdr.getCash() - amount);
    cmdr.setDebt(cmdr.getDebt() - amount);
    update();
    return true;
  }

  public void toggleInsurance() {
    if(cmdr.getInsurance()) {
      if(game.Dialogs().alert(AlertType.InsuranceStop) == DialogResult.Yes) {
        cmdr.setInsurance(false);
        cmdr.NoClaim(0);
      }
    } else if(!cmdr.getShip().getEscapePod()) {
      game.Dialogs().alert(AlertType.InsuranceNoEscapePod);
    } else {
      cmdr.setInsurance(true);
      cmdr.NoClaim(0);
    }
    update();
  }
}

