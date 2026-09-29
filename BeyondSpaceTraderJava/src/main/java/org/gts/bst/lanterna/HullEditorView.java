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
 * The hull editor: on the left the drawing with its colour letters (free letters,
 * each with its colour, background and blink; space erases a cell), on the right
 * the same hull painted with those colours. The drawing is never touched.
 */
public final class HullEditorView extends ArtEditorWindow {
  private static final String[] COLORS = {"white", "cyan", "red", "yellow", "green", "magenta", "blue", "orange",
      "purple", "pink", "lightgrey", "darkgrey"};
  private static final String[] BACKGROUNDS = {"", "black", "blue", "red", "green", "magenta", "yellow", "cyan"};
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
  private String hullSize = "";
  private String message = "";
  /** The chassis file the editor saves to (a field so the tests can point elsewhere). */
  private String hullsPath = ShipArtFile.resolve("chassis.txt").toString();

  public HullEditorView(List<ShipArtFile> hulls) {
    super("editor de fuselajes");
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
      message = "no hay fuselajes en chassis.txt";
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
    setTitle(hull == null ? "editor de fuselajes" : "fuselajes: " + hull.name());
  }

  @Override
  protected boolean handleKey(KeyStroke key) {
    if(key.getKeyType() == KeyType.Escape) {
      close();
      return true;
    }
    if(nameLetter && key.getKeyType() == KeyType.Character) {
      char letter = Character.toUpperCase(key.getCharacter());
      ShipArtFile.ColorLetter old = currentLetter();
      if(letter != ' ') {
        letters.set(letterIndex, new ShipArtFile.ColorLetter(letter, old.color(), old.bgColor(), old.blink()));
        message = "letra " + letter;
      }
      nameLetter = false;
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
      case Enter:
        grid.set(cursorX, cursorY, currentLetter().letter());
        message = "pintado " + currentLetter().letter() + " en x=" + cursorX + " y=" + cursorY;
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

  private void character(char character) {
    if(character == ' ') {
      grid.clear(cursorX, cursorY);
      message = "borrado en x=" + cursorX + " y=" + cursorY;
    } else if(character == ',') {
      loadHull(hullIndex - 1);
    } else if(character == '.') {
      loadHull(hullIndex + 1);
    } else if(character == 'n') {
      letterIndex = letters.isEmpty() ? 0 : (letterIndex + letters.size() - 1) % letters.size();
      message = "letra " + currentLetter().letter();
    } else if(character == 'p') {
      letterIndex = letters.isEmpty() ? 0 : (letterIndex + 1) % letters.size();
      message = "letra " + currentLetter().letter();
    } else if(character == '+') {
      addLetter();
    } else if(character == 'e') {
      nameLetter = true;
      message = "teclea la letra para la combinacion " + currentLetter().color();
    } else if(character == 'z') {
      int index = 0;
      for(int i = 0; i < SIZES.length; i++) {
        if(SIZES[i].equalsIgnoreCase(hullSize)) {
          index = i;
          break;
        }
      }
      hullSize = SIZES[(index + 1) % SIZES.length];
      message = "tamano: " + (hullSize.isEmpty() ? "(sin declarar)" : hullSize);
    } else if(character == 'c') {
      styleColor();
    } else if(character == 'b') {
      styleBackground();
    } else if(character == 'k') {
      ShipArtFile.ColorLetter letter = currentLetter();
      letters.set(letterIndex, new ShipArtFile.ColorLetter(letter.letter(), letter.color(), letter.bgColor(),
          !letter.blink()));
      message = "parpadeo " + (!letter.blink() ? "si" : "no") + " para " + letter.letter();
    } else if(character == 's') {
      save();
    }
  }

  private void addLetter() {
    char letter = 'A';
    while(used(letter) && letter < 'Z') {
      letter++;
    }
    letters.add(new ShipArtFile.ColorLetter(letter, COLORS[0], "", false));
    letterIndex = letters.size() - 1;
    message = "letra " + letter + " anyadida (c color, b fondo, k parpadeo)";
  }

  private boolean used(char letter) {
    for(ShipArtFile.ColorLetter other : letters) {
      if(other.letter() == letter) {
        return true;
      }
    }
    return false;
  }

  private void styleColor() {
    ShipArtFile.ColorLetter letter = currentLetter();
    int index = indexOf(COLORS, letter.color());
    letters.set(letterIndex, new ShipArtFile.ColorLetter(letter.letter(), COLORS[(index + 1) % COLORS.length],
        letter.bgColor(), letter.blink()));
    message = "color " + COLORS[(index + 1) % COLORS.length] + " para " + letter.letter();
  }

  private void styleBackground() {
    ShipArtFile.ColorLetter letter = currentLetter();
    int index = indexOf(BACKGROUNDS, letter.bgColor());
    letters.set(letterIndex, new ShipArtFile.ColorLetter(letter.letter(), letter.color(),
        BACKGROUNDS[(index + 1) % BACKGROUNDS.length], letter.blink()));
    message = "fondo " + (BACKGROUNDS[(index + 1) % BACKGROUNDS.length].isEmpty() ? "(ninguno)"
        : BACKGROUNDS[(index + 1) % BACKGROUNDS.length]) + " para " + letter.letter();
  }

  private static int indexOf(String[] values, String value) {
    for(int i = 0; i < values.length; i++) {
      if(values[i].equals(value)) {
        return i;
      }
    }
    return -1;
  }

  private void save() {
    storeCurrent();
    try {
      HullFile.save(hullsPath, hulls);
      message = "guardado: " + hullsPath;
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
    int rows = size.getRows() - STRIP_ROWS;
    int half = (size.getColumns() - 1) / 2;
    for(int row = 0; row < rows; row++) {
      graphics.setCharacter(half, row, '│');
    }
    ShipArtFile hull = hull();
    if(hull == null) {
      graphics.putString(1, 1, "no hay fuselajes en chassis.txt");
    } else {
      paintHull(graphics, hull, 0, 0, half, rows, true);
      paintHull(graphics, hull, half + 1, 0, size.getColumns() - half - 1, rows, false);
    }
    graphics.setForegroundColor(TextColor.ANSI.WHITE);
    graphics.putString(0, rows, EditorText.cut(hullsLine(), size.getColumns()));
    graphics.putString(0, rows + 1, EditorText.cut(lettersLine(), size.getColumns()));
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
            EditorText.glyph(graphics, originX + column, originY + row, codePoint, color(hull.color()), false);
          } else {
            EditorText.glyph(graphics, originX + column, originY + row, codePoint,
                color(zoneColor(hull, column, row)), false);
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
          EditorText.glyph(graphics, x + i, y, run.letter(), color(letterColor(run.letter())), false);
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

  /** The colour of a colour letter, with its background and blink taken into account. */
  private String letterColor(char letter) {
    for(ShipArtFile.ColorLetter style : letters) {
      if(style.letter() == letter) {
        return style.blink() && !blinkOn() ? (style.bgColor().isEmpty() ? "black" : style.bgColor()) : style.color();
      }
    }
    return "white";
  }

  private String zoneColor(ShipArtFile hull, int x, int y) {
    for(ShipArtFile.Zone zone : hull.zones()) {
      if(x >= zone.x() && x < zone.x() + Math.max(1, zone.w()) && y >= zone.y()
          && y < zone.y() + Math.max(1, zone.h())) {
        return letterColor(zone.letter());
      }
    }
    return hull.color();
  }

  private String hullsLine() {
    StringBuilder text = new StringBuilder("[,/.] chasis:");
    for(int i = 0; i < hulls.size(); i++) {
      text.append(i == hullIndex ? " [" : " ").append(hulls.get(i).name())
          .append(i == hullIndex ? "]" : "");
    }
    return text.toString();
  }

  private String lettersLine() {
    StringBuilder text = new StringBuilder("letras:");
    for(int i = 0; i < letters.size(); i++) {
      ShipArtFile.ColorLetter letter = letters.get(i);
      text.append(i == letterIndex ? " [" : " ").append(letter.letter()).append('=').append(letter.color());
      if(!letter.bgColor().isEmpty()) {
        text.append('/').append(letter.bgColor());
      }
      if(letter.blink()) {
        text.append("/blink");
      }
      text.append(i == letterIndex ? "]" : "");
    }
    if(letters.isEmpty()) {
      text.append(" (con + anyades una)");
    }
    return text.toString();
  }

  private String keysLine() {
    return "[ENTER] pintar · espacio borrar · [n/p] letra · [+] anyadir · [e] renombrar · [c] color · [b] fondo"
        + " · [k] parpadeo · [z] tamano · [s] guardar · [,/.] chasis · [ESC] salir"
        + (nameLetter ? "   ||   teclea la letra deseada" : message.isEmpty() ? "" : "   ||   " + message);
  }

  private static TextColor color(String name) {
    return org.gts.bst.view.ShipColors.color(name);
  }
}
