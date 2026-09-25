/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spacetrader.util.Util;


class UtilTest {
  @Test
  void stringsJoinPutsTheSeparatorBetweenElements() {
    assertEquals("a, b, c", Util.StringsJoin(", ", new String[]{"a", "b", "c"}));
    assertEquals("a", Util.StringsJoin(", ", new String[]{"a"}));
    assertEquals("", Util.StringsJoin(", ", new String[0]));
  }

  @Test
  void getFilesFiltersBySuffix(@TempDir Path dir) throws IOException {
    Files.createFile(dir.resolve("one.sst"));
    Files.createFile(dir.resolve("two.txt"));

    String[] files = Util.GetFiles(dir.toString(), ".sst");

    assertEquals(1, files.length);
    assertTrue(files[0].endsWith("one.sst"));
  }

  @Test
  void getFilesOfAMissingDirectoryIsEmpty() {
    assertEquals(0, Util.GetFiles("/no/such/directory", ".sst").length);
  }
}
