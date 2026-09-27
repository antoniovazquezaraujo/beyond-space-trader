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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;


class LanternaAppLanguageTest {
  @Test
  void theLanguageArgumentIsOptional() {
    assertNull(LanternaApp.languageFrom(new String[0]), "no argument means the system locale");
    assertNull(LanternaApp.languageFrom(new String[] {"--other"}));
    assertNull(LanternaApp.languageFrom(new String[] {"--lang"}));
    assertNull(LanternaApp.languageFrom(new String[] {"--lang="}));
  }

  @Test
  void theLanguageArgumentTakesALanguageAndAnOptionalCountry() {
    assertEquals(new Locale("es"), LanternaApp.languageFrom(new String[] {"--lang", "es"}));
    assertEquals(new Locale("es"), LanternaApp.languageFrom(new String[] {"--lang=es"}));
    assertEquals(new Locale("es", "ES"), LanternaApp.languageFrom(new String[] {"--lang", "es_ES"}));
    assertEquals(new Locale("es", "ES"), LanternaApp.languageFrom(new String[] {"--lang=es-ES"}));
  }

  @Test
  void textsFallBackToEnglishWhenThereIsNoTranslation() {
    ResourceBundle english = ResourceBundle.getBundle("spacetrader.Strings", Locale.ENGLISH);
    ResourceBundle missing = ResourceBundle.getBundle("spacetrader.Strings", new Locale("xx"));
    assertEquals(english.getString("MainChartGalactic"), missing.getString("MainChartGalactic"));
  }
}
