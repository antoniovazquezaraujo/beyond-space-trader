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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.ShipArt;
import org.gts.bst.view.ShipArtGenerator;
import spacetrader.Consts;
import spacetrader.Strings;


/**
 * Standalone viewer to play with the generated ship art: pick a ship with N/P
 * (or Tab), move it with the arrows, mirror it with M, see the zone map with Z,
 * cycle the palette with C (yours, pirate, police, abandoned) and switch the
 * engines off with D. A design tool, not the game.
 */
public final class ShipArtView extends BasicWindow {
  /** Colours per zone, as H, K, W, C, E. */
  private static final TextColor[][] PALETTES = {
      {TextColor.ANSI.WHITE, TextColor.ANSI.CYAN, TextColor.ANSI.RED, TextColor.ANSI.YELLOW, TextColor.ANSI.GREEN},
      {TextColor.ANSI.RED, TextColor.ANSI.YELLOW, TextColor.ANSI.MAGENTA, TextColor.ANSI.RED, TextColor.ANSI.YELLOW},
      {TextColor.ANSI.WHITE, TextColor.ANSI.BLUE, TextColor.ANSI.CYAN, TextColor.ANSI.WHITE, TextColor.ANSI.BLUE},
      {TextColor.ANSI.WHITE, TextColor.ANSI.WHITE, TextColor.ANSI.WHITE, TextColor.ANSI.WHITE, TextColor.ANSI.WHITE},
  };
  private static final String[] PALETTE_NAMES = {"tu nave", "pirata", "policia", "abandonada"};
  private static final char[] ZONES = {ShipArt.HULL, ShipArt.COCKPIT, ShipArt.WEAPON, ShipArt.CARGO,
      ShipArt.ENGINE};
  private static final String[] ZONE_NAMES = {"casco", "cabina", "armas", "bodegas", "motores"};
  private static final TextColor DARK = new TextColor.Indexed(238);

  private final List<ShipType> ships = List.of(ShipType.values());
  private final ShipCanvas canvas = new ShipCanvas();
  private int shipIndex;
  private int shipX = 3;
  private int shipY = 3;
  private int variant;
  private String lastSaved = "";
  private boolean mirrored;
  private boolean showZones;
  private boolean enginesOff;
  private int paletteIndex;

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
        } else if(character == 'r') {
          variant++;
          updateTitle();
        } else if(character == 's') {
          save();
        } else if(character == 'z') {
          showZones = !showZones;
        } else if(character == 'c') {
          paletteIndex = (paletteIndex + 1) % PALETTES.length;
        } else if(character == 'd') {
          enginesOff = !enginesOff;
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
    setTitle(shipName(type) + " (" + sizeName(type) + ")" + (variant > 0 ? " #" + (variant + 1) : ""));
  }

  private static String shipName(ShipType type) {
    return Strings.ShipNames.get(type.CastToInt());
  }

  private static String sizeName(ShipType type) {
    return Strings.Sizes.get(Consts.ShipSpecs.get(type.CastToInt()).getSize().CastToInt());
  }

  private ShipArt art() {
    ShipArt art = ShipArtGenerator.of(ships.get(shipIndex), variant);
    return mirrored ? art.mirrored() : art;
  }

  /** Saves the ship being shown so it can be kept as the model's art later. */
  private void save() {
    ShipArt art = art();
    ShipType type = ships.get(shipIndex);
    File file = new File(Consts.CustomDirectory, "ships/" + type.name() + "-" + variant + ".txt");
    File directory = file.getParentFile();
    if(directory != null && !directory.exists() && !directory.mkdirs()) {
      lastSaved = "no se pudo crear " + directory.getPath();
      return;
    }
    try(PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
      writer.println("# " + type.name() + " (" + sizeName(type) + ") variant " + variant);
      for(String line : art.lines()) {
        writer.println(line);
      }
      writer.println("---");
      for(String zone : art.zones()) {
        writer.println(zone);
      }
      lastSaved = file.getPath();
    } catch(IOException e) {
      lastSaved = "no se pudo guardar: " + e.getMessage();
    }
  }

  private TextColor zoneColor(char zone) {
    if(enginesOff && zone == ShipArt.ENGINE) {
      return DARK;
    }
    switch(zone) {
      case ShipArt.COCKPIT:
        return PALETTES[paletteIndex][1];
      case ShipArt.WEAPON:
        return PALETTES[paletteIndex][2];
      case ShipArt.CARGO:
        return PALETTES[paletteIndex][3];
      case ShipArt.ENGINE:
        return PALETTES[paletteIndex][4];
      default:
        return PALETTES[paletteIndex][0];
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
      int top = Math.max(0, Math.min(shipY, size.getRows() - art.height() - 3));
      for(int row = 0; row < art.height(); row++) {
        for(int col = 0; col < art.width(); col++) {
          char zone = art.zoneAt(row, col);
          char character = showZones ? zone : art.at(row, col);
          if(character != ' ') {
            graphics.setForegroundColor(zoneColor(zone));
            graphics.setCharacter(left + col, top + row, character);
          }
        }
      }
      drawLegend(graphics, 1, size.getRows() - 2);
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      String state = "[z] zonas " + (showZones ? "(si)" : "(no)") + " · [d] motores "
          + (enginesOff ? "(apagados)" : "(en marcha)") + " · [c] paleta: " + PALETTE_NAMES[paletteIndex]
          + " · [r] otra version" + (variant > 0 ? " (#" + (variant + 1) + ")" : "")
          + " · [s] guardar · [flechas] mover · [n/p] nave · [m] espejo · [ESC] salir"
          + (lastSaved.isEmpty() ? "" : "  ||  " + lastSaved);
      graphics.putString(1, size.getRows() - 1,
          state.substring(0, Math.min(state.length(), size.getColumns() - 2)));
    }

    private void drawLegend(TextGUIGraphics graphics, int x, int row) {
      int column = x;
      for(int i = 0; i < ZONES.length; i++) {
        graphics.setForegroundColor(zoneColor(ZONES[i]));
        graphics.setCharacter(column++, row, ZONES[i]);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        String text = " " + ZONE_NAMES[i] + "  ";
        graphics.putString(column, row, text);
        column += text.length();
      }
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      String palette = "paleta: " + PALETTE_NAMES[paletteIndex];
      graphics.putString(Math.max(column + 2, x), row, palette);
    }
  }
}
