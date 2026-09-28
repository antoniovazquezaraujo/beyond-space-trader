/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import com.googlecode.lanterna.TextCharacter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


/**
 * One definition of a ship part (a chassis or a piece) read from a text file:
 * a name, a colour name and the drawing, copied literally line by line, blank
 * rows included. A space is empty and everything else (dots and unicode
 * included) is part of the art. Rows are read as code points, so glyphs outside
 * the basic plane (a domino tile, an emoji) work; a glyph that the terminal
 * paints two columns wide takes two cells (the second one is reserved).
 */
public record ShipArtFile(String name, String color, List<int[]> cells, boolean blink, String bgColor) {
  /** The second cell of a glyph the terminal paints two columns wide. */
  public static final int CONTINUATION = -1;

  /** Loads the parts of a file, looking for it in the usual places. */
  public static List<ShipArtFile> load(String fileName) throws IOException {
    File file = resolve(fileName).toFile();
    if(file == null) {
      throw new IOException("no encuentro " + fileName + " (¿ejecutas desde la raíz del repositorio?)");
    }
    try(BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  /**
   * The path of a definition file: the first candidate that exists, or the first
   * one whose folder exists (so saving also lands next to the repository).
   */
  public static java.nio.file.Path resolve(String fileName) {
    String[] candidates = {"ships/" + fileName, "../ships/" + fileName, "BeyondSpaceTraderJava/ships/" + fileName};
    for(String candidate : candidates) {
      if(new File(candidate).isFile()) {
        return java.nio.file.Path.of(candidate);
      }
    }
    for(String candidate : candidates) {
      File folder = new File(candidate).getParentFile();
      if(folder != null && folder.isDirectory()) {
        return java.nio.file.Path.of(candidate);
      }
    }
    return java.nio.file.Path.of(candidates[0]);
  }

  /** A part as read, before the glyph widths are applied. */
  private record RawPart(String name, String color, List<String> lines, boolean blink, String bgColor) {
  }

  /**
   * Parses the parts of a reader: sections, optional colours and art lines.
   * The keys wide= and narrow= list code points that the terminal paints wider
   * or narrower than the tables say (hex, separated by spaces or commas).
   */
  public static List<ShipArtFile> parse(Reader reader) throws IOException {
    List<RawPart> raw = new ArrayList<>();
    java.util.Set<Integer> wide = new java.util.HashSet<>();
    java.util.Set<Integer> narrow = new java.util.HashSet<>();
    String name = null;
    String color = "white";
    boolean blink = false;
    String bgColor = "";
    List<String> lines = new ArrayList<>();
    BufferedReader buffered = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    for(String line = buffered.readLine(); line != null; line = buffered.readLine()) {
      String trimmed = line.strip();
      if(trimmed.startsWith(";;")) {
        continue;
      }
      if(trimmed.isEmpty()) {
        if(name != null) {
          lines.add(line); // una fila vacía también es parte del dibujo
        }
        continue;
      }
      if(trimmed.startsWith("wide=")) {
        addCodePoints(wide, trimmed.substring("wide=".length()));
        continue;
      } else if(trimmed.startsWith("narrow=")) {
        addCodePoints(narrow, trimmed.substring("narrow=".length()));
        continue;
      }
      if(trimmed.startsWith("[") && trimmed.endsWith("]")) {
        if(name != null) {
          raw.add(new RawPart(name, color, List.copyOf(lines), blink, bgColor));
        }
        name = trimmed.substring(1, trimmed.length() - 1).strip();
        color = "white";
        blink = false;
        bgColor = "";
        lines.clear();
      } else if(name != null && trimmed.startsWith("color=")) {
        color = trimmed.substring("color=".length()).strip();
      } else if(name != null && trimmed.startsWith("bgcolor=")) {
        bgColor = trimmed.substring("bgcolor=".length()).strip();
      } else if(name != null && trimmed.startsWith("blink=")) {
        String value = trimmed.substring("blink=".length()).strip().toLowerCase();
        blink = value.equals("true");
      } else if(name != null) {
        // The line is kept as written: a space is empty, a dot is ink.
        lines.add(line);
      }
    }
    if(name != null) {
      raw.add(new RawPart(name, color, List.copyOf(lines), blink, bgColor));
    }
    List<ShipArtFile> parts = new ArrayList<>();
    for(RawPart part : raw) {
      parts.add(new ShipArtFile(part.name(), part.color(), toCells(part.lines(), wide, narrow), part.blink(),
          part.bgColor()));
    }
    return parts;
  }

  /** Reads a list of code points: "21A0 21C9" or "U+21A0, U+21C9". */
  private static void addCodePoints(java.util.Set<Integer> codePoints, String text) {
    for(String token : text.strip().split("[\\s,]+")) {
      String hex = token.startsWith("U+") || token.startsWith("u+") ? token.substring(2) : token;
      try {
        codePoints.add(Integer.parseInt(hex, 16));
      } catch(NumberFormatException e) {
        // un token raro se ignora
      }
    }
  }

  /** Turns the text of the drawing into cells: one per column, wide glyphs included. */
  private static List<int[]> toCells(List<String> lines, java.util.Set<Integer> wide, java.util.Set<Integer> narrow) {
    List<int[]> cells = new ArrayList<>();
    for(String line : lines) {
      List<Integer> row = new ArrayList<>();
      for(int i = 0; i < line.length(); ) {
        int codePoint = line.codePointAt(i);
        i += Character.charCount(codePoint);
        row.add(codePoint);
        if((isWide(codePoint) || wide.contains(codePoint)) && !narrow.contains(codePoint)) {
          row.add(CONTINUATION); // la segunda columna que ocupa el glifo
        }
      }
      cells.add(row.stream().mapToInt(Integer::intValue).toArray());
    }
    return List.copyOf(cells);
  }

  /** True when the terminal paints the glyph two columns wide (emoji, CJK, dominoes). */
  public static boolean isWide(int codePoint) {
    if(codePoint < 0x1100) {
      return false; // below Hangul Jamo everything is narrow: fast path
    }
    return TextCharacter.fromString(new String(Character.toChars(codePoint)))[0].isDoubleWidth();
  }

  public int width() {
    int width = 0;
    for(int[] row : cells) {
      width = Math.max(width, row.length);
    }
    return width;
  }

  public int height() {
    return cells.size();
  }

  /** The code point of a cell, or CONTINUATION when a wide glyph covers it. */
  public int at(int row, int column) {
    int[] cellsOfRow = cells.get(row);
    return column < cellsOfRow.length ? cellsOfRow[column] : ' ';
  }
}
