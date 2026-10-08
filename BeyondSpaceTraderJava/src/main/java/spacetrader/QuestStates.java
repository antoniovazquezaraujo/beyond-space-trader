/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;


/**
 * Read-only view of the quest and special-crew states the arrival rumours ask
 * about. Game implements it, delegating the quest statuses in {@link Quests}
 * and reading the on-board checks from the commander.
 */
public interface QuestStates {
  int getQuestStatusDragonfly();
  int getQuestStatusExperiment();
  int getQuestStatusGemulon();
  int getQuestStatusJapori();
  int getQuestStatusPrincess();
  int getQuestStatusScarab();
  int getQuestStatusSpaceMonster();
  boolean artifactOnBoard();
  boolean jarekOnBoard();
  boolean wildOnBoard();
}
