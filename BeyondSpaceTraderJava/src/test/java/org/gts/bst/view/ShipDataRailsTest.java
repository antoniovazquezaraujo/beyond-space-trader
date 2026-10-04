/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package org.gts.bst.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.gts.bst.ship.ShipSize;
import org.gts.bst.ship.ShipType;
import org.junit.jupiter.api.Test;


/**
 * The data rails of the shipped art: the CI guard over the real
 * {@code ships/ships.txt} and {@code ships/chassis.txt}. The game and the ship
 * editor may only load designs whose {@code type=} is exactly one game type,
 * once per type, with a {@code chasis=} that exists and has the size of that
 * type, and with {@code group=} letters that are site letters. A broken file
 * fails here with the offending line, before it reaches the editor.
 */
class ShipDataRailsTest {
  /** The site letters of the game (docs/developer/ships.md): C, M, D, B, R, A, E, G and P. */
  private static final String SITE_LETTERS = "AEGCMDBRP";

  @Test
  void everyTypeAppearsExactlyOnceWithItsExactName() throws IOException {
    // Arrange: the designs of the file the game loads
    List<ShipDesign> designs = realDesigns();

    // Act: count every type, as the editor does when it fills its slots
    Map<ShipType, Integer> counts = new LinkedHashMap<>();
    for(ShipType type : ShipType.values()) {
      counts.put(type, 0);
    }
    List<String> unknown = new ArrayList<>();
    List<String> inexact = new ArrayList<>();
    for(ShipDesign design : designs) {
      ShipType type = ShipSites.typeOf(design.type());
      if(type == null) {
        unknown.add("[" + design.name() + "] type=" + design.type());
      } else {
        if(!type.name().equals(design.type())) {
          inexact.add("[" + design.name() + "] type=" + design.type() + " (¿" + type.name() + "?)");
        }
        counts.merge(type, 1, Integer::sum);
      }
    }

    // Assert
    assertTrue(unknown.isEmpty(), "ships.txt tiene diseños con un type= que no es de ShipType: " + unknown);
    assertTrue(inexact.isEmpty(), "el type= debe ser exactamente el nombre del enum: " + inexact);
    List<String> wrong = new ArrayList<>();
    for(Map.Entry<ShipType, Integer> count : counts.entrySet()) {
      if(count.getValue() != 1) {
        wrong.add(count.getKey() + "=" + count.getValue());
      }
    }
    assertTrue(wrong.isEmpty(), "cada ShipType debe aparecer exactamente una vez en ships.txt: " + wrong);
    assertEquals(ShipType.values().length, designs.size(),
        "ships.txt debe tener un diseño por tipo (" + ShipType.values().length + "), ni más ni menos");
  }

  @Test
  void everyDesignUsesAnExistingChassisOfItsTypeSize() throws IOException {
    // Arrange
    List<ShipDesign> designs = realDesigns();
    Map<String, ShipArtFile> hulls = realHulls();

    // Act + Assert: the chassis of every design, and the size of its type
    Map<String, List<String>> users = new LinkedHashMap<>();
    Map<String, List<ShipSize>> sizes = new LinkedHashMap<>();
    for(ShipDesign design : designs) {
      String where = "[" + design.name() + "] type=" + design.type();
      ShipArtFile hull = hulls.get(design.chassis().toLowerCase(Locale.ROOT));
      assertNotNull(hull, "el chasis=`" + design.chassis() + "` de " + where + " no está en chassis.txt");
      ShipSites.Budget budget = ShipSites.budgetOf(design.type());
      assertNotNull(budget, "el type= de " + where + " no es un tipo del juego con presupuesto");
      assertTrue(budget.size().name().equalsIgnoreCase(hull.size()),
          where + " es de talla " + budget.size() + " pero usa el chasis `" + hull.name() + "` "
              + (hull.size().isEmpty() ? "sin talla" : "de talla " + hull.size()));
      String key = hull.name().toLowerCase(Locale.ROOT);
      users.computeIfAbsent(key, name -> new ArrayList<>()).add(design.type());
      sizes.computeIfAbsent(key, name -> new ArrayList<>()).add(budget.size());
    }
    for(Map.Entry<String, List<ShipSize>> entry : sizes.entrySet()) {
      ShipSize first = entry.getValue().get(0);
      for(ShipSize size : entry.getValue()) {
        assertEquals(first, size, "el chasis `" + entry.getKey() + "` lo comparten " + users.get(entry.getKey())
            + ", de tallas distintas");
      }
    }
  }

  @Test
  void everyGroupUsesAValidSiteLetter() throws IOException {
    // Arrange
    List<ShipDesign> designs = realDesigns();

    // Act + Assert
    for(ShipDesign design : designs) {
      for(ShipDesign.LetterGroup group : design.groups()) {
        assertTrue(SITE_LETTERS.indexOf(Character.toUpperCase(group.letter())) >= 0,
            "[" + design.name() + "] type=" + design.type() + " usa group=" + group.letter()
                + ", que no es una letra de sitio (válidas: " + SITE_LETTERS + ")");
      }
    }
  }

  /** The real designs, from the file the game and the editor resolve. */
  private static List<ShipDesign> realDesigns() throws IOException {
    Path path = ShipArtFile.resolve("ships.txt");
    assertTrue(Files.isRegularFile(path), "no encuentro " + path + " (¿ejecutas desde la raíz del repositorio?)");
    List<ShipDesign> designs = ShipDesign.load(path.toString());
    assertFalse(designs.isEmpty(), "ships.txt no tiene ningún diseño: " + path);
    return designs;
  }

  /** The real hulls by name (in lower case), from the file the game resolves. */
  private static Map<String, ShipArtFile> realHulls() throws IOException {
    Path path = ShipArtFile.resolve("chassis.txt");
    assertTrue(Files.isRegularFile(path), "no encuentro " + path + " (¿ejecutas desde la raíz del repositorio?)");
    Map<String, ShipArtFile> hulls = new LinkedHashMap<>();
    for(ShipArtFile hull : ShipArtFile.load(path.toString())) {
      assertNull(hulls.put(hull.name().toLowerCase(Locale.ROOT), hull),
          "chassis.txt tiene dos secciones llamadas `" + hull.name() + "`");
    }
    assertFalse(hulls.isEmpty(), "chassis.txt no tiene ningún casco: " + path);
    return hulls;
  }
}
