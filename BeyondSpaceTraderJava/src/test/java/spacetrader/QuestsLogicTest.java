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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.GadgetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spacetrader.enums.AlertType;
import spacetrader.enums.GameEndType;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.TechLevel;


/**
 * Pins the quest logic of {@link Quests}: the special events and the passage of
 * days, plus the {@link Game} delegates. The component is built over the objects
 * of a real game (the universe generation needs a game behind it), but it never
 * sees the facade.
 */
class QuestsLogicTest {
  private TestDialogService dialogs;
  private Game game;
  private Commander cmdr;
  private Universe universe;
  private Market market;
  private Newspaper newspaper;
  private Quests quests;
  private boolean retired;

  @BeforeEach
  void setUp() {
    dialogs = new TestDialogService();
    game = new Game("Test", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
    cmdr = game.Commander();
    universe = Universe.from(game.Universe(), game.Wormholes());
    market = new Market();
    newspaper = new Newspaper();
    retired = false;
    quests = new Quests(cmdr, universe, market, newspaper, game.Mercenaries(), game.SpaceMonster(),
        game.Difficulty(), dialogs, () -> retired = true);
  }

  // HandleSpecialEvent: the switch and its effects on the quest, the commander and the ship.

  @Test
  void cargoForSaleLoadsCanistersAndChargesTheStory() {
    StarSystem system = eventSystem(SpecialEventType.CargoForSale);
    int cargo = total(cmdr.getShip().Cargo());
    int paid = total(cmdr.PriceCargo());

    quests.handleSpecialEvent();

    assertEquals(cargo + 3, total(cmdr.getShip().Cargo()));
    assertEquals(paid + eventPrice(SpecialEventType.CargoForSale), total(cmdr.PriceCargo()),
        "what the commander paid for the canisters must follow the story");
    assertTrue(dialogs.alerts().contains(AlertType.SpecialSealedCanisters));
    assertEquals(SpecialEventType.NA, system.SpecialEventType());
  }

  @Test
  void eraseRecordCleansThePoliceRecordAndRaisesTheSellPrices() {
    eventSystem(SpecialEventType.EraseRecord);
    cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreVillain);
    market.sell()[0] = 90;

    quests.handleSpecialEvent();

    assertEquals(Consts.PoliceRecordScoreClean, cmdr.getPoliceRecordScore());
    assertEquals(100, market.sell()[0], "the sell prices must be recalculated after the erasure");
    assertTrue(dialogs.alerts().contains(AlertType.SpecialCleanRecord));
  }

  @Test
  void skillTrainingIncreasesARandomSkill() {
    eventSystem(SpecialEventType.Skill);
    int skills = total(cmdr.Skills());

    quests.handleSpecialEvent();

    assertEquals(skills + 1, total(cmdr.Skills()));
    assertTrue(dialogs.alerts().contains(AlertType.SpecialSkillIncrease));
  }

  @Test
  void theMoonOfferIsBoughtWithItsPrice() {
    eventSystem(SpecialEventType.Moon);
    cmdr.setCash(600000);

    quests.handleSpecialEvent();

    assertEquals(SpecialEvent.StatusMoonBought, quests.questStatusMoon());
    assertEquals(100000, cmdr.getCash(), "the moon price must be paid");
  }

  @Test
  void moonRetirementRunsTheRetirementCallback() {
    eventSystem(SpecialEventType.MoonRetirement);
    quests.questStatusMoon(SpecialEvent.StatusMoonBought);

    quests.handleSpecialEvent();

    assertTrue(retired, "the component must retire through the game callback");
    assertEquals(SpecialEvent.StatusMoonDone, quests.questStatusMoon());
  }

  @Test
  void anInstallationWithAFreeSlotInstallsTheRewardAndEndsTheQuest() {
    StarSystem system = eventSystem(SpecialEventType.GemulonFuel);

    quests.handleSpecialEvent();

    assertTrue(cmdr.getShip().HasGadget(GadgetType.FuelCompactor));
    assertEquals(SpecialEvent.StatusGemulonDone, quests.questStatusGemulon());
    assertEquals(SpecialEventType.NA, system.SpecialEventType());
    assertTrue(dialogs.alerts().contains(AlertType.EquipmentFuelCompactor));
  }

  @Test
  void anInstallationWithoutSlotsWarnsAndKeepsTheOffer() {
    cmdr.getShip().AddEquipment(Consts.Gadgets.get(GadgetType.TargetingSystem.asInteger()));
    StarSystem system = eventSystem(SpecialEventType.GemulonFuel);

    quests.handleSpecialEvent();

    assertFalse(cmdr.getShip().HasGadget(GadgetType.FuelCompactor));
    assertEquals(SpecialEventType.GemulonFuel, system.SpecialEventType(), "the offer must stay in the system");
    assertTrue(dialogs.alerts().contains(AlertType.EquipmentNotEnoughSlots));
  }

  @Test
  void aDragonflyStageAdvancesTheMissionAndLeavesTheSystem() {
    StarSystem system = eventSystem(SpecialEventType.DragonflyBaratas);
    quests.questStatusDragonfly(SpecialEvent.StatusDragonflyFlyBaratas);

    quests.handleSpecialEvent();

    assertEquals(SpecialEvent.StatusDragonflyFlyMelina, quests.questStatusDragonfly());
    assertEquals(SpecialEventType.NA, system.SpecialEventType());
  }

  @Test
  void aDeliveryAdvancesTheMissionAndKeepsTheChainInTheSystem() {
    StarSystem system = eventSystem(SpecialEventType.SculptureDelivered);

    quests.handleSpecialEvent();

    assertEquals(SpecialEvent.StatusSculptureDelivered, quests.questStatusSculpture());
    assertEquals(SpecialEventType.SculptureHiddenBays, system.SpecialEventType(), "the chain must stay in the system");
  }

  // IncDays: the drift, the regeneration and the mission timers.

  @Test
  void daysDriftThePoliceRecordTowardsNeutral() {
    cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreClean + 30);

    quests.incDays(9);

    assertEquals(Consts.PoliceRecordScoreClean + 27, cmdr.getPoliceRecordScore());
    assertEquals(9, cmdr.getDays());
  }

  @Test
  void daysDriftTheCriminalRecordTowardsNeutral() {
    cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreDubious - 6);

    quests.incDays(3);

    assertEquals(Consts.PoliceRecordScoreDubious - 3, cmdr.getPoliceRecordScore(),
        "at normal difficulty the record drifts one point per day");
  }

  @Test
  void theSpaceMonsterRecoversFivePercentPerDay() {
    game.SpaceMonster().setHull(100);

    quests.incDays(1);

    assertEquals(105, game.SpaceMonster().getHull());
  }

  @Test
  void reachingTheDateInvadesGemulon() {
    quests.questStatusGemulon(SpecialEvent.StatusGemulonDate);
    StarSystem gemulon = game.Universe()[StarSystemId.Gemulon.CastToInt()];

    quests.incDays(1);

    assertEquals(SpecialEvent.StatusGemulonTooLate, quests.questStatusGemulon());
    assertEquals(SpecialEventType.GemulonInvaded, gemulon.SpecialEventType());
    assertEquals(TechLevel.t0, gemulon.TechLevel());
    assertEquals(PoliticalSystemType.Anarchy, gemulon.PoliticalSystemType());
  }

  @Test
  void theExperimentEndsWithTheFabricRipAndTheFailureOnDaled() {
    quests.questStatusExperiment(SpecialEvent.StatusExperimentDate);
    StarSystem daled = game.Universe()[StarSystemId.Daled.CastToInt()];

    quests.incDays(1);

    assertEquals(SpecialEvent.StatusExperimentPerformed, quests.questStatusExperiment());
    assertEquals(Consts.FabricRipInitialProbability, quests.fabricRipProbability());
    assertEquals(SpecialEventType.ExperimentFailed, daled.SpecialEventType());
    assertTrue(dialogs.alerts().contains(AlertType.SpecialExperimentPerformed));
    assertTrue(newspaper.events().contains(NewsEvent.ExperimentPerformed.CastToInt()),
        "the performed experiment must make the news");
  }

  @Test
  void jarekWarnsAndLosesHisSkillsWhenHeIsImpatient() {
    hire(CrewMemberId.Jarek);
    quests.questStatusJarek(SpecialEvent.StatusJarekImpatient / 2);

    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerConcernedJarek));
    assertEquals(SpecialEvent.StatusJarekImpatient / 2 + 1, quests.questStatusJarek());

    quests.questStatusJarek(SpecialEvent.StatusJarekImpatient - 1);
    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerImpatientJarek));
    assertSkillsAreZero(game.Mercenaries()[CrewMemberId.Jarek.CastToInt()]);
    assertEquals(SpecialEvent.StatusJarekImpatient, quests.questStatusJarek());
  }

  @Test
  void princessWarnsAndLosesHerSkillsWhenSheIsImpatient() {
    hire(CrewMemberId.Princess);
    quests.questStatusPrincess((SpecialEvent.StatusPrincessImpatient + SpecialEvent.StatusPrincessRescued) / 2);

    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerConcernedPrincess));

    quests.questStatusPrincess(SpecialEvent.StatusPrincessImpatient - 1);
    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerImpatientPrincess));
    assertSkillsAreZero(game.Mercenaries()[CrewMemberId.Princess.CastToInt()]);
    assertEquals(SpecialEvent.StatusPrincessImpatient, quests.questStatusPrincess());
  }

  @Test
  void wildWarnsAndLosesHisSkillsWhenHeIsImpatient() {
    hire(CrewMemberId.Wild);
    quests.questStatusWild(SpecialEvent.StatusWildImpatient / 2);

    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerConcernedWild));

    quests.questStatusWild(SpecialEvent.StatusWildImpatient - 1);
    quests.incDays(1);

    assertTrue(dialogs.alerts().contains(AlertType.SpecialPassengerImpatientWild));
    assertSkillsAreZero(game.Mercenaries()[CrewMemberId.Wild.CastToInt()]);
    assertEquals(SpecialEvent.StatusWildImpatient, quests.questStatusWild());
  }

  // The Game delegates.

  @Test
  void gameHandleSpecialEventEndsTheGameOnMoonRetirement() {
    cmdr.CurrentSystem().SpecialEventType(SpecialEventType.MoonRetirement);
    game.setQuestStatusMoon(SpecialEvent.StatusMoonBought);

    assertThrows(GameEndException.class, () -> game.HandleSpecialEvent());

    assertEquals(GameEndType.BoughtMoon, game.getEndStatus(),
        "the retirement callback must throw the end-of-game exception");
    assertEquals(SpecialEvent.StatusMoonDone, game.getQuestStatusMoon());
  }

  @Test
  void gameIncDaysDelegatesToTheQuestComponent() {
    game.IncDays(3);

    assertEquals(3, game.Commander().getDays());
  }

  private StarSystem eventSystem(SpecialEventType type) {
    StarSystem system = cmdr.CurrentSystem();
    system.SpecialEventType(type);
    return system;
  }

  private int eventPrice(SpecialEventType type) {
    return Consts.SpecialEvents.get(type.CastToInt()).Price();
  }

  /** Replaces the commander's ship with one with free crew quarters and hires the passenger. */
  private void hire(CrewMemberId id) {
    cmdr.setShip(new Ship(ShipType.Grasshopper));
    cmdr.getShip().Hire(game.Mercenaries()[id.CastToInt()]);
  }

  private static int total(int[] values) {
    int total = 0;
    for(int value : values) {
      total += value;
    }
    return total;
  }

  private static void assertSkillsAreZero(CrewMember member) {
    for(int skill : member.Skills()) {
      assertEquals(0, skill, "the impatient passenger must lose every skill");
    }
  }
}
