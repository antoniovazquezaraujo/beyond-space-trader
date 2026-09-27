/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.gts.bst.view.ShipArtFile;


/**
 * The chassis viewer of the ship composer: pick a chassis from ships/chassis.txt
 * (N/P), reload the file (R) and see it in its colour. Pieces come next.
 */
public final class ShipComposerView extends BasicWindow {
  private final ComposerCanvas canvas = new ComposerCanvas();
  private List<ShipArtFile> chassis;
  private int index;
  private String message = "";

  public ShipComposerView(List<ShipArtFile> chassis) {
    setHints(Set.of(Window.Hint.FULL_SCREEN));
    setComponent(canvas);
    setFocusedInteractable(canvas);
    this.chassis = chassis;
    updateTitle();
  }

  public int chassisIndex() {
    return index;
  }

  boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      close();
      return true;
    }
    if(key.getKeyType() == KeyType.Tab) {
      move(1);
      return true;
    }
    if(key.getKeyType() == KeyType.Character) {
      char character = Character.toLowerCase(key.getCharacter());
      if(character == 'n' || character == ' ') {
        move(1);
      } else if(character == 'p') {
        move(-1);
      } else if(character == 'r') {
        reload();
      } else if(character == 'q') {
        close();
      }
    }
    canvas.invalidate();
    return true;
  }

  private void move(int step) {
    if(!chassis.isEmpty()) {
      index = (chassis.size() + index + step) % chassis.size();
      updateTitle();
    }
  }

  private void reload() {
    try {
      List<ShipArtFile> loaded = ShipArtFile.load("chassis.txt");
      if(!loaded.isEmpty()) {
        chassis = loaded;
        index = Math.min(index, chassis.size() - 1);
        updateTitle();
        message = "chassis.txt recargado";
      }
    } catch(IOException e) {
      message = e.getMessage();
    }
  }

  private void updateTitle() {
    setTitle(chassis.isEmpty() ? "chasis" : "chasis: " + chassis.get(index).name());
  }

  /** Maps the colour names of the files to the terminal palette. */
  static TextColor color(String name) {
    switch(name.toLowerCase()) {
      case "cian":
        return TextColor.ANSI.CYAN;
      case "rojo":
        return TextColor.ANSI.RED;
      case "amarillo":
        return TextColor.ANSI.YELLOW;
      case "verde":
        return TextColor.ANSI.GREEN;
      case "magenta":
        return TextColor.ANSI.MAGENTA;
      case "azul":
        return TextColor.ANSI.BLUE;
      case "negro":
        return TextColor.ANSI.BLACK;
      default:
        return TextColor.ANSI.WHITE;
    }
  }

  private final class ComposerCanvas extends AbstractInteractableComponent<ComposerCanvas> {
    @Override
    protected InteractableRenderer<ComposerCanvas> createDefaultRenderer() {
      return new InteractableRenderer<ComposerCanvas>() {
        @Override
        public TerminalPosition getCursorLocation(ComposerCanvas component) {
          return null;
        }

        @Override
        public TerminalSize getPreferredSize(ComposerCanvas component) {
          return TerminalSize.ZERO;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, ComposerCanvas component) {
          paint(graphics);
        }
      };
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke key) {
      return handleKey(key) ? Result.HANDLED : Result.UNHANDLED;
    }

    private void paint(TextGUIGraphics graphics) {
      TerminalSize size = getSize();
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      for(int row = 0; row < size.getRows(); row++) {
        graphics.putString(0, row, " ".repeat(size.getColumns()));
      }
      if(!chassis.isEmpty()) {
        ShipArtFile part = chassis.get(index);
        int left = Math.max(1, (size.getColumns() - part.width()) / 2);
        int top = Math.max(1, (size.getRows() - part.height()) / 2);
        for(int row = 0; row < part.height(); row++) {
          for(int column = 0; column < part.width(); column++) {
            char character = part.at(row, column);
            if(character != ' ') {
              graphics.setForegroundColor(color(part.color()));
              graphics.setCharacter(left + column, top + row, character);
            }
          }
        }
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(1, size.getRows() - 2, part.name() + "  ·  color: " + part.color() + "  ·  "
            + part.width() + "x" + part.height() + "  ·  " + (index + 1) + "/" + chassis.size());
      } else {
        graphics.putString(1, 1, "No hay chasis en ships/chassis.txt");
      }
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.putString(1, size.getRows() - 1, "[n/p] chasis · [r] recargar fichero · [ESC] salir"
          + (message.isEmpty() ? "" : "   ||   " + message));
    }
  }
}
