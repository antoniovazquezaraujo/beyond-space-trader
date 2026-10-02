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

import java.util.List;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.Gadget;
import org.gts.bst.ship.equip.GadgetType;
import org.junit.jupiter.api.Test;
import spacetrader.enums.SkillType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;


class ShipStealableCargoTest {
  @Test
  void hiddenBaysBiggerThanTheLoadHideEverythingInsteadOfBlowingUp() {
    Ship ship = shipWithHiddenBay();
    // 2 units of cargo, but the hidden bay hides 5.
    ship.Cargo()[0] = 2;

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(), stealable, "the pirate finds nothing to steal");
  }

  @Test
  void exactlyAFullLoadOfHiddenBaysLeavesNothingInTheOpen() {
    Ship ship = shipWithHiddenBay();
    ship.Cargo()[0] = ship.HiddenCargoBays();

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(), stealable, "all the cargo fits in the hidden bay");
  }

  @Test
  void hiddenBaysHideTheMostValuableUnits() {
    Ship ship = shipWithHiddenBay();
    // 4 valuable units and 3 cheap ones; the 5 hidden units are the valuable ones
    // and just one cheap one, so two cheap units stay in the open.
    ship.Cargo()[3] = 4;
    ship.Cargo()[0] = 3;

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(0, 0), stealable, "only the cheapest units stay in the open");
  }

  @Test
  void withoutHiddenBaysEverythingIsStealable() {
    Ship ship = new QuestlessShip();
    ship.Cargo()[0] = 2;
    ship.Cargo()[3] = 1;

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(3, 0, 0), stealable, "most valuable first, nothing hidden");
  }

  @Test
  void emptyHoldsWithHiddenBaysHaveNothingToSteal() {
    Ship ship = shipWithHiddenBay();

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(), stealable, "no cargo, no loot (and no blow up)");
  }

  @Test
  void aHiddenBayHidesOneUnitLessWithThePrincessOnBoard() {
    Ship ship = shipWithHiddenBayAndPrincess();
    // The princess occupies one of the five hidden units, so four are hidden.
    ship.Cargo()[0] = 5;

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(0), stealable, "one unit stays in the open");
  }

  @Test
  void thePrincessWithoutHiddenBaysLeavesAllTheCargoInTheOpen() {
    Ship ship = new QuestlessShip(ShipType.Beetle);
    ship.Crew()[1] = princess();
    ship.Cargo()[0] = 2;

    List<Integer> stealable = ship.StealableCargo();

    assertEquals(List.of(0, 0), stealable, "a negative hidden count hides nothing");
  }

  private static Ship shipWithHiddenBay() {
    Ship ship = new QuestlessShip();
    // A hidden cargo bay hides 5 units.
    ship.AddEquipment(new Gadget(GadgetType.HiddenCargoBays, SkillType.NA, 60000, TechLevel.t8, 0));
    return ship;
  }

  private static Ship shipWithHiddenBayAndPrincess() {
    Ship ship = new QuestlessShip(ShipType.Beetle); // quarters for the princess
    ship.AddEquipment(new Gadget(GadgetType.HiddenCargoBays, SkillType.NA, 60000, TechLevel.t8, 0));
    ship.Crew()[1] = princess();
    return ship;
  }

  private static CrewMember princess() {
    return new CrewMember(CrewMemberId.Princess, 4, 3, 8, 9, StarSystemId.NA);
  }

  // StealableCargo is pure ship logic, but SculptureOnBoard asks the current
  // game; silencing that environment query keeps the scenario free of a Game.
  private static class QuestlessShip extends Ship {
    private QuestlessShip() {
      this(ShipType.Gnat);
    }

    private QuestlessShip(ShipType type) {
      super(type);
    }

    @Override
    public boolean SculptureOnBoard() {
      return false;
    }
  }
}
