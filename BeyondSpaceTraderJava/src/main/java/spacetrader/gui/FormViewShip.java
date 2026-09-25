package spacetrader.gui;
import java.awt.Point;
import java.util.Arrays;
import jwinforms.Button;
import jwinforms.Font;
import jwinforms.FormSize;
import jwinforms.GraphicsUnit;
import jwinforms.GroupBox;
import jwinforms.Label;
import jwinforms.WinformControl;
import jwinforms.WinformForm;
import jwinforms.enums.DialogResult;
import jwinforms.enums.FontStyle;
import jwinforms.enums.FormBorderStyle;
import jwinforms.enums.FormStartPosition;
import org.gts.bst.presenter.ShipPresenter;
import org.gts.bst.view.ShipView;
import org.gts.bst.view.ShipViewModel;
import spacetrader.Game;


public class FormViewShip extends WinformForm implements ShipView {
  private Button btnClose;
  private GroupBox boxSpecialCargo;
  private Label lblTypeLabel;
  private Label lblType;
  private Label lblSpecialCargo;
  private Label lblEquipLabel;
  private Label lblEquip;
  private Game game = Game.CurrentGame();

  public FormViewShip() {
    InitializeComponent();
    new ShipPresenter(game, this).update();
  }

  // Required method for Designer support - do not modify the contents of this method with the code editor.
  private void InitializeComponent() {
    lblTypeLabel = new Label();
    lblType = new Label();
    btnClose = new Button();
    lblEquipLabel = new Label();
    lblEquip = new Label();
    boxSpecialCargo = new GroupBox();
    lblSpecialCargo = new Label();
    boxSpecialCargo.SuspendLayout();
    SuspendLayout();
    // lblTypeLabel
    lblTypeLabel.setAutoSize(true);
    lblTypeLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblTypeLabel.setLocation(new Point(8, 8));
    lblTypeLabel.setSize(new FormSize(34, 13));
    lblTypeLabel.setTabIndex(2);
    lblTypeLabel.setText("Type:");
    // lblType
    lblType.setLocation(new Point(80, 8));
    lblType.setSize(new FormSize(100, 13));
    lblType.setTabIndex(4);
    lblType.setText("Grasshopper");
    // btnClose
    btnClose.setDialogResult(DialogResult.Cancel);
    btnClose.setLocation(new Point(-32, -32));
    btnClose.setSize(new FormSize(32, 32));
    btnClose.setTabIndex(32);
    btnClose.setTabStop(false);
    btnClose.setText("X");
    // lblEquipLabel
    lblEquipLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblEquipLabel.setLocation(new Point(8, 34));
    lblEquipLabel.setSize(new FormSize(64, 176));
    lblEquipLabel.setTabIndex(43);
    lblEquipLabel.setText("Hull:\r\n\r\nEquipment:\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\nUnfilled:");
    // lblEquip
    lblEquip.setLocation(new Point(80, 34));
    lblEquip.setSize(new FormSize(120, 176));
    lblEquip.setTabIndex(44);
    lblEquip.setText("Hardened\r\n\r\n1 Military Laser\r\n1 Morgan\'s Laser\r\n1 Energy Shield\r\n1 Reflective Shi"
        + "eld\r\n1 Lightning Shield\r\nNavigating System\r\nAuto-Repair System\r\n10 Extra Cargo Bays\r\nAn Escape Pod\r\n"
        + "\r\n1 weapon slot\r\n1 gadget slot");
    // boxSpecialCargo
    boxSpecialCargo.Controls.addAll((new WinformControl[] {lblSpecialCargo}));
    boxSpecialCargo.setLocation(new Point(192, 8));
    boxSpecialCargo.setSize(new FormSize(200, 204));
    boxSpecialCargo.setTabIndex(64);
    boxSpecialCargo.setTabStop(false);
    boxSpecialCargo.setText("Special Cargo");
    // lblSpecialCargo
    lblSpecialCargo.setLocation(new Point(8, 16));
    lblSpecialCargo.setSize(new FormSize(190, 176));
    lblSpecialCargo.setTabIndex(0);
    lblSpecialCargo.setText("No special items.");
    // FormViewShip
    setAutoScaleBaseSize(new FormSize(5, 13));
    setCancelButton(btnClose);
    setClientSize(new FormSize(402, 219));
    Controls.addAll(Arrays.asList(boxSpecialCargo, lblEquip, lblEquipLabel, btnClose, lblTypeLabel, lblType));
    setFormBorderStyle(FormBorderStyle.FixedDialog);
    setMaximizeBox(false);
    setMinimizeBox(false);
    setShowInTaskbar(false);
    setStartPosition(FormStartPosition.CenterParent);
    setText("Current Ship");
    boxSpecialCargo.ResumeLayout(false);
    ResumeLayout(false);
  }

  @Override
  public void render(ShipViewModel model) {
    lblType.setText(model.type());
    lblEquipLabel.setText(model.equipmentLabels());
    lblEquip.setText(model.equipmentValues());
    lblSpecialCargo.setText(model.specialCargo());
  }
}
