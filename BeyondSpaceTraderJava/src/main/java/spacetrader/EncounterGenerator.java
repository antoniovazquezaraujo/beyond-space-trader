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
 * generates: the ships and the encounter state stay in the game.
 */
public final class EncounterGenerator {
  private final Game game;

  public EncounterGenerator(Game game) {
    this.game = game;
  }

  /** Looks for an encounter during a trip; true when one has to be shown. */
  public boolean determine() {
    // If there is a specific encounter that needs to happen, it will, otherwise we'll generate a random encounter.
    return nonRandom() || randomEncounter();
  }

  private boolean nonRandom() {
    boolean showEncounter = false;
    // Encounter with space monster
    if(game.getClicks() == 1 && game.WarpSystem().Id() == StarSystemId.Acamar && game.getQuestStatusSpaceMonster() == SpecialEvent.StatusSpaceMonsterAtAcamar) {
      game.setOpponent(game.SpaceMonster());
      game.setEncounterType(game.Commander().getShip().Cloaked() ? EncounterType.SpaceMonsterIgnore : EncounterType.SpaceMonsterAttack);
      showEncounter = true;
    } else if(game.getArrivedViaWormhole() && game.getClicks() == 20 && game.WarpSystem().SpecialEventType() != SpecialEventType.NA
        && game.WarpSystem().SpecialEvent().Type() == SpecialEventType.ScarabDestroyed
        && game.getQuestStatusScarab() == SpecialEvent.StatusScarabHunting) {
      // Encounter with the stolen Scarab
      game.setOpponent(game.Scarab());
      game.setEncounterType(game.Commander().getShip().Cloaked() ? EncounterType.ScarabIgnore : EncounterType.ScarabAttack);
      showEncounter = true;
    } else if(game.getClicks() == 1 && game.WarpSystem().Id() == StarSystemId.Zalkon && game.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyZalkon) {
      // Encounter with stolen Dragonfly
      game.setOpponent(game.Dragonfly());
      game.setEncounterType(game.Commander().getShip().Cloaked() ? EncounterType.DragonflyIgnore : EncounterType.DragonflyAttack);
      showEncounter = true;
    } else if(game.getClicks() == 1 && game.WarpSystem().Id() == StarSystemId.Qonos && game.getQuestStatusPrincess() == SpecialEvent.StatusPrincessFlyQonos) {
      // Encounter with kidnappers in the Scorpion
      game.setOpponent(game.Scorpion());
      game.setEncounterType(game.Commander().getShip().Cloaked() ? EncounterType.ScorpionIgnore : EncounterType.ScorpionAttack);
      showEncounter = true;
    } else if(game.getClicks() == 1 && game.getJustLootedMarie()) {
      // ah, just when you thought you were gonna get away with it...
      generateOpponent(OpponentType.Police);
      game.setEncounterType(EncounterType.MarieCelestePolice);
      game.setJustLootedMarie(false);
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean pirateEncounter(boolean mantis) {
    boolean showEncounter = false;
    if(mantis) {
      generateOpponent(OpponentType.Mantis);
      game.setEncounterType(EncounterType.PirateAttack);
    } else {
      generateOpponent(OpponentType.Pirate);
      // If you have a cloak, they don't see you
      if(game.Commander().getShip().Cloaked()) {
        game.setEncounterType(EncounterType.PirateIgnore);
      } else if(game.getOpponent().Type().CastToInt() > game.Commander().getShip().Type().CastToInt()
          || game.getOpponent().Type().CastToInt() >= ShipType.Grasshopper.CastToInt()
          || Functions.GetRandom(Consts.ReputationScoreElite) > (game.Commander().getReputationScore() * 4)
          / (1 + game.getOpponent().Type().CastToInt())) {
        // Pirates will mostly attack, but they are cowardly: if your rep is too high, they tend to flee
        // if Pirates are in a better ship, they won't flee, even if you have a very scary reputation.
        game.setEncounterType(EncounterType.PirateAttack);
      } else {
        game.setEncounterType(EncounterType.PirateFlee);
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place
    if(game.getEncounterType() == EncounterType.PirateAttack || !(game.getOpponent().Cloaked() || game.Options().getAlwaysIgnorePirates())) {
      showEncounter = true;
    }
    return showEncounter;
  }

  private boolean policeEncounter() {
    boolean showEncounter = false;
    generateOpponent(OpponentType.Police);
    // If you are cloaked, they don't see you
    game.setEncounterType(EncounterType.PoliceIgnore);
    if(!game.Commander().getShip().Cloaked()) {
      if(game.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreDubious) {
        // If you're a criminal, the police will tend to attack
        // JAF - fixed this; there was code that didn't do anything.
        // if you're suddenly stuck in a lousy ship, Police won't flee even if you have a fearsome reputation.
        if(game.getOpponent().WeaponStrength() > 0
            && (game.Commander().getReputationScore() < Consts.ReputationScoreAverage
            || Functions.GetRandom(Consts.ReputationScoreElite) > (game.Commander().getReputationScore() / (1 + game.getOpponent().Type().CastToInt())))
            || game.getOpponent().Type().CastToInt() > game.Commander().getShip().Type().CastToInt()) {
          if(game.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreCriminal) {
            game.setEncounterType(EncounterType.PoliceSurrender);
          } else {
            game.setEncounterType(EncounterType.PoliceAttack);
          }
        } else if(game.getOpponent().Cloaked()) {
          game.setEncounterType(EncounterType.PoliceIgnore);
        } else {
          game.setEncounterType(EncounterType.PoliceFlee);
        }
      } else if(!game.getInspected()
          && (game.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreClean
          || (game.Commander().getPoliceRecordScore() < Consts.PoliceRecordScoreLawful && Functions.GetRandom(12 - game.Difficulty().CastToInt()) < 1)
          || (game.Commander().getPoliceRecordScore() >= Consts.PoliceRecordScoreLawful && Functions.GetRandom(40) == 0))) {
        // If you're reputation is dubious, the police will inspect you
        // If your record is clean, the police will inspect you with a chance of 10% on Normal
        // If your record indicates you are a lawful trader, the chance on inspection drops to 2.5%
        game.setEncounterType(EncounterType.PoliceInspect);
        game.setInspected(true);
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place. Otherwise it will - JAF
    if(game.getEncounterType() == EncounterType.PoliceAttack || game.getEncounterType() == EncounterType.PoliceInspect
        || !(game.getOpponent().Cloaked() || game.Options().getAlwaysIgnorePolice())) {
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
    if(game.WarpSystem().Id() == StarSystemId.Gemulon && game.getQuestStatusGemulon() == SpecialEvent.StatusGemulonTooLate) {
      if(Functions.GetRandom(10) > 4) {
        mantis = true;
      }
    } else {
      // Check if it is time for an encounter
      int encounter = Functions.GetRandom(44 - (2 * game.Difficulty().CastToInt()));
      int policeModifier = Math.max(1, 3 - PoliceRecord.GetPoliceRecordFromScore(game.Commander().getPoliceRecordScore()).Type().CastToInt());
      // encounters are half as likely if you're in a flea.
      if(game.Commander().getShip().Type() == ShipType.Flea) {
        encounter *= 2;
      }
      if(encounter < game.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt()) { // When you are already raided, other pirates have little to gain
        pirate = !game.getRaided();
      } else if(encounter < game.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt() + game.WarpSystem().PoliticalSystem().ActivityPolice().CastToInt() * policeModifier) {
        // policeModifier adapts itself to your criminal record: you'll encounter more police if you are a hardened criminal.
        police = true;
      } else if(encounter
          < game.WarpSystem().PoliticalSystem().ActivityPirates().CastToInt()
          + game.WarpSystem().PoliticalSystem().ActivityPolice().CastToInt() * policeModifier
          + game.WarpSystem().PoliticalSystem().ActivityTraders().CastToInt()) {
        trader = true;
      } else if(game.Commander().getShip().WildOnBoard() && game.WarpSystem().Id() == StarSystemId.Kravat) {
        // if you're coming in to Kravat & you have Wild onboard, there'll be swarms o' cops.
        police = Functions.GetRandom(100) < 100 / Math.max(2, Math.min(4, 5 - game.Difficulty().CastToInt()));
      } else if(game.Commander().getShip().ArtifactOnBoard() && Functions.GetRandom(20) <= 3) {
        mantis = true;
      }
    }
    if(police) {
      showEncounter = policeEncounter();
    } else if(pirate || mantis) {
      showEncounter = pirateEncounter(mantis);
    } else if(trader) {
      showEncounter = traderEncounter();
    } else if(game.Commander().getDays() > 10 && Functions.GetRandom(1000) < game.getChanceOfVeryRareEncounter() && game.VeryRareEncounters().size() > 0) {
      showEncounter = veryRareEncounter();
    }
    return showEncounter;
  }

  private boolean traderEncounter() {
    boolean showEncounter = false;
    generateOpponent(OpponentType.Trader);
    // If you are cloaked, they don't see you
    game.setEncounterType(EncounterType.TraderIgnore);
    if(!game.Commander().getShip().Cloaked()) {
      // If you're a criminal, traders tend to flee if you've got at least some reputation
      if(!game.Commander().getShip().Cloaked() && game.Commander().getPoliceRecordScore() <= Consts.PoliceRecordScoreCriminal
          && Functions.GetRandom(Consts.ReputationScoreElite) <= (game.Commander().getReputationScore() * 10) / (1 + game.getOpponent().Type().CastToInt())) {
        game.setEncounterType(EncounterType.TraderFlee);
      } else if(Functions.GetRandom(1000) < game.getChanceOfTradeInOrbit()) { // Will there be trade in orbit?
        if(game.Commander().getShip().FreeCargoBays() > 0 && game.getOpponent().HasTradeableItems()) {
          game.setEncounterType(EncounterType.TraderSell);
        } else if(game.Commander().getShip().HasTradeableItems()) {
          // we fudge on whether the trader has capacity to carry the stuff he's buying.
          game.setEncounterType(EncounterType.TraderBuy);
        }
      }
    }
    // If they ignore you or flee and you can't see them, the encounter doesn't take place
    // If you automatically don't want to confront someone who ignores you, the encounter may not take place; otherwise it will.
    if(!game.getOpponent().Cloaked()
        && !(game.Options().getAlwaysIgnoreTraders() && (game.getEncounterType() == EncounterType.TraderIgnore || game.getEncounterType() == EncounterType.TraderFlee))
        && !((game.getEncounterType() == EncounterType.TraderBuy || game.getEncounterType() == EncounterType.TraderSell) && game.Options().getAlwaysIgnoreTradeInOrbit())) {
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
    switch(game.VeryRareEncounters().get(Functions.GetRandom(game.VeryRareEncounters().size()))) {
      case MarieCeleste:
        // Marie Celeste cannot be at Acamar, Qonos, or Zalkon as it may cause problems with the Space Monster, Scorpion, or Dragonfly
        if(game.getClicks() > 1 && game.Commander().getCurrentSystemId() != StarSystemId.Acamar
            && game.Commander().getCurrentSystemId() != StarSystemId.Zalkon
            && game.Commander().getCurrentSystemId() != StarSystemId.Qonos) {
          game.VeryRareEncounters().remove(VeryRareEncounter.MarieCeleste);
          game.setEncounterType(EncounterType.MarieCeleste);
          generateOpponent(OpponentType.Trader);
          for(int i = 0; i < game.getOpponent().Cargo().length; i++) {
            game.getOpponent().Cargo()[i] = 0;
          }
          game.getOpponent().Cargo()[TradeItemType.Narcotics.CastToInt()] = Math.min(game.getOpponent().CargoBays(), 5);
          showEncounter = true;
        }
        break;
      case CaptainAhab:
        if(game.Commander().getShip().HasShield(ShieldType.Reflective) && game.Commander().Pilot() < 10
            && game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          game.VeryRareEncounters().remove(VeryRareEncounter.CaptainAhab);
          game.setEncounterType(EncounterType.CaptainAhab);
          generateOpponent(OpponentType.FamousCaptain);
          showEncounter = true;
        }
        break;
      case CaptainConrad:
        if(game.Commander().getShip().HasWeapon(WeaponType.MilitaryLaser, true) && game.Commander().Engineer() < 10
            && game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          game.VeryRareEncounters().remove(VeryRareEncounter.CaptainConrad);
          game.setEncounterType(EncounterType.CaptainConrad);
          generateOpponent(OpponentType.FamousCaptain);

          showEncounter = true;
        }
        break;
      case CaptainHuie:
        if(game.Commander().getShip().HasWeapon(WeaponType.MilitaryLaser, true) && game.Commander().Trader() < 10
            && game.Commander().getPoliceRecordScore() > Consts.PoliceRecordScoreCriminal) {
          game.VeryRareEncounters().remove(VeryRareEncounter.CaptainHuie);
          game.setEncounterType(EncounterType.CaptainHuie);
          generateOpponent(OpponentType.FamousCaptain);
          showEncounter = true;
        }
        break;
      case BottleOld:
        game.VeryRareEncounters().remove(VeryRareEncounter.BottleOld);
        game.setEncounterType(EncounterType.BottleOld);
        generateOpponent(OpponentType.Bottle);
        showEncounter = true;
        break;
      case BottleGood:
        game.VeryRareEncounters().remove(VeryRareEncounter.BottleGood);
        game.setEncounterType(EncounterType.BottleGood);
        generateOpponent(OpponentType.Bottle);
        showEncounter = true;
        break;
    }
    return showEncounter;
  }

  private void generateOpponent(OpponentType oppType) {
    game.setOpponent(new Ship(oppType));
  }
}
