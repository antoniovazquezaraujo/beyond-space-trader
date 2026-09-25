package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.view.MainStatusViewModel;
import org.gts.bst.view.MainView;
import org.gts.bst.view.SystemInfoViewModel;
import org.gts.bst.view.TargetSystemViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.StarSystem;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.enums.StarSystemId;


class MainPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void withoutAGameShowsTheEmptyState() {
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> null, view);

    presenter.updateStatusBar();
    presenter.updateSystemInfo();
    presenter.updateTargetSystemInfo();

    assertEquals("", view.status.cash());
    assertEquals("No Game Loaded.", view.status.extra());
    assertEquals("", view.system.name());
    assertFalse(view.system.newsVisible());
    assertFalse(view.target.navigationVisible());
    assertFalse(view.target.warpVisible());
  }

  @Test
  void showsTheStatusBarAndTheCurrentSystem() {
    Game game = newGame();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateStatusBar();
    presenter.updateSystemInfo();

    assertEquals("Cash: 1,000 cr.", view.status.cash());
    assertEquals("Bays: 0/15", view.status.bays());
    assertEquals("Current Costs: 0 cr.", view.status.costs());
    StarSystem system = game.Commander().CurrentSystem();
    assertEquals(system.Name(), view.system.name());
    assertEquals(Strings.Sizes[system.Size().CastToInt()], view.system.size());
    assertEquals(system.TechLevel().name, view.system.tech());
    assertTrue(view.system.pressurePreVisible());
    assertTrue(view.system.newsVisible());
  }

  @Test
  void showsTheTargetSystem() {
    Game game = newGame();
    game.SelectedSystemId(StarSystemId.FromInt(0));
    StarSystem target = game.WarpSystem();
    FakeView view = new FakeView();
    MainPresenter presenter = new MainPresenter(() -> game, view);

    presenter.updateTargetSystemInfo();

    assertTrue(view.target.navigationVisible());
    assertEquals(target.Name(), view.target.name());
    assertEquals("" + Functions.Distance(game.Commander().CurrentSystem(), target), view.target.distance());
    assertEquals(target.DestOk(), view.target.warpVisible());
  }

  private static Game newGame() {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, new TestDialogService());
  }

  private static class FakeView implements MainView {
    private MainStatusViewModel status;
    private SystemInfoViewModel system;
    private TargetSystemViewModel target;

    @Override
    public void renderStatusBar(MainStatusViewModel model) {
      status = model;
    }

    @Override
    public void renderSystemInfo(SystemInfoViewModel model) {
      system = model;
    }

    @Override
    public void renderTargetSystem(TargetSystemViewModel model) {
      target = model;
    }
  }
}
