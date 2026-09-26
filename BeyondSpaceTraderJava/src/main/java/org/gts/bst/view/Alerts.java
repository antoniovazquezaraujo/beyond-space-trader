/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import spacetrader.Strings;
import spacetrader.enums.AlertType;


/**
 * The predefined alerts (title, message and buttons), loaded from the strings
 * bundle. Both front-ends show the same definitions; the image alerts
 * (AppStart and the game-end ones) only have a title, see {@link #title(AlertType)}.
 */
public final class Alerts {
  private static final Map<AlertType, AlertDefinition> DEFINITIONS = build();

  private Alerts() {
  }

  /**
   * The definition of an alert, or {@code null} for the image alerts.
   */
  public static AlertDefinition get(AlertType type) {
    return DEFINITIONS.get(type);
  }

  /**
   * The title of any alert, including the image ones.
   */
  public static String title(AlertType type) {
    return Strings.text("Alert." + type + ".title");
  }

  private static Map<AlertType, AlertDefinition> build() {
    Map<AlertType, AlertDefinition> definitions = new EnumMap<>(AlertType.class);
    definitions.put(AlertType.Alert, definition("Alert", DialogResult.OK));
    definitions.put(AlertType.AntidoteOnBoard, definition("AntidoteOnBoard", DialogResult.OK));
    definitions.put(AlertType.AntidoteDestroyed, definition("AntidoteDestroyed", DialogResult.OK));
    definitions.put(AlertType.AntidoteTaken, definition("AntidoteTaken", DialogResult.OK));
    definitions.put(AlertType.ArrivalBuyNewspaper, definition("ArrivalBuyNewspaper", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.ArrivalIFFuel, definition("ArrivalIFFuel", DialogResult.OK));
    definitions.put(AlertType.ArrivalIFFuelRepairs, definition("ArrivalIFFuelRepairs", DialogResult.OK));
    definitions.put(AlertType.ArrivalIFNewspaper, definition("ArrivalIFNewspaper", DialogResult.OK));
    definitions.put(AlertType.ArrivalIFRepairs, definition("ArrivalIFRepairs", DialogResult.OK));
    definitions.put(AlertType.ArtifactLost, definition("ArtifactLost", DialogResult.OK));
    definitions.put(AlertType.ArtifactRelinquished, definition("ArtifactRelinquished", DialogResult.OK));
    definitions.put(AlertType.CargoIF, definition("CargoIF", DialogResult.OK));
    definitions.put(AlertType.CargoNoEmptyBays, definition("CargoNoEmptyBays", DialogResult.OK));
    definitions.put(AlertType.CargoNoneAvailable, definition("CargoNoneAvailable", DialogResult.OK));
    definitions.put(AlertType.CargoNoneToSell, definition("CargoNoneToSell", DialogResult.OK));
    definitions.put(AlertType.CargoNotInterested, definition("CargoNotInterested", DialogResult.OK));
    definitions.put(AlertType.CargoNotSold, definition("CargoNotSold", DialogResult.OK));
    definitions.put(AlertType.ChartJump, definition("ChartJump", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.ChartJumpCurrent, definition("ChartJumpCurrent", DialogResult.OK));
    definitions.put(AlertType.ChartJumpNoSystemSelected, definition("ChartJumpNoSystemSelected", DialogResult.OK));
    definitions.put(AlertType.ChartTrackSystem, definition("ChartTrackSystem", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.ChartWormholeUnreachable, definition("ChartWormholeUnreachable", DialogResult.OK));
    definitions.put(AlertType.Cheater, definition("Cheater", DialogResult.OK));
    definitions.put(AlertType.CrewFireMercenary, definition("CrewFireMercenary", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.CrewNoQuarters, definition("CrewNoQuarters", DialogResult.OK));
    definitions.put(AlertType.DebtNoBuy, definition("DebtNoBuy", DialogResult.OK));
    definitions.put(AlertType.DebtNone, definition("DebtNone", DialogResult.OK));
    definitions.put(AlertType.DebtReminder, definition("DebtReminder", DialogResult.OK));
    definitions.put(AlertType.DebtTooLargeGrounded, definition("DebtTooLargeGrounded", DialogResult.OK));
    definitions.put(AlertType.DebtTooLargeLoan, definition("DebtTooLargeLoan", DialogResult.OK));
    definitions.put(AlertType.DebtTooLargeTrade, definition("DebtTooLargeTrade", DialogResult.OK));
    definitions.put(AlertType.DebtWarning, definition("DebtWarning", DialogResult.OK));
    definitions.put(AlertType.Egg, definition("Egg", DialogResult.OK));
    definitions.put(AlertType.EncounterAliensSurrender, definition("EncounterAliensSurrender", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterArrested, definition("EncounterArrested", DialogResult.OK));
    definitions.put(AlertType.EncounterAttackCaptain, definition("EncounterAttackCaptain", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterAttackNoDisruptors, definition("EncounterAttackNoDisruptors", DialogResult.OK));
    definitions.put(AlertType.EncounterAttackNoLasers, definition("EncounterAttackNoLasers", DialogResult.OK));
    definitions.put(AlertType.EncounterAttackNoWeapons, definition("EncounterAttackNoWeapons", DialogResult.OK));
    definitions.put(AlertType.EncounterAttackPolice, definition("EncounterAttackPolice", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterAttackTrader, definition("EncounterAttackTrader", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterBothDestroyed, definition("EncounterBothDestroyed", DialogResult.OK));
    definitions.put(AlertType.EncounterDisabledOpponent, definition("EncounterDisabledOpponent", DialogResult.OK));
    definitions.put(AlertType.EncounterDrinkContents, definition("EncounterDrinkContents", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterDumpAll, definition("EncounterDumpAll", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterDumpWarning, definition("EncounterDumpWarning", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterEscaped, definition("EncounterEscaped", DialogResult.OK));
    definitions.put(AlertType.EncounterEscapedHit, definition("EncounterEscapedHit", DialogResult.OK));
    definitions.put(AlertType.EncounterEscapePodActivated, definition("EncounterEscapePodActivated", DialogResult.OK));
    definitions.put(AlertType.EncounterLooting, definition("EncounterLooting", DialogResult.OK));
    definitions.put(AlertType.EncounterMarieCeleste, definition("EncounterMarieCeleste", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterMarieCelesteNoBribe, definition("EncounterMarieCelesteNoBribe", DialogResult.OK));
    definitions.put(AlertType.EncounterOpponentEscaped, definition("EncounterOpponentEscaped", DialogResult.OK));
    definitions.put(AlertType.EncounterPiratesBounty, definition("EncounterPiratesBounty", DialogResult.OK));
    definitions.put(AlertType.EncounterPiratesExamineReactor, definition("EncounterPiratesExamineReactor", DialogResult.OK));
    definitions.put(AlertType.EncounterPiratesFindNoCargo, definition("EncounterPiratesFindNoCargo", DialogResult.OK));
    definitions.put(AlertType.EncounterPiratesSurrenderPrincess, definition("EncounterPiratesSurrenderPrincess", DialogResult.OK));
    definitions.put(AlertType.EncounterPiratesTakeSculpture, definition("EncounterPiratesTakeSculpture", DialogResult.OK));
    definitions.put(AlertType.EncounterPoliceBribe, definition("EncounterPoliceBribe", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterPoliceBribeCant, definition("EncounterPoliceBribeCant", DialogResult.OK));
    definitions.put(AlertType.EncounterPoliceBribeLowCash, definition("EncounterPoliceBribeLowCash", DialogResult.OK));
    definitions.put(AlertType.EncounterPoliceFine, definition("EncounterPoliceFine", DialogResult.OK));
    definitions.put(AlertType.EncounterPoliceNothingFound, definition("EncounterPoliceNothingFound", DialogResult.OK));
    definitions.put(AlertType.EncounterPoliceNothingIllegal, definition("EncounterPoliceNothingIllegal", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterPoliceSubmit, definition("EncounterPoliceSubmit", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterPoliceSurrender, definition("EncounterPoliceSurrender", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterPostMarie, definition("EncounterPostMarie", DialogResult.OK));
    definitions.put(AlertType.EncounterPostMarieFlee, definition("EncounterPostMarieFlee", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterScoop, definition("EncounterScoop", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterScoopNoRoom, definition("EncounterScoopNoRoom", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EncounterScoopNoScoop, definition("EncounterScoopNoScoop", DialogResult.OK));
    definitions.put(AlertType.EncounterSurrenderRefused, definition("EncounterSurrenderRefused", DialogResult.OK));
    definitions.put(AlertType.EncounterTonicConsumedGood, definition("EncounterTonicConsumedGood", DialogResult.OK));
    definitions.put(AlertType.EncounterTonicConsumedStrange, definition("EncounterTonicConsumedStrange", DialogResult.OK));
    definitions.put(AlertType.EncounterTradeCompleted, definition("EncounterTradeCompleted", DialogResult.OK));
    definitions.put(AlertType.EncounterYouLose, definition("EncounterYouLose", DialogResult.OK));
    definitions.put(AlertType.EncounterYouWin, definition("EncounterYouWin", DialogResult.OK));
    definitions.put(AlertType.EquipmentAlreadyOwn, definition("EquipmentAlreadyOwn", DialogResult.OK));
    definitions.put(AlertType.EquipmentBuy, definition("EquipmentBuy", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EquipmentEscapePod, definition("EquipmentEscapePod", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.EquipmentExtraBaysInUse, definition("EquipmentExtraBaysInUse", DialogResult.OK));
    definitions.put(AlertType.EquipmentFuelCompactor, definition("EquipmentFuelCompactor", DialogResult.OK));
    definitions.put(AlertType.EquipmentHiddenCompartments, definition("EquipmentHiddenCompartments", DialogResult.OK));
    definitions.put(AlertType.EquipmentIF, definition("EquipmentIF", DialogResult.OK));
    definitions.put(AlertType.EquipmentLightningShield, definition("EquipmentLightningShield", DialogResult.OK));
    definitions.put(AlertType.EquipmentMorgansLaser, definition("EquipmentMorgansLaser", DialogResult.OK));
    definitions.put(AlertType.EquipmentNotEnoughSlots, definition("EquipmentNotEnoughSlots", DialogResult.OK));
    definitions.put(AlertType.EquipmentQuantumDisruptor, definition("EquipmentQuantumDisruptor", DialogResult.OK));
    definitions.put(AlertType.EquipmentSell, definition("EquipmentSell", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.FileErrorOpen, definition("FileErrorOpen", DialogResult.OK));
    definitions.put(AlertType.FileErrorSave, definition("FileErrorSave", DialogResult.OK));
    definitions.put(AlertType.FleaBuilt, definition("FleaBuilt", DialogResult.OK));
    definitions.put(AlertType.GameAbandonConfirm, definition("GameAbandonConfirm", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.GameClearHighScores, definition("GameClearHighScores", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.GameEndHighScoreAchieved, definition("GameEndHighScoreAchieved", DialogResult.OK));
    definitions.put(AlertType.GameEndHighScoreCheat, definition("GameEndHighScoreCheat", DialogResult.OK));
    definitions.put(AlertType.GameEndHighScoreMissed, definition("GameEndHighScoreMissed", DialogResult.OK));
    definitions.put(AlertType.GameEndScore, definition("GameEndScore", DialogResult.OK));
    definitions.put(AlertType.GameRetire, definition("GameRetire", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.InsuranceNoEscapePod, definition("InsuranceNoEscapePod", DialogResult.OK));
    definitions.put(AlertType.InsurancePayoff, definition("InsurancePayoff", DialogResult.OK));
    definitions.put(AlertType.InsuranceStop, definition("InsuranceStop", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.JailConvicted, definition("JailConvicted", DialogResult.OK));
    definitions.put(AlertType.JailFleaReceived, definition("JailFleaReceived", DialogResult.OK));
    definitions.put(AlertType.JailHiddenCargoBaysRemoved, definition("JailHiddenCargoBaysRemoved", DialogResult.OK));
    definitions.put(AlertType.JailIllegalGoodsImpounded, definition("JailIllegalGoodsImpounded", DialogResult.OK));
    definitions.put(AlertType.JailInsuranceLost, definition("JailInsuranceLost", DialogResult.OK));
    definitions.put(AlertType.JailMercenariesLeave, definition("JailMercenariesLeave", DialogResult.OK));
    definitions.put(AlertType.JailShipSold, definition("JailShipSold", DialogResult.OK));
    definitions.put(AlertType.JarekTakenHome, definition("JarekTakenHome", DialogResult.OK));
    definitions.put(AlertType.LeavingIFInsurance, definition("LeavingIFInsurance", DialogResult.OK));
    definitions.put(AlertType.LeavingIFMercenaries, definition("LeavingIFMercenaries", DialogResult.OK));
    definitions.put(AlertType.LeavingIFWormholeTax, definition("LeavingIFWormholeTax", DialogResult.OK));
    definitions.put(AlertType.MeetCaptainAhab, definition("MeetCaptainAhab", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.MeetCaptainConrad, definition("MeetCaptainConrad", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.MeetCaptainHuie, definition("MeetCaptainHuie", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.NewGameConfirm, definition("NewGameConfirm", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.NewGameMoreSkillPoints, definition("NewGameMoreSkillPoints", DialogResult.OK));
    definitions.put(AlertType.OptionsNoGame, definition("OptionsNoGame", DialogResult.OK));
    definitions.put(AlertType.PreciousHidden, definition("PreciousHidden", DialogResult.OK));
    definitions.put(AlertType.PrincessTakenHome, definition("PrincessTakenHome", DialogResult.OK));
    definitions.put(AlertType.ReactorConfiscated, definition("ReactorConfiscated", DialogResult.OK));
    definitions.put(AlertType.ReactorDestroyed, definition("ReactorDestroyed", DialogResult.OK));
    definitions.put(AlertType.ReactorOnBoard, definition("ReactorOnBoard", DialogResult.OK));
    definitions.put(AlertType.ReactorMeltdown, definition("ReactorMeltdown", DialogResult.OK));
    definitions.put(AlertType.ReactorWarningFuel, definition("ReactorWarningFuel", DialogResult.OK));
    definitions.put(AlertType.ReactorWarningFuelGone, definition("ReactorWarningFuelGone", DialogResult.OK));
    definitions.put(AlertType.ReactorWarningTemp, definition("ReactorWarningTemp", DialogResult.OK));
    definitions.put(AlertType.RegistryError, definition("RegistryError", DialogResult.OK));
    definitions.put(AlertType.SculptureConfiscated, definition("SculptureConfiscated", DialogResult.OK));
    definitions.put(AlertType.SculptureSaved, definition("SculptureSaved", DialogResult.OK));
    definitions.put(AlertType.ShipBuyConfirm, definition("ShipBuyConfirm", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.ShipBuyCrewQuarters, definition("ShipBuyCrewQuarters", DialogResult.OK));
    definitions.put(AlertType.ShipBuyIF, definition("ShipBuyIF", DialogResult.OK));
    definitions.put(AlertType.ShipBuyIFTransfer, definition("ShipBuyIFTransfer", DialogResult.OK));
    definitions.put(AlertType.ShipBuyNoSlots, definition("ShipBuyNoSlots", DialogResult.OK));
    definitions.put(AlertType.ShipBuyNotAvailable, definition("ShipBuyNotAvailable", DialogResult.OK));
    definitions.put(AlertType.ShipBuyNoTransfer, definition("ShipBuyNoTransfer", DialogResult.OK));
    definitions.put(AlertType.ShipBuyPassengerQuarters, definition("ShipBuyPassengerQuarters", DialogResult.OK));
    definitions.put(AlertType.ShipBuyReactor, definition("ShipBuyReactor", DialogResult.OK));
    definitions.put(AlertType.ShipBuyTransfer, definition("ShipBuyTransfer", DialogResult.Yes, DialogResult.No));
    definitions.put(AlertType.ShipDesignIF, definition("ShipDesignIF", DialogResult.OK));
    definitions.put(AlertType.ShipDesignThanks, definition("ShipDesignThanks", DialogResult.OK));
    definitions.put(AlertType.ShipHullUpgraded, definition("ShipHullUpgraded", DialogResult.OK));
    definitions.put(AlertType.SpecialCleanRecord, definition("SpecialCleanRecord", DialogResult.OK));
    definitions.put(AlertType.SpecialExperimentPerformed, definition("SpecialExperimentPerformed", DialogResult.OK));
    definitions.put(AlertType.SpecialIF, definition("SpecialIF", DialogResult.OK));
    definitions.put(AlertType.SpecialMoonBought, definition("SpecialMoonBought", DialogResult.OK));
    definitions.put(AlertType.SpecialNoQuarters, definition("SpecialNoQuarters", DialogResult.OK));
    definitions.put(AlertType.SpecialNotEnoughBays, definition("SpecialNotEnoughBays", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerConcernedJarek, definition("SpecialPassengerConcernedJarek", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerConcernedPrincess, definition("SpecialPassengerConcernedPrincess", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerConcernedWild, definition("SpecialPassengerConcernedWild", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerImpatientJarek, definition("SpecialPassengerImpatientJarek", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerImpatientPrincess, definition("SpecialPassengerImpatientPrincess", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerImpatientWild, definition("SpecialPassengerImpatientWild", DialogResult.OK));
    definitions.put(AlertType.SpecialPassengerOnBoard, definition("SpecialPassengerOnBoard", DialogResult.OK));
    definitions.put(AlertType.SpecialSealedCanisters, definition("SpecialSealedCanisters", DialogResult.OK));
    definitions.put(AlertType.SpecialSkillIncrease, definition("SpecialSkillIncrease", DialogResult.OK));
    definitions.put(AlertType.SpecialTimespaceFabricRip, definition("SpecialTimespaceFabricRip", DialogResult.OK));
    definitions.put(AlertType.SpecialTrainingCompleted, definition("SpecialTrainingCompleted", DialogResult.OK));
    definitions.put(AlertType.TravelArrival, definition("TravelArrival", DialogResult.OK));
    definitions.put(AlertType.TravelUneventfulTrip, definition("TravelUneventfulTrip", DialogResult.OK));
    definitions.put(AlertType.TribblesAllDied, definition("TribblesAllDied", DialogResult.OK));
    definitions.put(AlertType.TribblesAteFood, definition("TribblesAteFood", DialogResult.OK));
    definitions.put(AlertType.TribblesGone, definition("TribblesGone", DialogResult.OK));
    definitions.put(AlertType.TribblesHalfDied, definition("TribblesHalfDied", DialogResult.OK));
    definitions.put(AlertType.TribblesKilled, definition("TribblesKilled", DialogResult.OK));
    definitions.put(AlertType.TribblesMostDied, definition("TribblesMostDied", DialogResult.OK));
    definitions.put(AlertType.TribblesOwn, definition("TribblesOwn", DialogResult.OK));
    definitions.put(AlertType.TribblesRemoved, definition("TribblesRemoved", DialogResult.OK));
    definitions.put(AlertType.TribblesInspector, definition("TribblesInspector", DialogResult.OK));
    definitions.put(AlertType.TribblesSqueek, definition("TribblesSqueek", DialogResult.OK));
    definitions.put(AlertType.TribblesTradeIn, definition("TribblesTradeIn", DialogResult.OK));
    definitions.put(AlertType.WildArrested, definition("WildArrested", DialogResult.OK));
    definitions.put(AlertType.WildChatsPirates, definition("WildChatsPirates", DialogResult.OK));
    definitions.put(AlertType.WildGoesPirates, definition("WildGoesPirates", DialogResult.OK));
    definitions.put(AlertType.WildLeavesShip, definition("WildLeavesShip", DialogResult.OK));
    definitions.put(AlertType.WildSculpture, definition("WildSculpture", DialogResult.OK));
    definitions.put(AlertType.WildWontBoardLaser, definition("WildWontBoardLaser", DialogResult.OK));
    definitions.put(AlertType.WildWontBoardReactor, definition("WildWontBoardReactor", DialogResult.OK));
    definitions.put(AlertType.WildWontStayAboardLaser, definition("WildWontStayAboardLaser", DialogResult.OK, DialogResult.Cancel));
    definitions.put(AlertType.WildWontStayAboardReactor, definition("WildWontStayAboardReactor", DialogResult.OK, DialogResult.Cancel));
    return Collections.unmodifiableMap(definitions);
  }

  private static AlertDefinition definition(String type, DialogResult result) {
    return new AlertDefinition(Strings.text("Alert." + type + ".title"), Strings.text("Alert." + type + ".message"),
        Strings.text("Alert." + type + ".button1"), result, null, DialogResult.None);
  }

  private static AlertDefinition definition(String type, DialogResult result, DialogResult secondResult) {
    return new AlertDefinition(Strings.text("Alert." + type + ".title"), Strings.text("Alert." + type + ".message"),
        Strings.text("Alert." + type + ".button1"), result, Strings.text("Alert." + type + ".button2"), secondResult);
  }
}
