package org.gts.bst.events;
import spacetrader.Strings;
import spacetrader.enums.SpaceTraderEnum;


public enum VeryRareEncounter implements SpaceTraderEnum {
  MarieCeleste, // = 0,
  CaptainAhab, // = 1,
  CaptainConrad, // = 2,
  CaptainHuie, // = 3,
  BottleOld, // = 4,
  BottleGood; // = 5
  public final String name;
  public final int id;

  private VeryRareEncounter() {
    name = Strings.text("VeryRareEncounter." + name());
    id = ordinal();
  }

  @Override
  public int CastToInt() {
    return ordinal();
  }

  public static VeryRareEncounter FromInt(int i) {
    return values()[i];
  }
}
