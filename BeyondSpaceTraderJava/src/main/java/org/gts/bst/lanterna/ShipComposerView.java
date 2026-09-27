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
import org.gts.bst.view.ShipAssembly;


/**
 * The ship composer: a chassis from ships/chassis.txt with pieces (from
 * ships/pieces.txt) placed on top, each in its colour; whatever is placed covers
 * the chassis. The assembly saves to and loads from ships/naves.txt, and R
 * reloads the definitions so they can be edited with any editor.
 */
public final class ShipComposerView extends BasicWindow {
  private static final String[] COLORS = {"blanco", "cian", "rojo", "amarillo", "verde", "magenta", "azul"};
  private final ComposerCanvas canvas = new ComposerCanvas();
  private List<ShipArtFile> chassis;
  private List<ShipArtFile> pieces;
  private ShipAssembly assembly;
  private int chassisIndex;
  private int pieceIndex;
  private int cursorX = 2;
  private int cursorY = 2;
  private String pendingColor;
  private boolean showPending = true;
  private boolean blinkOn = true;
  private String message = "";

  public ShipComposerView(List<ShipArtFile> chassis, List<ShipArtFile> pieces, ShipAssembly saved) {
    setHints(Set.of(Window.Hint.FULL_SCREEN));
    setComponent(canvas);
    setFocusedInteractable(canvas);
    this.chassis = chassis;
    this.pieces = pieces;
    java.util.Timer timer = new java.util.Timer("composer-blink", true);
    timer.scheduleAtFixedRate(new java.util.TimerTask() {
      @Override
      public void run() {
        blinkOn = !blinkOn;
        canvas.invalidate();
      }
    }, 500, 500);
    this.assembly = saved != null && !saved.chassis().isEmpty()
        ? saved : ShipAssembly.empty(chassis.isEmpty() ? "" : chassis.get(0).name());
    chassisIndex = Math.max(0, indexOf(chassis, assembly.chassis()));
    pendingColor = pieces.isEmpty() ? "blanco" : pieces.get(0).color();
    updateTitle();
  }

  public int placedCount() {
    return assembly.pieces().size();
  }

  boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      close();
      return true;
    }
    if(key.getKeyType() == KeyType.Tab) {
      chassisIndex = chassis.isEmpty() ? 0 : (chassisIndex + 1) % chassis.size();
      assembly = assembly.withChassis(chassis.get(chassisIndex).name());
      updateTitle();
      canvas.invalidate();
      return true;
    }
    if(key.getKeyType() == KeyType.Enter) {
      place();
      return true;
    }
    switch(key.getKeyType()) {
      case ArrowLeft:
        cursorX = Math.max(0, cursorX - 1);
        break;
      case ArrowRight:
        cursorX = Math.min(200, cursorX + 1);
        break;
      case ArrowUp:
        cursorY = Math.max(0, cursorY - 1);
        break;
      case ArrowDown:
        cursorY = Math.min(100, cursorY + 1);
        break;
      case Character:
        character(Character.toLowerCase(key.getCharacter()));
        break;
      default:
        break;
    }
    canvas.invalidate();
    return true;
  }

  private void character(char character) {
    if(character == 'n') {
      pieceIndex = pieces.isEmpty() ? 0 : (pieceIndex + 1) % pieces.size();
      pendingColor = pieces.isEmpty() ? "blanco" : pieces.get(pieceIndex).color();
    } else if(character == 'p') {
      pieceIndex = pieces.isEmpty() ? 0 : (pieces.size() + pieceIndex - 1) % pieces.size();
      pendingColor = pieces.isEmpty() ? "blanco" : pieces.get(pieceIndex).color();
    } else if(character == ' ' || character == 'o') {
      place();
    } else if(character == 'c') {
      pendingColor = COLORS[(java.util.Arrays.asList(COLORS).indexOf(pendingColor) + 1) % COLORS.length];
    } else if(character == 'x') {
      assembly = ShipAssembly.empty(assembly.chassis());
    } else if(character == 'h') {
      showPending = !showPending;
    } else if(character == 'u') {
      assembly = assembly.withoutLast();
    } else if(character == 's') {
      save();
    } else if(character == 'l') {
      loadAssembly();
    } else if(character == 'r') {
      reload();
    } else if(character == 'q') {
      close();
    }
  }

  private void place() {
    if(!pieces.isEmpty()) {
      assembly = assembly.with(new ShipAssembly.ShipPlacement(pieces.get(pieceIndex).name(), cursorX, cursorY,
          pendingColor));
    }
  }

  private void save() {
    try {
      ShipAssembly.save(ShipArtFile.resolve("naves.txt").toString(), assembly);
      message = "montaje guardado en " + ShipArtFile.resolve("naves.txt");
    } catch(IOException e) {
      message = "no se pudo guardar: " + e.getMessage();
    }
  }

  private void loadAssembly() {
    try {
      ShipAssembly saved = ShipAssembly.load(ShipArtFile.resolve("naves.txt").toString());
      if(saved != null) {
        assembly = saved;
        chassisIndex = Math.max(0, indexOf(chassis, assembly.chassis()));
        updateTitle();
        message = "montaje cargado (" + assembly.pieces().size() + " piezas)";
      }
    } catch(IOException e) {
      message = "no se pudo cargar: " + e.getMessage();
    }
  }

  private void reload() {
    try {
      List<ShipArtFile> loadedChassis = ShipArtFile.load("chassis.txt");
      List<ShipArtFile> loadedPieces = ShipArtFile.load("pieces.txt");
      if(!loadedChassis.isEmpty()) {
        chassis = loadedChassis;
        chassisIndex = Math.min(chassisIndex, chassis.size() - 1);
        updateTitle();
      }
      if(!loadedPieces.isEmpty()) {
        pieces = loadedPieces;
        pieceIndex = Math.min(pieceIndex, pieces.size() - 1);
      }
      message = "chassis.txt y pieces.txt recargados";
    } catch(IOException e) {
      message = e.getMessage();
    }
  }

  private static int indexOf(List<ShipArtFile> parts, String name) {
    for(int i = 0; i < parts.size(); i++) {
      if(parts.get(i).name().equals(name)) {
        return i;
      }
    }
    return 0;
  }

  private void updateTitle() {
    setTitle("compositor: " + assembly.chassis() + " (" + assembly.pieces().size() + " piezas)");
  }

  /** Maps the colours of the files (names, #rrggbb or 0-255) to the terminal. */
  static TextColor color(String name) {
    return org.gts.bst.view.ShipColors.color(name);
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
      if(chassis.isEmpty()) {
        graphics.putString(1, 1, "No hay chasis en ships/chassis.txt");
        return;
      }
      // el montaje: chasis de fondo y las piezas encima, tapando lo de debajo
      int width = chassis.get(chassisIndex).width() + 2;
      int height = chassis.get(chassisIndex).height() + 2;
      for(ShipAssembly.ShipPlacement placement : assembly.pieces()) {
        ShipArtFile piece = findPiece(placement.piece());
        if(piece != null) {
          width = Math.max(width, placement.x() + piece.width() + 2);
          height = Math.max(height, placement.y() + piece.height() + 2);
        }
      }
      if(!pieces.isEmpty()) {
        width = Math.max(width, cursorX + pieces.get(pieceIndex).width() + 2);
        height = Math.max(height, cursorY + pieces.get(pieceIndex).height() + 2);
      }
      char[][] cells = new char[height][width];
      TextColor[][] colors = new TextColor[height][width];
      for(int row = 0; row < height; row++) {
        for(int column = 0; column < width; column++) {
          cells[row][column] = ' ';
          colors[row][column] = TextColor.ANSI.WHITE;
        }
      }
      ShipArtFile hull = chassis.get(chassisIndex);
      overlay(cells, colors, hull, 1, 1, hull.color());
      for(ShipAssembly.ShipPlacement placement : assembly.pieces()) {
        ShipArtFile piece = findPiece(placement.piece());
        if(piece != null) {
          String color = piece.blink() && !blinkOn ? (piece.bgColor().isEmpty() ? "black" : piece.bgColor())
              : placement.color();
          overlay(cells, colors, piece, placement.x() + 1, placement.y() + 1, color);
        }
      }
      if(!pieces.isEmpty() && showPending) {
        ShipArtFile pending = pieces.get(pieceIndex);
        String color = pending.blink() && !blinkOn ? (pending.bgColor().isEmpty() ? "black" : pending.bgColor())
            : pendingColor;
        overlay(cells, colors, pending, cursorX + 1, cursorY + 1, color);
      }
      int left = Math.max(0, (size.getColumns() - width) / 2);
      int top = Math.max(0, (size.getRows() - height - 2) / 2);
      for(int row = 0; row < height && top + row < size.getRows() - 2; row++) {
        for(int column = 0; column < width && left + column < size.getColumns(); column++) {
          if(cells[row][column] != ' ') {
            graphics.setForegroundColor(colors[row][column]);
            graphics.setCharacter(left + column, top + row, cells[row][column]);
          }
        }
      }
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      String pieceName = pieces.isEmpty() ? "-" : pieces.get(pieceIndex).name();
      graphics.putString(1, size.getRows() - 2, hull.name() + " [" + hull.color() + "]  ·  pieza: " + pieceName
          + " (" + (pieces.isEmpty() ? 0 : pieceIndex + 1) + "/" + pieces.size() + ") x=" + cursorX + " y=" + cursorY
          + " [" + pendingColor + "]  ·  " + assembly.pieces().size() + " colocadas");
      graphics.putString(1, size.getRows() - 1, "[h] pieza " + (showPending ? "(si)" : "(no)") + " · [x] vaciar"
          + " · [flechas] mover · [ENTER] colocar · [n/p] pieza · [TAB] chasis · [c] color · [u] deshacer"
          + " · [s] guardar · [l] cargar · [r] recargar · [ESC] salir"
          + (message.isEmpty() ? "" : "   ||   " + message));
    }

    private void overlay(char[][] cells, TextColor[][] colors, ShipArtFile part, int x, int y, String colorName) {
      for(int row = 0; row < part.height(); row++) {
        for(int column = 0; column < part.width(); column++) {
          char character = part.at(row, column);
          if(character != ' ' && y + row >= 0 && y + row < cells.length && x + column >= 0
              && x + column < cells[0].length) {
            cells[y + row][x + column] = character;
            colors[y + row][x + column] = color(colorName);
          }
        }
      }
    }
  }

  private ShipArtFile findPiece(String name) {
    for(ShipArtFile piece : pieces) {
      if(piece.name().equals(name)) {
        return piece;
      }
    }
    return null;
  }
}
