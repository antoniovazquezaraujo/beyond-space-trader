/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.BankView;
import org.gts.bst.view.BankViewModel;
import org.gts.bst.ports.DialogResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Consts;
import spacetrader.Game;
import spacetrader.TestDialogService;
import spacetrader.enums.AlertType;


class BankPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheBankState() {
    FakeView view = new FakeView();

    new BankPresenter(newGame(new TestDialogService()), view).update();

    assertEquals("0 cr.", view.model.currentDebt());
    assertEquals("1,000 cr.", view.model.maxLoan());
    assertFalse(view.model.payBackVisible());
    assertEquals("9,000 cr.", view.model.shipValue());
    assertEquals("0%", view.model.noClaim());
    assertFalse(view.model.maxNoClaimVisible());
    assertEquals("0 cr. daily", view.model.insuranceCost());
    assertEquals("Buy Insurance", view.model.insuranceButtonText());
  }

  @Test
  void takingALoanAddsCashAndDebt() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    FakeView view = new FakeView();
    view.loanAnswer = 500;
    BankPresenter presenter = new BankPresenter(game, view);

    assertTrue(presenter.getLoan());
    assertEquals(1500, game.Commander().getCash());
    assertEquals(500, game.Commander().getDebt());
    assertEquals("500 cr.", view.model.currentDebt());
    assertTrue(view.model.payBackVisible());
  }

  @Test
  void warnsWhenTheLoanLimitIsReached() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    game.Commander().setPoliceRecordScore(Consts.PoliceRecordScoreDubious);
    game.Commander().setDebt(500);
    FakeView view = new FakeView();

    assertFalse(new BankPresenter(game, view).getLoan());
    assertEquals(List.of(AlertType.DebtTooLargeLoan), dialogs.alerts());
  }

  @Test
  void warnsWhenThereIsNothingToPayBack() {
    TestDialogService dialogs = new TestDialogService();
    FakeView view = new FakeView();

    assertFalse(new BankPresenter(newGame(dialogs), view).payBack());
    assertEquals(List.of(AlertType.DebtNone), dialogs.alerts());
  }

  @Test
  void insuranceRequiresAnEscapePod() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    FakeView view = new FakeView();

    new BankPresenter(game, view).toggleInsurance();

    assertEquals(List.of(AlertType.InsuranceNoEscapePod), dialogs.alerts());
    assertFalse(game.Commander().getInsurance());
  }

  @Test
  void buysAndStopsInsurance() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    game.Commander().getShip().setEscapePod(true);
    FakeView view = new FakeView();
    BankPresenter presenter = new BankPresenter(game, view);

    presenter.toggleInsurance();
    assertTrue(game.Commander().getInsurance());
    assertEquals("Stop Insurance", view.model.insuranceButtonText());

    dialogs.setResult(DialogResult.Yes);
    presenter.toggleInsurance();
    assertFalse(game.Commander().getInsurance());
    assertEquals("Buy Insurance", view.model.insuranceButtonText());
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static class FakeView implements BankView {
    private BankViewModel model;
    private Integer loanAnswer;
    private Integer payBackAnswer;

    @Override
    public void render(BankViewModel model) {
      this.model = model;
    }

    @Override
    public Integer askLoanAmount(int maxAmount) {
      return loanAnswer;
    }

    @Override
    public Integer askPayBackAmount() {
      return payBackAnswer;
    }
  }
}

