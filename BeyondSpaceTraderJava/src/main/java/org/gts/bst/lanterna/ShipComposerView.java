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
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipAssembly;
import org.gts.bst.view.ShipSites;


/**
 * The ship composer: a chassis from ships/chassis.txt with pieces (from
 * ships/pieces.txt) placed on top, each in its colour; whatever is placed covers
 * the chassis. The assembly saves to and loads from ships/ships.txt, and R
 * reloads the definitions so they can be edited with any editor.
 */
public final class ShipComposerView extends BasicWindow {
  private static final String[] COLORS = {"white", "cyan", "red", "yellow", "green", "magenta", "blue"};
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
  private boolean showGlyphStrip;
  /** Sample glyphs to check in the terminal: wide ones leave a hole after the marker. */
  private static final String GLYPH_SAMPLE = "🁣 🁩 🂓 ┃ ⚀ ⚅ ┃ ⣿ ⠿ ┃ ⧯ ⎅ ⏌ ⎚ ⛁ ┃ ↠ ⇉ ⦖ ⧎ ◒ ◈ ⍉ ⏚ ┃ 😀 中 ┃ ┌─┐";
  private boolean blinkOn = true;
  private String message = "";
  /** The ship type of the design, or "" when the chassis has no spec. */
  private String shipType = "";
  /** -1 when the preview is off, else the index of PREVIEWS. */
  private int preview = -1;
  private boolean siteMode;
  private final Map<Integer, Map<Integer, Integer>> siteEdits = new LinkedHashMap<>();
  /** The chassis as loaded: what the eraser restores. */
  private List<int[]> originalCells = new ArrayList<>();
  /** The chassis file the site mode saves to (a field so tests can point elsewhere). */
  private String chassisPath = ShipArtFile.resolve("chassis.txt").toString();
  private boolean typeListOpen;
  private int typeListIndex;
  private static final String[] PREVIEWS = {"vacia", "comerciante", "pirata", "policia", "a tope"};

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
    pendingColor = pieces.isEmpty() ? "white" : pieces.get(0).color();
    updateTitle();
    shipType = budgetName(assembly.chassis());
    snapshotCells();
  }

  public int placedCount() {
    return assembly.pieces().size();
  }

  boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      if(typeListOpen) {
        typeListOpen = false;
        canvas.invalidate();
        return true;
      }
      if(siteMode) {
        siteMode = false;
        canvas.invalidate();
        return true;
      }
      close();
      return true;
    }
    if(typeListOpen) {
      List<String> types = shipTypes();
      if(key.getKeyType() == KeyType.ArrowUp) {
        typeListIndex = (typeListIndex + types.size() - 1) % types.size();
      } else if(key.getKeyType() == KeyType.ArrowDown) {
        typeListIndex = (typeListIndex + 1) % types.size();
      } else if(key.getKeyType() == KeyType.Enter) {
        selectType(types.get(typeListIndex));
        return true;
      }
      canvas.invalidate();
      return true;
    }
    if(key.getKeyType() == KeyType.Tab) {
      chassisIndex = chassis.isEmpty() ? 0 : (chassisIndex + 1) % chassis.size();
      assembly = assembly.withChassis(chassis.get(chassisIndex).name());
      updateTitle();
      shipType = budgetName(assembly.chassis());
      preview = -1;
      snapshotCells();
      canvas.invalidate();
      return true;
    }
    if(key.getKeyType() == KeyType.Enter) {
      if(!siteMode) {
        place();
      }
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
    char upper = Character.toUpperCase(character);
    if(character == 'e' && !siteMode) {
      siteMode = true;
      message = "modo sitios: teclea C M D B R A E G P (espacio borra, ESC sale)";
    } else if(siteMode && ShipSites.isSite(upper)) {
      writeSite(upper);
    } else if(siteMode && character == ' ') {
      writeSite(' ');
    } else if(character == 's') {
      saveAll();
    } else if(character == 't') {
      typeListOpen = true;
      typeListIndex = Math.max(0, shipTypes().indexOf(shipType));
    } else if(character == 'v') {
      preview = preview >= PREVIEWS.length - 1 ? -1 : preview + 1;
      message = preview < 0 ? "previsualizacion: no" : "previsualizacion: " + PREVIEWS[preview];
    } else if(character == ',' && preview >= 0) {
      preview = (preview + PREVIEWS.length - 1) % PREVIEWS.length;
      message = "previsualizacion: " + PREVIEWS[preview];
    } else if(character == '.' && preview >= 0) {
      preview = (preview + 1) % PREVIEWS.length;
      message = "previsualizacion: " + PREVIEWS[preview];
    } else if(character == 'n') {
      pieceIndex = pieces.isEmpty() ? 0 : (pieceIndex + 1) % pieces.size();
      pendingColor = pieces.isEmpty() ? "white" : pieces.get(pieceIndex).color();
    } else if(character == 'p') {
      pieceIndex = pieces.isEmpty() ? 0 : (pieces.size() + pieceIndex - 1) % pieces.size();
      pendingColor = pieces.isEmpty() ? "white" : pieces.get(pieceIndex).color();
    } else if(character == ' ' || character == 'o') {
      place();
    } else if(character == 'c') {
      pendingColor = COLORS[(java.util.Arrays.asList(COLORS).indexOf(pendingColor) + 1) % COLORS.length];
    } else if(character == 'x') {
      assembly = ShipAssembly.empty(assembly.chassis());
    } else if(character == 'h') {
      showPending = !showPending;
    } else if(character == 'g') {
      showGlyphStrip = !showGlyphStrip;
    } else if(character == 'u') {
      assembly = assembly.withoutLast();
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

  /** Saves the chassis edits and the assembly. */
  private void saveAll() {
    String assemblyResult;
    try {
      ShipAssembly.save(ShipArtFile.resolve("ships.txt").toString(), assembly);
      assemblyResult = "montaje guardado";
    } catch(IOException e) {
      assemblyResult = "no se pudo guardar el montaje: " + e.getMessage();
    }
    message = saveChassisEdits() + " · " + assemblyResult;
    canvas.invalidate();
  }

  private void loadAssembly() {
    try {
      ShipAssembly saved = ShipAssembly.load(ShipArtFile.resolve("ships.txt").toString());
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
        snapshotCells();
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
      if(typeListOpen) {
        drawTypeList(graphics, size);
        return;
      }
      if(chassis.isEmpty()) {
        graphics.putString(1, 1, "No hay chasis en ships/chassis.txt");
        return;
      }
      int panelWidth = panelWidth(size);
      int canvasWidth = size.getColumns() - panelWidth;
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
      if(siteMode) {
        width = Math.max(width, cursorX + 2);
        height = Math.max(height, cursorY + 2);
      }
      int[][] cells = new int[height][width];
      TextColor[][] colors = new TextColor[height][width];
      for(int row = 0; row < height; row++) {
        for(int column = 0; column < width; column++) {
          cells[row][column] = ' ';
          colors[row][column] = TextColor.ANSI.WHITE;
        }
      }
      ShipArtFile hull = chassis.get(chassisIndex);
      overlay(cells, colors, hull, 1, 1, hull.color());
      // los sitios se ven en amarillo para localizarlos de un vistazo
      for(int row = 0; row < height; row++) {
        for(int column = 0; column < width; column++) {
          if(ShipSites.isSite(cells[row][column])) {
            colors[row][column] = TextColor.ANSI.YELLOW;
          }
        }
      }
      if(preview >= 0) {
        fillSites(cells, colors, hull);
        for(ShipAssembly.ShipPlacement placement : assembly.pieces()) {
          ShipArtFile piece = findPiece(placement.piece());
          if(piece != null) {
            String color = piece.blink() && !blinkOn ? (piece.bgColor().isEmpty() ? "black" : piece.bgColor())
                : placement.color();
            overlay(cells, colors, piece, placement.x() + 1, placement.y() + 1, color);
          }
        }
      }
      if(preview >= 0 && !pieces.isEmpty() && showPending) {
        ShipArtFile pending = pieces.get(pieceIndex);
        String color = pending.blink() && !blinkOn ? (pending.bgColor().isEmpty() ? "black" : pending.bgColor())
            : pendingColor;
        overlay(cells, colors, pending, cursorX + 1, cursorY + 1, color);
      }
      boolean siteCursor = siteMode && cursorY >= 0 && cursorY + 1 < height && cursorX + 1 < width;
      int left = Math.max(0, (canvasWidth - width) / 2);
      int top = Math.max(0, (size.getRows() - height - 2) / 2);
      for(int row = 0; row < height && top + row < size.getRows() - 2; row++) {
        for(int column = 0; column < width && left + column < canvasWidth; column++) {
          if(cells[row][column] != ' ' && cells[row][column] != ShipArtFile.CONTINUATION) {
            TextColor color = colors[row][column];
            if(siteCursor && row == cursorY + 1 && column == cursorX + 1) {
              drawGlyph(graphics, left + column, top + row, cells[row][column], color, true);
            } else {
              drawGlyph(graphics, left + column, top + row, cells[row][column], color);
            }
          }
        }
      }
      drawPanel(graphics, size, canvasWidth);
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      if(showGlyphStrip) {
        graphics.putString(1, size.getRows() - 2, cut("glifos: " + GLYPH_SAMPLE
            + "   (cada ┃: pegado al glifo = ocupa 2 columnas; con hueco = 1 columna)", size.getColumns() - 2));
      } else if(preview < 0) {
        int codePoint = cursorY < hull.height() && cursorX < hull.cells().get(cursorY).length
            ? hull.cells().get(cursorY)[cursorX] : ' ';
        String cell = codePoint == ' ' ? "vacio" : new String(Character.toChars(codePoint));
        graphics.putString(1, size.getRows() - 2, hull.name() + " [" + hull.color() + "]  ·  celda: " + cell
            + "  x=" + cursorX + " y=" + cursorY + "  ·  [e] escribir sitios · [v] previsualizar");
      } else {
        String pieceName = pieces.isEmpty() ? "-" : pieces.get(pieceIndex).name();
        graphics.putString(1, size.getRows() - 2, hull.name() + " [" + hull.color() + "]  ·  pieza: " + pieceName
            + " (" + (pieces.isEmpty() ? 0 : pieceIndex + 1) + "/" + pieces.size() + ") x=" + cursorX + " y=" + cursorY
            + " [" + pendingColor + "]  ·  " + assembly.pieces().size() + " colocadas");
      }
      if(siteMode) {
        long pending = siteEdits.values().stream().mapToLong(Map::size).sum();
        graphics.putString(1, size.getRows() - 1, "SITIOS: teclea C M D B R A E G P (ESPACIO borra)"
            + " · [s] guardar · [e]/[ESC] salir del modo"
            + (pending == 0 ? "" : "  ·  " + pending + " cambios sin guardar")
            + (message.isEmpty() ? "" : "   ||   " + message));
      } else {
        graphics.putString(1, size.getRows() - 1, "[h] pieza " + (showPending ? "(si)" : "(no)") + " · [x] vaciar"
            + " · [flechas] mover · [ENTER] colocar · [n/p] pieza · [t] tipo · [v] previsualizar"
            + (preview < 0 ? "" : " (" + PREVIEWS[preview] + ")")
            + " · [e] sitios · [TAB] chasis · [c] color · [u] deshacer · [g] glifos · [s] guardar · [l] cargar"
            + " · [r] recargar · [ESC] salir" + (message.isEmpty() ? "" : "   ||   " + message));
      }
    }

    /** Draws the list of ship types while it is open. */
    private void drawTypeList(TextGUIGraphics graphics, TerminalSize size) {
      List<String> types = shipTypes();
      int left = Math.max(1, (size.getColumns() - 24) / 2);
      int top = Math.max(0, (size.getRows() - types.size() - 2) / 2);
      graphics.putString(left, top, "tipo de nave:");
      for(int i = 0; i < types.size() && top + 1 + i < size.getRows() - 1; i++) {
        if(i == typeListIndex) {
          graphics.setForegroundColor(TextColor.ANSI.BLACK);
          graphics.setBackgroundColor(TextColor.ANSI.WHITE);
        }
        graphics.putString(left, top + 1 + i, cut(String.format("%-22s", types.get(i)), 22));
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      }
      graphics.putString(1, size.getRows() - 1, "[flechas] elegir · [ENTER] abrir · [ESC] cancelar");
    }

    /** The preview: fills the sites with the pieces the convention names. */
    private void fillSites(int[][] cells, TextColor[][] colors, ShipArtFile hull) {
      ShipSites.Budget budget = ShipSites.budgetOf(shipType);
      int[] quota = previewQuota(budget);
      String role = previewRole();
      int bays = budget == null ? 0 : budget.cargoBays() + (preview == 4 ? 5 * budget.gadgets() : 0);
      int weapons = 0;
      int shields = 0;
      int gadgets = 0;
      for(ShipSites.Site site : ShipSites.sitesOf(hull)) {
        clearRun(cells, site);
        ShipArtFile piece = null;
        switch(site.site()) {
          case ShipSites.COCKPIT:
            piece = findPiece("cabina");
            break;
          case ShipSites.ENGINES:
            piece = findPiece("motor");
            break;
          case ShipSites.FUEL:
            piece = findPiece("deposito");
            break;
          case ShipSites.POD:
            piece = findPiece("capsula");
            break;
          case ShipSites.ROLE:
            piece = role.isEmpty() ? null : findPiece("marca " + role);
            break;
          case ShipSites.WEAPON:
            piece = weapons++ < quota[0] ? findPieceStartingWith("torreta") : null;
            break;
          case ShipSites.SHIELD:
            piece = shields++ < quota[1] ? findPieceStartingWith("escudo") : null;
            break;
          case ShipSites.GADGET:
            piece = gadgets++ < quota[2] ? findPieceStartingWith("artilugio") : null;
            break;
          case ShipSites.CARGO:
            drawGauge(cells, colors, site, bays);
            break;
          default:
            break;
        }
        if(piece != null) {
          overlay(cells, colors, piece, site.x() + 1, site.y() + 1, piece.color());
        }
      }
    }

    private int[] previewQuota(ShipSites.Budget budget) {
      if(budget == null) {
        return new int[3];
      }
      switch(preview) {
        case 1:
          return new int[] {0, 1, 1};
        case 2:
          return new int[] {budget.weapons(), 0, 0};
        case 3:
          return new int[] {budget.weapons(), budget.shields(), 0};
        case 4:
          return new int[] {budget.weapons(), budget.shields(), budget.gadgets()};
        default:
          return new int[3];
      }
    }

    private String previewRole() {
      switch(preview) {
        case 1:
          return "comerciante";
        case 2:
          return "pirata";
        case 3:
        case 4:
          return "policia";
        default:
          return "";
      }
    }

    /** Empties the cells of a site run, so nothing of the marker shows under a piece. */
    private void clearRun(int[][] cells, ShipSites.Site site) {
      for(int i = 0; i < site.length() && site.x() + 1 + i < cells[0].length; i++) {
        cells[site.y() + 1][site.x() + 1 + i] = ' ';
      }
    }

    /** Draws the braille gauge of a capacity inside its site run. */
    private void drawGauge(int[][] cells, TextColor[][] colors, ShipSites.Site site, int bays) {
      String gauge = ShipSites.gauge(bays);
      int column = 0;
      for(int i = 0; i < gauge.length() && column < site.length(); ) {
        int codePoint = gauge.codePointAt(i);
        i += Character.charCount(codePoint);
        cells[site.y() + 1][site.x() + 1 + column] = codePoint;
        colors[site.y() + 1][site.x() + 1 + column] = color("green");
        column++;
      }
    }

    /** Draws one glyph (a code point) in its colour; the terminal decides how wide it is. */
    private static void drawGlyph(TextGUIGraphics graphics, int column, int row, int codePoint, TextColor color) {
      drawGlyph(graphics, column, row, codePoint, color, false);
    }

    private static void drawGlyph(TextGUIGraphics graphics, int column, int row, int codePoint, TextColor color,
        boolean reverse) {
      TextCharacter glyph = TextCharacter.fromString(new String(Character.toChars(codePoint)), color,
          TextColor.ANSI.BLACK)[0];
      graphics.setCharacter(column, row, reverse ? glyph.withModifier(SGR.REVERSE) : glyph);
    }

    private void overlay(int[][] cells, TextColor[][] colors, ShipArtFile part, int x, int y, String colorName) {
      for(int row = 0; row < part.height(); row++) {
        for(int column = 0; column < part.width(); column++) {
          int character = part.at(row, column);
          if(character != ' ' && y + row >= 0 && y + row < cells.length && x + column >= 0
              && x + column < cells[0].length) {
            cells[y + row][x + column] = character;
            colors[y + row][x + column] = color(colorName);
          }
        }
      }
    }
  }

  /** Trims a text to a number of cells, without breaking a glyph in half. */
  private static String cut(String text, int cells) {
    StringBuilder cut = new StringBuilder();
    int width = 0;
    for(int i = 0; i < text.length() && width < cells; ) {
      int codePoint = text.codePointAt(i);
      i += Character.charCount(codePoint);
      width += ShipArtFile.isWide(codePoint) ? 2 : 1;
      if(width > cells) {
        break;
      }
      cut.appendCodePoint(codePoint);
    }
    return cut.toString();
  }

  /** Writes a site letter (a space erases it and gives the drawing back) at the cursor. */
  private void writeSite(char letter) {
    ShipArtFile hull = chassis.get(chassisIndex);
    if(cursorY >= hull.height() || cursorX >= hull.cells().get(cursorY).length) {
      message = "esa celda no existe en el chasis (x=" + cursorX + " y=" + cursorY + ")";
      return;
    }
    int current = hull.cells().get(cursorY)[cursorX];
    if(letter == ' ') {
      setCell(originalCell(cursorY, cursorX));
      message = ShipSites.isSite(current) ? "sitio borrado (dibujo restaurado)"
          : "nada que borrar en x=" + cursorX + " y=" + cursorY;
      return;
    }
    if(current != ' ' && !ShipSites.isSite(current)) {
      message = "esa celda tiene dibujo (" + new String(Character.toChars(current)) + "): elige un hueco";
      return;
    }
    setCell(letter);
    message = "sitio " + letter + " en x=" + cursorX + " y=" + cursorY;
  }

  private void setCell(int codePoint) {
    chassis.get(chassisIndex).cells().get(cursorY)[cursorX] = codePoint;
    siteEdits.computeIfAbsent(cursorY, row -> new LinkedHashMap<>()).put(cursorX, codePoint);
    canvas.invalidate();
  }

  private int originalCell(int row, int column) {
    if(row < originalCells.size() && column < originalCells.get(row).length) {
      return originalCells.get(row)[column];
    }
    return ' ';
  }

  /** Keeps a copy of the chassis cells, so the eraser can give the drawing back. */
  private void snapshotCells() {
    originalCells = new ArrayList<>();
    if(chassisIndex < chassis.size()) {
      for(int[] row : chassis.get(chassisIndex).cells()) {
        originalCells.add(row.clone());
      }
    }
  }

  /** Writes the site edits back to the chassis file, leaving the rest of it untouched. */
  private String saveChassisEdits() {
    if(siteEdits.isEmpty()) {
      return "sin cambios en el chasis";
    }
    try {
      Path path = Path.of(chassisPath);
      List<String> lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
      String section = chassis.get(chassisIndex).name();
      boolean inSection = false;
      int row = -1;
      int changes = 0;
      for(int i = 0; i < lines.size(); i++) {
        String trimmed = lines.get(i).strip();
        if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
          inSection = trimmed.substring(1, trimmed.length() - 1).strip().equals(section);
          row = -1;
          continue;
        }
        if(!inSection || trimmed.startsWith(";;") || trimmed.startsWith("color=") || trimmed.startsWith("bgcolor=")
            || trimmed.startsWith("blink=") || trimmed.startsWith("wide=") || trimmed.startsWith("narrow=")) {
          continue;
        }
        row++;
        Map<Integer, Integer> edits = siteEdits.get(row);
        if(edits == null) {
          continue;
        }
        String text = lines.get(i);
        List<Integer> columns = new ArrayList<>(edits.keySet());
        columns.sort(java.util.Collections.reverseOrder());
        for(int column : columns) {
          int codePoints = text.codePointCount(0, text.length());
          String replacement = new String(Character.toChars(edits.get(column)));
          if(column >= codePoints) {
            text = text + replacement;
          } else {
            int index = text.offsetByCodePoints(0, column);
            text = text.substring(0, index) + replacement + text.substring(text.offsetByCodePoints(index, 1));
          }
          changes++;
        }
        lines.set(i, text);
      }
      Files.write(path, lines, StandardCharsets.UTF_8);
      siteEdits.clear();
      return "chasis guardado (" + changes + " sitios)";
    } catch(IOException e) {
      return "no se pudo guardar el chasis: " + e.getMessage();
    }
  }

  /** Points the site mode to another chassis file (the tests use a temp one). */
  void chassisPath(String path) {
    chassisPath = path;
  }

  /** The ship types, in the enum order, as names. */
  private static List<String> shipTypes() {
    List<String> types = new ArrayList<>();
    for(org.gts.bst.ship.ShipType type : org.gts.bst.ship.ShipType.values()) {
      types.add(type.name());
    }
    return types;
  }

  /** The name of the chassis when it is also a ship type, else "". */
  private static String budgetName(String chassisName) {
    return ShipSites.budgetOf(chassisName) == null ? "" : chassisName;
  }

  private static int indexOfName(List<ShipArtFile> parts, String name) {
    for(int i = 0; i < parts.size(); i++) {
      if(parts.get(i).name().equalsIgnoreCase(name)) {
        return i;
      }
    }
    return -1;
  }

  /** Opens a ship type: selects its chassis and its site budget. */
  private void selectType(String type) {
    shipType = type;
    typeListOpen = false;
    preview = -1;
    int index = indexOfName(chassis, type);
    if(index >= 0) {
      chassisIndex = index;
      assembly = assembly.withChassis(chassis.get(index).name());
      updateTitle();
      snapshotCells();
      message = "tipo: " + type;
    } else {
      message = "tipo: " + type + " (escribe [" + type + "] en chassis.txt)";
    }
    canvas.invalidate();
  }

  private ShipArtFile findPieceStartingWith(String prefix) {
    for(ShipArtFile piece : pieces) {
      if(piece.name().toLowerCase().startsWith(prefix)) {
        return piece;
      }
    }
    return null;
  }

  private int panelWidth(TerminalSize size) {
    return size.getColumns() < 60 ? 0 : Math.min(32, Math.max(24, size.getColumns() / 4));
  }

  /** The site panel of the right side: the budget and the warnings. */
  private void drawPanel(TextGUIGraphics graphics, TerminalSize size, int canvasWidth) {
    int panelWidth = size.getColumns() - canvasWidth;
    if(panelWidth <= 0) {
      return;
    }
    ShipArtFile hull = chassis.get(chassisIndex);
    List<ShipSites.Site> sites = ShipSites.sitesOf(hull);
    Map<Character, Integer> placed = ShipSites.count(sites);
    Map<Character, Integer> runs = ShipSites.longestRun(sites);
    List<String> lines = new ArrayList<>(ShipSites.panel(shipType, placed, runs));
    List<String> warnings = ShipSites.warnings(shipType, placed, runs);
    if(preview >= 0) {
      lines.add(1, "vista: " + PREVIEWS[preview]);
    }
    lines.add("");
    int warningStart = lines.size();
    int wide = Math.max(8, panelWidth - 4);
    for(String warning : warnings) {
      String rest = warning;
      while(rest.length() > wide) {
        int cutAt = rest.lastIndexOf(' ', wide);
        if(cutAt <= 0) {
          cutAt = wide;
        }
        lines.add("· " + rest.substring(0, cutAt));
        rest = rest.substring(cutAt).stripLeading();
      }
      lines.add("· " + rest);
    }
    for(int row = 0; row < lines.size() && row < size.getRows() - 2; row++) {
      graphics.setForegroundColor(row >= warningStart && !warnings.isEmpty() ? TextColor.ANSI.YELLOW
          : TextColor.ANSI.WHITE);
      graphics.putString(canvasWidth, row, cut("│ " + lines.get(row), panelWidth));
    }
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
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
