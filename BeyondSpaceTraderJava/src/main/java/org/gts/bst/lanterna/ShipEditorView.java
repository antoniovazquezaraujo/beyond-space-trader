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
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.HullFile;
import org.gts.bst.view.LetterGrid;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipDesign;
import org.gts.bst.view.ShipSites;


/**
 * The ship editor: on the left the 17 types of the game, always in enum order
 * and always complete (TAB moves through them), and the numbered elements (the
 * piece keys the game may fill a site with); then the hull with its site letters
 * (space paints or erases) and then the same ship with the real pieces. The type
 * of a design is fixed: only its name, its chassis and its groups can change.
 * The hull is never touched: the letters are the groups the game will fill with
 * the pieces that say `key=` them.
 */
public final class ShipEditorView extends ArtEditorWindow {
  private static final int STRIP_ROWS = 2;
  /** One slot per game type, in enum order: a design never leaves its type. */
  private final List<ShipDesign> designs;
  private final List<ShipArtFile> hulls;
  private final List<ShipArtFile> pieces;
  private final Map<ShipSites.Kind, Integer> variants = new LinkedHashMap<>();
  private int designIndex;
  /** The key of the element that space paints. */
  private char pen;
  private LetterGrid grid = new LetterGrid();
  private int cursorX = 2;
  private int cursorY = 2;
  private boolean listOpen;
  private int listIndex;
  private String message = "";
  /** Designs of the file dropped for not having a single known type. */
  private int ignored;
  /** True when a hull without size has taken the size of a type. */
  private boolean hullSizesAdopted;
  /** The ships file the editor saves to (a field so the tests can point elsewhere). */
  private String shipsPath = ShipArtFile.resolve("ships.txt").toString();
  /** The chassis file the editor saves to when it has adopted a hull size. */
  private String hullsPath = ShipArtFile.resolve("chassis.txt").toString();

  public ShipEditorView(List<ShipDesign> designs, List<ShipArtFile> hulls, List<ShipArtFile> pieces) {
    super("ship editor");
    this.hulls = hulls;
    this.pieces = pieces;
    this.designs = slots(designs);
    loadDesign(0);
    if(ignored > 0) {
      message = "ignored " + ignored + " designs with an unknown or repeated type";
    }
  }

  /** Points the editor to another ships file (the tests use a temp one). */
  void shipsPath(String path) {
    shipsPath = path;
  }

  /** Points the editor to another chassis file (the tests use a temp one). */
  void hullsPath(String path) {
    hullsPath = path;
  }

  /**
   * One design per game type, in enum order: the loaded design of that type, or
   * a new one (named after the type, with the first hull of its size and no
   * groups) when the file has none. Designs whose type is unknown or repeated
   * are dropped.
   */
  private List<ShipDesign> slots(List<ShipDesign> loaded) {
    Map<ShipType, ShipDesign> byType = new LinkedHashMap<>();
    for(ShipDesign design : loaded) {
      ShipType type = ShipSites.typeOf(design.type());
      if(type == null || byType.containsKey(type)) {
        ignored++;
        continue;
      }
      byType.put(type, design);
    }
    List<ShipDesign> slots = new ArrayList<>();
    for(ShipType type : ShipType.values()) {
      ShipDesign design = byType.get(type);
      if(design == null) {
        slots.add(new ShipDesign(type.name(), type.name(), newChassis(type), List.of()));
      } else {
        slots.add(new ShipDesign(design.name(), type.name(), design.chassis(), design.groups()));
      }
    }
    return slots;
  }

  /** The first hull of the size of a type; an unsized one takes that size. */
  private String newChassis(ShipType type) {
    ShipSites.Budget budget = ShipSites.budgetOf(type.name());
    ShipArtFile unsized = null;
    for(ShipArtFile hull : hulls) {
      if(hull.size().equalsIgnoreCase(budget.size().name())) {
        return hull.name();
      }
      if(unsized == null && isUnsized(hull.size())) {
        unsized = hull;
      }
    }
    if(unsized == null) {
      return "";
    }
    adoptSize(unsized.name(), budget.size());
    return unsized.name();
  }

  /** True when a hull declares no usable size (empty or `any`). */
  private static boolean isUnsized(String size) {
    return size == null || size.isEmpty() || size.equalsIgnoreCase("any");
  }

  /** Writes the size of a type into a hull that had none; true when the hull changed. */
  private boolean adoptSize(String hullName, ShipSize size) {
    for(int i = 0; i < hulls.size(); i++) {
      ShipArtFile hull = hulls.get(i);
      if(hull.name().equalsIgnoreCase(hullName) && isUnsized(hull.size())) {
        hulls.set(i, new ShipArtFile(hull.name(), hull.color(), hull.cells(), hull.blink(), hull.bgColor(),
            hull.letter(), hull.letters(), hull.zones(), size.name().toLowerCase(java.util.Locale.ROOT)));
        hullSizesAdopted = true;
        return true;
      }
    }
    return false;
  }

  private void loadDesign(int index) {
    designIndex = Math.max(0, Math.min(index, designs.size() - 1));
    ShipDesign design = designs.get(designIndex);
    grid = LetterGrid.ofDesign(design);
    if(pen == 0 || variantsOf(pen).isEmpty()) {
      pen = firstPen();
    }
    updateTitle();
  }

  /** Renames the open ship with a little dialog (the [name] of ships.txt). */
  private void renameDesign() {
    ShipDesign design = design();
    com.googlecode.lanterna.gui2.WindowBasedTextGUI gui = getTextGUI();
    if(gui == null) {
      return;
    }
    String typed = com.googlecode.lanterna.gui2.dialogs.TextInputDialog.showDialog(gui, "ship name",
        "Name (it will be the [name] in ships.txt):", design.name());
    rename(typed);
  }

  /** Renames the open ship; the type never changes (the tests call it without the dialog). */
  void rename(String name) {
    if(name == null || name.isBlank()) {
      return;
    }
    storeCurrent();
    ShipDesign design = design();
    designs.set(designIndex, new ShipDesign(name.strip(), design.type(), design.chassis(), design.groups()));
    updateTitle();
    message = "renamed: " + name.strip();
    redraw();
  }

  /** Moves to another ship, cycling: from the last one it goes back to the first. */
  private void nextDesign(int step) {
    if(designs.isEmpty()) {
      return;
    }
    storeCurrent();
    loadDesign((designIndex + step + designs.size()) % designs.size());
    announceShip();
    redraw();
  }

  /** Says which type is open, so TAB does not look like it does nothing. */
  private void announceShip() {
    ShipDesign design = design();
    message = "ship " + (designIndex + 1) + "/" + designs.size() + ": " + design.name();
  }

  private void storeCurrent() {
    ShipDesign design = designs.get(designIndex);
    List<ShipDesign.LetterGroup> groups = new ArrayList<>();
    for(LetterGrid.Run run : grid.runs()) {
      groups.add(new ShipDesign.LetterGroup(run.letter(), run.x(), run.y(), run.n()));
    }
    designs.set(designIndex, new ShipDesign(design.name(), design.type(), design.chassis(), List.copyOf(groups)));
  }

  private ShipDesign design() {
    return designs.get(designIndex);
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

  /** The first key of the tree, for the pen. */
  private char firstPen() {
    List<Character> pens = pens();
    return pens.isEmpty() ? 0 : pens.get(0);
  }

  /** The keys of the kinds with pieces, in the tree order. */
  private List<Character> pens() {
    List<Character> pens = new ArrayList<>();
    for(ShipSites.Kind kind : ShipSites.Kind.values()) {
      for(ShipArtFile piece : piecesOfKind(kind)) {
        char key = piece.letter().charAt(0);
        if(!pens.contains(key)) {
          pens.add(key);
        }
      }
    }
    return pens;
  }

  /** Moves the selected element through the panel (n and p). */
  private void cyclePen(int step) {
    List<Character> pens = pens();
    if(pens.isEmpty()) {
      message = "no pieces with a key in pieces.txt";
      return;
    }
    int index = pens.indexOf(pen);
    pen = index < 0 ? pens.get(step < 0 ? pens.size() - 1 : 0)
        : pens.get((index + step + pens.size()) % pens.size());
    message = "element: " + elementLabel(pen) + (variantsOf(pen).size() > 1 ? " · [v] variants" : "");
  }

  /** Picks an element by its number (the keys 1 to 9). */
  private void pickPen(int index) {
    List<Character> pens = pens();
    if(index >= pens.size()) {
      message = "no element " + (index + 1);
      return;
    }
    pen = pens.get(index);
    message = "element: " + elementLabel(pen) + (variantsOf(pen).size() > 1 ? " · [v] variants" : "");
  }

  /** The name of an element: the kind of piece and the key that paints it. */
  private String elementLabel(char letter) {
    return elementName(letter) + " (" + letter + ")";
  }

  /** The name of an element alone: the panel lists the key in its own column. */
  private String elementName(char letter) {
    return capitalized(ShipSites.kindName(ShipSites.kindOfLetter(letter, pieces)));
  }

  /** The `have/max` of an element: `?` when the type declares no maximum. */
  private static String countText(int there, int max) {
    return max < 0 ? "?" : there + "/" + max;
  }

  /** The colour of a mark: yellow when missing, the red of the palette when over, lime when just right. */
  private static TextColor markColor(String mark) {
    switch(mark) {
      case "⚠":
        return TextColor.ANSI.YELLOW;
      case "✗":
        return UiPalette.BAD;
      case "✓":
        return TextColor.ANSI.GREEN_BRIGHT;
      default:
        return TextColor.ANSI.WHITE;
    }
  }

  /** Space: any letter in the cell is erased; if it is empty, the pen is painted; the cursor moves right. */
  private void toggleCell() {
    char current = grid.at(cursorX, cursorY);
    if(current != ' ') {
      grid.clear(cursorX, cursorY);
      message = "erased " + current + " at x=" + cursorX + " y=" + cursorY;
      cursorX++;
      redraw();
      return;
    }
    if(pen == 0) {
      message = "empty cell and no element picked (use n, p or a number)";
      return;
    }
    if(!canPlace(pen)) {
      return;
    }
    grid.set(cursorX, cursorY, pen);
    ShipSites.Kind kind = ShipSites.kindOfLetter(pen, pieces);
    message = "painted " + pen + " (" + ShipSites.kindName(kind) + " " + countOfKind(kind) + "/" + maxOfKind(kind) + ")";
    cursorX++;
    redraw();
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
    return ofLetter.get(variants.getOrDefault(ShipSites.kindOfLetter(letter, pieces), 0) % ofLetter.size());
  }

  /** The window title: the name and the type are already in the ships list. */
  private void updateTitle() {
    setTitle("ship editor");
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
      nextDesign(1);
      return true;
    }
    if(key.getKeyType() == KeyType.ReverseTab) {
      nextDesign(-1);
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

  /** The letters are vim keys (move), the digits pick an element; the rest are commands. */
  private void character(char character) {
    if(character >= '1' && character <= '9') {
      pickPen(character - '1');
      return;
    }
    switch(Character.toLowerCase(character)) {
      case 'h':
        cursorX = Math.max(0, cursorX - 1);
        break;
      case 'l':
        cursorX++;
        break;
      case 'k':
        cursorY = Math.max(0, cursorY - 1);
        break;
      case 'j':
        cursorY++;
        break;
      case ' ':
        toggleCell();
        break;
      case 'n':
        cyclePen(1);
        break;
      case 'p':
        cyclePen(-1);
        break;
      case 'v':
        cycleVariant();
        break;
      case 't':
        renameDesign();
        break;
      case 'f':
        openList();
        break;
      case 's':
        save();
        break;
      default:
        break;
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
  /** Cycles which piece of the kind of the pen is shown. */
  private void cycleVariant() {
    ShipSites.Kind kind = ShipSites.kindOfLetter(pen, pieces);
    if(variantsOf(pen).size() <= 1) {
      message = "the " + ShipSites.kindName(kind) + " has no variants";
      return;
    }
    variants.merge(kind, 1, Integer::sum);
    message = "preview: " + pieceFor(pen).name();
  }

  private boolean canPlace(char letter) {
    ShipSites.Kind kind = ShipSites.kindOfLetter(letter, pieces);
    if(kind == ShipSites.Kind.PART) {
      message = "note: no piece uses the key " + letter;
      return true;
    }
    int max = maxOfKind(kind);
    if(max < 0) {
      message = "⚠ the type " + design().type() + " fixes no limit";
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
      message = "⚠ " + ShipSites.kindName(kind) + ": the " + design.type() + " takes " + max;
      return false;
    }
    return true;
  }

  /** The hulls the open type may use: the ones of its size or without size. */
  private List<ShipArtFile> offeredHulls() {
    List<ShipArtFile> offered = new ArrayList<>();
    ShipSites.Budget budget = ShipSites.budgetOf(design().type());
    if(budget == null) {
      return offered;
    }
    for(ShipArtFile hull : hulls) {
      if(offer(hull, budget.size())) {
        offered.add(hull);
      }
    }
    return offered;
  }

  /**
   * True when a hull suits a size. A hull with a size only suits that size; a
   * hull without one suits any size, but not when a type of another size is
   * already using it (it cannot serve two sizes).
   */
  private boolean offer(ShipArtFile hull, ShipSize size) {
    if(!isUnsized(hull.size())) {
      return ShipSites.sizeFits(hull.size(), size);
    }
    for(ShipDesign other : designs) {
      ShipSites.Budget budget = ShipSites.budgetOf(other.type());
      if(budget != null && budget.size() != size && other.chassis().equalsIgnoreCase(hull.name())) {
        return false;
      }
    }
    return true;
  }

  /** The items of the open list: the hulls the open type may use, in file order. */
  private List<String> items() {
    List<String> items = new ArrayList<>();
    for(ShipArtFile hull : offeredHulls()) {
      items.add(hull.name() + (isUnsized(hull.size()) ? " (no size)" : ""));
    }
    return items;
  }

  /** Opens the list of chassis, with the current one selected. */
  private void openList() {
    ShipDesign design = design();
    String current = design.chassis();
    List<ShipArtFile> offered = offeredHulls();
    listIndex = 0;
    for(int i = 0; i < offered.size(); i++) {
      if(offered.get(i).name().equalsIgnoreCase(current)) {
        listIndex = i;
        break;
      }
    }
    listOpen = true;
    message = "pick the chassis";
  }

  private void pick() {
    List<ShipArtFile> offered = offeredHulls();
    ShipDesign design = design();
    if(listIndex < 0 || listIndex >= offered.size()) {
      listOpen = false;
      redraw();
      return;
    }
    ShipArtFile hull = offered.get(listIndex);
    ShipSize size = ShipSites.budgetOf(design.type()).size();
    storeCurrent();
    designs.set(designIndex, new ShipDesign(design.name(), design.type(), hull.name(), design.groups()));
    listOpen = false;
    updateTitle();
    boolean adopted = adoptSize(hull.name(), size);
    message = "chasis=" + hull.name()
        + (adopted ? " (size=" + size.name().toLowerCase(java.util.Locale.ROOT) + ")" : "");
    redraw();
  }

  private void save() {
    storeCurrent();
    try {
      ShipDesign.save(shipsPath, designs);
      String chassis = "";
      if(hullSizesAdopted) {
        HullFile.save(hullsPath, hulls);
        chassis = " and " + hullsPath;
      }
      message = "saved: " + shipsPath + chassis + " (" + designs.size() + " ships)";
    } catch(IOException e) {
      message = "could not save: " + e.getMessage();
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
    int panel = panelWidth(size);
    int canvas = size.getColumns() - panel - 1;
    int half = Math.max(1, (canvas - 1) / 2);
    for(int row = 0; row < rows; row++) {
      graphics.setCharacter(panel, row, '│');
      if(canvas > 1) {
        graphics.setCharacter(panel + half + 1, row, '│');
      }
    }
    ShipArtFile hull = hull();
    if(hull == null) {
      ShipDesign design = design();
      graphics.putString(panel + 2, 1, EditorText.cut("cannot find the chassis " + design.chassis()
          + " (pick one from chassis.txt with [f])", canvas - 1));
      graphics.putString(panel + 2, 2, EditorText.cut("hulls: " + hullsLine(), canvas - 1));
    } else {
      paintHull(graphics, hull, panel + 1, 0, half, rows, true);
      paintHull(graphics, hull, panel + half + 2, 0, canvas - half - 1, rows, false);
    }
    paintPanel(graphics, panel, rows);
    graphics.setForegroundColor(message.startsWith("⚠") ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
    graphics.putString(0, rows, EditorText.cut(message, size.getColumns()));
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows + 1, EditorText.cut(keysLine(), size.getColumns()));
  }

  private int panelWidth(TerminalSize size) {
    // The five columns and the hull-size warning need room, but an 80-column
    // terminal still keeps at least 39 columns for the canvas.
    return Math.min(34, Math.max(16, size.getColumns() - 40));
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

  private static String capitalized(String text) {
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /** The vertical panel on the left: the 17 types (with their design) and the numbered elements. */
  private void paintPanel(TextGUIGraphics graphics, int width, int rows) {
    ShipDesign design = design();
    ShipSites.Budget budget = design == null ? null : ShipSites.budgetOf(design.type());
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, 0, EditorText.section("Ships", width));
    int row = 1;
    int nameColumn = shipNameColumn(width);
    int first = Math.max(0, designIndex - 5);
    for(int i = first; i < designs.size() && i < first + 6 && row < rows; i++, row++) {
      if(i == designIndex) {
        graphics.setForegroundColor(TextColor.ANSI.BLACK);
        graphics.setBackgroundColor(TextColor.ANSI.WHITE);
      }
      graphics.putString(0, row, shipRow(designs.get(i), nameColumn, width));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    }
    if(row < rows) {
      graphics.putString(0, row++, EditorText.section("Elements", width));
    }
    ShipArtFile hull = hull();
    if(row < rows && hull != null && budget != null && !hull.size().isEmpty()
        && !ShipSites.sizeFits(hull.size(), budget.size())) {
      graphics.setForegroundColor(TextColor.ANSI.YELLOW);
      graphics.putString(0, row++, EditorText.cut(hullWarning(hull, budget), width));
    }
    List<Character> pens = pens();
    if(pens.isEmpty()) {
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      if(row < rows) {
        graphics.putString(0, row, EditorText.cut("(no pieces with a key in pieces.txt)", width));
      }
      return;
    }
    Map<ShipSites.Kind, Integer> counts = ShipSites.countsByKind(currentGroups(), pieces);
    int numberColumn = String.valueOf(pens.size()).length();
    int elementColumn = elementNameColumn(pens);
    int countColumn = elementCountColumn(pens, budget, counts);
    for(int i = 0; i < pens.size() && row < rows; i++, row++) {
      char letter = pens.get(i);
      ShipSites.Kind kind = ShipSites.kindOfLetter(letter, pieces);
      int max = budget == null ? -1 : ShipSites.maxOfKind(budget, kind);
      int there = counts.getOrDefault(kind, 0);
      String count = countText(there, max);
      String mark = max < 0 ? "?" : there < max ? "⚠" : there == max ? "✓" : "✗";
      if(letter == pen) {
        graphics.setForegroundColor(TextColor.ANSI.BLACK);
        graphics.setBackgroundColor(TextColor.ANSI.WHITE);
      } else {
        graphics.setForegroundColor(markColor(mark));
      }
      String text = EditorText.padLeft(String.valueOf(i + 1), numberColumn) + "  " + letter + "  "
          + EditorText.padRight(elementName(letter), elementColumn) + "  "
          + EditorText.padLeft(count, countColumn) + "  " + mark;
      graphics.putString(0, row, EditorText.padRight(EditorText.cut(text, width), width));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    }
  }

  /**
   * The width of the name column of the ships: the longest name, always leaving
   * room for the type bracket so the type column does not jump.
   */
  private int shipNameColumn(int width) {
    int names = 0;
    int types = 0;
    for(ShipDesign other : designs) {
      names = Math.max(names, EditorText.width(other.name()));
      types = Math.max(types, other.type().isEmpty() ? 0 : EditorText.width(other.type()) + 3);
    }
    return Math.min(names, Math.max(1, width - types));
  }

  /** A ship as the panel lists it: the name and its type, each in its own column. */
  private static String shipRow(ShipDesign design, int nameColumn, int width) {
    String name = EditorText.cut(EditorText.padRight(design.name(), nameColumn), nameColumn);
    String text = design.type().isEmpty() ? name : name + " [" + design.type() + "]";
    return EditorText.padRight(EditorText.cut(text, width), width);
  }

  /** The width of the name column of the elements, fixed for every row. */
  private int elementNameColumn(List<Character> pens) {
    int column = 1;
    for(char letter : pens) {
      column = Math.max(column, elementName(letter).length());
    }
    return column;
  }

  /** The width of the `have/max` column of the elements, fixed for every row. */
  private int elementCountColumn(List<Character> pens, ShipSites.Budget budget, Map<ShipSites.Kind, Integer> counts) {
    int column = 1;
    for(char letter : pens) {
      ShipSites.Kind kind = ShipSites.kindOfLetter(letter, pieces);
      int max = budget == null ? -1 : ShipSites.maxOfKind(budget, kind);
      column = Math.max(column, countText(counts.getOrDefault(kind, 0), max).length());
    }
    return column;
  }

  /** The warning of a hull whose size does not match the size of the ship type. */
  private static String hullWarning(ShipArtFile hull, ShipSites.Budget budget) {
    return "⚠ hull size: " + hull.size() + " (type: " + budget.size().name().toLowerCase(java.util.Locale.ROOT) + ")";
  }

  /** Draws the open list of hulls or ship types. */
  private void paintList(TextGUIGraphics graphics, TerminalSize size) {
    List<String> items = items();
    int left = Math.max(1, (size.getColumns() - 24) / 2);
    int top = Math.max(0, (size.getRows() - items.size() - 2) / 2);
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(left, top, "chassis:");
    for(int i = 0; i < items.size() && top + 1 + i < size.getRows() - 1; i++) {
      if(i == listIndex) {
        graphics.setForegroundColor(TextColor.ANSI.BLACK);
        graphics.setBackgroundColor(TextColor.ANSI.WHITE);
      }
      graphics.putString(left, top + 1 + i, EditorText.cut(String.format("%-22s", items.get(i)), 22));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    }
    graphics.putString(1, size.getRows() - 1, "[arrows] choose · [ENTER] take · [ESC] cancel");
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
          EditorText.glyph(graphics, originX + column, originY + row, codePoint, zoneBrush(hull, column, row), false);
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
    // one piece per letter: a run of AAA is three weapons
    for(int i = 0; i < run.n(); i++) {
      ShipArtFile piece = pieceFor(run.letter());
      if(piece == null || x + i >= left + width) {
        break;
      }
      for(int row = 0; row < piece.height() && y + row < top + height; row++) {
        for(int column = 0; column < piece.width() && x + i + column < left + width; column++) {
          int codePoint = piece.at(row, column);
          if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
            EditorText.glyph(graphics, x + i + column, y + row, codePoint, pieceBrush(piece), false);
          }
        }
      }
    }
  }

  /** The brush of a piece: its colour, its background and its blink. */
  private EditorText.Brush pieceBrush(ShipArtFile piece) {
    return EditorText.brush(piece.color(), piece.bgColor(), piece.blink(), blinkOn());
  }

  /** The brush of a cell of the chassis: the colour letter of its zone, or the hull's own. */
  private EditorText.Brush zoneBrush(ShipArtFile hull, int x, int y) {
    for(ShipArtFile.Zone zone : hull.zones()) {
      if(x >= zone.x() && x < zone.x() + Math.max(1, zone.w()) && y >= zone.y()
          && y < zone.y() + Math.max(1, zone.h())) {
        for(ShipArtFile.ColorLetter letter : hull.letters()) {
          if(letter.letter() == zone.letter()) {
            return EditorText.brush(letter.color(), letter.bgColor(), letter.blink(), blinkOn());
          }
        }
      }
    }
    return EditorText.brush(hull.color(), hull.bgColor(), hull.blink(), blinkOn());
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
    return "[arrows] or hjkl move · space paint/erase · [n/p] element · [1-9] pick · [v] variant"
        + " · [t] rename · [f] frame" + (design == null ? "" : " (" + design.chassis() + ")")
        + " · [s] save · [TAB] type · [ESC] exit";
  }

  private static TextColor color(String name) {
    return org.gts.bst.view.ShipColors.color(name);
  }
}
