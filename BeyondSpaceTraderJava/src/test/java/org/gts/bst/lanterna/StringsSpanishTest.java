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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;


class StringsSpanishTest {
  @Test
  void theSpanishBundleHasEveryKeyOfTheEnglishOne() throws IOException {
    assertEquals(new TreeSet<>(keys("Strings.properties")), new TreeSet<>(keys("Strings_es.properties")),
        "Strings_es.properties must keep the keys of Strings.properties");
  }

  @Test
  void theSpanishBundleIsPickedUpWithTheSpanishLocale() {
    assertEquals("Mapa galáctico",
        ResourceBundle.getBundle("spacetrader.Strings", new Locale("es")).getString("MainChartGalactic"));
    assertEquals("Menú", ResourceBundle.getBundle("spacetrader.Strings", new Locale("es")).getString("MenuTitle"));
  }

  private static Set<String> keys(String resource) throws IOException {
    InputStream stream = StringsSpanishTest.class.getResourceAsStream("/spacetrader/" + resource);
    assertNotNull(stream, resource);
    Properties properties = new Properties();
    try(Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties.stringPropertyNames();
  }
}
