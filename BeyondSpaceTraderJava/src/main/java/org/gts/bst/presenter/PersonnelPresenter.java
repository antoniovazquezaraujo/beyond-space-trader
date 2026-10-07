/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelView;
import org.gts.bst.view.PersonnelViewModel;
import spacetrader.Commander;
import spacetrader.CrewMember;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.Strings;
import spacetrader.enums.AlertType;


/**
 * Fills the personnel screen and mediates the hire/fire action. No front-end types
 * involved: the view forwards the selected list indexes.
 */
public class PersonnelPresenter {
  private final Game game;
  private final Commander cmdr;
  private final Ship ship;
  private final PersonnelView view;
  private CrewMember selected = null;

  public PersonnelPresenter(Game game, PersonnelView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.ship = cmdr.getShip();
    this.view = view;
  }

  public void update() {
    selected = null;
    view.render(model());
  }

  public void selectCrew(int index) {
    CrewMember[] crew = ship.Crew();
    selected = index >= 0 && index + 1 < crew.length ? crew[index + 1] : null;
    view.renderInfo(info());
  }

  public void selectForHire(int index) {
    CrewMember[] mercs = cmdr.CurrentSystem().MercenariesForHire();
    selected = index >= 0 && index < mercs.length ? mercs[index] : null;
    view.renderInfo(info());
  }

  /**
   * Returns true when the crew member was hired or fired.
   */
  public boolean hireFire() {
    if(selected == null || !hireFireVisible()) {
      return false;
    }
    if(ship.HasCrew(selected.Id())) {
      if(game.Dialogs().alert(AlertType.CrewFireMercenary, selected.Name()) == DialogResult.Yes) {
        ship.Fire(selected.Id());
        update();
        return true;
      }
    } else if(ship.FreeCrewQuarters() == 0) {
      game.Dialogs().alert(AlertType.CrewNoQuarters, selected.Name());
    } else {
      ship.Hire(selected);
      update();
      return true;
    }
    return false;
  }

  private PersonnelViewModel model() {
    List<String> crew = crewEntries();
    List<String> forHire = forHireEntries();
    return new PersonnelViewModel(crew, !crew.isEmpty(), Strings.PersonnelNoQuarters,
        forHire, !forHire.isEmpty(), Strings.PersonnelNoMercenaries, info());
  }

  private List<String> crewEntries() {
    List<String> entries = new ArrayList<>();
    CrewMember[] crew = ship.Crew();
    for(int i = 1; i < crew.length; i++) {
      entries.add(crew[i] == null ? Strings.PersonnelVacancy : crew[i].toString());
    }
    return entries;
  }

  private List<String> forHireEntries() {
    List<String> entries = new ArrayList<>();
    for(CrewMember merc : cmdr.CurrentSystem().MercenariesForHire()) {
      entries.add(merc.toString());
    }
    return entries;
  }

  private PersonnelInfo info() {
    if(selected == null) {
      return new PersonnelInfo(false, "", false, "", "", "", "", "", false, "");
    }
    return new PersonnelInfo(true, selected.Name(), selected.Rate() > 0,
        Functions.StringVars(Strings.MoneyRateSuffix, Functions.FormatMoney(selected.Rate())),
        selected.Pilot() + "", selected.Fighter() + "", selected.Trader() + "", selected.Engineer() + "",
        hireFireVisible(), ship.HasCrew(selected.Id()) ? Strings.MercenaryFire : Strings.MercenaryHire);
  }

  private boolean hireFireVisible() {
    return selected != null && (selected.Rate() > 0 || selected.Id() == CrewMemberId.Zeethibal);
  }
}

