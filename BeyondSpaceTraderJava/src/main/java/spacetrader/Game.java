package spacetrader;
import org.gts.bst.ports.DialogResult;
import org.gts.bst.ports.GameWindow;
import org.gts.bst.cargo.TradeItem;
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
import org.gts.bst.ship.equip.WeaponType;
import org.gts.bst.ports.DialogService;
import org.gts.bst.ports.EncounterDialogHost;
import spacetrader.enums.AlertType;
import spacetrader.enums.GameEndType;
import spacetrader.enums.OpponentType;
import spacetrader.enums.SkillType;
import spacetrader.enums.StarSystemId;
import java.util.ArrayList;
import spacetrader.util.Hashtable;
import spacetrader.util.Util;

public final class Game extends STSerializableObject implements QuestStates {
  private static Game game;
  private Commander cmdr;
  // Game Data
  private Universe _universe;
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
  private final Quests _quests;
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
    _universe = Universe.generate();
    cmdr = NewGameSetup.InitializeCommander(name,
        new CrewMember(CrewMemberId.Commander, pilot, fighter, trader, engineer, StarSystemId.NA), _universe.systems(), Mercenaries());
    NewGameSetup.GenerateCrewMemberList(Mercenaries(), _universe.systems().length, _difficulty);
    NewGameSetup.CreateShips(Dragonfly(), _scarab, _scorpion, _spaceMonster, Mercenaries());
    _quests = new Quests(cmdr, _universe, _market, _newspaper, _mercenaries, _spaceMonster, _difficulty, _dialogs, questConsequences());
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
    _universe = Universe.from(hash);
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
    _newspaper.loadFrom(hash);
    encounter().setLitterWarning(GetValueFromHash(hash, "_litterWarning", encounter().getLitterWarning()));
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
    _market.loadFrom(hash);
    _quests = new Quests(cmdr, _universe, _market, _newspaper, _mercenaries, _spaceMonster, _difficulty, _dialogs, questConsequences());
    _quests.loadFrom(hash);
    encounter().setJustLootedMarie(GetValueFromHash(hash, "_justLootedMarie", encounter().getJustLootedMarie()));
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
    _universe.saveTo(ht);
    ht.add("_commander", cmdr.Serialize());
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
    _newspaper.saveTo(ht);
    ht.add("_litterWarning", encounter().getLitterWarning());
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
    _market.saveTo(ht);
    _quests.saveTo(ht);
    ht.add("_justLootedMarie", encounter().getJustLootedMarie());
    ht.add("_chanceOfVeryRareEncounter", _chanceOfVeryRareEncounter);
    ht.add("_veryRareEncounters", ArrayListToIntArray(_veryRareEncounters));
    ht.add("_options", _options.Serialize());
    return ht;
  }


  private void Arrival() {
    new Arrival(cmdr, _universe, _market, _newspaper, _options, _difficulty, Dialogs(), this,
        arrivalNavigation(), arrivalReactor(), () -> setTribbleMessage(false), arrivalConsequences()).arrive(WarpSystem());
  }

  /** The tracking callbacks the arrival uses, bound to the game state. */
  private Arrival.Navigation arrivalNavigation() {
    return new Arrival.Navigation() {
      @Override
      public StarSystem trackedSystem() {
        return TrackedSystem();
      }

      @Override
      public void clearTracked() {
        setTrackedSystemId(StarSystemId.NA);
      }
    };
  }

  /** The reactor mission state the arrival reads and writes, owned by the quests. */
  private Arrival.ReactorStatus arrivalReactor() {
    return _quests;
  }

  /**
   * The consequences of the arrival that only the game can run. {@code destroyed()}
   * throws the end-of-game exception in the middle of the arrival, as it always did.
   */
  private Arrival.Consequences arrivalConsequences() {
    return new Arrival.Consequences() {
      @Override
      public void escapeWithPod() {
        EscapeWithPod();
      }

      @Override
      public void destroyed() {
        throw new GameEndException(Game.this, GameEndType.Killed);
      }

      @Override
      public void showNewspaper() {
        ShowNewspaper();
      }
    };
  }

  /**
   * The consequences of the special events that only the game can run.
   * {@code retired()} throws the end-of-game exception in the middle of the
   * event, as it always did.
   */
  private Quests.Consequences questConsequences() {
    return new Quests.Consequences() {
      @Override
      public void retired() {
        throw new GameEndException(Game.this, GameEndType.BoughtMoon);
      }
    };
  }

  private void CalculatePrices(StarSystem system) {
    _market.calculate(system, cmdr.getPoliceRecordScore(), cmdr.getShip().Trader());
  }







  // Ref #229: the departure costs and days stay in Game, with the travel loop.
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
    return (_selectedSystemId == StarSystemId.NA ? null : _universe.systems()[_selectedSystemId.CastToInt()]);
  }

  public StarSystem TrackedSystem() {
    return _trackedSystemId == StarSystemId.NA ? null : _universe.systems()[_trackedSystemId.CastToInt()];
  }

  public StarSystem WarpSystem() {
    return _warpSystemId == StarSystemId.NA ? null : _universe.systems()[_warpSystemId.CastToInt()];
  }

  public StarSystem[] Universe() {
    return _universe.systems();
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
    return _newspaper.text(cmdr, _universe.systems(), _difficulty);
  }

  @SuppressWarnings("fallthrough")





  public boolean getArrivedViaWormhole() {
    return _arrivedViaWormhole;
  }

  public boolean getAutoSave() {
    return _autoSave;
  }

  public boolean getCanSuperWarp() {
    return _quests.canSuperWarp();
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
      SelectedSystemId(StarSystemId.FromInt(Functions.GetRandom(_universe.systems().length)));
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
    return _quests.fabricRipProbability();
  }

  public int getQuestStatusArtifact() {
    return _quests.questStatusArtifact();
  }

  public int getQuestStatusDragonfly() {
    return _quests.questStatusDragonfly();
  }

  public int getQuestStatusExperiment() {
    return _quests.questStatusExperiment();
  }

  public int getQuestStatusGemulon() {
    return _quests.questStatusGemulon();
  }

  public int getQuestStatusJapori() {
    return _quests.questStatusJapori();
  }

  public int getQuestStatusJarek() {
    return _quests.questStatusJarek();
  }

  public int getQuestStatusMoon() {
    return _quests.questStatusMoon();
  }

  public int getQuestStatusPrincess() {
    return _quests.questStatusPrincess();
  }

  public int getQuestStatusReactor() {
    return _quests.reactorStatus();
  }

  public int getQuestStatusScarab() {
    return _quests.questStatusScarab();
  }

  public int getQuestStatusSculpture() {
    return _quests.questStatusSculpture();
  }

  public int getQuestStatusSpaceMonster() {
    return _quests.questStatusSpaceMonster();
  }

  public int getQuestStatusWild() {
    return _quests.questStatusWild();
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
    return _universe.wormholes();
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
      StarSystem[] universe = _universe.systems();
      int system;
      for(system = 0; system < universe.length && universe[system].SpecialEventType() != SpecialEventType.Japori; system++) {
      }
      Dialogs().alert(AlertType.AntidoteDestroyed, universe[system].Name());
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
    _quests.handleSpecialEvent();
  }

  public void IncDays(int num) {
    _quests.incDays(num);
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
      int[] wormholes = _universe.wormholes();
      int wormIndex = Util.BruteSeek(wormholes, _selectedSystemId.CastToInt());
      _warpSystemId = StarSystemId.FromInt(wormholes[(wormIndex + 1) % wormholes.length]);
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
    _quests.canSuperWarp(canSuperWarp);
  }

  public void setClicks(int clicks) {
    _clicks = clicks;
  }








  public void setEndStatus(GameEndType endStatus) {
    _endStatus = endStatus;
  }

  public void setFabricRipProbability(int fabricRipProbability) {
    _quests.fabricRipProbability(fabricRipProbability);
  }





  public void setPaidForNewspaper(boolean paidForNewspaper) {
    _newspaper.paid(paidForNewspaper);
  }

  public void setQuestStatusArtifact(int questStatusArtifact) {
    _quests.questStatusArtifact(questStatusArtifact);
  }

  public void setQuestStatusDragonfly(int questStatusDragonfly) {
    _quests.questStatusDragonfly(questStatusDragonfly);
  }

  public void setQuestStatusExperiment(int questStatusExperiment) {
    _quests.questStatusExperiment(questStatusExperiment);
  }

  public void setQuestStatusGemulon(int questStatusGemulon) {
    _quests.questStatusGemulon(questStatusGemulon);
  }

  public void setQuestStatusJapori(int questStatusJapori) {
    _quests.questStatusJapori(questStatusJapori);
  }

  public void setQuestStatusJarek(int questStatusJarek) {
    _quests.questStatusJarek(questStatusJarek);
  }

  public void setQuestStatusMoon(int questStatusMoon) {
    _quests.questStatusMoon(questStatusMoon);
  }

  public void setQuestStatusPrincess(int questStatusPrincess) {
    _quests.questStatusPrincess(questStatusPrincess);
  }

  public void setQuestStatusReactor(int questStatusReactor) {
    _quests.reactorStatus(questStatusReactor);
  }

  public void setQuestStatusScarab(int questStatusScarab) {
    _quests.questStatusScarab(questStatusScarab);
  }

  public void setQuestStatusSculpture(int questStatusSculpture) {
    _quests.questStatusSculpture(questStatusSculpture);
  }

  public void setQuestStatusSpaceMonster(int questStatusSpaceMonster) {
    _quests.questStatusSpaceMonster(questStatusSpaceMonster);
  }

  public void setQuestStatusWild(int questStatusWild) {
    _quests.questStatusWild(questStatusWild);
  }


  public void setSelectedSystemByName(String value) {
    String nameToFind = value;
    boolean found = false;
    StarSystem[] universe = _universe.systems();
    for(int i = 0; i < universe.length && !found; i++) {
      String name = universe[i].Name();
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
