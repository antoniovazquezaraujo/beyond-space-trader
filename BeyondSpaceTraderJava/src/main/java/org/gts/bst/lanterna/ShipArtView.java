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
import java.util.List;
import java.util.Set;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.ShipArt;
import org.gts.bst.view.ShipArtGenerator;
import spacetrader.Consts;
import spacetrader.Strings;


/**
 * Standalone viewer to play with the generated ship art: pick a ship with N/P
 * (or Tab), move it with the arrows and mirror it with M. A design tool, not the
 * game: it only draws the art and the zones.
 */
public final class ShipArtView extends BasicWindow {
  private static final TextColor[] ZONE_COLORS = {
      TextColor.ANSI.WHITE, TextColor.ANSI.CYAN, TextColor.ANSI.RED, TextColor.ANSI.YELLOW, TextColor.ANSI.GREEN};

  private final List<ShipType> ships = List.of(ShipType.values());
  private final ShipCanvas canvas = new ShipCanvas();
  private int shipIndex;
  private int shipX = 3;
  private int shipY = 3;
  private boolean mirrored;

  public ShipArtView() {
    setHints(Set.of(Window.Hint.FULL_SCREEN));
    setComponent(canvas);
    setFocusedInteractable(canvas);
    updateTitle();
  }

  public int shipIndex() {
    return shipIndex;
  }

  public int shipX() {
    return shipX;
  }

  boolean handleKey(KeyStroke key) {
    switch(key.getKeyType()) {
      case ArrowLeft:
        shipX--;
        break;
      case ArrowRight:
        shipX++;
        break;
      case ArrowUp:
        shipY--;
        break;
      case ArrowDown:
        shipY++;
        break;
      case Escape:
        close();
        return true;
      case Tab:
        nextShip(1);
        break;
      case Character:
        char character = Character.toLowerCase(key.getCharacter());
        if(character == 'n' || character == ' ') {
          nextShip(1);
        } else if(character == 'p') {
          nextShip(-1);
        } else if(character == 'm') {
          mirrored = !mirrored;
        } else if(character == 'q') {
          close();
        }
        break;
      default:
        break;
    }
    canvas.invalidate();
    return true;
  }

  private void nextShip(int step) {
    shipIndex = (ships.size() + shipIndex + step) % ships.size();
    updateTitle();
  }

  private void updateTitle() {
    ShipType type = ships.get(shipIndex);
    setTitle(shipName(type) + " (" + sizeName(type) + ")");
  }

  private static String shipName(ShipType type) {
    return Strings.ShipNames.get(type.CastToInt());
  }

  private static String sizeName(ShipType type) {
    return Strings.Sizes.get(Consts.ShipSpecs.get(type.CastToInt()).getSize().CastToInt());
  }

  private ShipArt art() {
    ShipArt art = ShipArtGenerator.of(ships.get(shipIndex));
    return mirrored ? art.mirrored() : art;
  }

  private static TextColor zoneColor(char zone) {
    switch(zone) {
      case ShipArt.COCKPIT:
        return ZONE_COLORS[1];
      case ShipArt.WEAPON:
        return ZONE_COLORS[2];
      case ShipArt.CARGO:
        return ZONE_COLORS[3];
      case ShipArt.ENGINE:
        return ZONE_COLORS[4];
      default:
        return ZONE_COLORS[0];
    }
  }

  private final class ShipCanvas extends AbstractInteractableComponent<ShipCanvas> {
    @Override
    protected InteractableRenderer<ShipCanvas> createDefaultRenderer() {
      return new InteractableRenderer<ShipCanvas>() {
        @Override
        public TerminalPosition getCursorLocation(ShipCanvas component) {
          return null;
        }

        @Override
        public TerminalSize getPreferredSize(ShipCanvas component) {
          return TerminalSize.ZERO;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, ShipCanvas component) {
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
      ShipArt art = art();
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      for(int row = 0; row < size.getRows(); row++) {
        graphics.putString(0, row, " ".repeat(size.getColumns()));
      }
      int left = Math.max(0, Math.min(shipX, size.getColumns() - art.width()));
      int top = Math.max(0, Math.min(shipY, size.getRows() - art.height() - 2));
      for(int row = 0; row < art.height(); row++) {
        for(int col = 0; col < art.width(); col++) {
          char character = art.at(row, col);
          if(character != ' ') {
            graphics.setForegroundColor(zoneColor(art.zoneAt(row, col)));
            graphics.setCharacter(left + col, top + row, character);
          }
        }
      }
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.putString(1, size.getRows() - 2, shipName(ships.get(shipIndex)) + "  (" + art.width() + "x"
          + art.height() + ")  zonas: H casco · K cabina · W armas · C bodegas · E motores");
      graphics.putString(1, size.getRows() - 1,
          "[flechas] mover · [n/p] nave · [m] espejo " + (mirrored ? "(sí)" : "(no)") + " · [ESC] salir");
    }
  }
}
