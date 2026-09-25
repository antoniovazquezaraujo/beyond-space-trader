/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader.util;

import java.util.logging.Level;
import java.util.logging.Logger;


public final class Log {
  private static final Logger LOGGER = Logger.getLogger(Log.class.getName());

  private Log() {
  }

  public static void write(String message) {
    LOGGER.log(Level.INFO, message);
  }

  public static void error(String message, Throwable cause) {
    LOGGER.log(Level.WARNING, message, cause);
  }
}
