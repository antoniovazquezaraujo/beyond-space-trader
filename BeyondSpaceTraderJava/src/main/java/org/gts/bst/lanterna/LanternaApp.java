/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialog;
import com.googlecode.lanterna.gui2.dialogs.FileDialog;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.File;
import java.io.IOException;
import org.gts.bst.difficulty.Difficulty;
import org.gts.bst.presenter.MainPresenter;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import org.gts.bst.view.LanternaDialogService;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.Strings;
import spacetrader.enums.AlertType;
import spacetrader.util.Hashtable;
import spacetrader.util.Util;


/**
 * Starts the game with the text UI. The model and the presenters are the same as in
 * the Swing front-end; only the views change.
 */
public final class LanternaApp {
  private static final int TOTAL_SKILL_POINTS = 20;

  private LanternaApp() {
  }

  public static void main(String[] args) throws IOException {
    createDirectories();
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
      gui.setTheme(LanternaTheme.create());
      LanternaDialogService dialogs = new LanternaDialogService(new LanternaAlertDialogHost(gui));
      Game[] game = new Game[1];
      LanternaMainWindow window = new LanternaMainWindow(() -> game[0], gui);
      window.setGameActions(
          () -> newGame(gui, window, dialogs, game),
          () -> saveGame(gui, dialogs, game[0], window),
          () -> loadGame(gui, window, dialogs, game));
      game[0] = new Game("Antonio", Difficulty.Normal, 4, 4, 4, 4, window, dialogs);
      MainPresenter presenter = new MainPresenter(() -> game[0], window);
      window.setPresenter(presenter);
      presenter.updateAll();
      gui.addWindowAndWait(window.asWindow());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  private static void createDirectories() {
    String[] paths = {
      Consts.CustomDirectory,
      Consts.CustomImagesDirectory,
      Consts.CustomTemplatesDirectory,
      Consts.DataDirectory,
      Consts.SaveDirectory
    };
    for(String path : paths) {
      Util.CreateDirectory(path);
    }
  }

  private static void newGame(WindowBasedTextGUI gui, LanternaMainWindow window, DialogService dialogs, Game[] game) {
    if(!confirmAbandon(game[0])) {
      return;
    }
    String name = TextInputDialog.showDialog(gui, Strings.DialogNewNameTitle, Strings.DialogNewNamePrompt, "Antonio");
    if(name == null || name.trim().isEmpty()) {
      return;
    }
    Difficulty difficulty = askDifficulty(gui);
    if(difficulty == null) {
      return;
    }
    String[] labels = {Strings.SkillPilot, Strings.SkillFighter, Strings.SkillTrader, Strings.SkillEngineer};
    int[] extra = new int[labels.length];
    for(int i = 0; i < extra.length; i++) {
      int remaining = TOTAL_SKILL_POINTS - labels.length - (extra[0] + extra[1] + extra[2] + extra[3]);
      int max = Math.min(Consts.MaxSkill - 1, remaining);
      Integer value = LanternaDialogs.askAmount(gui, Strings.DialogSkillTitle,
          Functions.StringVars(Strings.DialogSkillPrompt, labels[i], "" + max), max);
      if(value == null) {
        return;
      }
      extra[i] = value;
    }
    game[0] = new Game(name.trim(), difficulty, 1 + extra[0], 1 + extra[1], 1 + extra[2], 1 + extra[3],
        window, dialogs);
    window.gameChanged();
    window.log(Functions.StringVars(Strings.MainNewGame, name.trim()));
  }

  private static Difficulty askDifficulty(WindowBasedTextGUI gui) {
    Difficulty[] chosen = {null};
    ActionListDialog.showDialog(gui, Strings.DialogDifficultyTitle, Strings.DialogDifficultyPrompt,
        () -> chosen[0] = Difficulty.Beginner,
        () -> chosen[0] = Difficulty.Easy,
        () -> chosen[0] = Difficulty.Normal,
        () -> chosen[0] = Difficulty.Hard,
        () -> chosen[0] = Difficulty.Impossible);
    return chosen[0];
  }

  private static void saveGame(WindowBasedTextGUI gui, DialogService dialogs, Game game, LanternaMainWindow window) {
    if(game == null) {
      return;
    }
    FileDialog dialog = new FileDialog(Strings.DialogSaveTitle, Strings.DialogSaveDescription,
        Strings.DialogSaveAction, new TerminalSize(60, 15), false, new File(Consts.SaveDirectory));
    File file = dialog.showDialog(gui);
    if(file == null) {
      return;
    }
    String path = file.getPath();
    if(!path.endsWith(".sav")) {
      path += ".sav";
    }
    if(Functions.SaveFile(path, game.Serialize(), dialogs)) {
      window.log(Strings.MainGameSaved);
    }
  }

  private static void loadGame(WindowBasedTextGUI gui, LanternaMainWindow window, DialogService dialogs, Game[] game) {
    if(!confirmAbandon(game[0])) {
      return;
    }
    FileDialog dialog = new FileDialog(Strings.DialogLoadTitle, Strings.DialogLoadDescription,
        Strings.DialogLoadAction, new TerminalSize(60, 15), false, new File(Consts.SaveDirectory));
    File file = dialog.showDialog(gui);
    if(file == null) {
      return;
    }
    Object obj = Functions.LoadFile(file.getPath(), false, dialogs);
    if(obj instanceof Hashtable) {
      game[0] = new Game((Hashtable)obj, window, dialogs);
      window.gameChanged();
      window.log(Strings.MainGameLoaded);
    }
  }

  private static boolean confirmAbandon(Game game) {
    if(game == null || game.Commander().getDays() == 0) {
      return true;
    }
    return game.Dialogs().alert(AlertType.GameAbandonConfirm) == DialogResult.Yes;
  }
}
