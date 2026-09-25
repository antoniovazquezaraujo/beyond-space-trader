package org.gts.bst.view;


/**
 * Everything the commander status screen displays, already formatted for the front-end.
 */
public record CommanderViewModel(
    String name,
    String difficulty,
    String time,
    String pilot,
    String fighter,
    String trader,
    String engineer,
    String cash,
    String debt,
    String netWorth,
    String kills,
    String record,
    String reputation,
    Bounty bounty) {

  public record Bounty(boolean visible, String label, String amount) {
  }
}
