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
import java.util.List;
import org.gts.bst.view.HullFile;
import org.gts.bst.view.LetterGrid;
import org.gts.bst.view.ShipArtFile;


/**
 * The hull editor: on the left the numbered colour elements (colour, background
 * and blink; added with + and removed with -), then the drawing with its colour
 * letters (space paints or erases) and then the same hull painted with those
 * colours. The drawing is never touched.
 */
public final class HullEditorView extends ArtEditorWindow {
  /** The colours of the art files (docs/ships.md), in the order the list shows them. */
  private static final String[] COLORS = {"white", "cyan", "red", "yellow", "green", "magenta", "blue", "orange",
      "purple", "pink", "brown", "lightgrey", "darkgrey", "gold", "navy", "teal", "olive", "maroon", "lime",
      "skyblue", "brightwhite", "brightcyan", "brightred", "brightyellow", "brightgreen", "brightmagenta",
      "brightblue"};
  /** The backgrounds: none first, then the dark and basic ones, then the rest. */
  private static final String[] BACKGROUNDS = {"", "black", "blue", "red", "green", "magenta", "yellow", "cyan",
      "darkgrey", "lightgrey", "navy", "teal", "olive", "maroon", "purple", "brown", "orange", "white", "gold",
      "pink", "lime", "skyblue", "brightwhite", "brightcyan", "brightred", "brightyellow", "brightgreen",
      "brightmagenta", "brightblue"};
  private static final int STRIP_ROWS = 3;
  private static final String[] SIZES = {"", "tiny", "small", "medium", "large", "huge", "any"};
  private final List<ShipArtFile> hulls;
  private int hullIndex;
  private LetterGrid grid;
  private List<ShipArtFile.ColorLetter> letters = new ArrayList<>();
  private int letterIndex;
  private int cursorX = 2;
  private int cursorY = 2;
  private boolean nameLetter;
  private boolean listOpen;
  private boolean backgroundList;
  private int listIndex;
  private String hullSize = "";
  private String message = "";
  /** The chassis file the editor saves to (a field so the tests can point elsewhere). */
  private String hullsPath = ShipArtFile.resolve("chassis.txt").toString();

  public HullEditorView(List<ShipArtFile> hulls) {
    super("hull editor");
    this.hulls = new ArrayList<>(hulls);
    loadHull(0);
  }

  /** Points the editor to another chassis file (the tests use a temp one). */
  void hullsPath(String path) {
    hullsPath = path;
  }

  private void loadHull(int index) {
    if(grid != null) {
      storeCurrent();
    }
    hullIndex = Math.max(0, Math.min(index, hulls.size() - 1));
    if(hulls.isEmpty()) {
      grid = new LetterGrid();
      letters = new ArrayList<>();
      message = "no hulls in chassis.txt";
      updateTitle();
      return;
    }
    ShipArtFile hull = hulls.get(hullIndex);
    grid = LetterGrid.ofZones(hull);
    hullSize = hull.size();
    letters = new ArrayList<>(hull.letters());
    letterIndex = 0;
    updateTitle();
  }

  /** Moves to another hull, cycling: from the last one it goes back to the first. */
  private void nextHull(int step) {
    if(hulls.isEmpty()) {
      return;
    }
    loadHull((hullIndex + step + hulls.size()) % hulls.size());
    message = "hull " + hulls.get(hullIndex).name();
    redraw();
  }

  private void storeCurrent() {
    if(hulls.isEmpty()) {
      return;
    }
    ShipArtFile hull = hulls.get(hullIndex);
    hulls.set(hullIndex, new ShipArtFile(hull.name(), hull.color(), hull.cells(), hull.blink(), hull.bgColor(), "",
        List.copyOf(letters), grid.zones(), hullSize));
  }

  private ShipArtFile hull() {
    return hulls.isEmpty() ? null : hulls.get(hullIndex);
  }

  private ShipArtFile.ColorLetter currentLetter() {
    if(letters.isEmpty()) {
      return new ShipArtFile.ColorLetter('?', "white", "", false);
    }
    letterIndex = Math.max(0, Math.min(letterIndex, letters.size() - 1));
    return letters.get(letterIndex);
  }

  private void updateTitle() {
    ShipArtFile hull = hull();
    setTitle(hull == null ? "hull editor" : "hulls: " + hull.name());
  }

  @Override
  protected boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      if(nameLetter) {
        nameLetter = false;
        message = "rename cancelled";
        redraw();
        return true;
      }
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
    if(nameLetter && key.getKeyType() == KeyType.Character) {
      char letter = Character.toUpperCase(key.getCharacter());
      ShipArtFile.ColorLetter old = currentLetter();
      if(letter != ' ') {
        letters.set(letterIndex, new ShipArtFile.ColorLetter(letter, old.color(), old.bgColor(), old.blink()));
        message = "letter " + letter;
      }
      nameLetter = false;
      redraw();
      return true;
    }
    if(key.getKeyType() == KeyType.Tab) {
      nextHull(1);
      return true;
    }
    if(key.getKeyType() == KeyType.ReverseTab) {
      nextHull(-1);
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
        character(Character.toLowerCase(key.getCharacter()));
        break;
      default:
        break;
    }
    redraw();
    return true;
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
    if(letters.isEmpty()) {
      message = "add a colour letter with [+] first";
      return;
    }
    char pen = currentLetter().letter();
    grid.set(cursorX, cursorY, pen);
    message = "painted " + pen + " at x=" + cursorX + " y=" + cursorY;
    cursorX++;
    redraw();
  }

  /** The letters are vim keys (move), the digits pick an element; the rest are commands. */
  private void character(char character) {
    if(character >= '1' && character <= '9') {
      pickLetter(character - '1');
      return;
    }
    switch(character) {
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
        cycleLetter(1);
        break;
      case 'p':
        cycleLetter(-1);
        break;
      case '+':
        addLetter();
        break;
      case '-':
        removeLetter();
        break;
      case 'r':
        nameLetter = true;
        message = "type the letter for the combination " + currentLetter().color();
        break;
      case 't':
        openList(false);
        break;
      case 'f':
        openList(true);
        break;
      case 'i':
        styleBlink();
        break;
      case 'z':
        styleSize();
        break;
      case 's':
        save();
        break;
      default:
        break;
    }
  }

  /** Moves the selected element through the panel (n and p). */
  private void cycleLetter(int step) {
    if(letters.isEmpty()) {
      message = "no elements: add one with [+]";
      return;
    }
    letterIndex = (letterIndex + step + letters.size()) % letters.size();
    message = "element: " + elementLabel(letterIndex);
  }

  /** Picks an element by its number (the keys 1 to 9). */
  private void pickLetter(int index) {
    if(index >= letters.size()) {
      message = "no element " + (index + 1);
      return;
    }
    letterIndex = index;
    message = "element: " + elementLabel(letterIndex);
  }

  /** The name of an element: its colours and the letter it paints. */
  private String elementLabel(int index) {
    ShipArtFile.ColorLetter letter = letters.get(index);
    StringBuilder text = new StringBuilder(capitalized(letter.color()));
    if(!letter.bgColor().isEmpty()) {
      text.append('/').append(capitalized(letter.bgColor()));
    }
    if(letter.blink()) {
      text.append("/Blink");
    }
    return text.append(" (").append(letter.letter()).append(')').toString();
  }

  private static String capitalized(String text) {
    return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  /** Removes the selected element, and with it the letters it had painted. */
  private void removeLetter() {
    if(letters.isEmpty()) {
      message = "no elements to remove";
      return;
    }
    char letter = currentLetter().letter();
    int painted = grid.clearLetter(letter);
    letters.remove(letterIndex);
    letterIndex = Math.max(0, Math.min(letterIndex, letters.size() - 1));
    message = "removed " + letter + (painted > 0 ? " and the letters it had on the hull" : "");
  }


  private void styleBlink() {
    ShipArtFile.ColorLetter letter = currentLetter();
    letters.set(letterIndex, new ShipArtFile.ColorLetter(letter.letter(), letter.color(), letter.bgColor(),
        !letter.blink()));
    message = "blink " + (!letter.blink() ? "on" : "off") + " for " + letter.letter();
  }

  private void styleSize() {
    int index = 0;
    for(int i = 0; i < SIZES.length; i++) {
      if(SIZES[i].equalsIgnoreCase(hullSize)) {
        index = i;
        break;
      }
    }
    hullSize = SIZES[(index + 1) % SIZES.length];
    message = "size: " + (hullSize.isEmpty() ? "(not set)" : hullSize);
  }

  private void addLetter() {
    char letter = 'A';
    while(used(letter) && letter < 'Z') {
      letter++;
    }
    letters.add(new ShipArtFile.ColorLetter(letter, COLORS[0], "", false));
    letterIndex = letters.size() - 1;
    message = "letter " + letter + " added (t colour, f background, i blink)";
  }

  private boolean used(char letter) {
    for(ShipArtFile.ColorLetter other : letters) {
      if(other.letter() == letter) {
        return true;
      }
    }
    return false;
  }

  /** The items of the open list: the colours or the backgrounds. */
  private List<String> items() {
    return java.util.Arrays.asList(backgroundList ? BACKGROUNDS : COLORS);
  }

  /** Opens the list of colours (or backgrounds) on the one the element has now. */
  private void openList(boolean backgrounds) {
    backgroundList = backgrounds;
    String current = backgrounds ? currentLetter().bgColor() : currentLetter().color();
    listIndex = Math.max(0, items().indexOf(current));
    listOpen = true;
    message = backgrounds ? "pick the background" : "pick the colour";
  }

  /** Takes the colour (or background) of the list for the element. */
  private void pick() {
    String value = items().get(listIndex);
    ShipArtFile.ColorLetter letter = currentLetter();
    letters.set(letterIndex, new ShipArtFile.ColorLetter(letter.letter(), backgroundList ? letter.color() : value,
        backgroundList ? value : letter.bgColor(), letter.blink()));
    listOpen = false;
    message = (backgroundList ? "background " : "colour ") + (value.isEmpty() ? "(none)" : value);
    redraw();
  }

  private void save() {
    storeCurrent();
    try {
      HullFile.save(hullsPath, hulls);
      message = "saved: " + hullsPath;
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
      graphics.putString(panel + 2, 1, "no hulls in chassis.txt");
    } else {
      paintHull(graphics, hull, panel + 1, 0, half, rows, true);
      paintHull(graphics, hull, panel + half + 2, 0, canvas - half - 1, rows, false);
    }
    paintPanel(graphics, panel, rows);
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows, EditorText.cut(hullsLine(), size.getColumns()));
    graphics.setForegroundColor(message.startsWith("⚠") ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
    graphics.putString(0, rows + 1, EditorText.cut(message, size.getColumns()));
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows + 2, EditorText.cut(keysLine(), size.getColumns()));
  }

  private void paintHull(TextGUIGraphics graphics, ShipArtFile hull, int left, int top, int width, int height,
      boolean withLetters) {
    int originX = left + Math.max(0, (width - hull.width()) / 2);
    int originY = top + Math.max(0, (height - hull.height()) / 2);
    for(int row = 0; row < hull.height() && originY + row < top + height; row++) {
      for(int column = 0; column < hull.width() && originX + column < left + width; column++) {
        int codePoint = hull.at(row, column);
        if(codePoint != ' ' && codePoint != ShipArtFile.CONTINUATION) {
          if(withLetters) {
            EditorText.glyph(graphics, originX + column, originY + row, codePoint, hullBrush(hull), false);
          } else {
            EditorText.glyph(graphics, originX + column, originY + row, codePoint, zoneBrush(hull, column, row), false);
          }
        }
      }
    }
    if(withLetters) {
      for(LetterGrid.Run run : grid.runs()) {
        int x = originX + run.x();
        int y = originY + run.y();
        if(y < top || y >= top + height) {
          continue;
        }
        for(int i = 0; i < run.n() && x + i < left + width; i++) {
          EditorText.glyph(graphics, x + i, y, run.letter(), letterBrush(run.letter()), false);
        }
      }
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

  /** The brush of a colour letter: its colour, background and blink (white when it has no style yet). */
  private EditorText.Brush letterBrush(char letter) {
    for(ShipArtFile.ColorLetter style : letters) {
      if(style.letter() == letter) {
        return EditorText.brush(style.color(), style.bgColor(), style.blink(), blinkOn());
      }
    }
    return EditorText.brush("white", "", false, blinkOn());
  }

  /** The brush of the drawing itself, away from the zones. */
  private EditorText.Brush hullBrush(ShipArtFile hull) {
    return EditorText.brush(hull.color(), hull.bgColor(), hull.blink(), blinkOn());
  }

  /** The brush of a cell of the drawing: the letter of its zone, or the hull's own. */
  private EditorText.Brush zoneBrush(ShipArtFile hull, int x, int y) {
    for(ShipArtFile.Zone zone : hull.zones()) {
      if(x >= zone.x() && x < zone.x() + Math.max(1, zone.w()) && y >= zone.y()
          && y < zone.y() + Math.max(1, zone.h())) {
        return letterBrush(zone.letter());
      }
    }
    return hullBrush(hull);
  }

  private String hullsLine() {
    StringBuilder text = new StringBuilder("[TAB] hull:");
    for(int i = 0; i < hulls.size(); i++) {
      text.append(i == hullIndex ? " [" : " ").append(hulls.get(i).name())
          .append(i == hullIndex ? "]" : "");
    }
    return text.toString();
  }

  /** Draws the open list of colours or backgrounds, every row with its own preview. */
  private void paintList(TextGUIGraphics graphics, TerminalSize size) {
    List<String> items = items();
    int left = Math.max(1, (size.getColumns() - 30) / 2);
    int top = Math.max(0, (size.getRows() - items.size() - 2) / 2);
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    graphics.putString(left, top, backgroundList ? "background:" : "colour:");
    for(int i = 0; i < items.size() && top + 1 + i < size.getRows() - 1; i++) {
      String item = items.get(i);
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
      graphics.putString(left, top + 1 + i, i == listIndex ? ">" : " ");
      graphics.setForegroundColor(org.gts.bst.view.ShipColors.color(backgroundList ? currentLetter().color() : item));
      graphics.setBackgroundColor(backgroundList && !item.isEmpty() ? org.gts.bst.view.ShipColors.color(item)
          : TextColor.ANSI.BLACK);
      graphics.putString(left + 2, top + 1 + i, "■ " + (item.isEmpty() ? "(none)" : item));
    }
    UiPalette.reset(graphics);
    graphics.putString(1, size.getRows() - 1, "[arrows] choose · [ENTER] take · [ESC] cancel");
  }

  /** The vertical panel on the left: the numbered colour elements, ready to pick. */
  private void paintPanel(TextGUIGraphics graphics, int width, int rows) {
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, 0, EditorText.section("Elements", width));
    int row = 1;
    if(letters.isEmpty()) {
      graphics.putString(0, row, EditorText.cut("(no elements: [+] adds one)", width));
      return;
    }
    for(int i = 0; i < letters.size() && row < rows; i++, row++) {
      String text = (i + 1) + ". " + elementLabel(i);
      if(i == letterIndex) {
        graphics.setForegroundColor(TextColor.ANSI.BLACK);
        graphics.setBackgroundColor(TextColor.ANSI.WHITE);
      }
      graphics.putString(0, row, EditorText.cut(String.format("%-" + width + "s", text), width));
      graphics.setForegroundColor(TextColor.ANSI.WHITE);
      graphics.setBackgroundColor(TextColor.ANSI.BLACK);
    }
  }

  private int panelWidth(TerminalSize size) {
    return Math.min(26, Math.max(16, size.getColumns() / 4));
  }

  private String keysLine() {
    return "[arrows] or hjkl move · space paint/erase · [n/p] element · [1-9] pick · [+] add · [-] remove"
        + " · [r] rename · [t] colour · [f] background · [i] blink · [z] size · [s] save"
        + " · [TAB] hull · [ESC] exit";
  }

}
