/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.gts.bst.view.ShipArtFile;
import spacetrader.Strings;


/**
 * The ship composer (a design tool, not the game): it reads the chassis and the
 * pieces from the text files next to the repository (ships/chassis.txt and
 * ships/pieces.txt), so they can be edited with any editor and reloaded. It opens
 * with its branded cover behind a centred menu.
 *
 * <pre>
 *   ./run-composer.sh              -> starts the composer
 *   ./run-composer.sh --lang es    -> Spanish texts
 * </pre>
 */
public final class ShipComposer {
  private ShipComposer() {
  }

  public static void main(String[] args) throws IOException {
    java.util.Locale locale = LanternaApp.languageFrom(args);
    if(locale != null) {
      java.util.Locale.setDefault(locale);
    }
    Screen screen = new DefaultTerminalFactory().createScreen();
    screen.startScreen();
    try {
      MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), ComposerCover.load());
      gui.setTheme(LanternaTheme.create());
      while(true) {
        final List<ShipArtFile> chassis;
        final List<ShipArtFile> pieces;
        final List<org.gts.bst.view.ShipDesign> designs;
        try {
          chassis = ShipArtFile.load("chassis.txt");
          pieces = ShipArtFile.load("pieces.txt");
          designs = org.gts.bst.view.ShipDesign.load(ShipArtFile.resolve("ships.txt").toString());
        } catch(IOException e) {
          System.err.println(e.getMessage());
          return;
        }
        int chosen = chooseMode(gui);
        if(chosen == 0) {
          gui.addWindowAndWait(new ShipEditorView(designs, chassis, pieces));
        } else if(chosen == 1) {
          gui.addWindowAndWait(new HullEditorView(chassis));
        } else {
          break;
        }
      }
    } finally {
      screen.stopScreen();
      screen.close();
    }
  }

  /** The little menu: the ship editor, the hull editor or quit (Esc quits too). */
  private static int chooseMode(MultiWindowTextGUI gui) {
    MenuWindow window = menu();
    gui.addWindowAndWait(window);
    return window.chosen();
  }

  /** The menu window, ready for the tests to drive it with a virtual GUI. */
  static MenuWindow menu() {
    return new MenuWindow();
  }

  /**
   * The menu over the cover: one row per editor and Quit. The arrows move the
   * selection, n and p move it too (wrapping), ENTER chooses the selected option
   * and ESC quits.
   */
  static final class MenuWindow extends BasicWindow {
    /** The row of Quit: the option ESC returns. */
    static final int QUIT = 2;

    private final ActionListBox menu = new ActionListBox();
    /** The chosen option, or -1 while the menu is open. */
    private int chosen = -1;

    MenuWindow() {
      // The cover already writes SHIP EDITOR: a title here would repeat it.
      super("");
      menu.addItem(Strings.ComposerShips, () -> choose(0));
      menu.addItem(Strings.ComposerHulls, () -> choose(1));
      menu.addItem(Strings.ComposerQuit, () -> choose(QUIT));
      Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
      panel.addComponent(menu);
      panel.addComponent(new Label(Strings.ComposerKeys));
      setComponent(panel);
      setHints(Set.of(Window.Hint.CENTERED));
      setFocusedInteractable(menu);
    }

    @Override
    public boolean handleInput(KeyStroke key) {
      if(key.getKeyType() == KeyType.Escape) {
        chosen = QUIT;
        close();
        return true;
      }
      if(moveSelection(key)) {
        return true;
      }
      return super.handleInput(key);
    }

    /** The n/p keys move the selection like the arrows, wrapping around. */
    private boolean moveSelection(KeyStroke key) {
      if(key.getKeyType() != KeyType.Character) {
        return false;
      }
      char character = Character.toLowerCase(key.getCharacter());
      if(character != 'n' && character != 'p') {
        return false;
      }
      int items = menu.getItemCount();
      if(items == 0) {
        return false;
      }
      int current = menu.getSelectedIndex();
      menu.setSelectedIndex(Math.floorMod(current + (character == 'n' ? 1 : -1), items));
      return true;
    }

    private void choose(int option) {
      chosen = option;
      close();
    }

    /** The chosen option; -1 while the menu is open (it is set when it closes). */
    int chosen() {
      return chosen;
    }

    /** The list of options, for the tests. */
    ActionListBox menu() {
      return menu;
    }
  }
}
