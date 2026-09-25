/*
 * This file is part of Beyond Space Trader.
 *
 * Distributed under the GNU General Public License, version 3 or later; see
 * the LICENSE file. Based on SpaceTrader for Java, which is based on Space
 * Trader for Windows, which is based on Space Trader by Pieter Spronck; see
 * the NOTICE file for the full provenance chain.
 */
package spacetrader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * A properties-backed settings file (the old Windows registry stub).
 */
public final class SettingsFile {
  private final File file;
  private final Properties properties = new Properties();

  public SettingsFile(File file) {
    this.file = file;
    FileInputStream stream = null;
    try {
      if(!file.exists() && !file.createNewFile()) {
        throw new IllegalStateException("Can't create the settings file " + file);
      }
      stream = new FileInputStream(file);
      properties.load(stream);
    } catch(IOException e) {
      throw new IllegalStateException("Can't create or load the settings file " + file, e);
    } finally {
      if(stream != null) {
        try {
          stream.close();
        } catch(IOException e) {
          Log.error("Couldn't close the settings file " + file, e);
        }
      }
    }
  }

  public Object getValue(String settingName) {
    return properties.getProperty(settingName);
  }

  public void setValue(String settingName, String settingValue) {
    properties.setProperty(settingName, settingValue);
  }

  public void close() {
    FileOutputStream stream;
    try {
      stream = new FileOutputStream(file);
    } catch(FileNotFoundException e) {
      Log.error("Couldn't save the settings file " + file, e);
      return;
    }
    try {
      properties.store(stream, "");
    } catch(IOException e) {
      Log.error("Couldn't save the settings file " + file, e);
    } finally {
      try {
        stream.close();
      } catch(IOException e) {
        Log.error("Couldn't close the settings file " + file, e);
      }
    }
  }
}
