/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.view.QuestsView;
import org.gts.bst.view.QuestsViewModel;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.SpecialEvent;
import spacetrader.Strings;
import java.util.ArrayList;
import spacetrader.util.Util;


/**
 * Fills the quests screen from the model. No front-end types involved.
 */
public class QuestsPresenter {
  private final Game game;
  private final QuestsView view;

  public QuestsPresenter(Game game, QuestsView view) {
    this.game = game;
    this.view = view;
  }

  public void update() {
    String[] quests = questStrings();
    view.render(new QuestsViewModel(
        quests.length == 0 ? Strings.QuestNone : Util.StringsJoin(Strings.newline + Strings.newline, quests),
        quests.length > 0));
  }

  public void selectSystem(String systemName) {
    game.setSelectedSystemByName(systemName);
  }

  private String[] questStrings() {
    ArrayList<String> quests = new ArrayList<>(12);
    if(game.getQuestStatusGemulon() > SpecialEvent.StatusGemulonNotStarted && game.getQuestStatusGemulon() < SpecialEvent.StatusGemulonDate) {
      if(game.getQuestStatusGemulon() == SpecialEvent.StatusGemulonDate - 1) {
        quests.add(Strings.QuestGemulonInformTomorrow);
      } else {
        quests.add(Functions.StringVars(Strings.QuestGemulonInformDays, Functions.Multiples(SpecialEvent.StatusGemulonDate - game.getQuestStatusGemulon(), Strings.TimeUnit)));
      }
    } else if(game.getQuestStatusGemulon() == SpecialEvent.StatusGemulonFuel) {
      quests.add(Strings.QuestGemulonFuel);
    }
    if(game.getQuestStatusExperiment() > SpecialEvent.StatusExperimentNotStarted && game.getQuestStatusExperiment() < SpecialEvent.StatusExperimentDate) {
      if(game.getQuestStatusExperiment() == SpecialEvent.StatusExperimentDate - 1) {
        quests.add(Strings.QuestExperimentInformTomorrow);
      } else {
        quests.add(Functions.StringVars(Strings.QuestExperimentInformDays, Functions.Multiples(SpecialEvent.StatusExperimentDate - game.getQuestStatusExperiment(), Strings.TimeUnit)));
      }
    }
    if(game.Commander().getShip().ReactorOnBoard()) {
      if(game.getQuestStatusReactor() == SpecialEvent.StatusReactorFuelOk) {
        quests.add(Strings.QuestReactor);
      } else {
        quests.add(Strings.QuestReactorFuel);
      }
    } else if(game.getQuestStatusReactor() == SpecialEvent.StatusReactorDelivered) {
      quests.add(Strings.QuestReactorLaser);
    }
    if(game.getQuestStatusSpaceMonster() == SpecialEvent.StatusSpaceMonsterAtAcamar) {
      quests.add(Strings.QuestSpaceMonsterKill);
    }
    if(game.getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
      quests.add(Strings.QuestJaporiDeliver);
    }
    switch(game.getQuestStatusDragonfly()) {
      case SpecialEvent.StatusDragonflyFlyBaratas:
        quests.add(Strings.QuestDragonflyBaratas);
        break;
      case SpecialEvent.StatusDragonflyFlyMelina:
        quests.add(Strings.QuestDragonflyMelina);
        break;
      case SpecialEvent.StatusDragonflyFlyRegulas:
        quests.add(Strings.QuestDragonflyRegulas);
        break;
      case SpecialEvent.StatusDragonflyFlyZalkon:
        quests.add(Strings.QuestDragonflyZalkon);
        break;
      case SpecialEvent.StatusDragonflyDestroyed:
        quests.add(Strings.QuestDragonflyShield);
        break;
      default:
        break;
    }
    switch(game.getQuestStatusPrincess()) {
      case SpecialEvent.StatusPrincessFlyCentauri:
        quests.add(Strings.QuestPrincessCentauri);
        break;
      case SpecialEvent.StatusPrincessFlyInthara:
        quests.add(Strings.QuestPrincessInthara);
        break;
      case SpecialEvent.StatusPrincessFlyQonos:
        quests.add(Strings.QuestPrincessQonos);
        break;
      case SpecialEvent.StatusPrincessRescued:
        if(game.Commander().getShip().PrincessOnBoard()) {
          if(game.getQuestStatusPrincess() == SpecialEvent.StatusPrincessImpatient) {
            quests.add(Functions.StringVars(Strings.QuestPrincessReturningImpatient, game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()));
          } else {
            quests.add(Functions.StringVars(Strings.QuestPrincessReturning, game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()));
          }
        } else {
          quests.add(Functions.StringVars(Strings.QuestPrincessReturn, game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()));
        }
        break;
      case SpecialEvent.StatusPrincessReturned:
        quests.add(Strings.QuestPrincessQuantum);
        break;
      default:
        break;
    }
    if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabHunting) {
      quests.add(Strings.QuestScarabFind);
    } else if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDestroyed) {
      if(Consts.SpecialEvents.get(SpecialEventType.ScarabUpgradeHull.CastToInt()).Location(game.Universe()) == null) {
        quests.add(Functions.StringVars(Strings.QuestScarabNotify,
            Consts.SpecialEvents.get(SpecialEventType.ScarabDestroyed.CastToInt()).Location(game.Universe()).Name()));
      } else {
        quests.add(Functions.StringVars(Strings.QuestScarabHull,
            Consts.SpecialEvents.get(SpecialEventType.ScarabUpgradeHull.CastToInt()).Location(game.Universe()).Name()));
      }
    }
    if(game.Commander().getShip().SculptureOnBoard()) {
      quests.add(Strings.QuestSculpture);
    } else if(game.getQuestStatusReactor() == SpecialEvent.StatusReactorDelivered) {
      quests.add(Strings.QuestSculptureHiddenBays);
    }
    if(game.getQuestStatusArtifact() == SpecialEvent.StatusArtifactOnBoard) {
      quests.add(Strings.QuestArtifact);
    }
    if(game.Commander().getShip().JarekOnBoard()) {
      if(game.getQuestStatusJarek() == SpecialEvent.StatusJarekImpatient) {
        quests.add(Strings.QuestJarekImpatient);
      } else {
        quests.add(Strings.QuestJarek);
      }
    }
    if(game.Commander().getShip().WildOnBoard()) {
      if(game.getQuestStatusWild() == SpecialEvent.StatusWildImpatient) {
        quests.add(Strings.QuestWildImpatient);
      } else {
        quests.add(Strings.QuestWild);
      }
    }
    if(game.Commander().getShip().getTribbles() > 0) {
      quests.add(Strings.QuestTribbles);
    }
    if(game.getQuestStatusMoon() == SpecialEvent.StatusMoonBought) {
      quests.add(Strings.QuestMoon);
    }
    return Functions.ArrayListtoStringArray(quests);
  }
}

