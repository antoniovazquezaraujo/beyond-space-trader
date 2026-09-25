package spacetrader.gui;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import jwinforms.WinformForm;
import jwinforms.enums.DialogResult;


public class Launcher {
  public static void runForm(WinformForm wf) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception ex) {
      // Fall back to the default look and feel.
    }
    SwingUtilities.updateComponentTreeUI(wf.asSwingObject());
    DialogResult res = wf.ShowDialog(null);
    System.out.println("Dialog result: " + res);
  }
}
