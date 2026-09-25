package spacetrader.stub;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import spacetrader.util.Log;


public class RegistryKey {
  private final File file;
  protected final Properties properties = new Properties();

  public RegistryKey(File regfile) {
    this.file = regfile;
    FileInputStream stream = null;
    try {
      regfile.createNewFile();
      stream = new FileInputStream(regfile);
      properties.load(stream);
    } catch(IOException e) {
      throw new IllegalStateException("Can't create or load the settings file " + regfile, e);
    } finally {
      if(stream != null) {
        try {
          stream.close();
        } catch(IOException e) {
          Log.error("Couldn't close the settings file " + regfile, e);
        }
      }
    }
  }

  public Object GetValue(String settingName) {
    return properties.getProperty(settingName);
  }

  public void Close() {
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

  public void SetValue(String settingName, String settingValue) {
    properties.setProperty(settingName, settingValue);
  }
}
