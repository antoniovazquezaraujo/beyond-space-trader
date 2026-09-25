package spacetrader.stub;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import spacetrader.util.Log;
import util.Convertor;
import util.Lisp;


public class Directory {
  public static String[] GetFiles(String path, String filter) {
    if(!filter.startsWith("*.")) {
      throw new IllegalArgumentException("Unsupported filter: " + filter);
    }
    final String suffix = filter.substring(2);
    File[] files = new File(path).listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File arg0, String filename) {
        return filename.endsWith(suffix);
      }
    });
    if(files == null) {
      Log.write("Directory not found or unreadable: " + path);
      return new String[0];
    }
    List<String> names = Lisp.map(files, new Convertor<String, File>() {
      @Override
      public String convert(File file) {
        return file.getPath();
      }
    });
    return names.toArray(new String[names.size()]);
  }

  public static boolean Exists(String path) {
    return new File(path).exists();
  }

  public static void CreateDirectory(String path) {
    if(!new File(path).mkdir()) {
      Log.write("Couldn't create the directory " + path);
    }
  }
}
