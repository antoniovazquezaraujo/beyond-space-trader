package spacetrader.gui;

import jwinforms.WinformPane;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import spacetrader.enums.AlertType;


/**
 * Swing implementation of {@link DialogService}, backed by {@link FormAlert}.
 */
public class SwingDialogService implements DialogService {
  private final WinformPane parent;

  public SwingDialogService(WinformPane parent) {
    this.parent = parent;
  }

  @Override
  public DialogResult alert(AlertType type, String... messageArgs) {
    return fromJWinForms(FormAlert.Alert(type, parent, messageArgs));
  }

  private static DialogResult fromJWinForms(jwinforms.enums.DialogResult result) {
    switch(result) {
      case OK:
        return DialogResult.OK;
      case Cancel:
        return DialogResult.Cancel;
      case Yes:
        return DialogResult.Yes;
      case No:
        return DialogResult.No;
      default:
        return DialogResult.None;
    }
  }
}
