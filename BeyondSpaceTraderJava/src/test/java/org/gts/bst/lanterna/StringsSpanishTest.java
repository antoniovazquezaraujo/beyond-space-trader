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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  /**
   * The six alerts the rival says under its ship (see ADR 0003) keep the texts the
   * author chose, in both languages: a test running in en_US would not notice a
   * changed Spanish text otherwise.
   */
  @Test
  void theSixSpokenAlertsKeepTheirChosenTextsInBothLanguages() throws IOException {
    Properties english = properties("Strings.properties");
    Properties spanish = properties("Strings_es.properties");
    String[][] texts = {
      {"EncounterPoliceFine",
        "You are carrying prohibited goods. They are confiscated. You will also have to pay a fine of ^1 credits.",
        "Lleva usted mercancías prohibidas. Quedan confiscadas. Además, tendrá que pagar una multa de ^1 créditos."},
      {"EncounterPoliceBribeCant",
        "Who do you think you are talking to? We do not accept bribes!",
        "¿Con quién cree que está hablando? ¡No aceptamos sobornos!"},
      {"EncounterMarieCelesteNoBribe",
        "We would love to take your money, but Space Command already knows you've got illegal goods onboard.",
        "Nos encantaría quedarnos con tu dinero, pero el Mando Espacial ya sabe que llevas mercancía ilegal a bordo."},
      {"EncounterSurrenderRefused",
        "Surrender? Hah! We want your HEAD!",
        "¿Rendirnos? ¡Ja! ¡Lo que queremos es tu cabeza!"},
      {"EncounterArrested",
        "You are under arrest. We will take you to the space station where you will appear before a tribunal.",
        "Queda detenido. Le llevaremos a la estación espacial donde comparecerá ante un tribunal."},
      {"EncounterPostMarie",
        "Your illegal cargo is confiscated. Since you cooperated, you will receive no penalty.",
        "Su carga ilegal queda confiscada. Debido a que ha cooperado no recibirá ninguna sanción."},
    };
    for(String[] text : texts) {
      String key = "Alert." + text[0] + ".message";
      assertEquals(text[1], english.getProperty(key), key + " (en)");
      assertEquals(text[2], spanish.getProperty(key), key + " (es)");
    }
    assertTrue(english.getProperty("Alert.EncounterPoliceFine.message").contains("^1"),
        "the English fine keeps the amount placeholder");
    assertTrue(spanish.getProperty("Alert.EncounterPoliceFine.message").contains("^1"),
        "the Spanish fine keeps the amount placeholder");
  }

  /**
   * The composer menu, its keys line, its Quit row and the editor titles are
   * localised: a test running in en_US would not notice a broken Spanish text
   * otherwise.
   */
  @Test
  void theComposerTextsAreLocalisedInBothBundles() throws IOException {
    Properties english = properties("Strings.properties");
    Properties spanish = properties("Strings_es.properties");
    String[][] texts = {
      {"ComposerShips", "Ships: design the ships (keys and pieces)", "Naves: diseñar las naves (teclas y piezas)"},
      {"ComposerHulls", "Hulls: paint the hulls' colours", "Cascos: pintar los colores de los cascos"},
      {"ComposerQuit", "Quit", "Salir"},
      {"ComposerKeys", "[↑↓/N/P] move · [ENTER] choose · [ESC] back",
        "[↑↓/N/P] mover · [INTRO] elegir · [ESC] volver"},
      {"EditorShipTitle", "ship editor", "editor de naves"},
      {"EditorHullTitle", "hull editor", "editor de cascos"},
      {"EditorHullNamed", "hulls: ^1", "cascos: ^1"},
    };
    for(String[] text : texts) {
      assertEquals(text[1], english.getProperty(text[0]), text[0] + " (en)");
      assertEquals(text[2], spanish.getProperty(text[0]), text[0] + " (es)");
    }
  }

  private static Set<String> keys(String resource) throws IOException {
    return properties(resource).stringPropertyNames();
  }

  private static Properties properties(String resource) throws IOException {
    InputStream stream = StringsSpanishTest.class.getResourceAsStream("/spacetrader/" + resource);
    assertNotNull(stream, resource);
    Properties properties = new Properties();
    try(Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }
}
