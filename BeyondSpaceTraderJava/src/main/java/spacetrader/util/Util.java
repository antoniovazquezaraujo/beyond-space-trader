package spacetrader.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


public class Util {
  public static <T> boolean ArrayContains(T[] array, T item) {
    for(T t : array) {
      if(t == item) {
        return true;
      }
    }
    return false;
  }

  public static int BruteSeek(int[] array, int a) {
    for(int i = 0; i < array.length; i++) {
      if(array[i] == a) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Returns the paths of the files in a directory that end with the given suffix.
   */
  public static String[] GetFiles(String path, String suffix) {
    try(Stream<Path> files = Files.list(Paths.get(path))) {
      return files.filter(file -> file.toString().endsWith(suffix))
          .map(Path::toString)
          .toArray(String[]::new);
    } catch(IOException e) {
      Log.write("Directory not found or unreadable: " + path);
      return new String[0];
    }
  }

  public static boolean Exists(String path) {
    return Files.exists(Paths.get(path));
  }

  public static void CreateDirectory(String path) {
    try {
      Files.createDirectories(Paths.get(path));
    } catch(IOException e) {
      Log.error("Couldn't create the directory " + path, e);
    }
  }

  public static String StringsJoin(String seperator, String[] values) {
    StringBuilder sb = new StringBuilder("");
    for(int i = 0; i < values.length; i++) {
      if(i > 0) {
        sb.append(seperator);
      }
      sb.append(values[i]);
    }
    return sb.toString();
  }
}
