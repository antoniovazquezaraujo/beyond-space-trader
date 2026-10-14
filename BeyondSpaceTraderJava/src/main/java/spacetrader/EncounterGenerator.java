/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.events.EncounterType;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import spacetrader.enums.OpponentType;
import spacetrader.enums.StarSystemId;


/**
 * Decides the encounters of a trip: who appears (pirates, police, traders, famous
 * captains, the quest ships and the very rare ones) and how they start. It only
 * generates: the ships and the encounter state stay in the game. The generator
 * asks the game through the narrow {@link EncounterContext} and never holds a
 * reference back to it.
 */
public final class EncounterGenerator {
  private final EncounterContext context;

  public EncounterGenerator(EncounterContext context) {
    this.context = context;
  }

  /** Looks for an encounter during a trip; true when one has to be shown. */
  public boolean determine() {
    // If there is a specific encounter that needs to happen, it will, otherwise we'll generate a random encounter.
    return nonRandom() || randomEncounter();
  }

  private boolean nonRandom() {
    boolean showEncounter = false;
    // Encounter with space monster
    if(context.getClicks() == 1 && context.WarpSystem().Id() == StarSystemId.Acamar && context.getQuestStatusSpaceMonster() == SpecialEvent.StatusSpaceMonsterAtAcamar) {
      context.encounter().setOpponent(context.SpaceMonster());
      context.encounter().setEncounterType(context.Commander().getShip().Cloaked() ? EncounterType.SpaceMonsterIgnore : EncounterType.SpaceMonsterAttack);
      showEncounter = true;
    } else if(context.getArrivedViaWormhole() && context.getClicks() == 20 && context.WarpSystem().SpecialEventType() != SpecialEventType.NA
        && context.WarpSystem().SpecialEvent().Type() == SpecialEventType.ScarabDestroyed
        && context.getQuestStatusScarab() == SpecialEvent.StatusScarabHunting) {
      // Encounter with the stolen Scarab
      context.encounter().setOpponent(context.Scarab());
      context.encounter().setEncounterType(context.Commander().getShip().Cloaked() ? EncounterType.ScarabIgnore : EncounterType.ScarabAttack);
      showEncounter = true;
    } else if(context.getClicks() == 1 && context.WarpSystem().Id() == StarSystemId.Zalkon && context.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyZalkon) {
      // Encounter with stolen Dragonfly
      context.encounter().setOpponent(context.Dragonfly());
      context.encounter().setEncounterType(context.Commander().getShip().Cloaked() ? EncounterType.DragonflyIgnore : EncounterType.DragonflyAttack);
      showEncounter = true;
    } else if(context.getClicks() == 1 && context.WarpSystem().Id() == StarSystemId.Qonos && context.getQuestStatusPrincess() == SpecialEvent.StatusPrincessFlyQonos) {
      // Encounter with kidnappers in the Scorpion
      context.encounter().setOpponent(context.Scorpion());
      context.encounter().setEncounterType(context.Commander().getShip().Cloaked() ? EncounterType.ScorpionIgnore : EncounterType.ScorpionAttack);
      showEncounter = true;
    } else if(context.getClicks() == 1 && context.encounter().getJustLootedMarie()) {
      // ah, just when you thought you were gonna get away with it...
      generateOpponent(OpponentType.Police);
      context.encounter().setEncounterType(EncounterType.MarieCelestePolice);
      context.encounter().setJustLootedMarie(false);
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean pirateEncounter(boolean mantis) {
    boolean showEncounter = false;
    if(mantis) {
      generateOpponent(OpponentType.Mantis);
      context.encounter().setEncounterType(EncounterType.PirateAttack);
    } else {
      generateOpponent(OpponentType.Pirate);
      // If you have a cloak, they don't see you
      if(context.Commander().getShip().Cloaked()) {
        context.encounter().setEncounterType(EncounterType.PirateIgnore);
      } else if(context.encounter().getOpponent().Type().CastToInt() > context.Commander().getShip().Type().CastToInt()
          || context.encounter().getOpponent().Type().CastToInt() >= ShipType.Grasshopper.CastToInt()
          || Functions.GetRandom(Consts.ReputationScoreElite) > (context.Commander().getReputationScore() * 4)
          / (1 + context.encounter().getOpponent().Type().CastToInt())) {
        // Pirates will mostly attack, but they are cowardly: if your rep is too high, they tend to flee
        // if Pirates are in a better ship, they won't flee, even if you have a very scary reputation.
        context.encounter().setEncounterType(EncounterType.PirateAttack);
      } else {
        context.encounter().setEncounterType(EncounterType.PirateFlee);
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place
    if(context.encounter().getEncounterType() == EncounterType.PirateAttack || !(context.encounter().getOpponent().Cloaked() || context.Options().getAlwaysIgnorePirates())) {
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean policeEncounter() {
    boolean showEncounter = false;
    generateOpponent(OpponentType.Police);
    // If you are cloaked, they don't see you
    context.encounter().setEncounterType(EncounterType.PoliceIgnore);
    if(!context.Commander().getShip().Cloaked()) {
      if(context.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreDubious) {
        // If you're a criminal, the police will tend to attack
        // JAF - fixed this; there was code that didn't do anything.
        // if you're suddenly stuck in a lousy ship, Police won't flee even if you have a fearsome reputation.
        if(context.encounter().getOpponent().WeaponStrength() > 0
            && (context.Commander().getReputationScore() < Consts.ReputationScoreAverage
            || Functions.GetRandom(Consts.ReputationScoreElite) > (context.Commander().getReputationScore() / (1 + context.encounter().getOpponent().Type().CastToInt())))
            || context.encounter().getOpponent().Type().CastToInt() > context.Commander().getShip().Type().CastToInt()) {
          if(context.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreCriminal) {
            context.encounter().setEncounterType(EncounterType.PoliceSurrender);
          } else {
            context.encounter().setEncounterType(EncounterType.PoliceAttack);
          }
        } else if(context.encounter().getOpponent().Cloaked()) {
          context.encounter().setEncounterType(EncounterType.PoliceIgnore);
        } else {
          context.encounter().setEncounterType(EncounterType.PoliceFlee);
        }
      } else if(!context.encounter().getInspected()
          && (context.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreClean
          || (context.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreLawful && Functions.GetRandom(12 - context.Difficulty().CastToInt()) < 1)
          || (context.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreLawful && Functions.GetRandom(40) == 0))) {
        // If you're reputation is dubious, the police will inspect you
        // If your record is clean, the police will inspect you with a chance of 10% on Normal
        // If your record indicates you are a lawful trader, the chance on inspection drops to 2.5%
        context.encounter().setEncounterType(EncounterType.PoliceInspect);
        context.encounter().setInspected(true);
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place. Otherwise it will - JAF
    if(context.encounter().getEncounterType() == EncounterType.PoliceAttack || context.encounter().getEncounterType() == EncounterType.PoliceInspect
        || !(context.encounter().getOpponent().Cloaked() || context.Options().getAlwaysIgnorePolice())) {
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean randomEncounter() {
    boolean showEncounter = false;
    boolean mantis = false;
    boolean pirate = false;
    boolean police = false;
    boolean trader = false;
    if(context.WarpSystem().Id() == StarSystemId.Gemulon && context.getQuestStatusGemulon() == SpecialEvent.StatusGemulonTooLate) {
      if(Functions.GetRandom(10) > 4) {
        mantis = true;
      }
    } else {
      // Check if it is time for an encounter
      int encounter = Functions.GetRandom(44 - (2 * context.Difficulty().CastToInt()));
      int policeModifier = Math.max(1, 3 - PoliceRecord.GetPoliceRecordFromScore(context.Commander().getPoliceRecordScore()).Type().CastToInt());
      // encounters are half as likely if you're in a flea.
      if(context.Commander().getShip().Type() == ShipType.Flea) {
        encounter *= 2;
      }
      if(encounter < context.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt()) { // When you are already raided, other pirates have little to gain
        pirate = !context.encounter().getRaided();
      } else if(encounter < context.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt() + context.WarpSystem().PoliticalSystem().ActivityPolice().CastToInt() * policeModifier) {
        // policeModifier adapts itself to your criminal record: you'll encounter more police if you are a hardened criminal.
        police = true;
      } else if(encounter
          < context.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt()
          + context.WarpSystem().PoliticalSystem().ActivityPolice().CastToInt() * policeModifier
          + context.WarpSystem().PoliticalSystem().ActivityTraders().CastToInt()) {
        trader = true;
      } else if(context.Commander().getShip().WildOnBoard() && context.WarpSystem().Id() == StarSystemId.Kravat) {
        // if you're coming in to Kravat & you have Wild onboard, there'll be swarms o' cops.
        police = Functions.GetRandom(100) < 100 / Math.max(2, Math.min(4, 5 - context.Difficulty().CastToInt()));
      } else if(context.Commander().getShip().ArtifactOnBoard() && Functions.GetRandom(20) <= 3) {
        mantis = true;
      }
    }
    if(police) {
      showEncounter = policeEncounter();
    } else if(pirate || mantis) {
      showEncounter = pirateEncounter(mantis);
    } else if(trader) {
      showEncounter = traderEncounter();
    } else if(context.Commander().getDays() > 10 && Functions.GetRandom(1000) < context.getChanceOfVeryRareEncounter() && context.VeryRareEncounters().size() > 0) {
      showEncounter = veryRareEncounter();
    }
    return showEncounter;
  }

  private boolean traderEncounter() {
    boolean showEncounter = false;
    generateOpponent(OpponentType.Trader);
    // If you are cloaked, they don't see you
    context.encounter().setEncounterType(EncounterType.TraderIgnore);
    if(!context.Commander().getShip().Cloaked()) {
      // If you're a criminal, traders tend to flee if you've got at least some reputation
      if(!context.Commander().getShip().Cloaked() && context.Commander().getPoliceRecordScore() <= Consts.PoliceRecordScoreCriminal
          && Functions.GetRandom(Consts.ReputationScoreElite) <= (context.Commander().getReputationScore() * 10) / (1 + context.encounter().getOpponent().Type().CastToInt())) {
        context.encounter().setEncounterType(EncounterType.TraderFlee);
      } else if(Functions.GetRandom(1000) < context.getChanceOfTradeInOrbit()) { // Will there be trade in orbit?
        if(context.Commander().getShip().FreeCargoBays() > 0 && context.encounter().getOpponent().HasTradeableItems()) {
          context.encounter().setEncounterType(EncounterType.TraderSell);
        } else if(context.Commander().getShip().HasTradeableItems()) {
          // we fudge on whether the trader has capacity to carry the stuff he's buying.
          context.encounter().setEncounterType(EncounterType.TraderBuy);
        }
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place; otherwise it will.
    if(!context.encounter().getOpponent().Cloaked()
        && !(context.Options().getAlwaysIgnoreTraders() && (context.encounter().getEncounterType() == EncounterType.TraderIgnore || context.encounter().getEncounterType() == EncounterType.TraderFlee))
        && !((context.encounter().getEncounterType() == EncounterType.TraderBuy || context.encounter().getEncounterType() == EncounterType.TraderSell) && context.Options().getAlwaysIgnoreTradeInOrbit())) {
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean veryRareEncounter() {
    boolean showEncounter = false;
    // Very Rare Random Events:
    // 1. Encounter the abandoned Marie Celeste, which you may loot.
    // 2. Captain Ahab will trade your Reflective Shield for skill points in Piloting.
    // 3. Captain Conrad will trade your Military Laser for skill points in Engineering.
    // 4. Captain Huie will trade your Military Laser for points in Trading.
    // 5. Encounter an out-of-date bottle of Captain Marmoset's Skill Tonic. This will affect skills depending on game difficulty level.
    // 6. Encounter a good bottle of Captain Marmoset's Skill Tonic, which will invoke IncreaseRandomSkill one or two times, depending on game difficulty.
    switch(context.VeryRareEncounters().get(Functions.GetRandom(context.VeryRareEncounters().size()))) {
      case MarieCeleste:
        // Marie Celeste cannot be at Acamar, Qonos, or Zalkon as it may cause problems with the Space Monster, Scorpion, or Dragonfly
        if(context.getClicks() > 1 && context.Commander().getCurrentSystemId() != StarSystemId.Acamar
            && context.Commander().getCurrentSystemId() != StarSystemId.Zalkon
            && context.Commander().getCurrentSystemId() != StarSystemId.Qonos) {
          context.VeryRareEncounters().remove(VeryRareEncounter.MarieCeleste);
          context.encounter().setEncounterType(EncounterType.MarieCeleste);
          generateOpponent(OpponentType.Trader);
          for(int i = 0; i < context.encounter().getOpponent().Cargo().length; i++) {
            context.encounter().getOpponent().Cargo()[i] = 0;
          }
          context.encounter().getOpponent().Cargo()[TradeItemType.Narcotics.CastToInt()] = Math.min(context.encounter().getOpponent().CargoBays(), 5);
          showEncounter = true;
        }
        break;
      case CaptainAhab:
        if(context.Commander().getShip().HasShield(ShieldType.Reflective) && context.Commander().Pilot() < 10
            && context.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          context.VeryRareEncounters().remove(VeryRareEncounter.CaptainAhab);
          context.encounter().setEncounterType(EncounterType.CaptainAhab);
          generateOpponent(OpponentType.FamousCaptain);
          showEncounter = true;
        }
        break;
      case CaptainConrad:
        if(context.Commander().getShip().HasWeapon(WeaponType.MilitaryLaser, true) && context.Commander().Engineer() < 10
            && context.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          context.VeryRareEncounters().remove(VeryRareEncounter.CaptainConrad);
          context.encounter().setEncounterType(EncounterType.CaptainConrad);
          generateOpponent(OpponentType.FamousCaptain);

          showEncounter = true;
        }
        break;
      case CaptainHuie:
        if(context.Commander().getShip().HasWeapon(WeaponType.MilitaryLaser, true) && context.Commander().Trader() < 10
            && context.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          context.VeryRareEncounters().remove(VeryRareEncounter.CaptainHuie);
          context.encounter().setEncounterType(EncounterType.CaptainHuie);
          generateOpponent(OpponentType.FamousCaptain);
          showEncounter = true;
        }
        break;
      case BottleOld:
        context.VeryRareEncounters().remove(VeryRareEncounter.BottleOld);
        context.encounter().setEncounterType(EncounterType.BottleOld);
        generateOpponent(OpponentType.Bottle);
        showEncounter = true;
        break;
      case BottleGood:
        context.VeryRareEncounters().remove(VeryRareEncounter.BottleGood);
        context.encounter().setEncounterType(EncounterType.BottleGood);
        generateOpponent(OpponentType.Bottle);
        showEncounter = true;
        break;
    }
    return showEncounter;
  }

  private void generateOpponent(OpponentType oppType) {
    context.encounter().setOpponent(new Ship(oppType));
  }
}
