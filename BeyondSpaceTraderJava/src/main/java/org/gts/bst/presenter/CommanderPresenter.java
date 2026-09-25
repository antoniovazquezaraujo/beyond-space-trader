/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.presenter;

import org.gts.bst.view.CommanderView;
import org.gts.bst.view.CommanderViewModel;
import org.gts.bst.view.CommanderViewModel.Bounty;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.PoliceRecord;
import spacetrader.Reputation;
import spacetrader.Strings;


/**
 * Fills the commander status screen from the model. No front-end types involved.
 */
public class CommanderPresenter {
  private final Game game;
  private final CommanderView view;

  public CommanderPresenter(Game game, CommanderView view) {
    this.game = game;
    this.view = view;
  }

  public void update() {
    Commander cmdr = game.Commander();
    view.render(new CommanderViewModel(
        cmdr.Name(),
        Strings.DifficultyLevels[game.Difficulty().CastToInt()],
        Functions.Multiples(cmdr.getDays(), Strings.TimeUnit),
        skill(cmdr.Pilot(), cmdr.getShip().Pilot()),
        skill(cmdr.Fighter(), cmdr.getShip().Fighter()),
        skill(cmdr.Trader(), cmdr.getShip().Trader()),
        skill(cmdr.Engineer(), cmdr.getShip().Engineer()),
        Functions.FormatMoney(cmdr.getCash()),
        Functions.FormatMoney(cmdr.getDebt()),
        Functions.FormatMoney(cmdr.Worth()),
        Functions.FormatNumber(cmdr.getKillsPirate() + cmdr.getKillsPolice() + cmdr.getKillsTrader()),
        PoliceRecord.GetPoliceRecordFromScore(cmdr.getPoliceRecordScore()).Name(),
        Reputation.GetReputationFromScore(cmdr.getReputationScore()).Name(),
        bounty(cmdr.getPoliceRecordScore())));
  }

  private static String skill(int commander, int ship) {
    return commander + " (" + ship + ")";
  }

  private static Bounty bounty(int score) {
    if(score <= Consts.PoliceRecordScoreCrook) {
      return new Bounty(true, "Bounty offered:", Functions.FormatMoney(-1000 * score));
    }
    if(score >= Consts.PoliceRecordScoreTrusted) {
      return new Bounty(true, "Angry kingpins:", Functions.FormatNumber(score / 5));
    }
    return new Bounty(false, "", "");
  }
}

