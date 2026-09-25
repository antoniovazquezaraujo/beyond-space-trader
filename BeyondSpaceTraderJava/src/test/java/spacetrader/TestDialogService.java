package spacetrader;

import java.util.ArrayList;
import java.util.List;
import org.gts.bst.view.DialogResult;
import org.gts.bst.view.DialogService;
import spacetrader.enums.AlertType;


/**
 * Test double for {@link DialogService} that records every alert shown.
 */
class TestDialogService implements DialogService {
  private final List<AlertType> alerts = new ArrayList<>();

  @Override
  public DialogResult alert(AlertType type, String... messageArgs) {
    alerts.add(type);
    return DialogResult.None;
  }

  List<AlertType> alerts() {
    return alerts;
  }
}
