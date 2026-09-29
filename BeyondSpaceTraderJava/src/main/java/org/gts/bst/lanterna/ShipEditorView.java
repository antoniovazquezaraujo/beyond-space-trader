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
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gts.bst.view.LetterGrid;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipDesign;
import org.gts.bst.view.ShipSites;


/**
 * The ship editor: on the left the hull of the ship with its site letters (typed
 * with the keyboard; space erases a whole group), on the right the same ship with
 * the real pieces. The hull is never touched: the letters are the groups the game
 * will fill with the pieces that say `key=` them.
 */
public final class ShipEditorView extends ArtEditorWindow {
  private static final int STRIP_ROWS = 3;
  private final List<ShipDesign> designs;
  private final List<ShipArtFile> hulls;
  private final List<ShipArtFile> pieces;
  private final Map<ShipSites.Kind, Integer> variants = new LinkedHashMap<>();
  private int designIndex;
  private LetterGrid grid = new LetterGrid();
  private int cursorX = 2;
  private int cursorY = 2;
  private int previewRole;
  private static final String[] ROLES = {"trader", "pirate", "police"};
  private boolean listOpen;
  private boolean typeList;
  private int listIndex;
  private String message = "";
  /** The ships file the editor saves to (a field so the tests can point elsewhere). */
  private String shipsPath = ShipArtFile.resolve("ships.txt").toString();

  public ShipEditorView(List<ShipDesign> designs, List<ShipArtFile> hulls, List<ShipArtFile> pieces) {
    super("editor de naves");
    this.designs = new ArrayList<>(designs);
    this.hulls = hulls;
    this.pieces = pieces;
    loadDesign(0);
  }

  /** Points the editor to another ships file (the tests use a temp one). */
  void shipsPath(String path) {
    shipsPath = path;
  }

  private void loadDesign(int index) {
    designIndex = Math.max(0, Math.min(index, designs.size() - 1));
    if(designs.isEmpty()) {
      grid = new LetterGrid();
      message = "no hay naves: escribe [nombre] con type= y chasis= en ships.txt";
      updateTitle();
      return;
    }
    ShipDesign design = designs.get(designIndex);
    grid = LetterGrid.ofDesign(design);
    updateTitle();
  }

  private void storeCurrent() {
    if(designs.isEmpty()) {
      return;
    }
    ShipDesign design = designs.get(designIndex);
    List<ShipDesign.LetterGroup> groups = new ArrayList<>();
    for(LetterGrid.Run run : grid.runs()) {
      groups.add(new ShipDesign.LetterGroup(run.letter(), run.x(), run.y(), run.n()));
    }
    designs.set(designIndex, new ShipDesign(design.name(), design.type(), design.chassis(), List.copyOf(groups)));
  }

  private ShipDesign design() {
    return designs.isEmpty() ? null : designs.get(designIndex);
  }

  private ShipArtFile hull() {
    ShipDesign design = design();
    if(design == null) {
      return null;
    }
    for(ShipArtFile hull : hulls) {
      if(hull.name().equalsIgnoreCase(design.chassis())) {
        return hull;
      }
    }
    return null;
  }

  /** The pieces that fill a letter, in file order. */
  private List<ShipArtFile> variantsOf(char letter) {
    List<ShipArtFile> ofLetter = new ArrayList<>();
    for(ShipArtFile piece : pieces) {
      if(!piece.letter().isEmpty()
          && Character.toUpperCase(piece.letter().charAt(0)) == Character.toUpperCase(letter)) {
        ofLetter.add(piece);
      }
    }
    return ofLetter;
  }

  /** The piece the preview draws for a letter (the chosen variant, or the role). */
  private ShipArtFile pieceFor(char letter) {
    List<ShipArtFile> ofLetter = variantsOf(letter);
    if(ofLetter.isEmpty()) {
      return null;
    }
    if(ShipSites.kindOfLetter(letter, pieces) == ShipSites.Kind.ROLE) {
      // el rol de la vista: el juego real elige el suyo en cada encuentro
      for(ShipArtFile piece : ofLetter) {
        if(piece.name().toLowerCase().endsWith(ROLES[previewRole])) {
          return piece;
        }
      }
    }
    return ofLetter.get(variants.getOrDefault(ShipSites.kindOfLetter(letter, pieces), 0) % ofLetter.size());
  }

  private void updateTitle() {
    ShipDesign design = design();
    setTitle(design == null ? "editor de naves" : "naves: " + design.name() + " [" + design.type() + "]");
  }

  @Override
  protected boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      if(listOpen) {
        listOpen = false;
        redraw();
        return true;
      }
      close();
      return true;
    }
    if(listOpen) {
      List<String> items = items();
      if(key.getKeyType() == KeyType.ArrowUp && !items.isEmpty()) {
        listIndex = (listIndex + items.size() - 1) % items.size();
      } else if(key.getKeyType() == KeyType.ArrowDown && !items.isEmpty()) {
        listIndex = (listIndex + 1) % items.size();
      } else if(key.getKeyType() == KeyType.Enter) {
        pick();
      }
      redraw();
      return true;
    }
    if(key.getKeyType() == KeyType.Tab) {
      storeCurrent();
      loadDesign(designIndex + 1);
      redraw();
      return true;
    }
    if(key.getKeyType() == KeyType.ReverseTab) {
      storeCurrent();
      loadDesign(designIndex - 1);
      redraw();
      return true;
    }
    switch(key.getKeyType()) {
      case ArrowLeft:
        cursorX = Math.max(0, cursorX - 1);
        break;
      case ArrowRight:
        cursorX++;
        break;
      case ArrowUp:
        cursorY = Math.max(0, cursorY - 1);
        break;
      case ArrowDown:
        cursorY++;
        break;
      case Character:
        character(key.getCharacter());
        break;
      default:
        break;
    }
    redraw();
    return true;
  }

  /** The letter to paint for a typed character: a piece's key or a suggested one. */
  private char keyLetter(char typed) {
    for(ShipArtFile piece : pieces) {
      if(!piece.letter().isEmpty()
          && Character.toUpperCase(piece.letter().charAt(0)) == Character.toUpperCase(typed)) {
        return piece.letter().charAt(0);
      }
    }
    return ShipSites.isSite(Character.toUpperCase(typed)) ? Character.toUpperCase(typed) : 0;
  }

  private void character(char character) {
    char letter = keyLetter(character);
    if(letter != 0) {
      if(canPlace(letter)) {
        grid.set(cursorX, cursorY, letter);
        ShipSites.Kind kind = ShipSites.kindOfLetter(letter, pieces);
        message = "sitio " + letter + " (" + ShipSites.kindName(kind) + " " + countOfKind(kind) + "/"
            + maxOfKind(kind) + ")";
      }
    } else if(character == ' ') {
      LetterGrid.Run run = grid.runAt(cursorX, cursorY);
      if(run == null) {
        message = "nada que borrar en x=" + cursorX + " y=" + cursorY;
      } else {
        for(int i = 0; i < run.n(); i++) {
          grid.clear(run.x() + i, run.y());
        }
        message = "grupo " + run.letter() + " borrado";
      }
    } else if(character == 'v') {
      char under = grid.at(cursorX, cursorY);
      if(under == ' ') {
        message = "pon el cursor sobre una letra para cambiar la vista de la pieza";
      } else if(variantsOf(under).size() <= 1) {
        message = "la pieza " + ShipSites.kindName(ShipSites.kindOfLetter(under, pieces)) + " no tiene variantes";
      } else {
        ShipSites.Kind kind = ShipSites.kindOfLetter(under, pieces);
        variants.merge(kind, 1, Integer::sum);
        message = "vista: " + pieceFor(under).name();
      }
    } else if(character == 'o') {
      previewRole = (previewRole + 1) % ROLES.length;
      message = "rol de la vista: " + ROLES[previewRole];
    } else if(character == 'h') {
      openList(false);
    } else if(character == 'y') {
      openList(true);
    } else if(character == 's') {
      save();
    }
  }

  /** The count of a kind in the design, as the grid has it now. */
  private int countOfKind(ShipSites.Kind kind) {
    return ShipSites.countsByKind(currentGroups(), pieces).getOrDefault(kind, 0);
  }

  private int maxOfKind(ShipSites.Kind kind) {
    ShipDesign design = design();
    ShipSites.Budget budget = design == null ? null : ShipSites.budgetOf(design.type());
    return budget == null ? -1 : ShipSites.maxOfKind(budget, kind);
  }

  /**
   * True when that letter can go in the cell. It tries it and counts every group
   * of the kind (next to each other or apart): if the ship type does not admit
   * that many, it gives the cell back and warns.
   */
  private boolean canPlace(char letter) {
    ShipSites.Kind kind = ShipSites.kindOfLetter(letter, pieces);
    if(kind == ShipSites.Kind.PART) {
      message = "aviso: ninguna pieza usa la letra " + letter;
      return true;
    }
    int max = maxOfKind(kind);
    if(max < 0) {
      message = "⚠ elige el type de la nave con [y] para poder colocar sitios";
      return false;
    }
    char previous = grid.at(cursorX, cursorY);
    grid.set(cursorX, cursorY, letter);
    int count = countOfKind(kind);
    if(previous == ' ') {
      grid.clear(cursorX, cursorY);
    } else {
      grid.set(cursorX, cursorY, previous);
    }
    ShipDesign design = design();
    if(count > max) {
      message = "⚠ " + ShipSites.kindName(kind) + ": el " + design.type() + " admite " + max;
      return false;
    }
    return true;
  }

  /** The items of the open list: the hulls of chassis.txt or the game ship types. */
  private List<String> items() {
    List<String> items = new ArrayList<>();
    if(typeList) {
      for(org.gts.bst.ship.ShipType type : org.gts.bst.ship.ShipType.values()) {
        items.add(type.name());
      }
    } else {
      for(ShipArtFile hull : hulls) {
        items.add(hull.name());
      }
    }
    return items;
  }

  private void openList(boolean types) {
    typeList = types;
    ShipDesign design = design();
    String current = design == null ? "" : types ? design.type() : design.chassis();
    List<String> items = items();
    listIndex = 0;
    for(int i = 0; i < items.size(); i++) {
      if(items.get(i).equalsIgnoreCase(current)) {
        listIndex = i;
        break;
      }
    }
    listOpen = true;
    message = types ? "elige el type de la nave" : "elige el chasis de la nave";
  }

  private void pick() {
    List<String> items = items();
    ShipDesign design = design();
    if(design == null || items.isEmpty() || listIndex >= items.size()) {
      listOpen = false;
      redraw();
      return;
    }
    String value = items.get(listIndex);
    storeCurrent();
    designs.set(designIndex, new ShipDesign(design.name(), typeList ? value : design.type(),
        typeList ? design.chassis() : value, design.groups()));
    listOpen = false;
    updateTitle();
    message = (typeList ? "type=" : "chasis=") + value;
    redraw();
  }

  private void save() {
    if(designs.isEmpty()) {
      message = "no hay naves que guardar";
      return;
    }
    storeCurrent();
    try {
      ShipDesign.save(shipsPath, designs);
      message = "guardado: " + shipsPath + " (" + designs.size() + " naves)";
    } catch(IOException e) {
      message = "no se pudo guardar: " + e.getMessage();
    }
  }

  @Override
  protected void paint(TextGUIGraphics graphics) {
    TerminalSize size = getSize();
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    for(int row = 0; row < size.getRows(); row++) {
      graphics.putString(0, row, " ".repeat(size.getColumns()));
    }
    if(listOpen) {
      paintList(graphics, size);
      return;
    }
    int rows = size.getRows() - STRIP_ROWS;
    int treeWidth = treeWidth(size);
    int canvas = size.getColumns() - treeWidth;
    int half = (canvas - 1) / 2;
    for(int row = 0; row < rows; row++) {
      graphics.setCharacter(half, row, '│');
      if(treeWidth > 0) {
        graphics.setCharacter(canvas, row, '│');
      }
    }
    ShipArtFile hull = hull();
    if(hull == null) {
      ShipDesign design = design();
      graphics.putString(1, 1, design == null ? "no hay naves en ships.txt"
          : "no encuentro el chasis " + design.chassis() + " (con [h] eliges uno de chassis.txt)");
      graphics.putString(1, 2, EditorText.cut("hay: " + hullsLine(), canvas - 2));
    } else {
      paintHull(graphics, hull, 0, 0, half, rows, true);
      paintHull(graphics, hull, half + 1, 0, canvas - half - 1, rows, false);
    }
    paintTree(graphics, size, canvas + 1, rows);
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows, EditorText.cut(shipsLine(), size.getColumns()));
    graphics.setForegroundColor(message.startsWith("⚠") ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
    graphics.putString(0, rows + 1, EditorText.cut(message, size.getColumns()));
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows + 2, EditorText.cut(keysLine(), size.getColumns()));
  }

  private int treeWidth(TerminalSize size) {
    return size.getColumns() < 80 ? 0 : Math.min(30, Math.max(24, size.getColumns() / 4));
  }

  /** The pieces that fill a kind of site (only the ones with a key). */
  private List<ShipArtFile> piecesOfKind(ShipSites.Kind kind) {
    List<ShipArtFile> ofKind = new ArrayList<>();
    for(ShipArtFile piece : pieces) {
      if(!piece.letter().isEmpty() && ShipSites.kindOf(piece.name()) == kind) {
        ofKind.add(piece);
      }
    }
    return ofKind;
  }

  /** The keys of some pieces, without repeating. */
  private static String keysOf(List<ShipArtFile> ofKind) {
    StringBuilder keys = new StringBuilder();
    for(ShipArtFile piece : ofKind) {
      if(keys.indexOf(piece.letter()) < 0) {
        keys.append(keys.length() == 0 ? "" : " ").append(piece.letter());
      }
    }
    return keys.length() == 0 ? "-" : keys.toString();
  }

  /** The glyph of a piece: its first drawn character. */
  private static String glyphOf(ShipArtFile piece) {
    for(int row = 0; row < piece.height(); row++) {
      for(int column = 0; column < piece.width(); column++) {
        int codePoint = piece.at(row, column);
        if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
          return new String(Character.toChars(codePoint));
        }
      }
    }
    return "?";
  }

  /** The vertical panel: the tree of sites (kinds) with their pieces. */
  private void paintTree(TextGUIGraphics graphics, TerminalSize size, int left, int rows) {
    if(size.getColumns() - left <= 1) {
      return;
    }
    ShipDesign design = design();
    ShipSites.Budget budget = design == null ? null : ShipSites.budgetOf(design.type());
    Map<ShipSites.Kind, Integer> counts = ShipSites.countsByKind(currentGroups(), pieces);
    graphics.putString(left, 0, EditorText.cut("sites and pieces", size.getColumns() - left));
    int row = 1;
    ShipArtFile hull = hull();
    if(hull != null && budget != null && !hull.size().isEmpty() && !ShipSites.sizeFits(hull.size(), budget.size())) {
      graphics.setForegroundColor(TextColor.ANSI.YELLOW);
      graphics.putString(left, row++, EditorText.cut("⚠ size " + hull.size() + " vs " + budget.size(),
          size.getColumns() - left));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
    }
    for(ShipSites.Kind kind : ShipSites.Kind.values()) {
      List<ShipArtFile> ofKind = piecesOfKind(kind);
      int max = budget == null ? -1 : ShipSites.maxOfKind(budget, kind);
      if(ofKind.isEmpty() && (max <= 0 || kind == ShipSites.Kind.PART)) {
        continue;
      }
      int there = counts.getOrDefault(kind, 0);
      String mark = max < 0 ? "?" : there < max ? "⚠" : there == max ? "ok" : "✗";
      graphics.setForegroundColor(mark.equals("⚠") || mark.equals("✗") ? TextColor.ANSI.YELLOW
          : TextColor.ANSI.WHITE);
      graphics.putString(left, row++, EditorText.cut(ShipSites.kindName(kind) + " (" + keysOf(ofKind) + ")  " + there
          + "/" + (max < 0 ? "?" : max) + " " + mark, size.getColumns() - left));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      for(ShipArtFile piece : ofKind) {
        if(row >= rows) {
          return;
        }
        // el medidor de bodegas no usa el dibujo de la pieza: es braille, automatico
        String glyph = ShipSites.kindOf(piece.name()) == ShipSites.Kind.CARGO ? ShipSites.gauge(capacity())
            : glyphOf(piece);
        graphics.putString(left + 2, row++, EditorText.cut("· " + piece.name() + "  " + glyph,
            Math.max(0, size.getColumns() - left - 2)));
      }
    }
  }

  /** Draws the open list of hulls or ship types. */
  private void paintList(TextGUIGraphics graphics, TerminalSize size) {
    List<String> items = items();
    int left = Math.max(1, (size.getColumns() - 24) / 2);
    int top = Math.max(0, (size.getRows() - items.size() - 2) / 2);
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(left, top, typeList ? "type de la nave:" : "chasis de la nave:");
    for(int i = 0; i < items.size() && top + 1 + i < size.getRows() - 1; i++) {
      if(i == listIndex) {
        graphics.setForegroundColor(TextColor.ANSI.BLACK);
        graphics.setBackgroundColor(TextColor.ANSI.WHITE);
      }
      graphics.putString(left, top + 1 + i, EditorText.cut(String.format("%-22s", items.get(i)), 22));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    }
    graphics.putString(1, size.getRows() - 1, "[flechas] elegir · [ENTER] aceptar · [ESC] cancelar");
  }

  /** Draws a hull (with its colour zones), the letters on the left, the pieces on the right. */
  private void paintHull(TextGUIGraphics graphics, ShipArtFile hull, int left, int top, int width, int height,
      boolean withLetters) {
    int originX = left + Math.max(0, (width - hull.width()) / 2);
    int originY = top + Math.max(0, (height - hull.height()) / 2);
    for(int row = 0; row < hull.height() && originY + row < top + height; row++) {
      for(int column = 0; column < hull.width() && originX + column < left + width; column++) {
        int codePoint = hull.at(row, column);
        if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
          EditorText.glyph(graphics, originX + column, originY + row, codePoint, color(zoneColor(hull, column, row)),
              false);
        }
      }
    }
    for(LetterGrid.Run run : grid.runs()) {
      int x = originX + run.x();
      int y = originY + run.y();
      if(y < top || y >= top + height) {
        continue;
      }
      if(withLetters) {
        for(int i = 0; i < run.n() && x + i < left + width; i++) {
          EditorText.glyph(graphics, x + i, y, run.letter(), TextColor.ANSI.YELLOW, false);
        }
      } else {
        paintPiece(graphics, run, x, y, left, width, top, height);
      }
    }
    if(withLetters) {
      int x = originX + cursorX;
      int y = originY + cursorY;
      if(y >= top && y < top + height && x >= left && x < left + width) {
        char letter = grid.at(cursorX, cursorY);
        int codePoint = letter != ' ' ? letter
            : cursorY < hull.height() && cursorX < hull.width() ? hull.at(cursorY, cursorX) : '·';
        if(codePoint == ' ' || codePoint == ShipArtFile.CONTINUATION) {
          codePoint = '·';
        }
        EditorText.glyph(graphics, x, y, codePoint, TextColor.ANSI.YELLOW_BRIGHT, true);
      }
    }
  }

  private void paintPiece(TextGUIGraphics graphics, LetterGrid.Run run, int x, int y, int left, int width, int top,
      int height) {
    if(ShipSites.kindOfLetter(run.letter(), pieces) == ShipSites.Kind.CARGO) {
      String gauge = ShipSites.gauge(capacity());
      for(int i = 0; i < gauge.length() && i < run.n() && x + i < left + width; i++) {
        EditorText.glyph(graphics, x + i, y, gauge.codePointAt(i), color("green"), false);
      }
      return;
    }
    // una pieza por letra: una fila de AAA son tres armas
    for(int i = 0; i < run.n(); i++) {
      ShipArtFile piece = pieceFor(run.letter());
      if(piece == null || x + i >= left + width) {
        break;
      }
      for(int row = 0; row < piece.height() && y + row < top + height; row++) {
        for(int column = 0; column < piece.width() && x + i + column < left + width; column++) {
          int codePoint = piece.at(row, column);
          if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
            EditorText.glyph(graphics, x + i + column, y + row, codePoint, color(pieceColor(piece)), false);
          }
        }
      }
    }
  }

  private String pieceColor(ShipArtFile piece) {
    return piece.blink() && !blinkOn() ? (piece.bgColor().isEmpty() ? "black" : piece.bgColor()) : piece.color();
  }

  private String zoneColor(ShipArtFile hull, int x, int y) {
    for(ShipArtFile.Zone zone : hull.zones()) {
      if(x >= zone.x() && x < zone.x() + Math.max(1, zone.w()) && y >= zone.y()
          && y < zone.y() + Math.max(1, zone.h())) {
        for(ShipArtFile.ColorLetter letter : hull.letters()) {
          if(letter.letter() == zone.letter()) {
            return letter.color();
          }
        }
      }
    }
    return hull.color();
  }

  private int capacity() {
    ShipDesign design = design();
    ShipSites.Budget budget = design == null ? null : ShipSites.budgetOf(design.type());
    return budget == null ? 0 : budget.cargoBays();
  }

  private String hullsLine() {
    StringBuilder text = new StringBuilder();
    for(ShipArtFile hull : hulls) {
      text.append(text.length() == 0 ? "" : ", ").append(hull.name());
    }
    return text.toString();
  }

  private String shipsLine() {
    StringBuilder text = new StringBuilder("[TAB] nave:");
    for(int i = 0; i < designs.size(); i++) {
      text.append(i == designIndex ? " [" : " ").append(designs.get(i).name()).append(i == designIndex ? "]" : "");
    }
    return text.toString();
  }


  /** The groups of the design, as the grid has them now. */
  private List<ShipDesign.LetterGroup> currentGroups() {
    List<ShipDesign.LetterGroup> groups = new ArrayList<>();
    for(LetterGrid.Run run : grid.runs()) {
      groups.add(new ShipDesign.LetterGroup(run.letter(), run.x(), run.y(), run.n()));
    }
    return groups;
  }


  private String keysLine() {
    ShipDesign design = design();
    return "teclea C M D B R A E G P · espacio borra el grupo · [flechas] cursor"
        + (design == null ? "" : " · chasis: " + design.chassis())
        + " · [v] vista · [h] chasis · [y] type · [o] role preview (" + ROLES[previewRole] + ")"
        + " · [s] guardar · [TAB] nave · [ESC] salir";
  }

  private static TextColor color(String name) {
    return org.gts.bst.view.ShipColors.color(name);
  }
}
