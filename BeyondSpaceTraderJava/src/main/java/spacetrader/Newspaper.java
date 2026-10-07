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
import java.util.Arrays;
import java.util.List;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import spacetrader.enums.ShipyardId;
import spacetrader.enums.SystemPressure;
import spacetrader.util.Util;


/**
 * The newspaper: the rumours lived through (in order) and whether the paper of
 * the current system is paid for. It owns the state and the logic (the
 * masthead, the text and the arrival rumours); the game only delegates.
 */
public final class Newspaper {
  private final ArrayList<Integer> _newsEvents = new ArrayList<>(30);
  private boolean _paidForNewspaper = false; // once you buy a paper on a system, you don't have to pay again.

  /** The live list of news events, in order. */
  public ArrayList<Integer> events() {
    return _newsEvents;
  }

  /** Replaces the events with the ones read from a saved game. */
  public void events(Integer[] values) {
    _newsEvents.clear();
    _newsEvents.addAll(Arrays.asList(values));
  }

  /** The last news event. Throws an {@link IndexOutOfBoundsException} when there are none. */
  public int latest() {
    return _newsEvents.get(_newsEvents.size() - 1);
  }

  public void add(NewsEvent ne) {
    _newsEvents.add(ne.CastToInt());
  }

  public void replace(int oldEvent, int newEvent) {
    if(_newsEvents.indexOf(oldEvent) >= 0) {
      _newsEvents.remove(oldEvent);
    }
    _newsEvents.add(newEvent);
  }

  public void reset() {
    _newsEvents.clear();
  }

  public boolean paid() {
    return _paidForNewspaper;
  }

  public void paid(boolean paidForNewspaper) {
    _paidForNewspaper = paidForNewspaper;
  }

  /** The masthead of the paper of the commander's current system. */
  public String head(Commander cmdr) {
    List<String> heads = Strings.NewsMastheads.get(cmdr.CurrentSystem().PoliticalSystemType().CastToInt());
    String head = heads.get(cmdr.CurrentSystem().Id().CastToInt() % heads.size());
    return Functions.StringVars(head, cmdr.CurrentSystem().Name());
  }

  /** The text of the paper of the commander's current system. */
  public String text(Commander cmdr, StarSystem[] universe, Difficulty difficulty) {
    StarSystem curSys = cmdr.CurrentSystem();
    ArrayList<String> items = new ArrayList<>();
    // We're using the GetRandom2 function so that the same number is generated each time for the same "version" of the newspaper. -JAF
    Functions.RandSeed(curSys.Id().CastToInt(), cmdr.getDays());
    for(Integer event : _newsEvents) {
      items.add(Functions.StringVars(Strings.NewsEvent.get(event), new String[]{
            cmdr.Name(), cmdr.CurrentSystem().Name(), cmdr.getShip().Name()}));
    }
    if(curSys.SystemPressure() != SystemPressure.None) {
      items.add(Strings.NewsPressureInternal.get(curSys.SystemPressure().CastToInt()));
    }
    if(cmdr.getPoliceRecordScore() <= Consts.PoliceRecordScoreVillain) {
      String baseStr = Strings.NewsPoliceRecordPsychopath.get(Functions.GetRandom2(Strings.NewsPoliceRecordPsychopath.size()));
      items.add(Functions.StringVars(baseStr, cmdr.Name(), curSys.Name()));
    } else if(cmdr.getPoliceRecordScore() >= Consts.PoliceRecordScoreHero) {
      String baseStr = Strings.NewsPoliceRecordHero.get(Functions.GetRandom2(Strings.NewsPoliceRecordHero.size()));
      items.add(Functions.StringVars(baseStr, cmdr.Name(), curSys.Name()));
    }
    // and now, finally, useful news (if any); base probability of a story showing up is (50 / MAXTECHLEVEL) * Current Tech Level
    // This is then modified by adding 10% for every level of play less than Impossible
    boolean realNews = false;
    for(int i = 0; i < universe.length; i++) {
      if(universe[i].DestOk() && universe[i] != curSys) {
        // Special stories that always get shown: moon, millionaire, shipyard
        if(universe[i].SpecialEventType() != SpecialEventType.NA) {
          if(universe[i].SpecialEventType() == SpecialEventType.Moon) {
            items.add(Functions.StringVars(Strings.NewsMoonForSale, universe[i].Name()));
          } else if(universe[i].SpecialEventType() == SpecialEventType.TribbleBuyer) {
            items.add(Functions.StringVars(Strings.NewsTribbleBuyer, universe[i].Name()));
          }
        }
        if(universe[i].ShipyardId() != ShipyardId.NA) {
          items.add(Functions.StringVars(Strings.NewsShipyard, universe[i].Name()));
        }
        // And not-always-shown stories
        if(universe[i].SystemPressure() != SystemPressure.None
            && Functions.GetRandom2(100) <= Consts.StoryProbability * curSys.TechLevel().ordinal() + 10 * (5 - difficulty.CastToInt())) {
          int index = Functions.GetRandom2(Strings.NewsPressureExternal.size());
          String baseStr = Strings.NewsPressureExternal.get(index);
          String pressure = Strings.NewsPressureExternalPressures.get(universe[i].SystemPressure().CastToInt());
          items.add(Functions.StringVars(baseStr, pressure, universe[i].Name()));
          realNews = true;
        }
      }
    }
    // if there's no useful news, we throw up at least one headline from our canned news list.
    if(!realNews) {
      List<String> headlines = Strings.NewsHeadlines.get(curSys.PoliticalSystemType().CastToInt());
      boolean[] shown = new boolean[headlines.size()];
      int toShow = Functions.GetRandom2(headlines.size());
      for(int i = 0; i <= toShow; i++) {
        int index = Functions.GetRandom2(headlines.size());
        if(!shown[index]) {
          items.add(headlines.get(index));
          shown[index] = true;
        }
      }
    }
    return Util.StringsJoin(Strings.newline + Strings.newline, Functions.ArrayListtoStringArray(items));
  }

  /** Adds the rumour the current system tells about when the commander arrives. */
  public void addEventsOnArrival(StarSystem currentSystem, QuestStates quests) {
    if(currentSystem.SpecialEventType() != SpecialEventType.NA) {
      switch(currentSystem.SpecialEventType()) {
        case ArtifactDelivery:
          if(quests.artifactOnBoard()) {
            add(NewsEvent.ArtifactDelivery);
          }
          break;
        case Dragonfly:
          add(NewsEvent.Dragonfly);
          break;
        case DragonflyBaratas:
          if(quests.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyBaratas) {
            add(NewsEvent.DragonflyBaratas);
          }
          break;
        case DragonflyDestroyed:
          if(quests.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyZalkon) {
            add(NewsEvent.DragonflyZalkon);
          } else if(quests.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyDestroyed) {
            add(NewsEvent.DragonflyDestroyed);
          }
          break;
        case DragonflyMelina:
          if(quests.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyMelina) {
            add(NewsEvent.DragonflyMelina);
          }
          break;
        case DragonflyRegulas:
          if(quests.getQuestStatusDragonfly() == SpecialEvent.StatusDragonflyFlyRegulas) {
            add(NewsEvent.DragonflyRegulas);
          }
          break;
        case ExperimentFailed:
          add(NewsEvent.ExperimentFailed);
          break;
        case ExperimentStopped:
          if(quests.getQuestStatusExperiment() > SpecialEvent.StatusExperimentNotStarted
              && quests.getQuestStatusExperiment() < SpecialEvent.StatusExperimentPerformed) {
            add(NewsEvent.ExperimentStopped);
          }
          break;
        case Gemulon:
          add(NewsEvent.Gemulon);
          break;
        case GemulonRescued:
          if(quests.getQuestStatusGemulon() > SpecialEvent.StatusGemulonNotStarted) {
            if(quests.getQuestStatusGemulon() < SpecialEvent.StatusGemulonTooLate) {
              add(NewsEvent.GemulonRescued);
            } else {
              add(NewsEvent.GemulonInvaded);
            }
          }
          break;
        case Japori:
          if(quests.getQuestStatusJapori() == SpecialEvent.StatusJaporiNotStarted) {
            add(NewsEvent.Japori);
          }
          break;
        case JaporiDelivery:
          if(quests.getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
            add(NewsEvent.JaporiDelivery);
          }
          break;
        case JarekGetsOut:
          if(quests.jarekOnBoard()) {
            add(NewsEvent.JarekGetsOut);
          }
          break;
        case Princess:
          add(NewsEvent.Princess);
          break;
        case PrincessCentauri:
          if(quests.getQuestStatusPrincess() == SpecialEvent.StatusPrincessFlyCentauri) {
            add(NewsEvent.PrincessCentauri);
          }
          break;
        case PrincessInthara:
          if(quests.getQuestStatusPrincess() == SpecialEvent.StatusPrincessFlyInthara) {
            add(NewsEvent.PrincessInthara);
          }
          break;
        case PrincessQonos:
          if(quests.getQuestStatusPrincess() == SpecialEvent.StatusPrincessFlyQonos) {
            add(NewsEvent.PrincessQonos);
          } else if(quests.getQuestStatusPrincess() == SpecialEvent.StatusPrincessRescued) {
            add(NewsEvent.PrincessRescued);
          }
          break;
        case PrincessReturned:
          if(quests.getQuestStatusPrincess() == SpecialEvent.StatusPrincessReturned) {
            add(NewsEvent.PrincessReturned);
          }
          break;
        case Scarab:
          add(NewsEvent.Scarab);
          break;
        case ScarabDestroyed:
          if(quests.getQuestStatusScarab() == SpecialEvent.StatusScarabHunting) {
            add(NewsEvent.ScarabHarass);
          } else if(quests.getQuestStatusScarab() >= SpecialEvent.StatusScarabDestroyed) {
            add(NewsEvent.ScarabDestroyed);
          }
          break;
        case Sculpture:
          add(NewsEvent.SculptureStolen);
          break;
        case SculptureDelivered:
          add(NewsEvent.SculptureTracked);
          break;
        case SpaceMonsterKilled:
          if(quests.getQuestStatusSpaceMonster() == SpecialEvent.StatusSpaceMonsterAtAcamar) {
            add(NewsEvent.SpaceMonster);
          } else if(quests.getQuestStatusSpaceMonster() >= SpecialEvent.StatusSpaceMonsterDestroyed) {
            add(NewsEvent.SpaceMonsterKilled);
          }
          break;
        case WildGetsOut:
          if(quests.wildOnBoard()) {
            add(NewsEvent.WildGetsOut);
          }
          break;
        default:
          break;
      }
    }
  }
}
