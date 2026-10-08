/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterType;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacetrader.enums.StarSystemId;
import spacetrader.util.Hashtable;


/**
 * The encounter generator behind a hand-written {@link EncounterContext}, with
 * no game at all: the space monster path pins that the generator only asks the
 * narrow context.
 */
class EncounterGeneratorTest {
  @BeforeEach
  void withoutAGame() {
    Game.CurrentGame(null);
  }

  @Test
  void spaceMonsterAtAcamarAsksTheContextAndMarksTheEncounter() {
    FakeContext context = new FakeContext();
    context.warpSystem = system(StarSystemId.Acamar);
    context.clicks = 1;
    context.questStatusSpaceMonster = SpecialEvent.StatusSpaceMonsterAtAcamar;
    context.spaceMonster = new Ship(ShipType.SpaceMonster);

    boolean showEncounter = new EncounterGenerator(context).determine();

    assertTrue(showEncounter, "the monster of the quest must be shown");
    assertSame(context.spaceMonster, context.encounter.getOpponent(), "the opponent must be the quest monster");
    assertEquals(EncounterType.SpaceMonsterAttack, context.encounter.getEncounterType(),
        "the encounter must be an attack when the ship is not cloaked");
  }

  /** Builds a system without randomizing its trade items (which reads the game). */
  private static StarSystem system(StarSystemId id) {
    Hashtable hash = new Hashtable();
    hash.add("_id", id.CastToInt());
    return new StarSystem(hash);
  }

  /** Hand-written context: only the space monster path is wired. */
  private static final class FakeContext implements EncounterContext {
    // The path only marks the encounter; the methods it calls on it never ask
    // the encounter for its game.
    private final Encounter encounter = new Encounter(null);
    private final Commander commander = new Commander(
        new CrewMember(CrewMemberId.Commander, 4, 4, 4, 4, StarSystemId.NA));
    private StarSystem warpSystem;
    private Ship spaceMonster;
    private int clicks;
    private int questStatusSpaceMonster;

    private FakeContext() {
      commander.setShip(new VisibleShip());
    }

    @Override
    public Encounter encounter() {
      return encounter;
    }

    @Override
    public Commander Commander() {
      return commander;
    }

    @Override
    public StarSystem WarpSystem() {
      return warpSystem;
    }

    @Override
    public int getClicks() {
      return clicks;
    }

    @Override
    public Ship SpaceMonster() {
      return spaceMonster;
    }

    @Override
    public int getQuestStatusSpaceMonster() {
      return questStatusSpaceMonster;
    }

    @Override
    public boolean getArrivedViaWormhole() {
      throw notOnThePath("getArrivedViaWormhole()");
    }

    @Override
    public GameOptions Options() {
      throw notOnThePath("Options()");
    }

    @Override
    public Difficulty Difficulty() {
      throw notOnThePath("Difficulty()");
    }

    @Override
    public ArrayList<VeryRareEncounter> VeryRareEncounters() {
      throw notOnThePath("VeryRareEncounters()");
    }

    @Override
    public int getChanceOfVeryRareEncounter() {
      throw notOnThePath("getChanceOfVeryRareEncounter()");
    }

    @Override
    public int getChanceOfTradeInOrbit() {
      throw notOnThePath("getChanceOfTradeInOrbit()");
    }

    @Override
    public Ship Scorpion() {
      throw notOnThePath("Scorpion()");
    }

    @Override
    public Ship Scarab() {
      throw notOnThePath("Scarab()");
    }

    @Override
    public Ship Dragonfly() {
      throw notOnThePath("Dragonfly()");
    }

    @Override
    public int getQuestStatusScarab() {
      throw notOnThePath("getQuestStatusScarab()");
    }

    @Override
    public int getQuestStatusPrincess() {
      throw notOnThePath("getQuestStatusPrincess()");
    }

    @Override
    public int getQuestStatusGemulon() {
      throw notOnThePath("getQuestStatusGemulon()");
    }

    @Override
    public int getQuestStatusDragonfly() {
      throw notOnThePath("getQuestStatusDragonfly()");
    }

    private static UnsupportedOperationException notOnThePath(String method) {
      return new UnsupportedOperationException(method + " is not part of the space monster path");
    }
  }

  /** Ship double: the real Cloaked() reads the game this test does not have. */
  private static final class VisibleShip extends Ship {
    private VisibleShip() {
      super(ShipType.Gnat);
    }

    @Override
    public boolean Cloaked() {
      return false;
    }
  }
}
