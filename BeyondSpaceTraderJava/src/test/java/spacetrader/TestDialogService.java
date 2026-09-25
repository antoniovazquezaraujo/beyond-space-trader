package spacetrader;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import spacetrader.enums.AlertType;


/**
 * Test double for {@link DialogService} that records every alert shown and returns a
 * configurable result.
 */
public class TestDialogService implements DialogService {
  private final List<AlertType> alerts = new ArrayList<>();
  private DialogResult result = DialogResult.None;

  public void setResult(DialogResult result) {
    this.result = result;
  }

  @Override
  public DialogResult alert(AlertType type, String... messageArgs) {
    alerts.add(type);
    return result;
  }

  public List<AlertType> alerts() {
    return alerts;
  }
}
