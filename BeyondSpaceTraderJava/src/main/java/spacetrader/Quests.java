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
 * The quest and special-event state of a game: the status of the thirteen
 * mission threads and the flags they keep (the fabric rip probability and the
 * portable singularity). It owns the state and the reactor accessors the
 * arrival reads and writes; the game only delegates.
 */
public final class Quests implements Arrival.ReactorStatus {
  private int _questStatusArtifact = 0; // 0 = not given yet, 1 = Artifact on board, 2 = Artifact no longer on board (either delivered or lost)
  private int _questStatusDragonfly = 0; // 0 = not available, 1 = Go to Baratas, 2 = Go to Melina, 3 = Go to Regulas, 4 = Go to Zalkon, 5 = Dragonfly destroyed, 6 = Got Shield
  private int _questStatusExperiment = 0; // 0 = not given yet, 1-11 = days from start; 12 = performed, 13 = cancelled
  private int _questStatusGemulon = 0; // 0 = not given yet, 1-7 = days from start, 8 = too late, 9 = in time, 10 = done
  private int _questStatusJapori = 0; // 0 = no disease, 1 = Go to Japori (always at least 10 medicine cannisters), 2 = Assignment finished or canceled
  private int _questStatusJarek = 0; // 0 = not delivered, 1-11 = on board, 12 = delivered
  private int _questStatusMoon = 0; // 0 = not bought, 1 = bought, 2 = claimed
  private int _questStatusPrincess = 0; // 0 = not available, 1 = Go to Centauri, 2 = Go to Inthara, 3 = Go to Qonos, 4 = Princess Rescued, 5-14 = On Board, 15 = Princess Returned, 16 = Got Quantum Disruptor
  private int _questStatusReactor = 0; // 0 = not encountered, 1-20 = days of mission (bays of fuel left = 10 - (ReactorStatus / 2), 21 = delivered, 22 = Done
  private int _questStatusScarab = 0; // 0 = not given yet, 1 = not destroyed, 2 = destroyed - upgrade not performed, 3 = destroyed - hull upgrade performed
  private int _questStatusSculpture = 0; // 0 = not given yet, 1 = on board, 2 = delivered, 3 = done
  private int _questStatusSpaceMonster = 0; // 0 = not available, 1 = Space monster is in Acamar system, 2 = Space monster is destroyed, 3 = Claimed reward
  private int _questStatusWild = 0; // 0 = not delivered, 1-11 = on board, 12 = delivered
  private int _fabricRipProbability = 0; // if Experiment = 12, this is the probability of being warped to a random planet.
  private boolean _canSuperWarp = false; // Do you have the Portable Singularity on board?

  public int questStatusArtifact() {
    return _questStatusArtifact;
  }

  public void questStatusArtifact(int questStatusArtifact) {
    _questStatusArtifact = questStatusArtifact;
  }

  public int questStatusDragonfly() {
    return _questStatusDragonfly;
  }

  public void questStatusDragonfly(int questStatusDragonfly) {
    _questStatusDragonfly = questStatusDragonfly;
  }

  public int questStatusExperiment() {
    return _questStatusExperiment;
  }

  public void questStatusExperiment(int questStatusExperiment) {
    _questStatusExperiment = questStatusExperiment;
  }

  public int questStatusGemulon() {
    return _questStatusGemulon;
  }

  public void questStatusGemulon(int questStatusGemulon) {
    _questStatusGemulon = questStatusGemulon;
  }

  public int questStatusJapori() {
    return _questStatusJapori;
  }

  public void questStatusJapori(int questStatusJapori) {
    _questStatusJapori = questStatusJapori;
  }

  public int questStatusJarek() {
    return _questStatusJarek;
  }

  public void questStatusJarek(int questStatusJarek) {
    _questStatusJarek = questStatusJarek;
  }

  public int questStatusMoon() {
    return _questStatusMoon;
  }

  public void questStatusMoon(int questStatusMoon) {
    _questStatusMoon = questStatusMoon;
  }

  public int questStatusPrincess() {
    return _questStatusPrincess;
  }

  public void questStatusPrincess(int questStatusPrincess) {
    _questStatusPrincess = questStatusPrincess;
  }

  public int questStatusScarab() {
    return _questStatusScarab;
  }

  public void questStatusScarab(int questStatusScarab) {
    _questStatusScarab = questStatusScarab;
  }

  public int questStatusSculpture() {
    return _questStatusSculpture;
  }

  public void questStatusSculpture(int questStatusSculpture) {
    _questStatusSculpture = questStatusSculpture;
  }

  public int questStatusSpaceMonster() {
    return _questStatusSpaceMonster;
  }

  public void questStatusSpaceMonster(int questStatusSpaceMonster) {
    _questStatusSpaceMonster = questStatusSpaceMonster;
  }

  public int questStatusWild() {
    return _questStatusWild;
  }

  public void questStatusWild(int questStatusWild) {
    _questStatusWild = questStatusWild;
  }

  public int fabricRipProbability() {
    return _fabricRipProbability;
  }

  public void fabricRipProbability(int fabricRipProbability) {
    _fabricRipProbability = fabricRipProbability;
  }

  public boolean canSuperWarp() {
    return _canSuperWarp;
  }

  public void canSuperWarp(boolean canSuperWarp) {
    _canSuperWarp = canSuperWarp;
  }

  @Override
  public int reactorStatus() {
    return _questStatusReactor;
  }

  @Override
  public void reactorStatus(int status) {
    _questStatusReactor = status;
  }
}
