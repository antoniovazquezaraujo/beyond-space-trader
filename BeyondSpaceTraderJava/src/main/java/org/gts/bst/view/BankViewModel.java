package org.gts.bst.view;


/**
 * Everything the bank screen displays, already formatted for the front-end.
 */
public record BankViewModel(
    String currentDebt,
    String maxLoan,
    boolean payBackVisible,
    String shipValue,
    String noClaim,
    boolean maxNoClaimVisible,
    String insuranceCost,
    String insuranceButtonText) {
}
