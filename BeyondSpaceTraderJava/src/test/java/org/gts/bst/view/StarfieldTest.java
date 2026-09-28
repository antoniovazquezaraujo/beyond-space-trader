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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


class StarfieldTest {
  @Test
  void drawsTheSameSkyEveryTime() {
    Starfield.Frame one = new Starfield(40, 10, 0.2, 42).frame(40, 10);
    Starfield.Frame two = new Starfield(40, 10, 0.2, 42).frame(40, 10);
    assertEquals(one.lines(), two.lines());
  }

  @Test
  void onlyBrailleDotsAndGreyShades() {
    Starfield.Frame frame = new Starfield(40, 10, 0.3, 7).frame(40, 10);
    boolean someStar = false;
    for(int row = 0; row < 10; row++) {
      for(int column = 0; column < 40; column++) {
        char character = frame.lines().get(row).charAt(column);
        assertTrue(character == ' ' || (character >= 0x2800 && character <= 0x28FF), "braille");
        int shade = frame.shades()[row][column];
        assertTrue(shade == -1 || (shade >= 232 && shade <= 255), "grey shade");
        someStar |= shade >= 0;
      }
    }
    assertTrue(someStar, "there must be stars");
  }

  @Test
  void theStarsDriftLeftAndStayInTheSky() {
    Starfield sky = new Starfield(40, 10, 0.2, 3);
    int rightmost = 0;
    for(int i = 0; i < sky.stars().size(); i++) {
      if(sky.stars().get(i).x() > sky.stars().get(rightmost).x()) {
        rightmost = i;
      }
    }
    Starfield.Star before = sky.stars().get(rightmost);
    sky.advance();
    assertEquals(before.x() - before.speed(), sky.stars().get(rightmost).x(), 1e-9,
        "the stars drift left at their own speed");

    for(int i = 0; i < 500; i++) {
      sky.advance();
    }
    for(Starfield.Star star : sky.stars()) {
      assertTrue(star.x() > -2 && star.x() < sky.dotWidth() * 1.3, "wraps back from the right");
      assertTrue(star.y() >= 0 && star.y() < sky.dotHeight());
    }
  }
}
