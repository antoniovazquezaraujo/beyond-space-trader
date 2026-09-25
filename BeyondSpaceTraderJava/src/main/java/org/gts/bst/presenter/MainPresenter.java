package org.gts.bst.presenter;

import java.util.function.Supplier;
import org.gts.bst.view.MainStatusViewModel;
import org.gts.bst.view.MainView;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import spacetrader.Commander;
import spacetrader.CrewMember;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.StarSystem;
import spacetrader.Strings;


/**
 * Fills the main window areas from the model. No front-end types involved. The game
 * reference is provided through a supplier because the application replaces it when a
 * new game is started or a save is loaded.
 */
public class MainPresenter {
  private final Supplier<Game> gameSupplier;
  private final MainView view;

  public MainPresenter(Supplier<Game> gameSupplier, MainView view) {
    this.gameSupplier = gameSupplier;
    this.view = view;
  }

  public void updateStatusBar() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderStatusBar(new MainStatusViewModel("", "", "", "No Game Loaded."));
      return;
    }
    Commander cmdr = game.Commander();
    view.renderStatusBar(new MainStatusViewModel(
        "Cash: " + Functions.FormatMoney(cmdr.getCash()),
        "Bays: " + cmdr.getShip().FilledCargoBays() + "/" + cmdr.getShip().CargoBays(),
        "Current Costs: " + Functions.FormatMoney(game.CurrentCosts()),
        ""));
  }

  public void updateSystemInfo() {
    Game game = gameSupplier.get();
    if(game == null || game.Commander().CurrentSystem() == null) {
      view.renderSystemInfo(new SystemInfoViewModel("", "", "", "", "", "", "", "", false, false, false, false, "", ""));
      return;
    }
    StarSystem system = game.Commander().CurrentSystem();
    CrewMember[] mercs = system.MercenariesForHire();
    boolean mercVisible = mercs.length > 0;
    boolean specialVisible = system.ShowSpecialButton();
    view.renderSystemInfo(new SystemInfoViewModel(
        system.Name(),
        Strings.Sizes[system.Size().CastToInt()],
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.SpecialResource().name,
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPolice().CastToInt()],
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPirates().CastToInt()],
        system.SystemPressure().name,
        true,
        true,
        mercVisible,
        specialVisible,
        mercVisible
            ? Functions.StringVars(Strings.MercenariesForHire,
                mercs.length == 1 ? mercs[0].Name() : mercs.length + Strings.Mercenaries)
            : "",
        specialVisible ? system.SpecialEvent().Title() : ""));
  }

  public void updateTargetSystemInfo() {
    Game game = gameSupplier.get();
    if(game == null) {
      view.renderTargetSystem(emptyTarget(false));
      return;
    }
    StarSystem system = game.WarpSystem();
    if(system == null) {
      view.renderTargetSystem(emptyTarget(true));
      return;
    }
    Commander cmdr = game.Commander();
    boolean outOfRange = !system.DestOk() && system != cmdr.CurrentSystem();
    view.renderTargetSystem(new TargetSystemViewModel(
        true,
        system.Name(),
        Strings.Sizes[system.Size().CastToInt()],
        system.TechLevel().name,
        system.PoliticalSystem().Name(),
        system.Visited() ? system.SpecialResource().name : Strings.Unknown,
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPolice().CastToInt()],
        Strings.ActivityLevels[system.PoliticalSystem().ActivityPirates().CastToInt()],
        "" + Functions.Distance(cmdr.CurrentSystem(), system),
        outOfRange,
        system.DestOk(),
        outOfRange && system != game.TrackedSystem()));
  }

  private static TargetSystemViewModel emptyTarget(boolean navigationVisible) {
    return new TargetSystemViewModel(navigationVisible, "", "", "", "", "", "", "", "", false, false, false);
  }
}
