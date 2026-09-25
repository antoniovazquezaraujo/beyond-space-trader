package org.gts.bst.view;


/**
 * One row of the cargo table, already formatted. {@code sellBold} and {@code buyBold}
 * tell the front-end whether to highlight those prices (profitable trade).
 */
public record CargoRowViewModel(
    String sellPrice,
    String sellQty,
    String sellButtonText,
    boolean sellVisible,
    String buyPrice,
    String buyQty,
    boolean buyVisible,
    String targetPrice,
    String targetDiff,
    String targetPct,
    boolean sellBold,
    boolean buyBold) {

  public static CargoRowViewModel empty() {
    return new CargoRowViewModel("", "", "", false, "", "", false, "", "", "", false, false);
  }
}
