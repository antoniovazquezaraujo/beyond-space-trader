package spacetrader;
import spacetrader.enums.ReputationType;


public class Reputation {
  private ReputationType _type;
  private int _minScore;

  public Reputation(ReputationType type, int minScore) {
    _type = type;
    _minScore = minScore;
  }

  public static Reputation GetReputationFromScore(int ReputationScore) {
    int i;
    for(i = 0; i < Consts.Reputations.size() && ReputationScore >= Consts.Reputations.get(i).MinScore(); i++) {
    }
    return Consts.Reputations.get(Math.max(0, i - 1));
  }

  public int MinScore() {
    return _minScore;
  }

  public String Name() {
    return Strings.ReputationNames.get(_type.CastToInt());
  }

  public ReputationType Type() {
    return _type;
  }
}
