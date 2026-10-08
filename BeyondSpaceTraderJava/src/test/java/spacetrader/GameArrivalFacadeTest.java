/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.ports.GameWindow;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;
import spacetrader.enums.GameEndType;
import spacetrader.enums.StarSystemId;


/**
 * Pins the {@code Game.Arrival()} facade: the travel loop runs for real with a
 * window that never interrupts, so the state and the callbacks it assembles are
 * asserted on the game. The fabric-rip case covers the destination read after
 * {@code Travel()} switched it.
 */
class GameArrivalFacadeTest {
  private TestDialogService dialogs;
  private Game game;

  @BeforeEach
  void setUp() {
    dialogs = new TestDialogService();
    game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, new ContinueWindow(), dialogs);
    game.Options().setNewsAutoShow(false);
    game.Options().setNewsAutoPay(false);
    game.Options().setTrackAutoOff(true);
  }

  @Test
  void warpDelegatesTheArrivalOfTheDestination() {
    StarSystem target = reachableSystem();
    game.setPaidForNewspaper(true);
    game.setTrackedSystemId(target.Id());
    game.SelectedSystemId(target.Id());

    game.Warp(false);

    assertSame(target, game.Commander().CurrentSystem(), "the arrival must move the commander");
    assertTrue(target.Visited(), "the arrival must mark the destination visited");
    assertFalse(game.getPaidForNewspaper(), "the arrival must reset the newspaper payment");
    assertSame(StarSystemId.NA, game.getTrackedSystemId(), "the arrival must clear the tracked system");
    assertFalse(dialogs.alerts().contains(AlertType.ArrivalBuyNewspaper),
        "the newspaper must not be shown with the option off");
  }

  @Test
  void warpArrivesAtTheSystemTheFabricRipSwitchedTo() throws ReflectiveOperationException {
    Random previous = replaceRandom(new Random(20261007L));
    try {
      game.setQuestStatusExperiment(SpecialEvent.StatusExperimentPerformed);
      // The departure hides a day, and IncDays decays the probability, so this
      // is the initial value by the time Travel checks it.
      game.setFabricRipProbability(Consts.FabricRipInitialProbability + 1);
      StarSystem target = reachableSystem();
      game.SelectedSystemId(target.Id());

      game.Warp(false);

      assertTrue(dialogs.alerts().contains(AlertType.SpecialTimespaceFabricRip));
      StarSystem arrived = game.WarpSystem();
      assertNotSame(target, arrived, "the rip must switch the destination");
      assertSame(arrived, game.Commander().CurrentSystem(),
          "the arrival must use the destination read after travel");
      assertTrue(arrived.Visited());
    } finally {
      replaceRandom(previous);
    }
  }

  @Test
  void warpRecalculatesThePricesOfTheArrivalSystem() {
    StarSystem target = reachableSystem();
    game.SelectedSystemId(target.Id());

    game.Warp(false);

    assertArrayEquals(
        TradeCalculator.CalculateBuyPrices(target, game.PriceCargoSell(), game.Commander().getPoliceRecordScore(),
            game.Commander().getShip().Trader()),
        game.PriceCargoBuy());
  }

  @Test
  void warpWithAReactorMeltdownAndNoPodEndsTheGame() {
    game.setQuestStatusReactor(SpecialEvent.StatusReactorDate);
    game.Commander().getShip().setEscapePod(false);
    game.SelectedSystemId(reachableSystem().Id());

    assertThrows(GameEndException.class, () -> game.Warp(false));

    assertEquals(GameEndType.Killed, game.getEndStatus(),
        "the end of game must be pinned by the constructor of the exception");
    assertTrue(dialogs.alerts().contains(AlertType.ReactorMeltdown));
    assertTrue(dialogs.alerts().contains(AlertType.ReactorDestroyed));
  }

  @Test
  void warpWithAReactorMeltdownAndAPodEscapesBeforeTheOtherChecks() {
    Ship oldShip = game.Commander().getShip();
    oldShip.setEscapePod(true);
    oldShip.setTribbles(100);
    game.setTribbleMessage(true);
    game.setQuestStatusReactor(SpecialEvent.StatusReactorDate);
    game.SelectedSystemId(reachableSystem().Id());

    game.Warp(false);

    assertEquals(SpecialEvent.StatusReactorNotStarted, game.getQuestStatusReactor(),
        "the meltdown check must write the mission state through the callback");
    assertEquals(ShipType.Flea, game.Commander().getShip().Type(), "the pod must replace the ship");
    assertTrue(dialogs.alerts().contains(AlertType.EncounterEscapePodActivated));
    assertTrue(dialogs.alerts().contains(AlertType.TribblesKilled));
    assertFalse(dialogs.alerts().contains(AlertType.TribblesHalfDied),
        "the tribbles check must see the new ship after the escape");
    assertFalse(dialogs.alerts().contains(AlertType.TribblesAllDied));
    assertEquals(100, oldShip.getTribbles(), "the escape must run before the tribbles check");
    assertEquals(4, game.Commander().getDays(), "one warp day plus the three days of the flea");
    assertTrue(game.getTribbleMessage(), "the empty new ship must not clear the tribble message");
  }

  @Test
  void warpClearsTheTribbleMessageWhenTheShipHasTribbles() {
    game.Commander().getShip().setTribbles(100);
    game.setTribbleMessage(true);
    game.SelectedSystemId(reachableSystem().Id());

    game.Warp(false);

    assertFalse(game.getTribbleMessage(), "the tribbles check must clear the message through the callback");
  }

  @Test
  void warpShowsTheNewspaperThroughTheCallbackWhenTheOptionAsksForIt() {
    game.Options().setNewsAutoShow(true);
    game.SelectedSystemId(reachableSystem().Id());

    game.Warp(false);

    assertTrue(dialogs.alerts().contains(AlertType.ArrivalBuyNewspaper),
        "the arrival must call the newspaper callback");
  }

  private StarSystem reachableSystem() {
    for(StarSystem system : game.Universe()) {
      if(system.DestOk()) {
        return system;
      }
    }
    throw new AssertionError("the galaxy must have a reachable system");
  }

  /** Seeds the shared random source of {@link Functions}; returns the source to restore. */
  private static Random replaceRandom(Random random) throws ReflectiveOperationException {
    Field rand = Functions.class.getDeclaredField("rand");
    rand.setAccessible(true);
    Random previous = (Random)rand.get(null);
    rand.set(null, random);
    return previous;
  }

  /** Window double: encounters continue so the travel loop never stops the test. */
  private static final class ContinueWindow implements GameWindow {
    @Override
    public EncounterResult showEncounter() {
      return EncounterResult.Continue;
    }

    @Override
    public void showNewspaper() {
    }

    @Override
    public void UpdateStatusBar() {
    }

    @Override
    public void UpdateAll() {
    }
  }
}
