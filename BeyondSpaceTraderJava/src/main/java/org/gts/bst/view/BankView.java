package org.gts.bst.view;


/**
 * Bank screen. The presenter renders the state and mediates the loan, pay-back and
 * insurance actions; the view only shows the model and asks for the amounts.
 */
public interface BankView {
  void render(BankViewModel model);

  /**
   * Opens the loan dialog and returns the requested amount, or {@code null} when cancelled.
   */
  Integer askLoanAmount(int maxAmount);

  /**
   * Opens the pay-back dialog and returns the amount to pay, or {@code null} when cancelled.
   */
  Integer askPayBackAmount();
}
