package spacetrader.gui;
import java.awt.Point;
import jwinforms.Button;
import jwinforms.EventArgs;
import jwinforms.EventHandler;
import jwinforms.Font;
import jwinforms.FormSize;
import jwinforms.GraphicsUnit;
import jwinforms.GroupBox;
import jwinforms.Label;
import jwinforms.ListBox;
import jwinforms.WinformForm;
import jwinforms.enums.BorderStyle;
import jwinforms.enums.DialogResult;
import jwinforms.enums.FlatStyle;
import jwinforms.enums.FontStyle;
import jwinforms.enums.FormBorderStyle;
import jwinforms.enums.FormStartPosition;
import org.gts.bst.presenter.PersonnelPresenter;
import org.gts.bst.view.PersonnelInfo;
import org.gts.bst.view.PersonnelView;
import org.gts.bst.view.PersonnelViewModel;
import spacetrader.Game;


public class FormViewPersonnel extends WinformForm implements PersonnelView {
  private final Game game = Game.CurrentGame();
  private final PersonnelPresenter presenter;
  private Button btnClose;
  private Button btnHireFire;
  private GroupBox boxForHire;
  private GroupBox boxInfo;
  private GroupBox boxCurrentCrew;
  private Label lblRate;
  private Label lblName;
  private Label lblEngineer;
  private Label lblTrader;
  private Label lblFighter;
  private Label lblPilot;
  private Label lblEngineerLabel;
  private Label lblTraderLabel;
  private Label lblFighterLabel;
  private Label lblPilotLabel;
  private Label lblCrewNoQuarters;
  private Label lblForHireNone;
  private ListBox lstForHire;
  private ListBox lstCrew;
  private boolean handlingSelect = false;

  public FormViewPersonnel() {
    InitializeComponent();
    presenter = new PersonnelPresenter(game, this);
    presenter.update();
  }

  // Required method for Designer support - do not modify the contents of this method with the code editor.
  private void InitializeComponent() {
    btnClose = new Button();
    boxCurrentCrew = new GroupBox();
    lstCrew = new ListBox();
    boxForHire = new GroupBox();
    lstForHire = new ListBox();
    boxInfo = new GroupBox();
    btnHireFire = new Button();
    lblRate = new Label();
    lblName = new Label();
    lblEngineer = new Label();
    lblTrader = new Label();
    lblFighter = new Label();
    lblPilot = new Label();
    lblEngineerLabel = new Label();
    lblTraderLabel = new Label();
    lblFighterLabel = new Label();
    lblPilotLabel = new Label();
    lblCrewNoQuarters = new Label();
    lblForHireNone = new Label();
    boxCurrentCrew.SuspendLayout();
    boxForHire.SuspendLayout();
    boxInfo.SuspendLayout();
    SuspendLayout();
    // btnClose
    btnClose.setDialogResult(DialogResult.Cancel);
    btnClose.setLocation(new Point(-32, -32));
    btnClose.setName("btnClose");
    btnClose.setSize(new FormSize(32, 32));
    btnClose.setTabIndex(32);
    btnClose.setTabStop(false);
    btnClose.setText("X");
    // boxCurrentCrew
    boxCurrentCrew.Controls.add(lblCrewNoQuarters);
    boxCurrentCrew.Controls.add(lstCrew);
    boxCurrentCrew.setLocation(new Point(8, 8));
    boxCurrentCrew.setName("boxCurrentCrew");
    boxCurrentCrew.setSize(new FormSize(144, 114));
    boxCurrentCrew.setTabIndex(33);
    boxCurrentCrew.setTabStop(false);
    boxCurrentCrew.setText("Current Crew");
    // lstCrew
    lstCrew.setBorderStyle(BorderStyle.FixedSingle);
    lstCrew.setLocation(new Point(8, 24));
    lstCrew.setName("lstCrew");
    lstCrew.setSize(new FormSize(126, 80));
    lstCrew.setTabIndex(6);
    lstCrew.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        HireFire(sender, e);
      }
    });
    lstCrew.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        crewSelected(sender, e);
      }
    });
    // boxForHire
    boxForHire.Controls.add(lblForHireNone);
    boxForHire.Controls.add(lstForHire);
    boxForHire.setLocation(new Point(160, 8));
    boxForHire.setName("boxForHire");
    boxForHire.setSize(new FormSize(144, 114));
    boxForHire.setTabIndex(34);
    boxForHire.setTabStop(false);
    boxForHire.setText("Mercenaries For Hire");
    // lstForHire
    lstForHire.setBorderStyle(BorderStyle.FixedSingle);
    lstForHire.setLocation(new Point(8, 24));
    lstForHire.setName("lstForHire");
    lstForHire.setSize(new FormSize(126, 80));
    lstForHire.setTabIndex(5);
    lstForHire.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        HireFire(sender, e);
      }
    });
    lstForHire.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        forHireSelected(sender, e);
      }
    });
    // boxInfo
    boxInfo.Controls.add(btnHireFire);
    boxInfo.Controls.add(lblRate);
    boxInfo.Controls.add(lblName);
    boxInfo.Controls.add(lblEngineer);
    boxInfo.Controls.add(lblTrader);
    boxInfo.Controls.add(lblFighter);
    boxInfo.Controls.add(lblPilot);
    boxInfo.Controls.add(lblEngineerLabel);
    boxInfo.Controls.add(lblTraderLabel);
    boxInfo.Controls.add(lblFighterLabel);
    boxInfo.Controls.add(lblPilotLabel);
    boxInfo.setLocation(new Point(312, 8));
    boxInfo.setName("boxInfo");
    boxInfo.setSize(new FormSize(168, 114));
    boxInfo.setTabIndex(35);
    boxInfo.setTabStop(false);
    boxInfo.setText("Mercenary Information");
    // btnHireFire
    btnHireFire.setFlatStyle(FlatStyle.Flat);
    btnHireFire.setLocation(new Point(120, 80));
    btnHireFire.setName("btnHireFire");
    btnHireFire.setSize(new FormSize(36, 22));
    btnHireFire.setTabIndex(4);
    btnHireFire.setText("Hire");
    btnHireFire.setClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        HireFire(sender, e);
      }
    });
    // lblRate
    lblRate.setLocation(new Point(104, 40));
    lblRate.setName("lblRate");
    lblRate.setSize(new FormSize(59, 13));
    lblRate.setTabIndex(97);
    lblRate.setText("88 cr. daily");
    // lblName
    lblName.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblName.setLocation(new Point(12, 18));
    lblName.setName("lblName");
    lblName.setSize(new FormSize(72, 13));
    lblName.setTabIndex(96);
    lblName.setText("Xxxxxxxxxxx");
    // lblEngineer
    lblEngineer.setLocation(new Point(64, 88));
    lblEngineer.setName("lblEngineer");
    lblEngineer.setSize(new FormSize(17, 13));
    lblEngineer.setTabIndex(95);
    lblEngineer.setText("88");
    // lblTrader
    lblTrader.setLocation(new Point(64, 72));
    lblTrader.setName("lblTrader");
    lblTrader.setSize(new FormSize(17, 13));
    lblTrader.setTabIndex(94);
    lblTrader.setText("88");
    // lblFighter
    lblFighter.setLocation(new Point(64, 56));
    lblFighter.setName("lblFighter");
    lblFighter.setSize(new FormSize(17, 13));
    lblFighter.setTabIndex(93);
    lblFighter.setText("88");
    // lblPilot
    lblPilot.setLocation(new Point(64, 40));
    lblPilot.setName("lblPilot");
    lblPilot.setSize(new FormSize(17, 13));
    lblPilot.setTabIndex(92);
    lblPilot.setText("88");
    // lblEngineerLabel
    lblEngineerLabel.setAutoSize(true);
    lblEngineerLabel.setLocation(new Point(12, 88));
    lblEngineerLabel.setName("lblEngineerLabel");
    lblEngineerLabel.setSize(new FormSize(53, 16));
    lblEngineerLabel.setTabIndex(91);
    lblEngineerLabel.setText("Engineer:");
    // lblTraderLabel
    lblTraderLabel.setAutoSize(true);
    lblTraderLabel.setLocation(new Point(12, 72));
    lblTraderLabel.setName("lblTraderLabel");
    lblTraderLabel.setSize(new FormSize(41, 16));
    lblTraderLabel.setTabIndex(90);
    lblTraderLabel.setText("Trader:");
    // lblFighterLabel
    lblFighterLabel.setAutoSize(true);
    lblFighterLabel.setLocation(new Point(12, 56));
    lblFighterLabel.setName("lblFighterLabel");
    lblFighterLabel.setSize(new FormSize(43, 16));
    lblFighterLabel.setTabIndex(89);
    lblFighterLabel.setText("Fighter:");
    // lblPilotLabel
    lblPilotLabel.setAutoSize(true);
    lblPilotLabel.setLocation(new Point(12, 40));
    lblPilotLabel.setName("lblPilotLabel");
    lblPilotLabel.setSize(new FormSize(29, 16));
    lblPilotLabel.setTabIndex(88);
    lblPilotLabel.setText("Pilot:");
    // lblCrewNoQuarters
    lblCrewNoQuarters.setLocation(new Point(16, 24));
    lblCrewNoQuarters.setName("lblCrewNoQuarters");
    lblCrewNoQuarters.setSize(new FormSize(120, 16));
    lblCrewNoQuarters.setTabIndex(7);
    lblCrewNoQuarters.setText("No quarters available");
    lblCrewNoQuarters.setVisible(false);
    // lblForHireNone
    lblForHireNone.setLocation(new Point(16, 24));
    lblForHireNone.setName("lblForHireNone");
    lblForHireNone.setSize(new FormSize(120, 16));
    lblForHireNone.setTabIndex(8);
    lblForHireNone.setText("No one for hire");
    lblForHireNone.setVisible(false);
    // FormViewPersonnel
    setAutoScaleBaseSize(new FormSize(5, 13));
    setCancelButton(btnClose);
    setClientSize(new FormSize(488, 129));
    Controls.add(boxInfo);
    Controls.add(boxForHire);
    Controls.add(boxCurrentCrew);
    Controls.add(btnClose);
    setFormBorderStyle(FormBorderStyle.FixedDialog);
    setMaximizeBox(false);
    setMinimizeBox(false);
    setName("FormViewPersonnel");
    setShowInTaskbar(false);
    setStartPosition(FormStartPosition.CenterParent);
    setText("Personnel");
    boxCurrentCrew.ResumeLayout(false);
    boxForHire.ResumeLayout(false);
    boxInfo.ResumeLayout(false);
    ResumeLayout(false);
  }

  @Override
  public void render(PersonnelViewModel model) {
    handlingSelect = true;
    try {
      lstCrew.Items.clear();
      for(String entry : model.crewEntries()) {
        lstCrew.Items.add(entry);
      }
      lstCrew.setVisible(model.crewVisible());
      lblCrewNoQuarters.setVisible(!model.crewVisible());
      lblCrewNoQuarters.setText(model.crewEmptyText());
      if(model.crewVisible()) {
        lstCrew.setHeight(lstCrew.getItemHeight() * Math.min(model.crewEntries().size(), 6) + 2);
      }
      lstForHire.Items.clear();
      for(String entry : model.forHireEntries()) {
        lstForHire.Items.add(entry);
      }
      lstForHire.setVisible(model.forHireVisible());
      lblForHireNone.setVisible(!model.forHireVisible());
      lblForHireNone.setText(model.forHireEmptyText());
      if(model.forHireVisible()) {
        lstForHire.setHeight(lstForHire.getItemHeight() * Math.min(model.forHireEntries().size(), 6) + 2);
      }
    } finally {
      handlingSelect = false;
    }
    renderInfo(model.info());
  }

  @Override
  public void renderInfo(PersonnelInfo info) {
    lblName.setVisible(info.visible());
    lblName.setText(info.name());
    lblRate.setVisible(info.rateVisible());
    lblRate.setText(info.rate());
    lblPilotLabel.setVisible(info.visible());
    lblFighterLabel.setVisible(info.visible());
    lblTraderLabel.setVisible(info.visible());
    lblEngineerLabel.setVisible(info.visible());
    lblPilot.setVisible(info.visible());
    lblPilot.setText(info.pilot());
    lblFighter.setVisible(info.visible());
    lblFighter.setText(info.fighter());
    lblTrader.setVisible(info.visible());
    lblTrader.setText(info.trader());
    lblEngineer.setVisible(info.visible());
    lblEngineer.setText(info.engineer());
    btnHireFire.setVisible(info.hireFireVisible());
    btnHireFire.setText(info.hireFireText());
  }

  private void crewSelected(Object sender, EventArgs e) {
    if(!handlingSelect) {
      handlingSelect = true;
      lstForHire.clearSelected();
      handlingSelect = false;
      presenter.selectCrew(lstCrew.getSelectedIndex());
    }
  }

  private void forHireSelected(Object sender, EventArgs e) {
    if(!handlingSelect) {
      handlingSelect = true;
      lstCrew.clearSelected();
      handlingSelect = false;
      presenter.selectForHire(lstForHire.getSelectedIndex());
    }
  }

  private void HireFire(Object sender, EventArgs e) {
    if(presenter.hireFire()) {
      game.getParentWindow().UpdateAll(); // TODO: move to the application navigator (#4)
    }
  }
}
