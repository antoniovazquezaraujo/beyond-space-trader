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
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.enums.StarSystemId;
import java.util.ArrayList;
import java.util.List;


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
    view.render(new QuestsViewModel(questEntries()));
  }

  public void selectSystem(String systemName) {
    game.setSelectedSystemByName(systemName);
  }

  private List<QuestsViewModel.Entry> questEntries() {
    List<QuestsViewModel.Entry> quests = new ArrayList<>(12);
    if(game.getQuestStatusGemulon() > SpecialEvent.StatusGemulonNotStarted && game.getQuestStatusGemulon() < SpecialEvent.StatusGemulonDate) {
      if(game.getQuestStatusGemulon() == SpecialEvent.StatusGemulonDate - 1) {
        quests.add(quest(Strings.QuestGemulonInformTomorrow, StarSystemId.Gemulon));
      } else {
        quests.add(quest(Functions.StringVars(Strings.QuestGemulonInformDays,
            Functions.Multiples(SpecialEvent.StatusGemulonDate - game.getQuestStatusGemulon(), Strings.TimeUnit)),
            StarSystemId.Gemulon));
      }
    } else if(game.getQuestStatusGemulon() == SpecialEvent.StatusGemulonFuel) {
      quests.add(quest(Strings.QuestGemulonFuel, StarSystemId.Gemulon));
    }
    if(game.getQuestStatusExperiment() > SpecialEvent.StatusExperimentNotStarted && game.getQuestStatusExperiment() < SpecialEvent.StatusExperimentDate) {
      if(game.getQuestStatusExperiment() == SpecialEvent.StatusExperimentDate - 1) {
        quests.add(quest(Strings.QuestExperimentInformTomorrow, StarSystemId.Daled));
      } else {
        quests.add(quest(Functions.StringVars(Strings.QuestExperimentInformDays,
            Functions.Multiples(SpecialEvent.StatusExperimentDate - game.getQuestStatusExperiment(), Strings.TimeUnit)),
            StarSystemId.Daled));
      }
    }
    if(game.Commander().getShip().ReactorOnBoard()) {
      if(game.getQuestStatusReactor() == SpecialEvent.StatusReactorFuelOk) {
        quests.add(quest(Strings.QuestReactor, StarSystemId.Nix));
      } else {
        quests.add(quest(Strings.QuestReactorFuel, StarSystemId.Nix));
      }
    } else if(game.getQuestStatusReactor() == SpecialEvent.StatusReactorDelivered) {
      quests.add(quest(Strings.QuestReactorLaser, StarSystemId.Nix));
    }
    if(game.getQuestStatusSpaceMonster() == SpecialEvent.StatusSpaceMonsterAtAcamar) {
      quests.add(quest(Strings.QuestSpaceMonsterKill, StarSystemId.Acamar));
    }
    if(game.getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
      quests.add(quest(Strings.QuestJaporiDeliver, StarSystemId.Japori));
    }
    switch(game.getQuestStatusDragonfly()) {
      case SpecialEvent.StatusDragonflyFlyBaratas:
        quests.add(quest(Strings.QuestDragonflyBaratas, StarSystemId.Baratas));
        break;
      case SpecialEvent.StatusDragonflyFlyMelina:
        quests.add(quest(Strings.QuestDragonflyMelina, StarSystemId.Melina));
        break;
      case SpecialEvent.StatusDragonflyFlyRegulas:
        quests.add(quest(Strings.QuestDragonflyRegulas, StarSystemId.Regulas));
        break;
      case SpecialEvent.StatusDragonflyFlyZalkon:
        quests.add(quest(Strings.QuestDragonflyZalkon, StarSystemId.Zalkon));
        break;
      case SpecialEvent.StatusDragonflyDestroyed:
        quests.add(quest(Strings.QuestDragonflyShield, StarSystemId.Zalkon));
        break;
      default:
        break;
    }
    switch(game.getQuestStatusPrincess()) {
      case SpecialEvent.StatusPrincessFlyCentauri:
        quests.add(quest(Strings.QuestPrincessCentauri, StarSystemId.Centauri));
        break;
      case SpecialEvent.StatusPrincessFlyInthara:
        quests.add(quest(Strings.QuestPrincessInthara, StarSystemId.Inthara));
        break;
      case SpecialEvent.StatusPrincessFlyQonos:
        quests.add(quest(Strings.QuestPrincessQonos, StarSystemId.Qonos));
        break;
      case SpecialEvent.StatusPrincessRescued:
        if(game.Commander().getShip().PrincessOnBoard()) {
          if(game.getQuestStatusPrincess() == SpecialEvent.StatusPrincessImpatient) {
            quests.add(quest(Functions.StringVars(Strings.QuestPrincessReturningImpatient,
                game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()), StarSystemId.Galvon));
          } else {
            quests.add(quest(Functions.StringVars(Strings.QuestPrincessReturning,
                game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()), StarSystemId.Galvon));
          }
        } else {
          quests.add(quest(Functions.StringVars(Strings.QuestPrincessReturn,
              game.Mercenaries()[CrewMemberId.Princess.CastToInt()].Name()), StarSystemId.Galvon));
        }
        break;
      case SpecialEvent.StatusPrincessReturned:
        quests.add(quest(Strings.QuestPrincessQuantum, StarSystemId.Galvon));
        break;
      default:
        break;
    }
    if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabHunting) {
      quests.add(questAt(Strings.QuestScarabFind, scarabLocation()));
    } else if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDestroyed) {
      StarSystem upgrade = location(SpecialEventType.ScarabUpgradeHull);
      if(upgrade == null) {
        StarSystem destroyed = scarabLocation();
        quests.add(questAt(Functions.StringVars(Strings.QuestScarabNotify, destroyed.Name()), destroyed));
      } else {
        quests.add(questAt(Functions.StringVars(Strings.QuestScarabHull, upgrade.Name()), upgrade));
      }
    }
    if(game.Commander().getShip().SculptureOnBoard()) {
      quests.add(quest(Strings.QuestSculpture, StarSystemId.Endor));
    } else if(game.getQuestStatusSculpture() == SpecialEvent.StatusSculptureDelivered) {
      quests.add(quest(Strings.QuestSculptureHiddenBays, StarSystemId.Endor));
    }
    if(game.getQuestStatusArtifact() == SpecialEvent.StatusArtifactOnBoard) {
      quests.add(questAt(Strings.QuestArtifact, location(SpecialEventType.ArtifactDelivery)));
    }
    if(game.Commander().getShip().JarekOnBoard()) {
      if(game.getQuestStatusJarek() == SpecialEvent.StatusJarekImpatient) {
        quests.add(quest(Strings.QuestJarekImpatient, StarSystemId.Devidia));
      } else {
        quests.add(quest(Strings.QuestJarek, StarSystemId.Devidia));
      }
    }
    if(game.Commander().getShip().WildOnBoard()) {
      if(game.getQuestStatusWild() == SpecialEvent.StatusWildImpatient) {
        quests.add(quest(Strings.QuestWildImpatient, StarSystemId.Kravat));
      } else {
        quests.add(quest(Strings.QuestWild, StarSystemId.Kravat));
      }
    }
    if(game.Commander().getShip().getTribbles() > 0) {
      quests.add(questAt(Strings.QuestTribbles, null));
    }
    if(game.getQuestStatusMoon() == SpecialEvent.StatusMoonBought) {
      quests.add(quest(Strings.QuestMoon, StarSystemId.Utopia));
    }
    return quests;
  }

  /** The destination of the fixed quests: the name of that system in this universe. */
  private QuestsViewModel.Entry quest(String text, StarSystemId id) {
    return new QuestsViewModel.Entry(text, game.Universe()[id.CastToInt()].Name());
  }

  /** The destination of the dynamic quests: the system of a placed event, or none. */
  private static QuestsViewModel.Entry questAt(String text, StarSystem location) {
    return new QuestsViewModel.Entry(text, location == null ? null : location.Name());
  }

  /** The wormhole system where the Scarab hides and can be destroyed. */
  private StarSystem scarabLocation() {
    return location(SpecialEventType.ScarabDestroyed);
  }

  private StarSystem location(SpecialEventType type) {
    return Consts.SpecialEvents.get(type.CastToInt()).Location(game.Universe());
  }
}
