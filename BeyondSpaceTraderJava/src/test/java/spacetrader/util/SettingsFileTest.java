/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


class SettingsFileTest {
  @Test
  void storesAndReloadsSettings(@TempDir Path dir) {
    File file = dir.resolve("settings.properties").toFile();

    SettingsFile settings = new SettingsFile(file);
    settings.setValue("X", "42");
    settings.close();

    SettingsFile reloaded = new SettingsFile(file);
    assertEquals("42", reloaded.getValue("X"));
    assertNull(reloaded.getValue("Missing"));
    reloaded.close();
  }
}
