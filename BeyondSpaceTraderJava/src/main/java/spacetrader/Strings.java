/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.ship.ShipType;


/**
 * The game texts. They live in the {@code spacetrader/Strings.properties} resource
 * bundle so they can be translated: drop a {@code Strings_<locale>.properties}
 * next to it (same keys, keeping the ^1/^2 placeholders) and run the game with
 * that locale. The names and tables are loaded once at startup.
 */
public final class Strings {
  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("spacetrader.Strings");

  private Strings() {
  }

  /**
   * The text of a bundle key; also used by the model enums whose names are texts.
   */
  public static String text(String key) {
    return BUNDLE.getString(key);
  }

  private static List<String> list(String key) {
    List<String> values = new ArrayList<>();
    for(int i = 0; BUNDLE.containsKey(key + "." + i); i++) {
      values.add(BUNDLE.getString(key + "." + i));
    }
    return Collections.unmodifiableList(values);
  }

  private static List<List<String>> table(String key) {
    List<List<String>> rows = new ArrayList<>();
    for(int row = 0; BUNDLE.containsKey(key + "." + row + ".0"); row++) {
      List<String> values = new ArrayList<>();
      for(int col = 0; BUNDLE.containsKey(key + "." + row + "." + col); col++) {
        values.add(BUNDLE.getString(key + "." + row + "." + col));
      }
      rows.add(Collections.unmodifiableList(values));
    }
    return Collections.unmodifiableList(rows);
  }

  public static final String newline = text("newline");
  public static final String CargoSellStatementDump = text("CargoSellStatementDump");
  public static final String CargoTitle = text("CargoTitle");
  public static final String CargoUnit = text("CargoUnit");
  public static final String DistanceUnit = text("DistanceUnit");
  public static final String EncounterActionOppAttacks = text("EncounterActionOppAttacks");
  public static final String EncounterHidePrincess = text("EncounterHidePrincess");
  public static final String EncounterHideSculpture = text("EncounterHideSculpture");
  public static final String EncounterHullStrength = text("EncounterHullStrength");
  public static final String EncounterPiratesDestroyed = text("EncounterPiratesDestroyed");
  public static final String EncounterPiratesDisabled = text("EncounterPiratesDisabled");
  public static final String EncounterPiratesLocation = text("EncounterPiratesLocation");
  public static final String EncounterPoliceSubmitArrested = text("EncounterPoliceSubmitArrested");
  public static final String EncounterPoliceSubmitGoods = text("EncounterPoliceSubmitGoods");
  public static final String EncounterPoliceSubmitReactor = text("EncounterPoliceSubmitReactor");
  public static final String EncounterPoliceSubmitSculpture = text("EncounterPoliceSubmitSculpture");
  public static final String EncounterPoliceSubmitWild = text("EncounterPoliceSubmitWild");
  public static final String EncounterPoliceSurrenderCargo = text("EncounterPoliceSurrenderCargo");
  public static final String EncounterPoliceSurrenderAction = text("EncounterPoliceSurrenderAction");
  public static final String EncounterPoliceSurrenderReactor = text("EncounterPoliceSurrenderReactor");
  public static final String EncounterPoliceSurrenderSculpt = text("EncounterPoliceSurrenderSculpt");
  public static final String EncounterPoliceSurrenderWild = text("EncounterPoliceSurrenderWild");
  public static final String EncounterPretextAlien = text("EncounterPretextAlien");
  public static final String EncounterPretextBottle = text("EncounterPretextBottle");
  public static final String EncounterPretextCaptainAhab = text("EncounterPretextCaptainAhab");
  public static final String EncounterPretextCaptainConrad = text("EncounterPretextCaptainConrad");
  public static final String EncounterPretextCaptainHuie = text("EncounterPretextCaptainHuie");
  public static final String EncounterPretextMarie = text("EncounterPretextMarie");
  public static final String EncounterPretextMariePolice = text("EncounterPretextMariePolice");
  public static final String EncounterPretextPirate = text("EncounterPretextPirate");
  public static final String EncounterPretextPolice = text("EncounterPretextPolice");
  public static final String EncounterPretextScorpion = text("EncounterPretextScorpion");
  public static final String EncounterPretextSpaceMonster = text("EncounterPretextSpaceMonster");
  public static final String EncounterPretextStolen = text("EncounterPretextStolen");
  public static final String EncounterPretextTrader = text("EncounterPretextTrader");
  public static final String EncounterPrincessRescued = text("EncounterPrincessRescued");
  public static final String EncounterShieldStrength = text("EncounterShieldStrength");
  public static final String EncounterShieldNone = text("EncounterShieldNone");
  public static final String EncounterShipCaptain = text("EncounterShipCaptain");
  public static final String EncounterShipMantis = text("EncounterShipMantis");
  public static final String EncounterShipPirate = text("EncounterShipPirate");
  public static final String EncounterShipPolice = text("EncounterShipPolice");
  public static final String EncounterShipTrader = text("EncounterShipTrader");
  public static final String EncounterText = text("EncounterText");
  public static final String EncounterTextBottle = text("EncounterTextBottle");
  public static final String EncounterTextFamousCaptain = text("EncounterTextFamousCaptain");
  public static final String EncounterTextMarieCeleste = text("EncounterTextMarieCeleste");
  public static final String EncounterTextOpponentAttack = text("EncounterTextOpponentAttack");
  public static final String EncounterTextOpponentFlee = text("EncounterTextOpponentFlee");
  public static final String EncounterTextOpponentIgnore = text("EncounterTextOpponentIgnore");
  public static final String EncounterTextOpponentNoNotice = text("EncounterTextOpponentNoNotice");
  public static final String EncounterTextPoliceInspection = text("EncounterTextPoliceInspection");
  public static final String EncounterTextPolicePostMarie = text("EncounterTextPolicePostMarie");
  public static final String EncounterTextPoliceSurrender = text("EncounterTextPoliceSurrender");
  public static final String EncounterTextTrader = text("EncounterTextTrader");
  public static final String EquipmentNoneForSale = text("EquipmentNoneForSale");
  public static final String EquipmentNoSlots = text("EquipmentNoSlots");
  public static final String EquipmentFreeSlot = text("EquipmentFreeSlot");
  public static final String FileFormatBad = text("FileFormatBad");
  public static final String FileFutureVersion = text("FileFutureVersion");
  public static final String HighScoreStatus = text("HighScoreStatus");
  public static final String Mercenaries = text("Mercenaries");
  public static final String MercenariesForHire = text("MercenariesForHire");
  public static final String MercenaryFire = text("MercenaryFire");
  public static final String MercenaryHire = text("MercenaryHire");
  public static final String MercOnBoard = text("MercOnBoard");
  public static final String MoneyRateSuffix = text("MoneyRateSuffix");
  public static final String MoneyUnit = text("MoneyUnit");
  public static final String NA = text("NA");
  public static final String NewsMoonForSale = text("NewsMoonForSale");
  public static final String NewsShipyard = text("NewsShipyard");
  public static final String NewsTribbleBuyer = text("NewsTribbleBuyer");
  public static final String PersonnelNoMercenaries = text("PersonnelNoMercenaries");
  public static final String PersonnelNoQuarters = text("PersonnelNoQuarters");
  public static final String PersonnelVacancy = text("PersonnelVacancy");
  public static final String QuestNone = text("QuestNone");
  public static final String QuestArtifact = text("QuestArtifact");
  public static final String QuestDragonflyBaratas = text("QuestDragonflyBaratas");
  public static final String QuestDragonflyMelina = text("QuestDragonflyMelina");
  public static final String QuestDragonflyRegulas = text("QuestDragonflyRegulas");
  public static final String QuestDragonflyShield = text("QuestDragonflyShield");
  public static final String QuestDragonflyZalkon = text("QuestDragonflyZalkon");
  public static final String QuestExperimentInformDays = text("QuestExperimentInformDays");
  public static final String QuestExperimentInformTomorrow = text("QuestExperimentInformTomorrow");
  public static final String QuestGemulonFuel = text("QuestGemulonFuel");
  public static final String QuestGemulonInformDays = text("QuestGemulonInformDays");
  public static final String QuestGemulonInformTomorrow = text("QuestGemulonInformTomorrow");
  public static final String QuestJarek = text("QuestJarek");
  public static final String QuestJarekImpatient = text("QuestJarekImpatient");
  public static final String QuestJaporiDeliver = text("QuestJaporiDeliver");
  public static final String QuestMoon = text("QuestMoon");
  public static final String QuestPrincessCentauri = text("QuestPrincessCentauri");
  public static final String QuestPrincessInthara = text("QuestPrincessInthara");
  public static final String QuestPrincessQonos = text("QuestPrincessQonos");
  public static final String QuestPrincessQuantum = text("QuestPrincessQuantum");
  public static final String QuestPrincessReturn = text("QuestPrincessReturn");
  public static final String QuestPrincessReturning = text("QuestPrincessReturning");
  public static final String QuestPrincessReturningImpatient = text("QuestPrincessReturningImpatient");
  public static final String QuestReactor = text("QuestReactor");
  public static final String QuestReactorFuel = text("QuestReactorFuel");
  public static final String QuestReactorLaser = text("QuestReactorLaser");
  public static final String QuestScarabFind = text("QuestScarabFind");
  public static final String QuestScarabHull = text("QuestScarabHull");
  public static final String QuestScarabNotify = text("QuestScarabNotify");
  public static final String QuestSculpture = text("QuestSculpture");
  public static final String QuestSculptureHiddenBays = text("QuestSculptureHiddenBays");
  public static final String QuestSpaceMonsterKill = text("QuestSpaceMonsterKill");
  public static final String QuestTribbles = text("QuestTribbles");
  public static final String QuestWild = text("QuestWild");
  public static final String QuestWildImpatient = text("QuestWildImpatient");
  public static final String ShipBuyGotOne = text("ShipBuyGotOne");
  public static final String ShipBuyTransfer = text("ShipBuyTransfer");
  public static final String ShipInfoEscapePod = text("ShipInfoEscapePod");
  public static final String ShipNameCurrentShip = text("ShipNameCurrentShip");
  public static final String ShipNameCustomShip = text("ShipNameCustomShip");
  public static final String ShipNameModified = text("ShipNameModified");
  public static final String ShipNameTemplateSuffixDefault = text("ShipNameTemplateSuffixDefault");
  public static final String ShipNameTemplateSuffixMinimum = text("ShipNameTemplateSuffixMinimum");
  public static final String ShipyardEquipForSale = text("ShipyardEquipForSale");
  public static final String ShipyardEquipNoSale = text("ShipyardEquipNoSale");
  public static final String ShipyardPodCost = text("ShipyardPodCost");
  public static final String ShipyardPodIF = text("ShipyardPodIF");
  public static final String ShipyardPodInstalled = text("ShipyardPodInstalled");
  public static final String ShipyardPodNoSale = text("ShipyardPodNoSale");
  public static final String ShipyardShipForSale = text("ShipyardShipForSale");
  public static final String ShipyardShipNoSale = text("ShipyardShipNoSale");
  public static final String ShipyardSizeItem = text("ShipyardSizeItem");
  public static final String ShipyardTitle = text("ShipyardTitle");
  public static final String ShipyardUnit = text("ShipyardUnit");
  public static final String ShipyardWarning = text("ShipyardWarning");
  public static final String ShipyardWelcome = text("ShipyardWelcome");
  public static final String SpecialCargoArtifact = text("SpecialCargoArtifact");
  public static final String SpecialCargoExperiment = text("SpecialCargoExperiment");
  public static final String SpecialCargoJapori = text("SpecialCargoJapori");
  public static final String SpecialCargoJarek = text("SpecialCargoJarek");
  public static final String SpecialCargoNone = text("SpecialCargoNone");
  public static final String SpecialCargoReactor = text("SpecialCargoReactor");
  public static final String SpecialCargoSculpture = text("SpecialCargoSculpture");
  public static final String SpecialCargoReactorBays = text("SpecialCargoReactorBays");
  public static final String SpecialCargoTribblesInfest = text("SpecialCargoTribblesInfest");
  public static final String SpecialCargoTribblesCute = text("SpecialCargoTribblesCute");
  public static final String TimeUnit = text("TimeUnit");
  public static final String TribbleDangerousNumber = text("TribbleDangerousNumber");
  public static final String Unknown = text("Unknown");
  public static final List<String> ActivityLevels = list("ActivityLevels");
  public static final List<String> CargoBuyOps = list("CargoBuyOps");
  public static final List<String> CargoSellOps = list("CargoSellOps");
  private static final List<String> MUTABLE_CREWMEMBERNAMES = new ArrayList<>(list("CrewMemberNames"));
  public static final List<String> CrewMemberNames = Collections.unmodifiableList(MUTABLE_CREWMEMBERNAMES);

  public static void SetCrewMemberName(CrewMemberId id, String value) {
    MUTABLE_CREWMEMBERNAMES.set(id.CastToInt(), value);
  }
  public static final List<String> DifficultyLevels = list("DifficultyLevels");
  public static final List<List<String>> EquipmentDescriptions = table("EquipmentDescriptions");
  public static final List<String> EquipmentTypes = list("EquipmentTypes");
  public static final List<String> GadgetNames = list("GadgetNames");
  public static final List<String> GameCompletionTypes = list("GameCompletionTypes");
  public static final List<String> ListStrings = list("ListStrings");
  public static final List<String> NewsEvent = list("NewsEvent");
  public static final List<List<String>> NewsHeadlines = table("NewsHeadlines");
  public static final List<List<String>> NewsMastheads = table("NewsMastheads");
  public static final List<String> NewsPoliceRecordHero = list("NewsPoliceRecordHero");
  public static final List<String> NewsPoliceRecordPsychopath = list("NewsPoliceRecordPsychopath");
  public static final List<String> NewsPressureExternal = list("NewsPressureExternal");
  public static final List<String> NewsPressureExternalPressures = list("NewsPressureExternalPressures");
  public static final List<String> NewsPressureInternal = list("NewsPressureInternal");
  public static final List<String> PoliceRecordNames = list("PoliceRecordNames");
  public static final List<String> PoliticalSystemNames = list("PoliticalSystemNames");
  public static final List<String> ReputationNames = list("ReputationNames");
  public static final List<String> ShieldNames = list("ShieldNames");
  private static final List<String> MUTABLE_SHIPNAMES = new ArrayList<>(list("ShipNames"));
  public static final List<String> ShipNames = Collections.unmodifiableList(MUTABLE_SHIPNAMES);

  public static void SetShipName(ShipType type, String value) {
    MUTABLE_SHIPNAMES.set(type.CastToInt(), value);
  }
  public static final List<String> ShipyardEngineers = list("ShipyardEngineers");
  public static final List<String> ShipyardNames = list("ShipyardNames");
  public static final List<String> ShipyardSkillDescriptions = list("ShipyardSkillDescriptions");
  public static final List<String> ShipyardSkills = list("ShipyardSkills");
  public static final List<String> Sizes = list("Sizes");
  public static final List<String> SpecialEventStrings = list("SpecialEventStrings");
  public static final List<String> SpecialEventTitles = list("SpecialEventTitles");
  public static final List<String> SystemNames = list("SystemNames");
  public static final List<String> VeryRareEncounters = list("VeryRareEncounters");
  public static final List<String> WeaponNames = list("WeaponNames");
  public static final String StatusBarCash = text("StatusBarCash");
  public static final String StatusBarBays = text("StatusBarBays");
  public static final String StatusBarCosts = text("StatusBarCosts");
  public static final String StatusBarNoGame = text("StatusBarNoGame");
  public static final String CommanderBountyOffered = text("CommanderBountyOffered");
  public static final String CommanderAngryKingpins = text("CommanderAngryKingpins");
  public static final String SkillWithShipBonus = text("SkillWithShipBonus");
  public static final String NoTrade = text("NoTrade");
  public static final String NotSold = text("NotSold");
  public static final String CargoSellAll = text("CargoSellAll");
  public static final String CargoDumpButton = text("CargoDumpButton");
  public static final String CargoTargetPriceUnknown = text("CargoTargetPriceUnknown");
  public static final String CargoTargetDiffUnknown = text("CargoTargetDiffUnknown");
  public static final String CargoTargetPctUnknown = text("CargoTargetPctUnknown");
  public static final String DockFuelStatus = text("DockFuelStatus");
  public static final String DockFuelCost = text("DockFuelCost");
  public static final String DockTankFull = text("DockTankFull");
  public static final String DockHullStatus = text("DockHullStatus");
  public static final String DockRepairCost = text("DockRepairCost");
  public static final String DockNoRepairs = text("DockNoRepairs");
  public static final String InsuranceButton = text("InsuranceButton");
  public static final String InsuranceBuy = text("InsuranceBuy");
  public static final String InsuranceStop = text("InsuranceStop");
  public static final String ShipHullLabel = text("ShipHullLabel");
  public static final String ShipHullHardened = text("ShipHullHardened");
  public static final String ShipEquipmentLabel = text("ShipEquipmentLabel");
  public static final String ShipUnfilledLabel = text("ShipUnfilledLabel");
  public static final String ShipWeaponSlot = text("ShipWeaponSlot");
  public static final String ShipShieldSlot = text("ShipShieldSlot");
  public static final String ShipGadgetSlot = text("ShipGadgetSlot");
  public static final String ShipBayUnit = text("ShipBayUnit");
}
