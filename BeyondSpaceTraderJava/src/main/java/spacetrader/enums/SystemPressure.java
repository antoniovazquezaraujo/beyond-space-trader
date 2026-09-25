package spacetrader.enums;
import spacetrader.Strings;


public enum SystemPressure implements SpaceTraderEnum {
  None,//= 0,
  War,//= 1,
  Plague,//= 2,
  Drought,//= 3,
  Boredom,//= 4,
  Cold,//= 5,
  CropFailure,//= 6,
  Employment;//= 7
  public final String name;

  private SystemPressure() {
    name = Strings.text("SystemPressure." + name());
  }

  public static SystemPressure FromInt(int i) {
    return values()[i];
  }

  @Override
  public int CastToInt() {
    return ordinal();
  }
}
