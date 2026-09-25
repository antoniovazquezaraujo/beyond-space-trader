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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.SpecialResource;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.SystemPressure;
import spacetrader.enums.TechLevel;


class NewGameSetupTest {
  @Test
  void createsTheCommanderInAReachableSystem() {
    newGame();
    CrewMember[] mercenaries = new CrewMember[Strings.CrewMemberNames.size()];
    StarSystem[] universe = systems(20, TechLevel.t3);

    Commander cmdr = NewGameSetup.InitializeCommander("Antonio",
        new CrewMember(CrewMemberId.Commander, 4, 4, 4, 4, StarSystemId.NA), universe, mercenaries);

    assertNotNull(cmdr.CurrentSystem());
    assertTrue(cmdr.CurrentSystem().Visited());
    assertSame(cmdr, mercenaries[CrewMemberId.Commander.CastToInt()]);
    assertEquals("Antonio", Strings.CrewMemberNames.get(CrewMemberId.Commander.CastToInt()));
  }

  @Test
  void createsTheMercenaries() {
    newGame();
    CrewMember[] mercenaries = new CrewMember[Strings.CrewMemberNames.size()];

    NewGameSetup.GenerateCrewMemberList(mercenaries, Strings.SystemNames.size(), Difficulty.Normal);

    for(int i = 1; i < mercenaries.length; i++) {
      assertNotNull(mercenaries[i], "mercenary " + i);
      for(int skill : mercenaries[i].Skills()) {
        assertTrue(skill >= 1 && skill <= 10, "skill out of range: " + skill);
      }
    }
    assertSame(CrewMemberId.Princess, mercenaries[CrewMemberId.Princess.CastToInt()].Id());
  }

  @Test
  void equipsTheSpecialShips() {
    newGame();
    CrewMember[] mercenaries = new CrewMember[Strings.CrewMemberNames.size()];
    NewGameSetup.GenerateCrewMemberList(mercenaries, Strings.SystemNames.size(), Difficulty.Normal);
    Ship dragonfly = new Ship(ShipType.Dragonfly);
    Ship scarab = new Ship(ShipType.Scarab);
    Ship scorpion = new Ship(ShipType.Scorpion);
    Ship spaceMonster = new Ship(ShipType.SpaceMonster);

    NewGameSetup.CreateShips(dragonfly, scarab, scorpion, spaceMonster, mercenaries);

    assertSame(mercenaries[CrewMemberId.Dragonfly.CastToInt()], dragonfly.Crew()[0]);
    assertNotNull(dragonfly.Weapons()[0]);
    assertNotNull(dragonfly.Shields()[0]);
    assertNotNull(dragonfly.Gadgets()[0]);
    assertNotNull(scarab.Weapons()[0]);
    assertNotNull(scorpion.Weapons()[0]);
    assertNotNull(scorpion.Shields()[0]);
    assertNotNull(spaceMonster.Weapons()[2]);
  }

  private static StarSystem[] systems(int count, TechLevel tech) {
    StarSystem[] universe = new StarSystem[count];
    for(int i = 0; i < count; i++) {
      universe[i] = new StarSystem(StarSystemId.FromInt(i), i, i, ShipSize.Small, tech,
          PoliticalSystemType.Anarchy, SystemPressure.None, SpecialResource.Nothing);
    }
    return universe;
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }
}
