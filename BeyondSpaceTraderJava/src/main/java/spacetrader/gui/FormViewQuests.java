package spacetrader.gui;
import java.awt.Point;
import java.util.Arrays;
import jwinforms.Button;
import jwinforms.EventHandler;
import jwinforms.FormSize;
import jwinforms.LinkArea;
import jwinforms.LinkLabel;
import jwinforms.LinkLabelLinkClickedEventArgs;
import jwinforms.WinformForm;
import jwinforms.enums.DialogResult;
import jwinforms.enums.FormBorderStyle;
import jwinforms.enums.FormStartPosition;
import org.gts.bst.presenter.QuestsPresenter;
import org.gts.bst.view.MainWindow;
import org.gts.bst.view.QuestsView;
import org.gts.bst.view.QuestsViewModel;
import spacetrader.Game;
import spacetrader.Strings;


public class FormViewQuests extends WinformForm implements QuestsView {
  private final Game game = Game.CurrentGame();
  private final MainWindow mainWindow;
  private final QuestsPresenter presenter;
  private Button btnClose;
  private LinkLabel lblQuests;

  public FormViewQuests(MainWindow mainWindow) {
    InitializeComponent();
    this.mainWindow = mainWindow;
    presenter = new QuestsPresenter(game, this);
    presenter.update();
  }

  // Required method for Designer support - do not modify the contents of this method with the code editor.
  private void InitializeComponent() {
    btnClose = new Button();
    lblQuests = new LinkLabel();
    SuspendLayout();
    // btnClose
    btnClose.setDialogResult(DialogResult.Cancel);
    btnClose.setLocation(new Point(-32, -32));
    btnClose.setName("btnClose");
    btnClose.setSize(new FormSize(32, 32));
    btnClose.setTabIndex(32);
    btnClose.setTabStop(false);
    btnClose.setText("X");
    // lblQuests
    lblQuests.LinkArea = new LinkArea(0, 0);
    lblQuests.setLocation(new Point(8, 8));
    lblQuests.setName("lblQuests");
    lblQuests.setSize(new FormSize(368, 312));
    lblQuests.setTabIndex(44);
    lblQuests.setText("Kill the space monster at Acamar.\n\n"
        + "Get your lightning shield at Zalkon.\n\n"
        + "Deliver antidote to Japori.\n\n"
        + "Deliver the alien artifact to Professor Berger at some hi-tech system.\n\n"
        + "Bring ambassador Jarek to Devidia. Jarek is wondering why the journey is taking so long, and is no longer of much help in negotiating trades.\n\n"
        + "Inform Gemulon about alien invasion within 8 days.\n\n"
        + "Stop Dr. Fehler's experiment at Daled within 8 days.\n\n"
        + "Deliver the unstable reactor to Nix before it consumes all its fuel.\n\n"
        + "Find and destroy the Scarab (which is hiding at the exit to a wormhole).\n\n"
        + "Smuggle Jonathan Wild to Kravat. Wild is getting impatient, and will no longer aid your crew along the way.\n\n"
        + "Get rid of those pesky tribbles.\n\n"
        + "Claim your moon at Utopia.");
    lblQuests.LinkClicked = new EventHandler<Object, LinkLabelLinkClickedEventArgs>() {
      @Override
      public void handle(Object sender, LinkLabelLinkClickedEventArgs e) {
        lblQuests_LinkClicked(sender, e);
      }
    };
    // FormViewQuests
    setAutoScaleBaseSize(new FormSize(5, 13));
    setCancelButton(btnClose);
    setClientSize(new FormSize(378, 325));
    Controls.addAll(Arrays.asList(btnClose, lblQuests));
    setFormBorderStyle(FormBorderStyle.FixedDialog);
    setMaximizeBox(false);
    setMinimizeBox(false);
    setName("FormViewQuests");
    setShowInTaskbar(false);
    setStartPosition(FormStartPosition.CenterParent);
    setText("Quests");
    ResumeLayout(false);
  }

  @Override
  public void render(QuestsViewModel model) {
    lblQuests.setText(model.text());
    if(model.hasQuests()) {
      for(int i = 0; i < Strings.SystemNames.length; i++) {
        String systemName = Strings.SystemNames[i];
        int start = 0;
        int index = -1;
        while((index = lblQuests.getText().indexOf(systemName, start)) >= 0) {
          lblQuests.Links.add(index, systemName.length(), systemName);
          start = index + systemName.length();
        }
      }
    }
  }

  private void lblQuests_LinkClicked(Object sender, LinkLabelLinkClickedEventArgs e) {
    presenter.selectSystem(e.Link.LinkData.toString());
    mainWindow.refresh();
    Close();
  }
}
