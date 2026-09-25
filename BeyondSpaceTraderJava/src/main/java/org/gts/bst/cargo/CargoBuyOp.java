package org.gts.bst.cargo;
import spacetrader.Strings;


public enum CargoBuyOp {
  BuySystem(0),
  BuyTrader(1),
  InPlunder(2);
  public final String name;
  public final int id;

  private CargoBuyOp(int i) {
    name = Strings.text("CargoBuyOp." + name());
    id = i;
  }

  public static CargoBuyOp fromId(int i) {
    return values()[i];
  }
}
