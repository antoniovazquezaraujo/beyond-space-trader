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
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.FileDialog;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
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
    Locale locale = languageFrom(args);
    if(locale != null) {
      // The texts are loaded once at startup, so the language is chosen before Strings.
      Locale.setDefault(locale);
    }
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
      // The game starts on the title screen: the logo, the starfield and the menu.
      // "New game (F2)" or "Load (F9)" bring a game in.
      MainPresenter presenter = new MainPresenter(() -> game[0], window);
      dialogs.quietTo(window::alertLog);
      window.setPresenter(presenter);
      window.showTitleScreen();
      presenter.updateAll();
      gui.addWindowAndWait(window.asWindow());
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /**
   * The locale of the optional {@code --lang} argument ({@code --lang es}, {@code --lang=es},
   * also with a country: {@code es_ES} or {@code es-ES}); {@code null} for the system locale.
   */
  static Locale languageFrom(String[] args) {
    for(int i = 0; i < args.length; i++) {
      String code = null;
      if(args[i].startsWith("--lang=")) {
        code = args[i].substring("--lang=".length());
      } else if("--lang".equals(args[i]) && i + 1 < args.length) {
        code = args[++i];
      }
      if(code != null && !code.isEmpty()) {
        String[] parts = code.replace('-', '_').split("_");
        return parts.length > 1 ? new Locale(parts[0], parts[1]) : new Locale(parts[0]);
      }
    }
    return null;
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
    String name = InputDialog.show(gui, Strings.DialogNewNameTitle, Strings.DialogNewNamePrompt, "Antonio");
    if(name == null) {
      // Only backing out of the first dialog cancels the game; the rest is forgiving.
      window.log(Strings.MainNewGameCancelled + " (" + Strings.DialogNewNameTitle + ")");
      return;
    }
    name = name.trim().isEmpty() ? "Antonio" : name.trim();
    Difficulty difficulty = askDifficulty(gui);
    if(difficulty == null) {
      difficulty = Difficulty.Normal;
      window.log(Strings.DialogDifficultyTitle + ": " + Strings.DifficultyLevels.get(difficulty.CastToInt()));
    }
    String[] labels = {Strings.SkillPilot, Strings.SkillFighter, Strings.SkillTrader, Strings.SkillEngineer};
    int[] extra = new int[labels.length];
    for(int i = 0; i < extra.length; i++) {
      int remaining = TOTAL_SKILL_POINTS - labels.length - (extra[0] + extra[1] + extra[2] + extra[3]);
      int max = Math.max(0, Math.min(Consts.MaxSkill - 1, remaining));
      extra[i] = skillPoints(InputDialog.show(gui, Strings.DialogSkillTitle,
          Functions.StringVars(Strings.DialogSkillPrompt, labels[i], "" + max), "0"), max);
    }
    game[0] = new Game(name, difficulty, 1 + extra[0], 1 + extra[1], 1 + extra[2], 1 + extra[3],
        window, dialogs);
    window.gameChanged();
    window.log(Functions.StringVars(Strings.MainNewGame, name));
  }

  /**
   * The extra points typed in a skill dialog: a cancelled dialog, a nonsense value or
   * one out of range counts as zero (or the maximum), so it never cancels the game.
   */
  static int skillPoints(String typed, int max) {
    if(typed == null) {
      return 0;
    }
    try {
      return Math.max(0, Math.min(max, Integer.parseInt(typed.trim())));
    } catch(NumberFormatException e) {
      return 0;
    }
  }

  private static Difficulty askDifficulty(WindowBasedTextGUI gui) {
    Difficulty[] chosen = {null};
    List<Runnable> choices = List.of(
        () -> chosen[0] = Difficulty.Beginner,
        () -> chosen[0] = Difficulty.Easy,
        () -> chosen[0] = Difficulty.Normal,
        () -> chosen[0] = Difficulty.Hard,
        () -> chosen[0] = Difficulty.Impossible);
    ActionListDialogBuilder builder = new ActionListDialogBuilder()
        .setTitle(Strings.DialogDifficultyTitle)
        .setDescription(Strings.DialogDifficultyPrompt);
    for(int i = 0; i < choices.size(); i++) {
      builder.addAction(Strings.DifficultyLevels.get(i), choices.get(i));
    }
    ActionListDialog dialog = builder.build();
    dialog.setCloseWindowWithEscape(true);
    dialog.showDialog(gui);
    return chosen[0];
  }

  private static void saveGame(WindowBasedTextGUI gui, DialogService dialogs, Game game, LanternaMainWindow window) {
    if(game == null) {
      return;
    }
    FileDialog dialog = new FileDialog(Strings.DialogSaveTitle, Strings.DialogSaveDescription,
        Strings.DialogSaveAction, new TerminalSize(60, 15), false, new File(Consts.SaveDirectory));
    dialog.setCloseWindowWithEscape(true);
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
    dialog.setCloseWindowWithEscape(true);
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
