/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import com.googlecode.lanterna.TextColor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.ShipArtFile;
import org.gts.bst.view.ShipColors;


/**
 * The drawing of the title screen, read once from its resource
 * ({@code /org/gts/bst/lanterna/splash.txt}). Lines starting with {@code ;;} are
 * comments, {@code color=} sets the default colour (cyan when missing) and
 * {@code zone=<colour> x= y= w= h=} paints a rectangle of colour over the cells
 * of the drawing, in drawing cells with (0, 0) at the top left: the last zone
 * that catches a cell wins. The remaining lines are the drawing, one character
 * per cell; spaces are left empty (the black screen shows through) and a glyph
 * the terminal paints two columns wide takes two cells.
 *
 * <p>{@link #shared()} returns {@code null} when the resource is missing, so the
 * title screen can fall back to the banner.</p>
 */
public final class TitleSplash {
  /** The second cell of a glyph the terminal paints two columns wide. */
  public static final int CONTINUATION = ShipArtFile.CONTINUATION;

  private static final String RESOURCE = "/org/gts/bst/lanterna/splash.txt";
  private static final TextColor DEFAULT_COLOR = TextColor.ANSI.CYAN;
  private static final TitleSplash SHARED = load();

  /** A colour zone: the colour and its rectangle in cells of the drawing. */
  private record Zone(TextColor color, int x, int y, int w, int h) {
    boolean contains(int column, int row) {
      return column >= x && column < x + w && row >= y && row < y + h;
    }
  }

  private final List<String> lines;
  private final int width;
  private final int height;
  private final TextColor defaultColor;
  private final List<Zone> zones;

  private TitleSplash(List<String> lines, TextColor defaultColor, List<Zone> zones) {
    this.lines = List.copyOf(lines);
    this.defaultColor = defaultColor;
    this.zones = List.copyOf(zones);
    int widest = 0;
    for(String line : lines) {
      widest = Math.max(widest, EditorText.width(line));
    }
    width = widest;
    height = lines.size();
  }

  /** The splash of the game, read once, or null when the resource is missing. */
  public static TitleSplash shared() {
    return SHARED;
  }

  /** The drawing, top to bottom, as written (the spaces are part of its shape). */
  public List<String> lines() {
    return lines;
  }

  /** The width of the drawing in screen cells (wide glyphs count two). */
  public int width() {
    return width;
  }

  /** The height of the drawing in rows. */
  public int height() {
    return height;
  }

  /**
   * The code point of a cell of the drawing: {@code ' '} for a space (or beyond
   * the line) and {@link #CONTINUATION} for the second cell of a wide glyph.
   */
  public int codePointAt(int x, int y) {
    if(y < 0 || y >= lines.size() || x < 0 || x >= width) {
      return ' ';
    }
    String line = lines.get(y);
    int column = 0;
    for(int i = 0; i < line.length(); ) {
      int codePoint = line.codePointAt(i);
      i += Character.charCount(codePoint);
      int cells = ShipArtFile.isWide(codePoint) ? 2 : 1;
      if(x < column + cells) {
        return x == column ? codePoint : CONTINUATION;
      }
      column += cells;
    }
    return ' ';
  }

  /**
   * The colour of a cell of the drawing, or null when the cell holds no glyph (a
   * space or the continuation of a wide one). The last zone that catches the cell
   * wins over the earlier zones; outside every zone the colour is the default.
   */
  public TextColor colorAt(int x, int y) {
    int codePoint = codePointAt(x, y);
    if(codePoint == ' ' || codePoint == CONTINUATION) {
      return null;
    }
    TextColor color = defaultColor;
    for(Zone zone : zones) {
      if(zone.contains(x, y)) {
        color = zone.color();
      }
    }
    return color;
  }

  /** Parses a splash definition: comments, default colour, zones and drawing. */
  public static TitleSplash parse(Reader reader) throws IOException {
    List<String> lines = new ArrayList<>();
    TextColor defaultColor = DEFAULT_COLOR;
    List<Zone> zones = new ArrayList<>();
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    for(String line = buffered.readLine(); line != null; line = buffered.readLine()) {
      String trimmed = line.strip();
      if(trimmed.startsWith(";;")) {
        continue;
      }
      if(trimmed.startsWith("color=")) {
        defaultColor = ShipColors.color(trimmed.substring("color=".length()).strip());
      } else if(trimmed.startsWith("zone=")) {
        Zone zone = parseZone(trimmed.substring("zone=".length()).strip());
        if(zone != null) {
          zones.add(zone);
        }
      } else {
        // The line is part of the drawing: a space is empty, a dot is ink.
        lines.add(line);
      }
    }
    return new TitleSplash(lines, defaultColor, zones);
  }

  private static Zone parseZone(String definition) {
    String[] tokens = definition.split("\\s+");
    if(tokens.length == 0 || tokens[0].isEmpty()) {
      return null;
    }
    TextColor color = ShipColors.color(tokens[0]);
    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;
    for(int i = 1; i < tokens.length; i++) {
      String[] pair = tokens[i].split("=", 2);
      if(pair.length != 2) {
        continue;
      }
      try {
        switch(pair[0]) {
          case "x":
            x = Integer.parseInt(pair[1]);
            break;
          case "y":
            y = Integer.parseInt(pair[1]);
            break;
          case "w":
            w = Integer.parseInt(pair[1]);
            break;
          case "h":
            h = Integer.parseInt(pair[1]);
            break;
          default:
            break;
        }
      } catch(NumberFormatException e) {
        // A strange number is ignored, like in the ship art.
      }
    }
    return new Zone(color, x, y, w, h);
  }

  private static TitleSplash load() {
    try(InputStream stream = TitleSplash.class.getResourceAsStream(RESOURCE)) {
      if(stream == null) {
        return null;
      }
      return parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch(IOException e) {
      // The resource is missing or unreadable: the title falls back to the banner.
      return null;
    }
  }
}
