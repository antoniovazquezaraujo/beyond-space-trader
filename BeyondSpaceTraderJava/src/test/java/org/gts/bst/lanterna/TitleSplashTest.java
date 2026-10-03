/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.lanterna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.TextColor;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;


class TitleSplashTest {
  @Test
  void commentsAndCommandsAreNotPartOfTheDrawing() throws Exception {
    TitleSplash splash = TitleSplash.parse(new StringReader(
        ";; the title drawing\n"
        + ";; another comment\n"
        + "color=green\n"
        + "zone=green x=0 y=0 w=3 h=1\n"
        + "abc\n"));

    assertEquals(List.of("abc"), splash.lines());
    assertEquals(3, splash.width());
    assertEquals(1, splash.height());
  }

  @Test
  void theDefaultColourIsCyanAndColorChangesIt() throws Exception {
    assertEquals(TextColor.ANSI.CYAN, TitleSplash.parse(new StringReader("x\n")).colorAt(0, 0));
    assertEquals(TextColor.ANSI.GREEN, TitleSplash.parse(new StringReader("color=green\nx\n")).colorAt(0, 0));
  }

  @Test
  void theLastZoneThatCatchesACellWins() throws Exception {
    TitleSplash splash = TitleSplash.parse(new StringReader(
        "color=cyan\n"
        + "zone=green x=0 y=0 w=3 h=1\n"
        + "zone=yellow x=0 y=0 w=1 h=1\n"
        + "abcd\n"));

    assertEquals(TextColor.ANSI.YELLOW, splash.colorAt(0, 0), "the last zone over the cell wins");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(1, 0));
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(2, 0));
    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(3, 0), "outside the zones the default colour shows");
  }

  @Test
  void spacesAndContinuationCellsHaveNoColour() throws Exception {
    // Cells: space, a, space, 一 (two cells). The wide glyph ends the drawing.
    TitleSplash splash = TitleSplash.parse(new StringReader("color=cyan\nzone=green x=0 y=0 w=5 h=1\n a 一\n"));

    assertNull(splash.colorAt(0, 0), "a space is skipped");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(1, 0));
    assertNull(splash.colorAt(2, 0), "another space is skipped");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(3, 0), "the wide glyph takes the zone colour");
    assertNull(splash.colorAt(4, 0), "the continuation of a wide glyph is skipped");
    assertEquals(5, splash.width(), "the wide glyph counts two cells");
    assertNull(splash.colorAt(5, 0), "beyond the drawing there is nothing");
  }

  @Test
  void sharedLoadsTheRealResource() {
    TitleSplash splash = TitleSplash.shared();

    assertNotNull(splash, "the splash resource is packaged");
    assertTrue(splash.width() > 0 && splash.height() > 0, "the splash has a drawing");
    boolean planet = false;
    boolean moon = false;
    for(int y = 0; y < splash.height(); y++) {
      for(int x = 0; x < splash.width(); x++) {
        TextColor color = splash.colorAt(x, y);
        planet |= TextColor.ANSI.GREEN.equals(color);
        moon |= TextColor.ANSI.YELLOW.equals(color);
      }
    }
    assertTrue(planet, "the resource paints the planet in green");
    assertTrue(moon, "the resource paints the moon in yellow");
  }

  @Test
  void aZoneCoversItsLastCellAndStopsAtTheOnePastIt() throws Exception {
    TitleSplash splash = TitleSplash.parse(new StringReader(
        "color=cyan\n"
        + "zone=green x=1 y=1 w=2 h=2\n"
        + "aaaa\n"
        + "bbbb\n"
        + "cccc\n"
        + "dddd\n"));

    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(0, 1), "the cell before the zone");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(1, 1), "the first cell inside");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(2, 1), "the last column of the zone is inside");
    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(3, 1), "the column past the zone is outside");
    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(1, 0), "the row before the zone");
    assertEquals(TextColor.ANSI.GREEN, splash.colorAt(1, 2), "the last row of the zone is inside");
    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(1, 3), "the row past the zone is outside");
  }

  @Test
  void theDefaultColourIsCyanWhenTheColorLineIsMissing() throws Exception {
    TitleSplash splash = TitleSplash.parse(new StringReader("ab\ncd\n"));

    for(int y = 0; y < splash.height(); y++) {
      for(int x = 0; x < splash.width(); x++) {
        assertEquals(TextColor.ANSI.CYAN, splash.colorAt(x, y), "cell " + x + "," + y);
      }
    }
  }

  @Test
  void aWideGlyphReservesItsSecondCell() throws Exception {
    TitleSplash splash = TitleSplash.parse(new StringReader("color=cyan\n一x\n"));

    assertEquals(3, splash.width(), "the wide glyph counts two cells");
    assertEquals('一', splash.codePointAt(0, 0));
    assertEquals(TitleSplash.CONTINUATION, splash.codePointAt(1, 0), "the second cell is the continuation");
    assertEquals('x', splash.codePointAt(2, 0));
    assertNull(splash.colorAt(1, 0), "the continuation cell is skipped");
    assertEquals(TextColor.ANSI.CYAN, splash.colorAt(2, 0));
  }

  @Test
  void theShadedJarCarriesTheSplash() throws Exception {
    Path jar = shadedJar();
    Assumptions.assumeTrue(jar != null, "the shaded jar is built after the tests (mvn package)");

    try(JarFile jarFile = new JarFile(jar.toFile())) {
      JarEntry entry = jarFile.getJarEntry("org/gts/bst/lanterna/splash.txt");
      assertNotNull(entry, "the jar carries the splash resource");
      String text = new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
      TitleSplash packaged = TitleSplash.parse(new StringReader(text));

      assertTrue(packaged.width() > 0 && packaged.height() > 0, "the packaged splash has a drawing");
      boolean planet = false;
      boolean moon = false;
      for(int y = 0; y < packaged.height(); y++) {
        for(int x = 0; x < packaged.width(); x++) {
          TextColor color = packaged.colorAt(x, y);
          planet |= TextColor.ANSI.GREEN.equals(color);
          moon |= TextColor.ANSI.YELLOW.equals(color);
        }
      }
      assertTrue(planet, "the packaged splash paints the planet in green");
      assertTrue(moon, "the packaged splash paints the moon in yellow");
    }
  }

  /** The shaded jar of the module, or null when it has not been packaged yet. */
  private static Path shadedJar() throws IOException {
    for(String folder : List.of("target", "BeyondSpaceTraderJava/target")) {
      Path target = Path.of(folder);
      if(!Files.isDirectory(target)) {
        continue;
      }
      try(Stream<Path> files = Files.list(target)) {
        Path jar = files
            .filter(path -> path.getFileName().toString().endsWith(".jar"))
            .filter(path -> !path.getFileName().toString().startsWith("original-"))
            .findFirst().orElse(null);
        if(jar != null) {
          return jar;
        }
      }
    }
    return null;
  }
}
