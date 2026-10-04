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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.gts.bst.events.SpecialEventType;
import org.junit.jupiter.api.Test;


/**
 * The offer dialog shows {@link SpecialEvent#Title()} and {@link SpecialEvent#String()}
 * exactly as they come from the bundle: nothing resolves their placeholders. Every one
 * of the 46 events therefore needs both texts, in every language, non-empty and clean.
 */
class SpecialEventTextsTest {
  private static final int SPECIAL_EVENT_COUNT = 46;

  @Test
  void everySpecialEventHasANonEmptyTitleAndStoryInBothBundles() throws IOException {
    Map<String, Properties> bundles = new LinkedHashMap<>();
    bundles.put("en", properties("Strings.properties"));
    bundles.put("es", properties("Strings_es.properties"));

    int seen = 0;
    for(SpecialEventType type : SpecialEventType.values()) {
      if(type == SpecialEventType.NA) {
        continue;
      }
      seen++;
      int index = type.CastToInt();
      assertTrue(index >= 0 && index < SPECIAL_EVENT_COUNT,
          type.name() + " must map to 0..45 but maps to " + index);
      for(Map.Entry<String, Properties> bundle : bundles.entrySet()) {
        String language = bundle.getKey();
        assertUsableText(bundle.getValue(), "SpecialEventTitles." + index, language);
        assertUsableText(bundle.getValue(), "SpecialEventStrings." + index, language);
      }
    }
    assertEquals(SPECIAL_EVENT_COUNT, seen, "the loop must cover the 46 events of the enum");
  }

  @Test
  void theEventTableCoversTheEnumInOrderAndTakesItsTextsFromTheBundle() throws IOException {
    Properties english = properties("Strings.properties");
    assertEquals(SPECIAL_EVENT_COUNT, Consts.SpecialEvents.size(), "one event per type");
    assertEquals(SPECIAL_EVENT_COUNT, Strings.SpecialEventTitles.size(), "one title per type");
    assertEquals(SPECIAL_EVENT_COUNT, Strings.SpecialEventStrings.size(), "one story per type");

    for(int index = 0; index < SPECIAL_EVENT_COUNT; index++) {
      SpecialEvent event = Consts.SpecialEvents.get(index);
      assertEquals(index, event.Type().CastToInt(),
          "the event table follows the enum order at " + index);
      assertEquals(english.getProperty("SpecialEventTitles." + index), event.Title(),
          "title " + index + " comes from the English bundle");
      assertEquals(english.getProperty("SpecialEventStrings." + index), event.String(),
          "story " + index + " comes from the English bundle");
      assertEquals(event.Title(), Strings.SpecialEventTitles.get(index),
          "the runtime title list aligns with the event table at " + index);
      assertEquals(event.String(), Strings.SpecialEventStrings.get(index),
          "the runtime story list aligns with the event table at " + index);
    }
  }

  private static void assertUsableText(Properties bundle, String key, String language) {
    String text = bundle.getProperty(key);
    assertNotNull(text, language + " bundle is missing " + key);
    assertTrue(!text.isBlank(), language + " " + key + " must not be empty");
    // A ^1 would reach the screen unformatted: the offer does not run StringVars.
    assertTrue(!text.contains("^"), language + " " + key + " keeps a placeholder: " + text);
  }

  private static Properties properties(String resource) throws IOException {
    InputStream stream = SpecialEventTextsTest.class.getResourceAsStream("/spacetrader/" + resource);
    assertNotNull(stream, resource);
    Properties properties = new Properties();
    try(Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }
}
