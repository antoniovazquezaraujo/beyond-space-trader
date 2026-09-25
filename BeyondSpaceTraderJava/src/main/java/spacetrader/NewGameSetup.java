/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;


/**
 * New game setup: the commander, the mercenary list and the special ships. It is
 * stateless; Game keeps the created objects.
 */
public final class NewGameSetup {
  private NewGameSetup() {
  }

  public static Commander InitializeCommander(String name, CrewMember commanderCrewMember, StarSystem[] universe, CrewMember[] mercenaries) {
    Commander cmdr = new Commander(commanderCrewMember);
    mercenaries[CrewMemberId.Commander.CastToInt()] = cmdr;
    Strings.SetCrewMemberName(CrewMemberId.Commander, name);
    while(cmdr.CurrentSystem() == null) {
      StarSystem system = universe[Functions.GetRandom(universe.length)];
      if(system.SpecialEventType() == SpecialEventType.NA
          && system.TechLevel().ordinal() > TechLevel.t0.ordinal()
          && system.TechLevel().ordinal() < TechLevel.t7.ordinal()) {
        // Make sure at least three other systems can be reached
        int close = 0;
        for(int i = 0; i < universe.length && close < 3; i++) {
          if(i != system.Id().CastToInt() && Functions.Distance(universe[i], system) <= cmdr.getShip().FuelTanks()) {
            close++;
          }
        }
        if(close >= 3) {
          cmdr.CurrentSystem(system);
        }
      }
    }
    cmdr.CurrentSystem().Visited(true);
    return cmdr;
  }

  public static void GenerateCrewMemberList(CrewMember[] mercenaries, int universeSize, Difficulty difficulty) {
    int[] used = new int[universeSize];
    int d = difficulty.CastToInt();
    // Zeethibal may be on Kravat
    used[StarSystemId.Kravat.CastToInt()] = 1;
    // special individuals:
    // Zeethibal, Jonathan Wild's Nephew - skills will be set later.
    // Wild, Jonathan Wild earns his keep now - JAF.
    // Jarek, Ambassador Jarek earns his keep now - JAF.
    // Dummy pilots for opponents.
    mercenaries[CrewMemberId.Zeethibal.CastToInt()] = new CrewMember(CrewMemberId.Zeethibal, 5, 5, 5, 5, StarSystemId.NA);
    mercenaries[CrewMemberId.Opponent.CastToInt()] = new CrewMember(CrewMemberId.Opponent, 5, 5, 5, 5, StarSystemId.NA);
    mercenaries[CrewMemberId.Wild.CastToInt()] = new CrewMember(CrewMemberId.Wild, 7, 10, 2, 5, StarSystemId.NA);
    mercenaries[CrewMemberId.Jarek.CastToInt()] = new CrewMember(CrewMemberId.Jarek, 3, 2, 10, 4, StarSystemId.NA);
    mercenaries[CrewMemberId.Princess.CastToInt()] = new CrewMember(CrewMemberId.Princess, 4, 3, 8, 9, StarSystemId.NA);
    mercenaries[CrewMemberId.FamousCaptain.CastToInt()] = new CrewMember(CrewMemberId.FamousCaptain, 10, 10, 10, 10, StarSystemId.NA);
    mercenaries[CrewMemberId.Dragonfly.CastToInt()] = new CrewMember(CrewMemberId.Dragonfly, 4 + d, 6 + d, 1, 6 + d, StarSystemId.NA);
    mercenaries[CrewMemberId.Scarab.CastToInt()] = new CrewMember(CrewMemberId.Scarab, 5 + d, 6 + d, 1, 6 + d, StarSystemId.NA);
    mercenaries[CrewMemberId.Scorpion.CastToInt()] = new CrewMember(CrewMemberId.Scorpion, 8 + d, 8 + d, 1, 6 + d, StarSystemId.NA);
    mercenaries[CrewMemberId.SpaceMonster.CastToInt()] = new CrewMember(CrewMemberId.SpaceMonster, 8 + d, 8 + d, 1, 1 + d, StarSystemId.NA);
    // JAF - Changing this to allow multiple mercenaries in each system, but no more than three.
    for(int i = 1; i < mercenaries.length; i++) {
      // Only create a CrewMember Object if one doesn't already exist in this slot in the array.
      if(mercenaries[i] == null) {
        StarSystemId id;
        boolean ok = false;
        do {
          id = StarSystemId.FromInt(Functions.GetRandom(universeSize));
          if(used[id.CastToInt()] < 3) {
            used[id.CastToInt()]++;
            ok = true;
          }
        } while(!ok);
        mercenaries[i] = new CrewMember(CrewMemberId.FromInt(i), Functions.RandomSkill(), Functions.RandomSkill(), Functions.RandomSkill(), Functions.RandomSkill(), id);
      }
    }
  }

  public static void CreateShips(Ship dragonfly, Ship scarab, Ship scorpion, Ship spaceMonster, CrewMember[] mercenaries) {
    // set the details of the Dragonfly...
    dragonfly.Crew()[0] = mercenaries[CrewMemberId.Dragonfly.CastToInt()];
    dragonfly.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    dragonfly.AddEquipment(Consts.WeapObjs.get(WeaponType.PulseLaser.id));
    dragonfly.AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
    dragonfly.AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
    dragonfly.AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
    dragonfly.AddEquipment(Consts.Gadgets.get(GadgetType.AutoRepairSystem.asInteger()));
    dragonfly.AddEquipment(Consts.Gadgets.get(GadgetType.TargetingSystem.asInteger()));
    // set the details of the Scarab...
    scarab.Crew()[0] = mercenaries[CrewMemberId.Scarab.CastToInt()];
    scarab.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    scarab.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    // set the details of the Scorpion...
    scorpion.Crew()[0] = mercenaries[CrewMemberId.Scorpion.CastToInt()];
    scorpion.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    scorpion.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    scorpion.AddEquipment(Consts.Shields.get(ShieldType.Reflective.id));
    scorpion.AddEquipment(Consts.Shields.get(ShieldType.Reflective.id));
    scorpion.AddEquipment(Consts.Gadgets.get(GadgetType.AutoRepairSystem.asInteger()));
    scorpion.AddEquipment(Consts.Gadgets.get(GadgetType.TargetingSystem.asInteger()));
    // set the details of the Space Monster...
    spaceMonster.Crew()[0] = mercenaries[CrewMemberId.SpaceMonster.CastToInt()];
    spaceMonster.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    spaceMonster.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
    spaceMonster.AddEquipment(Consts.WeapObjs.get(WeaponType.MilitaryLaser.id));
  }
}
