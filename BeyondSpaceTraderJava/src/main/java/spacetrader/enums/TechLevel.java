package spacetrader.enums;
import spacetrader.Strings;


public enum TechLevel {
  pt(-1),
  t0(0), // = 0, pre-agricultural
  t1(1), // = 1, agricultural
  t2(2), // = 2, medieval
  t3(3), // = 3, renaissance
  t4(4), // = 4, early industrial
  t5(5), // = 5, industrial
  t6(6), // = 6, post-industrial
  t7(7), // = 7, high-tech
  t8(8),// = 8 unavailable
  xt(9);
  public final String abbr;
  public final String name;
  public final int id;
  public final int tl;

  private TechLevel(int i) {
    tl = i;
    abbr = Strings.text("TechLevel." + name() + ".abbr");
    name = Strings.text("TechLevel." + name() + ".name");
    id = ordinal();
  }

  public static TechLevel FromInt(int i) {
    return values()[i];
  }
}
