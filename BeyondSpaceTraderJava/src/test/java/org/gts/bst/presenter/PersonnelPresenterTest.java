package org.gts.bst.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.gts.bst.crew.CrewMemberId;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelView;
import org.gts.bst.view.PersonnelViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spacetrader.CrewMember;
import spacetrader.Game;
import spacetrader.Ship;
import spacetrader.Strings;
import spacetrader.TestDialogService;
import spacetrader.enums.AlertType;


class PersonnelPresenterTest {
  @BeforeAll
  static void formattingIsLocaleStable() {
    Locale.setDefault(Locale.US);
  }

  @Test
  void rendersTheEmptyCrewListOfTheStartingShip() {
    FakeView view = new FakeView();

    new PersonnelPresenter(newGame(new TestDialogService()), view).update();

    assertTrue(view.model.crewEntries().isEmpty());
    assertFalse(view.model.crewVisible());
    assertEquals(Strings.PersonnelNoQuarters, view.model.crewEmptyText());
    assertFalse(view.info.visible());
  }

  @Test
  void showsTheSelectedMercenary() {
    Game game = newGame(new TestDialogService());
    CrewMember merc = moveToAMercenarySystem(game);
    FakeView view = new FakeView();
    PersonnelPresenter presenter = new PersonnelPresenter(game, view);
    presenter.update();

    presenter.selectForHire(forHireIndex(game, merc));

    assertTrue(view.info.visible());
    assertEquals(merc.Name(), view.info.name());
    assertEquals(merc.Pilot() + "", view.info.pilot());
    assertEquals(Strings.MercenaryHire, view.info.hireFireText());
  }

  @Test
  void warnsWhenThereAreNoQuarters() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    CrewMember merc = moveToAMercenarySystem(game);
    FakeView view = new FakeView();
    PersonnelPresenter presenter = new PersonnelPresenter(game, view);
    presenter.update();
    presenter.selectForHire(forHireIndex(game, merc));

    assertFalse(presenter.hireFire());
    assertEquals(List.of(AlertType.CrewNoQuarters), dialogs.alerts());
  }

  @Test
  void hiresAndFiresAMercenary() {
    TestDialogService dialogs = new TestDialogService();
    Game game = newGame(dialogs);
    Ship ship = new Ship(ShipType.Beetle);
    game.Commander().setShip(ship);
    ship.Crew()[0] = game.Commander();
    CrewMember merc = moveToAMercenarySystem(game);
    FakeView view = new FakeView();
    PersonnelPresenter presenter = new PersonnelPresenter(game, view);
    presenter.update();
    presenter.selectForHire(forHireIndex(game, merc));

    assertTrue(presenter.hireFire());
    assertTrue(ship.HasCrew(merc.Id()));
    assertFalse(view.info.visible());

    presenter.selectCrew(0);
    assertEquals(merc.Name(), view.info.name());
    assertEquals(Strings.MercenaryFire, view.info.hireFireText());

    dialogs.setResult(DialogResult.Yes);
    assertTrue(presenter.hireFire());
    assertFalse(ship.HasCrew(merc.Id()));
  }

  private static Game newGame(TestDialogService dialogs) {
    return new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, null, dialogs);
  }

  private static CrewMember moveToAMercenarySystem(Game game) {
    for(CrewMember candidate : game.Mercenaries()) {
      if(candidate != null && candidate.Id() != CrewMemberId.Commander && candidate.CurrentSystem() != null) {
        game.Commander().CurrentSystem(candidate.CurrentSystem());
        return candidate;
      }
    }
    throw new AssertionError("no mercenaries in the universe");
  }

  private static int forHireIndex(Game game, CrewMember merc) {
    CrewMember[] forHire = game.Commander().CurrentSystem().MercenariesForHire();
    for(int i = 0; i < forHire.length; i++) {
      if(forHire[i] == merc) {
        return i;
      }
    }
    throw new AssertionError("mercenary not for hire");
  }

  private static class FakeView implements PersonnelView {
    private PersonnelViewModel model;
    private PersonnelInfo info;

    @Override
    public void render(PersonnelViewModel model) {
      this.model = model;
      this.info = model.info();
    }

    @Override
    public void renderInfo(PersonnelInfo info) {
      this.info = info;
    }
  }
}
