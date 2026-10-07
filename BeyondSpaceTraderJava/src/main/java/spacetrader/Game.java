package spacetrader;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.GameWindow;
import org.gts.bst.cargo.TradeItem;
import org.gts.bst.cargo.TradeItemType;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.events.EncounterResult;
import org.gts.bst.events.EncounterType;
import org.gts.bst.events.NewsEvent;
import org.gts.bst.events.SpecialEventType;
import org.gts.bst.events.VeryRareEncounter;
import org.gts.bst.ship.ShipType;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.ship.equip.GadgetType;
import org.gts.bst.ship.equip.ShieldType;
import org.gts.bst.ship.equip.WeaponType;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.EncounterDialogHost;
import spacetrader.enums.AlertType;
import spacetrader.enums.GameEndType;
import spacetrader.enums.OpponentType;
import spacetrader.enums.PoliticalSystemType;
import spacetrader.enums.SkillType;
import spacetrader.enums.StarSystemId;
import spacetrader.enums.SystemPressure;
import spacetrader.enums.TechLevel;
import java.util.ArrayList;
import spacetrader.util.Hashtable;
import spacetrader.util.Util;

public final class Game extends STSerializableObject implements QuestStates {
  private static Game game;
  private Commander cmdr;
  // Game Data
  private StarSystem[] _universe;
  private int[] _wormholes = new int[6];
  private CrewMember[] _mercenaries = new CrewMember[Strings.CrewMemberNames.size()];
  private Ship _dragonfly = new Ship(ShipType.Dragonfly);
  private Ship _scarab = new Ship(ShipType.Scarab);
  private Ship _scorpion = new Ship(ShipType.Scorpion);
  private Ship _spaceMonster = new Ship(ShipType.SpaceMonster);
  private int _chanceOfTradeInOrbit = 100;
  private int _clicks = 0; // Distance from target system, 0 = arrived
  private boolean _tribbleMessage = false; // Is true if the Ship Yard on the current system informed you about the tribbles
  private boolean _arrivedViaWormhole = false; // flag to indicate whether player arrived on current planet via wormhole
  private final Newspaper _newspaper = new Newspaper();
  private final Market _market = new Market();
  // Current Selections
  private Difficulty _difficulty = Difficulty.Normal; // Difficulty level
  private boolean _cheatEnabled = false;
  private boolean _autoSave = false;
  private boolean _easyEncounters = false;
  private Encounter _encounter;
  private GameEndType _endStatus = GameEndType.NA;
  private StarSystemId _selectedSystemId = StarSystemId.NA; // Current system on chart
  private StarSystemId _warpSystemId = StarSystemId.NA; // Target system for warp
  private StarSystemId _trackedSystemId = StarSystemId.NA; // The short-range chart will display an arrow towards this system if the value is not null
  private boolean _targetWormhole = false; // Wormhole selected?
  // Status of Quests
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
  private int _chanceOfVeryRareEncounter = 5;
  private ArrayList<VeryRareEncounter> _veryRareEncounters = new ArrayList<>(6); // Array of Very Rare encounters not done yet.
  // Options
  private GameOptions _options = new GameOptions(true);
  // The rest of the member variables are not saved between games.
  private GameWindow _parentWin = null;
  private final DialogService _dialogs;

  public Game(String name, Difficulty difficulty, int pilot, int fighter, int trader, int engineer, GameWindow parentWin,
      DialogService dialogs) {
    Game.CurrentGame(this);
    _parentWin = parentWin;
    _dialogs = dialogs;
    _difficulty = difficulty;
    // Keep generating a new universe until all the special events and shipyards are placed.
    do {
      UniverseGenerator.Generated generated = UniverseGenerator.Generate(Strings.SystemNames.size(), _wormholes.length);
      _universe = generated.systems();
      _wormholes = generated.wormholes();
    } while(!(UniverseGenerator.PlaceSpecialEvents(_universe, _wormholes) && UniverseGenerator.PlaceShipyards(_universe)));
    cmdr = NewGameSetup.InitializeCommander(name,
        new CrewMember(CrewMemberId.Commander, pilot, fighter, trader, engineer, StarSystemId.NA), _universe, Mercenaries());
    NewGameSetup.GenerateCrewMemberList(Mercenaries(), _universe.length, _difficulty);
    NewGameSetup.CreateShips(Dragonfly(), _scarab, _scorpion, _spaceMonster, Mercenaries());
    CalculatePrices(cmdr.CurrentSystem());
    Game.this.ResetVeryRareEncounters();
    if(_difficulty.CastToInt() < Difficulty.Normal.CastToInt()) {
      cmdr.CurrentSystem().SpecialEventType(SpecialEventType.Lottery);
    }
  }

  @SuppressWarnings("unchecked")
  public Game(Hashtable hash, GameWindow parentWin, DialogService dialogs) {
    super(hash);
    Game.CurrentGame(this);
    _parentWin = parentWin;
    _dialogs = dialogs;
    String version = GetValueFromHash(hash, "_version", String.class);
    if(version.compareTo(Consts.CurrentVersion) > 0) {
      throw new FutureVersionException();
    }
    _universe = (StarSystem[])ArrayListToArray(GetValueFromHash(hash, "_universe", ArrayList.class), "StarSystem");
    _wormholes = GetValueFromHash(hash, "_wormholes", _wormholes, int[].class);
    _mercenaries = (CrewMember[])ArrayListToArray(GetValueFromHash(hash, "_mercenaries", ArrayList.class), "CrewMember");
    cmdr = new Commander(GetValueFromHash(hash, "_commander", Hashtable.class));
    _dragonfly = new Ship(GetValueFromHash(hash, "_dragonfly", _dragonfly.Serialize(), Hashtable.class));
    _scarab = new Ship(GetValueFromHash(hash, "_scarab", _scarab.Serialize(), Hashtable.class));
    _scorpion = new Ship(GetValueFromHash(hash, "_scorpion", _scorpion.Serialize(), Hashtable.class));
    _spaceMonster = new Ship(GetValueFromHash(hash, "_spaceMonster", _spaceMonster.Serialize(), Hashtable.class));
    encounter().setOpponent(new Ship(GetValueFromHash(hash, "_opponent", encounter().getOpponent().Serialize(), Hashtable.class)));
    _chanceOfTradeInOrbit = GetValueFromHash(hash, "_chanceOfTradeInOrbit", _chanceOfTradeInOrbit);
    _clicks = GetValueFromHash(hash, "_clicks", _clicks);
    encounter().setRaided(GetValueFromHash(hash, "_raided", encounter().getRaided()));
    encounter().setInspected(GetValueFromHash(hash, "_inspected", encounter().getInspected()));
    _tribbleMessage = GetValueFromHash(hash, "_tribbleMessage", _tribbleMessage);
    _arrivedViaWormhole = GetValueFromHash(hash, "_arrivedViaWormhole", _arrivedViaWormhole);
    _newspaper.paid(GetValueFromHash(hash, "_paidForNewspaper", _newspaper.paid()));
    encounter().setLitterWarning(GetValueFromHash(hash, "_litterWarning", encounter().getLitterWarning()));
    _newspaper.events(GetValueFromHash(hash, "_newsEvents", _newspaper.events().toArray(new Integer[0])));
    _difficulty = Difficulty.FromInt(GetValueFromHash(hash, "_difficulty", _difficulty, Integer.class));
    _cheatEnabled = GetValueFromHash(hash, "_cheatEnabled", _cheatEnabled);
    _autoSave = GetValueFromHash(hash, "_autoSave", _autoSave);
    _easyEncounters = GetValueFromHash(hash, "_easyEncounters", _easyEncounters);
    _endStatus = GameEndType.FromInt(GetValueFromHash(hash, "_endStatus", _endStatus, Integer.class));
    encounter().setEncounterType(EncounterType.FromInt(GetValueFromHash(hash, "_encounterType", encounter().getEncounterType(), Integer.class)));
    _selectedSystemId = StarSystemId.FromInt(GetValueFromHash(hash, "_selectedSystemId", _selectedSystemId, Integer.class));
    _warpSystemId = StarSystemId.FromInt(GetValueFromHash(hash, "_warpSystemId", _warpSystemId, Integer.class));
    _trackedSystemId = StarSystemId.FromInt(GetValueFromHash(hash, "_trackedSystemId", _trackedSystemId, Integer.class));
    _targetWormhole = GetValueFromHash(hash, "_targetWormhole", _targetWormhole);
    _market.buy(GetValueFromHash(hash, "_priceCargoBuy", _market.buy(), int[].class));
    _market.sell(GetValueFromHash(hash, "_priceCargoSell", _market.sell(), int[].class));
    _questStatusArtifact = GetValueFromHash(hash, "_questStatusArtifact", _questStatusArtifact);
    _questStatusDragonfly = GetValueFromHash(hash, "_questStatusDragonfly", _questStatusDragonfly);
    _questStatusExperiment = GetValueFromHash(hash, "_questStatusExperiment", _questStatusExperiment);
    _questStatusGemulon = GetValueFromHash(hash, "_questStatusGemulon", _questStatusGemulon);
    _questStatusJapori = GetValueFromHash(hash, "_questStatusJapori", _questStatusJapori);
    _questStatusJarek = GetValueFromHash(hash, "_questStatusJarek", _questStatusJarek);
    _questStatusMoon = GetValueFromHash(hash, "_questStatusMoon", _questStatusMoon);
    _questStatusPrincess = GetValueFromHash(hash, "_questStatusPrincess", _questStatusPrincess);
    _questStatusReactor = GetValueFromHash(hash, "_questStatusReactor", _questStatusReactor);
    _questStatusScarab = GetValueFromHash(hash, "_questStatusScarab", _questStatusScarab);
    _questStatusSculpture = GetValueFromHash(hash, "_questStatusSculpture", _questStatusSculpture);
    _questStatusSpaceMonster = GetValueFromHash(hash, "_questStatusSpaceMonster", _questStatusSpaceMonster);
    _questStatusWild = GetValueFromHash(hash, "_questStatusWild", _questStatusWild);
    _fabricRipProbability = GetValueFromHash(hash, "_fabricRipProbability", _fabricRipProbability);
    encounter().setJustLootedMarie(GetValueFromHash(hash, "_justLootedMarie", encounter().getJustLootedMarie()));
    _canSuperWarp = GetValueFromHash(hash, "_canSuperWarp", _canSuperWarp);
    _chanceOfVeryRareEncounter = GetValueFromHash(hash, "_chanceOfVeryRareEncounter", _chanceOfVeryRareEncounter);
    Integer[] veryRareIds = GetValueFromHash(hash, "_veryRareEncounters", new Integer[0]);
    _veryRareEncounters = new ArrayList<>(veryRareIds.length);
    for(Integer id : veryRareIds) {
      _veryRareEncounters.add(VeryRareEncounter.FromInt(id));
    }
    _options = new GameOptions(GetValueFromHash(hash, "_options", _options.Serialize(), Hashtable.class));
  }

  @Override
  public Hashtable Serialize() {
    Hashtable ht = super.Serialize();
    ht.add("_version", "2.00");
    ht.add("_universe", ArrayToArrayList(_universe));
    ht.add("_commander", cmdr.Serialize());
    ht.add("_wormholes", _wormholes);
    ht.add("_mercenaries", ArrayToArrayList(_mercenaries));
    ht.add("_dragonfly", _dragonfly.Serialize());
    ht.add("_scarab", _scarab.Serialize());
    ht.add("_scorpion", _scorpion.Serialize());
    ht.add("_spaceMonster", _spaceMonster.Serialize());
    ht.add("_opponent", encounter().getOpponent().Serialize());
    ht.add("_chanceOfTradeInOrbit", _chanceOfTradeInOrbit);
    ht.add("_clicks", _clicks);
    ht.add("_raided", encounter().getRaided());
    ht.add("_inspected", encounter().getInspected());
    ht.add("_tribbleMessage", _tribbleMessage);
    ht.add("_arrivedViaWormhole", _arrivedViaWormhole);
    ht.add("_paidForNewspaper", _newspaper.paid());
    ht.add("_litterWarning", encounter().getLitterWarning());
    ht.add("_newsEvents", _newspaper.events().toArray(new Integer[0]));
    ht.add("_difficulty", _difficulty.CastToInt());
    ht.add("_cheatEnabled", _cheatEnabled);
    ht.add("_autoSave", _autoSave);
    ht.add("_easyEncounters", _easyEncounters);
    ht.add("_endStatus", _endStatus.CastToInt());
    ht.add("_encounterType", encounter().getEncounterType().CastToInt());
    ht.add("_selectedSystemId", _selectedSystemId.CastToInt());
    ht.add("_warpSystemId", _warpSystemId.CastToInt());
    ht.add("_trackedSystemId", _trackedSystemId.CastToInt());
    ht.add("_targetWormhole", _targetWormhole);
    ht.add("_priceCargoBuy", _market.buy());
    ht.add("_priceCargoSell", _market.sell());
    ht.add("_questStatusArtifact", _questStatusArtifact);
    ht.add("_questStatusDragonfly", _questStatusDragonfly);
    ht.add("_questStatusExperiment", _questStatusExperiment);
    ht.add("_questStatusGemulon", _questStatusGemulon);
    ht.add("_questStatusJapori", _questStatusJapori);
    ht.add("_questStatusJarek", _questStatusJarek);
    ht.add("_questStatusMoon", _questStatusMoon);
    ht.add("_questStatusPrincess", _questStatusPrincess);
    ht.add("_questStatusReactor", _questStatusReactor);
    ht.add("_questStatusScarab", _questStatusScarab);
    ht.add("_questStatusSculpture", _questStatusSculpture);
    ht.add("_questStatusSpaceMonster", _questStatusSpaceMonster);
    ht.add("_questStatusWild", _questStatusWild);
    ht.add("_fabricRipProbability", _fabricRipProbability);
    ht.add("_justLootedMarie", encounter().getJustLootedMarie());
    ht.add("_canSuperWarp", _canSuperWarp);
    ht.add("_chanceOfVeryRareEncounter", _chanceOfVeryRareEncounter);
    ht.add("_veryRareEncounters", ArrayListToIntArray(_veryRareEncounters));
    ht.add("_options", _options.Serialize());
    return ht;
  }


  private void Arrival() {
    cmdr.CurrentSystem(WarpSystem());
    cmdr.CurrentSystem().Visited(true);
    setPaidForNewspaper(false);
    if(TrackedSystem() == cmdr.CurrentSystem() && _options.getTrackAutoOff()) {
      setTrackedSystemId(StarSystemId.NA);
    }
    ArrivalCheckReactor();
    ArrivalCheckTribbles();
    ArrivalCheckDebt();
    ArrivalPerformRepairs();
    ArrivalUpdatePressuresAndQuantities();
    ArrivalCheckEasterEgg();
    CalculatePrices(cmdr.CurrentSystem());
    NewsAddEventsOnArrival();
    if(_options.getNewsAutoShow()) {
      ShowNewspaper();
    }
  }

  private void ArrivalCheckDebt() {
    // Check for Large Debt - 06/30/01 SRA
    if(cmdr.getDebt() >= Consts.DebtWarning) {
      Dialogs().alert(AlertType.DebtWarning);
    } else if(cmdr.getDebt() > 0 && _options.getRemindLoans() && cmdr.getDays() % 5 == 0) { // Debt Reminder
      Dialogs().alert(AlertType.DebtReminder, Functions.Multiples(cmdr.getDebt(), Strings.MoneyUnit));
    }
  }

  private void ArrivalCheckEasterEgg() {
    /* This Easter Egg gives the commander a Lighting Shield */
    if(cmdr.CurrentSystem().Id() == StarSystemId.Og) {
      boolean egg = true;
      for(int i = 0; i < cmdr.getShip().Cargo().length && egg; i++) {
        if(cmdr.getShip().Cargo()[i] != 1) {
          egg = false;
        }
      }
      if(egg && cmdr.getShip().FreeSlotsShield() > 0) {
        Dialogs().alert(AlertType.Egg);
        cmdr.getShip().AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
        for(int i = 0; i < cmdr.getShip().Cargo().length; i++) {
          cmdr.getShip().Cargo()[i] = 0;
          cmdr.PriceCargo()[i] = 0;
        }
      }
    }
  }

  private void ArrivalCheckReactor() {
    if(getQuestStatusReactor() == SpecialEvent.StatusReactorDate) {
      Dialogs().alert(AlertType.ReactorMeltdown);
      setQuestStatusReactor(SpecialEvent.StatusReactorNotStarted);
      if(cmdr.getShip().getEscapePod()) {
        EscapeWithPod();
      } else {
        Dialogs().alert(AlertType.ReactorDestroyed);
        throw new GameEndException(this, GameEndType.Killed);
      }
    } else {
      // Reactor warnings:
      if(getQuestStatusReactor() == SpecialEvent.StatusReactorFuelOk + 1) { // now they know the quest has a time constraint!
        Dialogs().alert(AlertType.ReactorWarningFuel);
      } else if(getQuestStatusReactor() == SpecialEvent.StatusReactorDate - 4) { // better deliver it soon!
        Dialogs().alert(AlertType.ReactorWarningFuelGone);
      } else if(getQuestStatusReactor() == SpecialEvent.StatusReactorDate - 2) { // last warning!
        Dialogs().alert(AlertType.ReactorWarningTemp);
      }
    }
  }

  private void ArrivalCheckTribbles() {
    Ship ship = cmdr.getShip();
    if(ship.getTribbles() > 0) {
      int previousTribbles = ship.getTribbles();
      int narc = TradeItemType.Narcotics.CastToInt();
      int food = TradeItemType.Food.CastToInt();
      if(ship.ReactorOnBoard()) {
        if(ship.getTribbles() < 20) {
          ship.setTribbles(0);
          Dialogs().alert(AlertType.TribblesAllDied);
        } else {
          ship.setTribbles(ship.getTribbles() / 2);
          Dialogs().alert(AlertType.TribblesHalfDied);
        }
      } else if(ship.Cargo()[narc] > 0) {
        int dead = Math.min(1 + Functions.GetRandom(3), ship.Cargo()[narc]);
        cmdr.PriceCargo()[narc] = cmdr.PriceCargo()[narc] * (ship.Cargo()[narc] - dead) / ship.Cargo()[narc];
        ship.Cargo()[narc] -= dead;
        ship.Cargo()[TradeItemType.Furs.CastToInt()] += dead;
        ship.setTribbles(ship.getTribbles() - Math.min(dead * (Functions.GetRandom(5) + 98), ship.getTribbles() - 1));
        Dialogs().alert(AlertType.TribblesMostDied);
      } else {
        if(ship.Cargo()[food] > 0 && ship.getTribbles() < Consts.MaxTribbles) {
          int eaten = ship.Cargo()[food] - Functions.GetRandom(ship.Cargo()[food]);
          cmdr.PriceCargo()[food] -= cmdr.PriceCargo()[food] * eaten / ship.Cargo()[food];
          ship.Cargo()[food] -= eaten;
          ship.setTribbles(ship.getTribbles() + (eaten * 100));
          Dialogs().alert(AlertType.TribblesAteFood);
        }
        if(ship.getTribbles() < Consts.MaxTribbles) {
          ship.setTribbles(ship.getTribbles() + (1 + Functions.GetRandom(ship.Cargo()[food] > 0 ? ship.getTribbles() : ship.getTribbles() / 2)));
        }
        if(ship.getTribbles() > Consts.MaxTribbles) {
          ship.setTribbles(Consts.MaxTribbles);
        }
        if((previousTribbles < 100 && ship.getTribbles() >= 100)
            || (previousTribbles < 1000 && ship.getTribbles() >= 1000)
            || (previousTribbles < 10000 && ship.getTribbles() >= 10000)
            || (previousTribbles < 50000 && ship.getTribbles() >= 50000)
            || (previousTribbles < Consts.MaxTribbles && ship.getTribbles() == Consts.MaxTribbles)) {
          String qty = ship.getTribbles() == Consts.MaxTribbles ? Strings.TribbleDangerousNumber : Functions.FormatNumber(ship.getTribbles());
          Dialogs().alert(AlertType.TribblesInspector, qty);
        }
      }
      setTribbleMessage(false);
    }
  }

  private void ArrivalPerformRepairs() {
    Ship ship = cmdr.getShip();
    if(ship.getHull() < ship.HullStrength()) {
      ship.setHull(ship.getHull() + Math.min(ship.HullStrength() - ship.getHull(), Functions.GetRandom(ship.Engineer())));
    }
    for(int i = 0; i < ship.Shields().length; ++i) {
      if(ship.Shields()[i] != null) {
        ship.Shields()[i].setCharge(ship.Shields()[i].Power());
      }
    }
    boolean fuelOk = true;
    int toAdd = ship.FuelTanks() - ship.getFuel();
    if(_options.getAutoFuel() && toAdd > 0) {
      if(cmdr.getCash() >= toAdd * ship.getFuelCost()) {
        ship.setFuel(ship.getFuel() + toAdd);
        cmdr.setCash(cmdr.getCash() - (toAdd * ship.getFuelCost()));
      } else {
        fuelOk = false;
      }
    }
    boolean repairOk = true;
    toAdd = ship.HullStrength() - ship.getHull();
    if(_options.getAutoRepair() && toAdd > 0) {
      if(cmdr.getCash() >= toAdd * ship.getRepairCost()) {
        ship.setHull(ship.getHull() + toAdd);
        cmdr.setCash(cmdr.getCash() - (toAdd * ship.getRepairCost()));
      } else {
        repairOk = false;
      }
    }
    if(!fuelOk && !repairOk) {
      Dialogs().alert(AlertType.ArrivalIFFuelRepairs);
    } else if(!fuelOk) {
      Dialogs().alert(AlertType.ArrivalIFFuel);
    } else if(!repairOk) {
      Dialogs().alert(AlertType.ArrivalIFRepairs);
    }
  }

  private void ArrivalUpdatePressuresAndQuantities() {
    for(int i = 0; i < _universe.length; i++) {
      if(Functions.GetRandom(100) < 15) {
        _universe[i].SystemPressure((SystemPressure.FromInt(_universe[i].SystemPressure() == SystemPressure.None
            ? Functions.GetRandom(SystemPressure.War.CastToInt(), SystemPressure.Employment.CastToInt() + 1) : SystemPressure.None.CastToInt())));
      }
      if(_universe[i].CountDown() > 0) {
        _universe[i].CountDown(_universe[i].CountDown() - 1);
        if(_universe[i].CountDown() > CountDownStart()) {
          _universe[i].CountDown(CountDownStart());
        } else if(_universe[i].CountDown() <= 0) {
          _universe[i].InitializeTradeItems();
        } else {
          for(int j = 0; j < Consts.TradeItems.size(); j++) {
            if(WarpSystem().ItemTraded(Consts.TradeItems.get(j))) {
              _universe[i].TradeItems()[j] = Math.max(0, _universe[i].TradeItems()[j] + Functions.GetRandom(-4, 5));
            }
          }
        }
      }
    }
  }

  private void CalculatePrices(StarSystem system) {
    _market.calculate(system, cmdr.getPoliceRecordScore(), cmdr.getShip().Trader());
  }







  private void NormalDeparture(int fuel) {
    cmdr.setCash(cmdr.getCash() - (MercenaryCosts() + InsuranceCosts() + WormholeCosts()));
    cmdr.getShip().setFuel(cmdr.getShip().getFuel() - fuel);
    cmdr.PayInterest();
    IncDays(1);
  }

  public ArrayList<Integer> NewsEvents() {
    return _newspaper.events();
  }

  public ArrayList<VeryRareEncounter> VeryRareEncounters() {
    return _veryRareEncounters;
  }

  public Commander Commander() {
    return cmdr;
  }

  public CrewMember[] Mercenaries() {
    return _mercenaries;
  }

  public Difficulty Difficulty() {
    return _difficulty;
  }





  public GameEndType getEndStatus() {
    return _endStatus;
  }

  public GameOptions Options() {
    return _options;
  }

  public int NewsLatestEvent() {
    return _newspaper.latest();
  }

  public Ship Dragonfly() {
    return _dragonfly;
  }


  public Ship Scarab() {
    return _scarab;
  }

  public Ship Scorpion() {
    return _scorpion;
  }

  public Ship SpaceMonster() {
    return _spaceMonster;
  }

  public GameWindow getParentWindow() {
    return _parentWin;
  }

  public DialogService Dialogs() {
    return _dialogs;
  }

  public StarSystem SelectedSystem() {
    return (_selectedSystemId == StarSystemId.NA ? null : _universe[_selectedSystemId.CastToInt()]);
  }

  public StarSystem TrackedSystem() {
    return _trackedSystemId == StarSystemId.NA ? null : _universe[_trackedSystemId.CastToInt()];
  }

  public StarSystem WarpSystem() {
    return _warpSystemId == StarSystemId.NA ? null : _universe[_warpSystemId.CastToInt()];
  }

  public StarSystem[] Universe() {
    return _universe;
  }

  public StarSystemId getTrackedSystemId() {
    return _trackedSystemId;
  }

  public StarSystemId SelectedSystemId() {
    return _selectedSystemId;
  }






  public String NewspaperHead() {
    return _newspaper.head(cmdr);
  }

  public String NewspaperText() {
    return _newspaper.text(cmdr, _universe, _difficulty);
  }

  @SuppressWarnings("fallthrough")





  public boolean getArrivedViaWormhole() {
    return _arrivedViaWormhole;
  }

  public boolean getAutoSave() {
    return _autoSave;
  }

  public boolean getCanSuperWarp() {
    return _canSuperWarp;
  }

  public boolean getCheatEnabled() {
    return _cheatEnabled;
  }

  public boolean getEasyEncounters() {
    return _easyEncounters;
  }














  public boolean getPaidForNewspaper() {
    return _newspaper.paid();
  }


  public boolean getTribbleMessage() {
    return _tribbleMessage;
  }

  public boolean TargetWormhole() {
    return _targetWormhole;
  }

  public boolean Travel() {
    // Returns true if an encounter occurred.
    // if timespace is ripped, we may switch the warp system here.
    if(getQuestStatusExperiment() == SpecialEvent.StatusExperimentPerformed && getFabricRipProbability() > 0
        && (getFabricRipProbability() == Consts.FabricRipInitialProbability || Functions.GetRandom(100) < getFabricRipProbability())) {
      Dialogs().alert(AlertType.SpecialTimespaceFabricRip);
      SelectedSystemId(StarSystemId.FromInt(Functions.GetRandom(_universe.length)));
    }
    boolean uneventful = true;
    encounter().setRaided(false);
    encounter().setInspected(false);
    encounter().setLitterWarning(false);
    setClicks(Consts.StartClicks);
    while(getClicks() > 0) {
      cmdr.getShip().PerformRepairs();
      if(new EncounterGenerator(this).determine()) {
        uneventful = false;
        EncounterResult result = getParentWindow().showEncounter();
        getParentWindow().UpdateStatusBar();
        switch(result) {
          case Arrested:
            setClicks(0);
            Arrested();
            break;
          case EscapePod:
            setClicks(0);
            EscapeWithPod();
            break;
          case Killed:
            throw new GameEndException(this, GameEndType.Killed);
        }
      }
      setClicks(getClicks() - 1);
    }
    return !uneventful;
  }

  public int CountDownStart() {
    return _difficulty.CastToInt() + 3;
  }

  public int CurrentCosts() {
    return InsuranceCosts() + InterestCosts() + MercenaryCosts() + WormholeCosts();
  }


  public int getChanceOfTradeInOrbit() {
    return _chanceOfTradeInOrbit;
  }

  public int getChanceOfVeryRareEncounter() {
    return _chanceOfVeryRareEncounter;
  }

  public int getClicks() {
    return _clicks;
  }

  public int getFabricRipProbability() {
    return _fabricRipProbability;
  }

  public int getQuestStatusArtifact() {
    return _questStatusArtifact;
  }

  public int getQuestStatusDragonfly() {
    return _questStatusDragonfly;
  }

  public int getQuestStatusExperiment() {
    return _questStatusExperiment;
  }

  public int getQuestStatusGemulon() {
    return _questStatusGemulon;
  }

  public int getQuestStatusJapori() {
    return _questStatusJapori;
  }

  public int getQuestStatusJarek() {
    return _questStatusJarek;
  }

  public int getQuestStatusMoon() {
    return _questStatusMoon;
  }

  public int getQuestStatusPrincess() {
    return _questStatusPrincess;
  }

  public int getQuestStatusReactor() {
    return _questStatusReactor;
  }

  public int getQuestStatusScarab() {
    return _questStatusScarab;
  }

  public int getQuestStatusSculpture() {
    return _questStatusSculpture;
  }

  public int getQuestStatusSpaceMonster() {
    return _questStatusSpaceMonster;
  }

  public int getQuestStatusWild() {
    return _questStatusWild;
  }

  @Override
  public boolean artifactOnBoard() {
    return cmdr.getShip().ArtifactOnBoard();
  }

  @Override
  public boolean jarekOnBoard() {
    return cmdr.getShip().JarekOnBoard();
  }

  @Override
  public boolean wildOnBoard() {
    return cmdr.getShip().WildOnBoard();
  }

  public int InsuranceCosts() {
    return cmdr.getInsurance() ? (int)Math.max(1, cmdr.getShip().BaseWorth(true) * Consts.InsRate * (100 - cmdr.NoClaim()) / 100) : 0;
  }

  public int InterestCosts() {
    return cmdr.getDebt() > 0 ? (int)Math.max(1, cmdr.getDebt() * Consts.IntRate) : 0;
  }

  public int MercenaryCosts() {
    int total = 0;
    for(int i = 1; i < cmdr.getShip().Crew().length && cmdr.getShip().Crew()[i] != null; i++) {
      total += cmdr.getShip().Crew()[i].Rate();
    }
    return total;
  }

  public int Score() {
    int worth = cmdr.Worth() < 1000000 ? cmdr.Worth() : 1000000 + ((cmdr.Worth() - 1000000) / 10);
    int daysMoon = 0;
    int modifier = 0;
    switch(getEndStatus()) {
      case Killed:
        modifier = 90;
        break;
      case Retired:
        modifier = 95;
        break;
      case BoughtMoon:
        daysMoon = Math.max(0, (_difficulty.CastToInt() + 1) * 100 - cmdr.getDays());
        modifier = 100;
        break;
      default:
        break;
    }
    return (_difficulty.CastToInt() + 1) * modifier * (daysMoon * 1000 + worth) / 250000;
  }

  public int WormholeCosts() {
    return Functions.WormholeExists(cmdr.CurrentSystem(), WarpSystem()) ? Consts.WormDist * cmdr.getShip().getFuelCost() : 0;
  }

  public int[] PriceCargoBuy() {
    return _market.buy();
  }

  public int[] PriceCargoSell() {
    return _market.sell();
  }

  public int[] Wormholes() {
    return _wormholes;
  }

  public void Arrested() {
    int term = Math.max(30, -cmdr.getPoliceRecordScore());
    int fine = (1 + cmdr.Worth() * Math.min(80, -cmdr.getPoliceRecordScore()) / 50000) * 500;
    if(cmdr.getShip().WildOnBoard()) {
      fine = (int)(fine * 1.05);
    }
    Dialogs().alert(AlertType.EncounterArrested);
    Dialogs().alert(AlertType.JailConvicted, Functions.Multiples(term, Strings.TimeUnit), Functions.Multiples(fine, Strings.MoneyUnit));
    if(cmdr.getShip().HasGadget(GadgetType.HiddenCargoBays)) {
      while(cmdr.getShip().HasGadget(GadgetType.HiddenCargoBays)) {
        cmdr.getShip().RemoveEquipment(EquipmentType.Gadget, GadgetType.HiddenCargoBays);
      }
      Dialogs().alert(AlertType.JailHiddenCargoBaysRemoved);
    }
    if(cmdr.getShip().ReactorOnBoard()) {
      Dialogs().alert(AlertType.ReactorConfiscated);
      setQuestStatusReactor(SpecialEvent.StatusReactorNotStarted);
    }
    if(cmdr.getShip().SculptureOnBoard()) {
      Dialogs().alert(AlertType.SculptureConfiscated);
      setQuestStatusSculpture(SpecialEvent.StatusSculptureNotStarted);
    }
    if(cmdr.getShip().WildOnBoard()) {
      Dialogs().alert(AlertType.WildArrested);
      NewsAddEvent(NewsEvent.WildArrested);
      setQuestStatusWild(SpecialEvent.StatusWildNotStarted);
    }
    if(cmdr.getShip().AnyIllegalCargo()) {
      Dialogs().alert(AlertType.JailIllegalGoodsImpounded);
      cmdr.getShip().RemoveIllegalGoods();
    }
    if(cmdr.getInsurance()) {
      Dialogs().alert(AlertType.JailInsuranceLost);
      cmdr.setInsurance(false);
      cmdr.NoClaim(0);
    }
    if(cmdr.getShip().CrewCount() - cmdr.getShip().SpecialCrew().length > 1) {
      Dialogs().alert(AlertType.JailMercenariesLeave);
      for(int i = 1; i < cmdr.getShip().Crew().length; i++) {
        cmdr.getShip().Crew()[i] = null;
      }
    }
    if(cmdr.getShip().JarekOnBoard()) {
      Dialogs().alert(AlertType.JarekTakenHome);
      setQuestStatusJarek(SpecialEvent.StatusJarekNotStarted);
    }
    if(cmdr.getShip().PrincessOnBoard()) {
      Dialogs().alert(AlertType.PrincessTakenHome);
      setQuestStatusPrincess(SpecialEvent.StatusPrincessNotStarted);
    }
    if(getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
      Dialogs().alert(AlertType.AntidoteTaken);
      setQuestStatusJapori(SpecialEvent.StatusJaporiDone);
    }
    if(cmdr.getCash() >= fine) {
      cmdr.setCash(cmdr.getCash() - fine);
    } else {
      cmdr.setCash(Math.max(0, cmdr.getCash() + cmdr.getShip().Worth(true) - fine));
      Dialogs().alert(AlertType.JailShipSold);
      if(cmdr.getShip().getTribbles() > 0) {
        Dialogs().alert(AlertType.TribblesRemoved);
      }
      Dialogs().alert(AlertType.FleaBuilt);
      CreateFlea();
    }
    if(cmdr.getDebt() > 0) {
      int paydown = Math.min(cmdr.getCash(), cmdr.getDebt());
      cmdr.setDebt(cmdr.getDebt() - paydown);
      cmdr.setCash(cmdr.getCash() - paydown);
      if(cmdr.getDebt() > 0) {
        for(int i = 0; i < term; i++) {
          cmdr.PayInterest();
        }
      }
    }
    cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreDubious);
    IncDays(term);
  }

  public void CreateFlea() {
    cmdr.setShip(new Ship(ShipType.Flea));
    cmdr.getShip().Crew()[0] = Commander();
    cmdr.setInsurance(false);
    cmdr.NoClaim(0);
  }






  public void EscapeWithPod() {
    Dialogs().alert(AlertType.EncounterEscapePodActivated);
    if(cmdr.getShip().SculptureOnBoard()) {
      Dialogs().alert(AlertType.SculptureSaved);
    }
    if(cmdr.getShip().ReactorOnBoard()) {
      Dialogs().alert(AlertType.ReactorDestroyed);
      setQuestStatusReactor(SpecialEvent.StatusReactorDone);
    }
    if(cmdr.getShip().getTribbles() > 0) {
      Dialogs().alert(AlertType.TribblesKilled);
    }
    if(getQuestStatusJapori() == SpecialEvent.StatusJaporiInTransit) {
      int system;
      for(system = 0; system < _universe.length && _universe[system].SpecialEventType() != SpecialEventType.Japori; system++) {
      }
      Dialogs().alert(AlertType.AntidoteDestroyed, _universe[system].Name());
      setQuestStatusJapori(SpecialEvent.StatusJaporiNotStarted);
    }
    if(cmdr.getShip().ArtifactOnBoard()) {
      Dialogs().alert(AlertType.ArtifactLost);
      setQuestStatusArtifact(SpecialEvent.StatusArtifactDone);
    }
    if(cmdr.getShip().JarekOnBoard()) {
      Dialogs().alert(AlertType.JarekTakenHome);
      setQuestStatusJarek(SpecialEvent.StatusJarekNotStarted);
    }
    if(cmdr.getShip().PrincessOnBoard()) {
      Dialogs().alert(AlertType.PrincessTakenHome);
      setQuestStatusPrincess(SpecialEvent.StatusPrincessNotStarted);
    }
    if(cmdr.getShip().WildOnBoard()) {
      Dialogs().alert(AlertType.WildArrested);
      cmdr.setPoliceRecordScore(cmdr.getPoliceRecordScore() + Consts.ScoreCaughtWithWild);
      NewsAddEvent(NewsEvent.WildArrested);
      setQuestStatusWild(SpecialEvent.StatusWildNotStarted);
    }
    if(cmdr.getInsurance()) {
      Dialogs().alert(AlertType.InsurancePayoff);
      cmdr.setCash(cmdr.getCash() + cmdr.getShip().BaseWorth(true));
    }
    if(cmdr.getCash() > Consts.FleaConversionCost) {
      cmdr.setCash(cmdr.getCash() - Consts.FleaConversionCost);
    } else {
      cmdr.setDebt(cmdr.getDebt() + (Consts.FleaConversionCost - cmdr.getCash()));
      cmdr.setCash(0);
    }
    Dialogs().alert(AlertType.FleaBuilt);
    IncDays(3);
    CreateFlea();
  }

  public void HandleSpecialEvent() {
    StarSystem curSys = cmdr.CurrentSystem();
    Ship ship = cmdr.getShip();
    boolean remove = true;
    switch(curSys.SpecialEventType()) {
      case Artifact:
        setQuestStatusArtifact(SpecialEvent.StatusArtifactOnBoard);
        break;
      case ArtifactDelivery:
        setQuestStatusArtifact(SpecialEvent.StatusArtifactDone);
        break;
      case CargoForSale:
        Dialogs().alert(AlertType.SpecialSealedCanisters);
        int tradeItem = Functions.GetRandom(Consts.TradeItems.size());
        ship.Cargo()[tradeItem] += 3;
        cmdr.PriceCargo()[tradeItem] += cmdr.CurrentSystem().SpecialEvent().Price();
        break;
      case Dragonfly:
      case DragonflyBaratas:
      case DragonflyMelina:
      case DragonflyRegulas:
        setQuestStatusDragonfly(getQuestStatusDragonfly() + 1);
        break;
      case DragonflyDestroyed:
        curSys.SpecialEventType(SpecialEventType.DragonflyShield);
        remove = false;
        break;
      case DragonflyShield:
        if(ship.FreeSlotsShield() == 0) {
          Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          Dialogs().alert(AlertType.EquipmentLightningShield);
          ship.AddEquipment(Consts.Shields.get(ShieldType.Lightning.id));
          setQuestStatusDragonfly(SpecialEvent.StatusDragonflyDone);
        }
        break;
      case EraseRecord:
        Dialogs().alert(AlertType.SpecialCleanRecord);
        cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreClean);
        RecalculateSellPrices(curSys);
        break;
      case Experiment:
        setQuestStatusExperiment(SpecialEvent.StatusExperimentStarted);
        break;
      case ExperimentFailed:
        // The failure is narrative only: it changes neither the ship nor the quest,
        // so the event is kept in the system for the player to reread its story.
        remove = false;
        break;
      case ExperimentStopped:
        setQuestStatusExperiment(SpecialEvent.StatusExperimentCancelled);
        setCanSuperWarp(true);
        break;
      case Gemulon:
        setQuestStatusGemulon(SpecialEvent.StatusGemulonStarted);
        break;
      case GemulonFuel:
        if(ship.FreeSlotsGadget() == 0) {
          Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          Dialogs().alert(AlertType.EquipmentFuelCompactor);
          ship.AddEquipment(Consts.Gadgets.get(GadgetType.FuelCompactor.asInteger()));
          setQuestStatusGemulon(SpecialEvent.StatusGemulonDone);
        }
        break;
      case GemulonRescued:
        curSys.SpecialEventType(SpecialEventType.GemulonFuel);
        setQuestStatusGemulon(SpecialEvent.StatusGemulonFuel);
        remove = false;
        break;
      case GemulonInvaded:
        // Like ExperimentFailed, the invasion report is narrative only: the event is
        // kept so its bad news can be read again instead of vanishing.
        remove = false;
        break;
      case Japori:
        // The japori quest should not be removed since you can fail and start it over again.
        remove = false;
        if(ship.FreeCargoBays() < 10) {
          Dialogs().alert(AlertType.CargoNoEmptyBays);
        } else {
          Dialogs().alert(AlertType.AntidoteOnBoard);
          setQuestStatusJapori(SpecialEvent.StatusJaporiInTransit);
        }
        break;
      case JaporiDelivery:
        setQuestStatusJapori(SpecialEvent.StatusJaporiDone);
        cmdr.IncreaseRandomSkill();
        cmdr.IncreaseRandomSkill();
        break;
      case Jarek:
        if(ship.FreeCrewQuarters() == 0) {
          Dialogs().alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else {
          CrewMember jarek = Mercenaries()[CrewMemberId.Jarek.CastToInt()];
          Dialogs().alert(AlertType.SpecialPassengerOnBoard, jarek.Name());
          ship.Hire(jarek);
          setQuestStatusJarek(SpecialEvent.StatusJarekStarted);
        }
        break;
      case JarekGetsOut:
        setQuestStatusJarek(SpecialEvent.StatusJarekDone);
        ship.Fire(CrewMemberId.Jarek);
        break;
      case Lottery:
        break;
      case Moon:
        Dialogs().alert(AlertType.SpecialMoonBought);
        setQuestStatusMoon(SpecialEvent.StatusMoonBought);
        break;
      case MoonRetirement:
        setQuestStatusMoon(SpecialEvent.StatusMoonDone);
        throw new GameEndException(this, GameEndType.BoughtMoon);
      case Princess:
        curSys.SpecialEventType(SpecialEventType.PrincessReturned);
        remove = false;
        setQuestStatusPrincess(getQuestStatusPrincess() + 1);
        break;
      case PrincessCentauri:
      case PrincessInthara:
        setQuestStatusPrincess(getQuestStatusPrincess() + 1);
        break;
      case PrincessQonos:
        if(ship.FreeCrewQuarters() == 0) {
          Dialogs().alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else {
          CrewMember princess = Mercenaries()[CrewMemberId.Princess.CastToInt()];
          Dialogs().alert(AlertType.SpecialPassengerOnBoard, princess.Name());
          ship.Hire(princess);
        }
        break;
      case PrincessQuantum:
        if(ship.FreeSlotsWeapon() == 0) {
          Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          Dialogs().alert(AlertType.EquipmentQuantumDisruptor);
          ship.AddEquipment(Consts.WeapObjs.get(WeaponType.QuantumDistruptor.id));
          setQuestStatusPrincess(SpecialEvent.StatusPrincessDone);
        }
        break;
      case PrincessReturned:
        ship.Fire(CrewMemberId.Princess);
        curSys.SpecialEventType(SpecialEventType.PrincessQuantum);
        setQuestStatusPrincess(SpecialEvent.StatusPrincessReturned);
        remove = false;
        break;
      case Reactor:
        if(ship.FreeCargoBays() < 15) {
          Dialogs().alert(AlertType.CargoNoEmptyBays);
          remove = false;
        } else {
          if(ship.WildOnBoard()) {
            if(Dialogs().alert(AlertType.WildWontStayAboardReactor, curSys.Name()) == DialogResult.OK) {
              Dialogs().alert(AlertType.WildLeavesShip, curSys.Name());
              setQuestStatusWild(SpecialEvent.StatusWildNotStarted);
            } else {
              remove = false;
            }
          }
          if(remove) {
            Dialogs().alert(AlertType.ReactorOnBoard);
            setQuestStatusReactor(SpecialEvent.StatusReactorFuelOk);
          }
        }
        break;
      case ReactorDelivered:
        curSys.SpecialEventType(SpecialEventType.ReactorLaser);
        setQuestStatusReactor(SpecialEvent.StatusReactorDelivered);
        remove = false;
        break;
      case ReactorLaser:
        if(ship.FreeSlotsWeapon() == 0) {
          Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          Dialogs().alert(AlertType.EquipmentMorgansLaser);
          ship.AddEquipment(Consts.WeapObjs.get(WeaponType.MorgansLaser.id));
          setQuestStatusReactor(SpecialEvent.StatusReactorDone);
        }
        break;
      case Scarab:
        setQuestStatusScarab(SpecialEvent.StatusScarabHunting);
        break;
      case ScarabDestroyed:
        setQuestStatusScarab(SpecialEvent.StatusScarabDestroyed);
        curSys.SpecialEventType(SpecialEventType.ScarabUpgradeHull);
        remove = false;
        break;
      case ScarabUpgradeHull:
        Dialogs().alert(AlertType.ShipHullUpgraded);
        ship.setHullUpgraded(true);
        ship.setHull(ship.getHull() + Consts.HullUpgrade);
        setQuestStatusScarab(SpecialEvent.StatusScarabDone);
        remove = false;
        break;
      case Sculpture:
        setQuestStatusSculpture(SpecialEvent.StatusSculptureInTransit);
        break;
      case SculptureDelivered:
        setQuestStatusSculpture(SpecialEvent.StatusSculptureDelivered);
        curSys.SpecialEventType(SpecialEventType.SculptureHiddenBays);
        remove = false;
        break;
      case SculptureHiddenBays:
        setQuestStatusSculpture(SpecialEvent.StatusSculptureDone);
        if(ship.FreeSlotsGadget() == 0) {
          Dialogs().alert(AlertType.EquipmentNotEnoughSlots);
          remove = false;
        } else {
          Dialogs().alert(AlertType.EquipmentHiddenCompartments);
          ship.AddEquipment(Consts.Gadgets.get(GadgetType.HiddenCargoBays.asInteger()));
          setQuestStatusSculpture(SpecialEvent.StatusSculptureDone);
        }
        break;
      case Skill:
        Dialogs().alert(AlertType.SpecialSkillIncrease);
        cmdr.IncreaseRandomSkill();
        break;
      case SpaceMonster:
        setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterAtAcamar);
        break;
      case SpaceMonsterKilled:
        setQuestStatusSpaceMonster(SpecialEvent.StatusSpaceMonsterDone);
        break;
      case Tribble:
        Dialogs().alert(AlertType.TribblesOwn);
        ship.setTribbles(1);
        break;
      case TribbleBuyer:
        Dialogs().alert(AlertType.TribblesGone);
        cmdr.setCash(cmdr.getCash() + (ship.getTribbles() / 2));
        ship.setTribbles(0);
        break;
      case Wild:
        if(ship.FreeCrewQuarters() == 0) {
          Dialogs().alert(AlertType.SpecialNoQuarters);
          remove = false;
        } else if(!ship.HasWeapon(WeaponType.BeamLaser, false)) {
          Dialogs().alert(AlertType.WildWontBoardLaser);
          remove = false;
        } else if(ship.ReactorOnBoard()) {
          Dialogs().alert(AlertType.WildWontBoardReactor);
          remove = false;
        } else {
          CrewMember wild = Mercenaries()[CrewMemberId.Wild.CastToInt()];
          Dialogs().alert(AlertType.SpecialPassengerOnBoard, wild.Name());
          ship.Hire(wild);
          setQuestStatusWild(SpecialEvent.StatusWildStarted);
          if(ship.SculptureOnBoard()) {
            Dialogs().alert(AlertType.WildSculpture);
          }
        }
        break;
      case WildGetsOut:
        // Zeethibal has a 10 in player's lowest score, an 8 in the next lowest score, and 5 elsewhere.
        CrewMember zeethibal = Mercenaries()[CrewMemberId.Zeethibal.CastToInt()];
        zeethibal.CurrentSystem(_universe[StarSystemId.Kravat.CastToInt()]);
        int lowest1 = cmdr.NthLowestSkill(1);
        int lowest2 = cmdr.NthLowestSkill(2);
        for(int i = 0; i < zeethibal.Skills().length; i++) {
          zeethibal.Skills()[i] = (i == lowest1 ? 10 : (i == lowest2 ? 8 : 5));
        }
        setQuestStatusWild(SpecialEvent.StatusWildDone);
        cmdr.setPoliceRecordScore(Consts.PoliceRecordScoreClean);
        ship.Fire(CrewMemberId.Wild);
        RecalculateSellPrices(curSys);
        break;
      default:
        break;
    }
    if(curSys.SpecialEvent().Price() != 0) {
      cmdr.setCash(cmdr.getCash() - curSys.SpecialEvent().Price());
    }
    if(remove) {
      curSys.SpecialEventType(SpecialEventType.NA);
    }
  }

  public void IncDays(int num) {
    cmdr.setDays(cmdr.getDays() + num);
    if(cmdr.getInsurance()) {
      cmdr.NoClaim(cmdr.NoClaim() + num);
    }
    // Police Record will gravitate towards neutral (0).
    if(cmdr.getPoliceRecordScore() > Consts.PoliceRecordScoreClean) {
      cmdr.setPoliceRecordScore(Math.max(Consts.PoliceRecordScoreClean, cmdr.getPoliceRecordScore() - num / 3));
    } else if(cmdr.getPoliceRecordScore() < Consts.PoliceRecordScoreDubious) {
      cmdr.setPoliceRecordScore(Math.min(Consts.PoliceRecordScoreDubious, cmdr.getPoliceRecordScore()
          + num / (_difficulty.CastToInt() <= Difficulty.Normal.CastToInt() ? 1 : _difficulty.CastToInt())));
    }
    // The Space Monster's strength increases 5% per day until it is back to full strength.
    if(_spaceMonster.getHull() < _spaceMonster.HullStrength()) {
      _spaceMonster.setHull(Math.min(_spaceMonster.HullStrength(), (int)(_spaceMonster.getHull() * Math.pow(1.05, num))));
    }
    if(getQuestStatusGemulon() > SpecialEvent.StatusGemulonNotStarted && getQuestStatusGemulon() < SpecialEvent.StatusGemulonTooLate) {
      setQuestStatusGemulon(Math.min(getQuestStatusGemulon() + num, SpecialEvent.StatusGemulonTooLate));
      if(getQuestStatusGemulon() == SpecialEvent.StatusGemulonTooLate) {
        StarSystem gemulon = _universe[StarSystemId.Gemulon.CastToInt()];
        gemulon.SpecialEventType(SpecialEventType.GemulonInvaded);
        gemulon.TechLevel(TechLevel.t0);
        gemulon.PoliticalSystemType(PoliticalSystemType.Anarchy);
      }
    }
    if(cmdr.getShip().ReactorOnBoard()) {
      setQuestStatusReactor(Math.min(getQuestStatusReactor() + num, SpecialEvent.StatusReactorDate));
    }
    if(getQuestStatusExperiment() > SpecialEvent.StatusExperimentNotStarted
        && getQuestStatusExperiment() < SpecialEvent.StatusExperimentPerformed) {
      setQuestStatusExperiment(Math.min(getQuestStatusExperiment() + num, SpecialEvent.StatusExperimentPerformed));
      if(getQuestStatusExperiment() == SpecialEvent.StatusExperimentPerformed) {
        setFabricRipProbability(Consts.FabricRipInitialProbability);
        _universe[StarSystemId.Daled.CastToInt()].SpecialEventType(SpecialEventType.ExperimentFailed);
        Dialogs().alert(AlertType.SpecialExperimentPerformed);
        NewsAddEvent(NewsEvent.ExperimentPerformed);
      }
    } else if(getQuestStatusExperiment() == SpecialEvent.StatusExperimentPerformed && getFabricRipProbability() > 0) {
      setFabricRipProbability(getFabricRipProbability() - num);
    }
    if(cmdr.getShip().JarekOnBoard()) {
      if(getQuestStatusJarek() == SpecialEvent.StatusJarekImpatient / 2) {
        Dialogs().alert(AlertType.SpecialPassengerConcernedJarek);
      } else if(getQuestStatusJarek() == SpecialEvent.StatusJarekImpatient - 1) {
        Dialogs().alert(AlertType.SpecialPassengerImpatientJarek);
        Mercenaries()[CrewMemberId.Jarek.CastToInt()].Pilot(0);
        Mercenaries()[CrewMemberId.Jarek.CastToInt()].Fighter(0);
        Mercenaries()[CrewMemberId.Jarek.CastToInt()].Trader(0);
        Mercenaries()[CrewMemberId.Jarek.CastToInt()].Engineer(0);
      }
      if(getQuestStatusJarek() < SpecialEvent.StatusJarekImpatient) {
        setQuestStatusJarek(getQuestStatusJarek() + 1);
      }
    }
    if(cmdr.getShip().PrincessOnBoard()) {
      if(getQuestStatusPrincess() == (SpecialEvent.StatusPrincessImpatient + SpecialEvent.StatusPrincessRescued) / 2) {
        Dialogs().alert(AlertType.SpecialPassengerConcernedPrincess);
      } else if(getQuestStatusPrincess() == SpecialEvent.StatusPrincessImpatient - 1) {
        Dialogs().alert(AlertType.SpecialPassengerImpatientPrincess);
        Mercenaries()[CrewMemberId.Princess.CastToInt()].Pilot(0);
        Mercenaries()[CrewMemberId.Princess.CastToInt()].Fighter(0);
        Mercenaries()[CrewMemberId.Princess.CastToInt()].Trader(0);
        Mercenaries()[CrewMemberId.Princess.CastToInt()].Engineer(0);
      }
      if(getQuestStatusPrincess() < SpecialEvent.StatusPrincessImpatient) {
        setQuestStatusPrincess(getQuestStatusPrincess() + 1);
      }
    }
    if(cmdr.getShip().WildOnBoard()) {
      if(getQuestStatusWild() == SpecialEvent.StatusWildImpatient / 2) {
        Dialogs().alert(AlertType.SpecialPassengerConcernedWild);
      } else if(getQuestStatusWild() == SpecialEvent.StatusWildImpatient - 1) {
        Dialogs().alert(AlertType.SpecialPassengerImpatientWild);
        Mercenaries()[CrewMemberId.Wild.CastToInt()].Pilot(0);
        Mercenaries()[CrewMemberId.Wild.CastToInt()].Fighter(0);
        Mercenaries()[CrewMemberId.Wild.CastToInt()].Trader(0);
        Mercenaries()[CrewMemberId.Wild.CastToInt()].Engineer(0);
      }
      if(getQuestStatusWild() < SpecialEvent.StatusWildImpatient) {
        setQuestStatusWild(getQuestStatusWild() + 1);
      }
    }
  }

  public void NewsAddEvent(NewsEvent ne) {
    _newspaper.add(ne);
  }

  public void NewsAddEventsOnArrival() {
    _newspaper.addEventsOnArrival(cmdr.CurrentSystem(), this);
  }

  public void NewsReplaceEvent(int oldEvent, int newEvent) {
    _newspaper.replace(oldEvent, newEvent);
  }

  public void NewsResetEvents() {
    _newspaper.reset();
  }

  public void RecalculateBuyPrices(StarSystem system) {
    _market.recalculateBuyPrices(system, cmdr.getPoliceRecordScore(), cmdr.getShip().Trader());
  }

  public void RecalculateSellPrices(StarSystem system) { // After erasure of police record, selling prices must be recalculated
    _market.recalculateSellPrices();
  }

  public void ResetVeryRareEncounters() {
    _veryRareEncounters.clear();
    _veryRareEncounters.add(VeryRareEncounter.MarieCeleste);
    _veryRareEncounters.add(VeryRareEncounter.CaptainAhab);
    _veryRareEncounters.add(VeryRareEncounter.CaptainConrad);
    _veryRareEncounters.add(VeryRareEncounter.CaptainHuie);
    _veryRareEncounters.add(VeryRareEncounter.BottleOld);
    _veryRareEncounters.add(VeryRareEncounter.BottleGood);
  }

  public void SelectedSystemId(StarSystemId value) {
    _selectedSystemId = value;
    _warpSystemId = value;
    _targetWormhole = false;
  }

  public void ShowNewspaper() {
    if(!getPaidForNewspaper()) {
      int cost = _difficulty.CastToInt() + 1;
      if(cmdr.getCash() < cost) {
        Dialogs().alert(AlertType.ArrivalIFNewspaper, Functions.Multiples(cost, "credit"));
      } else if(_options.getNewsAutoPay()
          || Dialogs().alert(AlertType.ArrivalBuyNewspaper, Functions.Multiples(cost, "credit")) == DialogResult.Yes) {
        cmdr.setCash(cmdr.getCash() - cost);
        setPaidForNewspaper(true);
        getParentWindow().UpdateAll();
      }
    }
    if(getPaidForNewspaper() && getParentWindow() != null) {
      getParentWindow().showNewspaper();
    }
  }

  public void TargetWormhole(boolean b) {
    _targetWormhole = b;
    if(_targetWormhole) {
      int wormIndex = Util.BruteSeek(_wormholes, _selectedSystemId.CastToInt());
      _warpSystemId = StarSystemId.FromInt(_wormholes[(wormIndex + 1) % _wormholes.length]);
    }
  }

  public void Warp(boolean viaSingularity) {
    if(cmdr.getDebt() > Consts.DebtTooLarge) {
      Dialogs().alert(AlertType.DebtTooLargeGrounded);
    } else if(cmdr.getCash() < MercenaryCosts()) {
      Dialogs().alert(AlertType.LeavingIFMercenaries);
    } else if(cmdr.getCash() < MercenaryCosts() + InsuranceCosts()) {
      Dialogs().alert(AlertType.LeavingIFInsurance);
    } else if(cmdr.getCash() < MercenaryCosts() + InsuranceCosts() + WormholeCosts()) {
      Dialogs().alert(AlertType.LeavingIFWormholeTax);
    } else {
      boolean wildOk = true;
      // if Wild is aboard, make sure ship is armed!
      if(cmdr.getShip().WildOnBoard() && !cmdr.getShip().HasWeapon(WeaponType.BeamLaser, false)) {
        if(Dialogs().alert(AlertType.WildWontStayAboardLaser, cmdr.CurrentSystem().Name()) == DialogResult.Cancel) {
          wildOk = false;
        } else {
          Dialogs().alert(AlertType.WildLeavesShip, cmdr.CurrentSystem().Name());
          setQuestStatusWild(SpecialEvent.StatusWildNotStarted);
        }
      }
      if(wildOk) {
        setArrivedViaWormhole(Functions.WormholeExists(cmdr.CurrentSystem(), WarpSystem()));
        if(viaSingularity) {
          NewsAddEvent(NewsEvent.ExperimentArrival);
        } else {
          NormalDeparture(viaSingularity || getArrivedViaWormhole() ? 0 : Functions.Distance(cmdr.CurrentSystem(), WarpSystem()));
        }
        cmdr.CurrentSystem().CountDown(CountDownStart());
        NewsResetEvents();
        CalculatePrices(WarpSystem());
        // Clicks will be -1 if we were arrested or used the escape pod.
        if(!Travel()) {
          Dialogs().alert(AlertType.TravelUneventfulTrip);
        }
        Arrival();
      }
    }
  }

  public void setArrivedViaWormhole(boolean arrivedViaWormhole) {
    _arrivedViaWormhole = arrivedViaWormhole;
  }

  public void setAutoSave(boolean autoSave) {
    _autoSave = autoSave;
  }

  public void setCanSuperWarp(boolean canSuperWarp) {
    _canSuperWarp = canSuperWarp;
  }

  public void setClicks(int clicks) {
    _clicks = clicks;
  }








  public void setEndStatus(GameEndType endStatus) {
    _endStatus = endStatus;
  }

  public void setFabricRipProbability(int fabricRipProbability) {
    _fabricRipProbability = fabricRipProbability;
  }





  public void setPaidForNewspaper(boolean paidForNewspaper) {
    _newspaper.paid(paidForNewspaper);
  }

  public void setQuestStatusArtifact(int questStatusArtifact) {
    _questStatusArtifact = questStatusArtifact;
  }

  public void setQuestStatusDragonfly(int questStatusDragonfly) {
    _questStatusDragonfly = questStatusDragonfly;
  }

  public void setQuestStatusExperiment(int questStatusExperiment) {
    _questStatusExperiment = questStatusExperiment;
  }

  public void setQuestStatusGemulon(int questStatusGemulon) {
    _questStatusGemulon = questStatusGemulon;
  }

  public void setQuestStatusJapori(int questStatusJapori) {
    _questStatusJapori = questStatusJapori;
  }

  public void setQuestStatusJarek(int questStatusJarek) {
    _questStatusJarek = questStatusJarek;
  }

  public void setQuestStatusMoon(int questStatusMoon) {
    _questStatusMoon = questStatusMoon;
  }

  public void setQuestStatusPrincess(int questStatusPrincess) {
    _questStatusPrincess = questStatusPrincess;
  }

  public void setQuestStatusReactor(int questStatusReactor) {
    _questStatusReactor = questStatusReactor;
  }

  public void setQuestStatusScarab(int questStatusScarab) {
    _questStatusScarab = questStatusScarab;
  }

  public void setQuestStatusSculpture(int questStatusSculpture) {
    _questStatusSculpture = questStatusSculpture;
  }

  public void setQuestStatusSpaceMonster(int questStatusSpaceMonster) {
    _questStatusSpaceMonster = questStatusSpaceMonster;
  }

  public void setQuestStatusWild(int questStatusWild) {
    _questStatusWild = questStatusWild;
  }


  public void setSelectedSystemByName(String value) {
    String nameToFind = value;
    boolean found = false;
    for(int i = 0; i < _universe.length && !found; i++) {
      String name = _universe[i].Name();
      if(name.toLowerCase().indexOf(nameToFind.toLowerCase()) >= 0) {
        SelectedSystemId(StarSystemId.FromInt(i));
        found = true;
      }
    }
  }

  public void setTrackedSystemId(StarSystemId trackedSystemId) {
    _trackedSystemId = trackedSystemId;
  }

  public void setTribbleMessage(boolean b) {
    _tribbleMessage = b;
  }

  public static Game CurrentGame() {
    return game;
  }

  public static void CurrentGame(Game g) {
    game = g;
  }

  /** The encounter of the current trip: its state and its rules. */
  public Encounter encounter() {
    if(_encounter == null) {
      _encounter = new Encounter(this);
    }
    return _encounter;
  }
}
