package spacetrader;
import spacetrader.enums.GameEndType;


public class GameEndException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public GameEndException(Game game, GameEndType endType) {
    game.setEndStatus(endType);
  }
}
