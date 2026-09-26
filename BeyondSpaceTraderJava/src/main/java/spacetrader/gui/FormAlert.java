package spacetrader.gui;
import org.gts.bst.view.AlertDefinition;
import org.gts.bst.view.Alerts;
import jwinforms.Button;
import jwinforms.Container;
import jwinforms.EventArgs;
import jwinforms.EventHandler;
import jwinforms.FormSize;
import jwinforms.Graphics;
import jwinforms.IContainer;
import jwinforms.ImageList;
import jwinforms.ImageListStreamer;
import jwinforms.Label;
import jwinforms.ResourceManager;
import jwinforms.SizeF;
import jwinforms.Timer;
import jwinforms.WinformForm;
import jwinforms.WinformPane;
import jwinforms.enums.ColorDepth;
import jwinforms.enums.DialogResult;
import jwinforms.enums.FlatStyle;
import jwinforms.enums.FormBorderStyle;
import jwinforms.enums.FormStartPosition;
import spacetrader.Functions;
import spacetrader.enums.AlertType;
import spacetrader.enums.GameEndType;


public class FormAlert extends WinformForm {
  private static final String _80_CHARS = "01234567890123456789012345678901234567890123456789012345678901234567890123456789";
  private static final int SPLASH_INDEX = 4;
  private Button btn1;
  private Button btn2;
  private ImageList ilImages;
  private Label lblText;
  private Timer tmrTick;
  private IContainer components;

  private FormAlert() {
    InitializeComponent();
  }

  public FormAlert(String title, String text, String button1Text, DialogResult button1Result, String button2Text, DialogResult button2Result, String[] args) {
    this();
    Graphics g = CreateGraphics();
    // Replace any variables.
    if(args != null) {
      title = Functions.StringVars(title, args);
      text = Functions.StringVars(text, args);
    }
    lblText.setWidth(g.MeasureString((text.length() > 80 ? _80_CHARS : text), getFont()).width + 25);
    lblText.setText(text);
    lblText.setHeight(30 + 30 * text.length() / 80);
    // Size the buttons.
    btn1.setText(button1Text);
    btn1.setDialogResult(button1Result);
    btn1.setWidth(Math.max(40, g.MeasureString(btn1.getText(), getFont()).width + 35));
    int btnWidth = btn1.getWidth();
    if(button2Text != null) {
      btn2.setText(button2Text);
      btn2.setWidth(Math.max((int)Math.ceil(g.MeasureString(btn2.getText(), getFont()).width) + 10, 40));
      btn2.setVisible(true);
      btn2.setDialogResult(button2Result);
      btnWidth += btn2.getWidth() + 6;
    }
    // Size the form.
    setWidth(Math.max(btnWidth, lblText.getWidth()) + 16);
    setHeight(lblText.getHeight() + 75);
    // Locate the controls.
    lblText.setLeft((getWidth() - lblText.getWidth()) / 2);
    btn1.setTop(lblText.getHeight() + 19);
    btn1.setLeft((getWidth() - btnWidth) / 2);
    btn2.setTop(btn1.getTop());
    btn2.setLeft(btn1.getLeft() + btn1.getWidth() + 6);
    // Set the title.
    setText(title);
  }

  public FormAlert(String title, int imageIndex) {
    this();
    // Make sure the extra controls are hidden.
    lblText.setVisible(false);
    btn2.setVisible(false);
    // Move btn1 off-screen.
    btn1.setLeft(-btn1.getWidth());
    btn1.setTop(-btn1.getHeight());
    setAcceptButton(btn1);
    setCancelButton(btn1);
    // Set the background image.
    setBackgroundImage(ilImages.getImages()[imageIndex]);
    setClientSize((new SizeF(getBackgroundImage().getWidth(), getBackgroundImage().getHeight())));
    // Set the title.
    setText(title);
    // If this is the splash screen, get rid of the title bar and start the timer.
    if(imageIndex == SPLASH_INDEX) {
      setFormBorderStyle(FormBorderStyle.None);
      tmrTick.Start();
    }
  }

  private void FormAlert_Click(Object sender, EventArgs e) {
    // If the button is off-screen, this is an image and can be clicked away.
    if(btn1.getLeft() < 0) {
      Close();
    }
  }

  private void InitializeComponent() {
    // Required method for Designer support - do not modify the contents of this method with the code editor.
    components = new Container();
    ResourceManager resources = new ResourceManager(FormAlert.class);
    lblText = new Label();
    btn1 = new Button();
    btn2 = new Button();
    ilImages = new ImageList(components);
    tmrTick = new Timer(components);
    SuspendLayout();
    // lblText
    lblText.setLocation(new java.awt.Point(8, 8));
    lblText.setName("lblText");
    lblText.setTabIndex(3);
    lblText.setText("X");
    // btn1
    btn1.setDialogResult(DialogResult.OK);
    btn1.setFlatStyle(FlatStyle.Flat);
    btn1.setLocation(new java.awt.Point(115, 32));
    btn1.setName("btn1");
    btn1.setSize(new FormSize(40, 22));
    btn1.setTabIndex(1);
    btn1.setText("Ok");
    // btn2
    btn2.setDialogResult(DialogResult.No);
    btn2.setFlatStyle(FlatStyle.Flat);
    btn2.setLocation(new java.awt.Point(200, 32));
    btn2.setName("btn2");
    btn2.setSize(new FormSize(40, 22));
    btn2.setTabIndex(2);
    btn2.setText("No");
    btn2.setVisible(false);
    // ilImages
    ilImages.ColorDepth = ColorDepth.Depth24Bit;
    ilImages.setImageSize(new FormSize(160, 160));
    ilImages.setImageStream(((ImageListStreamer)(resources.GetObject("ilImages.ImageStream"))));
    ilImages.setTransparentColor(null);
    // tmrTick
    tmrTick.setInterval(4000);
    tmrTick.Tick = new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        tmrTick_Tick();
      }
    };
    // FormAlert
    setAutoScaleBaseSize(new FormSize(5, 13));
    setClientSize(new FormSize(270, 63));
    setControlBox(false);
    Controls.add(btn2);
    Controls.add(btn1);
    Controls.add(lblText);
    setFormBorderStyle(FormBorderStyle.FixedDialog);
    setName("FormAlert");
    setShowInTaskbar(false);
    setStartPosition(FormStartPosition.CenterParent);
    setText("Title");
    setClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        FormAlert_Click(sender, e);
      }
    });
    ResumeLayout(false);
  }

  private void tmrTick_Tick() {
    Close();
  }

  public static DialogResult Alert(AlertType at, WinformPane wp) {
    return Alert(at, wp, new String[]{});
  }

  public static DialogResult Alert(AlertType at, WinformPane wp, String s) {
    return Alert(at, wp, new String[]{s});
  }

  public static DialogResult Alert(AlertType at, WinformPane wp, String s, String t) {
    return Alert(at, wp, new String[]{s, t});
  }

  public static DialogResult Alert(AlertType at, WinformPane wp, String s, String t, String u) {
    return Alert(at, wp, new String[]{s, t, u});
  }

  private static DialogResult toJWinForms(org.gts.bst.view.DialogResult result) {
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

  public static DialogResult Alert(AlertType at, WinformPane wp, String[] ss) {
    DialogResult dr = DialogResult.None;
    if(ss.length == 0) {
      ss = null;
    }
    AlertDefinition definition = Alerts.get(at);
    if(definition != null) {
      dr = (new FormAlert(definition.title(), definition.message(), definition.button1(), toJWinForms(definition.result1()),
          definition.button2(), toJWinForms(definition.result2()), ss)).ShowDialog(wp);
    } else {
      switch(at) {
        case AppStart:
          (new FormAlert(Alerts.title(at), SPLASH_INDEX)).ShowDialog(wp);
          break;
        case GameEndBoughtMoon:
          (new FormAlert(Alerts.title(at), GameEndType.BoughtMoon.CastToInt())).ShowDialog(wp);
          break;
        case GameEndBoughtMoonGirl:
          (new FormAlert(Alerts.title(at), GameEndType.BoughtMoonGirl.CastToInt())).ShowDialog(wp);
          break;
        case GameEndKilled:
          (new FormAlert(Alerts.title(at), GameEndType.Killed.CastToInt())).ShowDialog(wp);
          break;
        case GameEndRetired:
          (new FormAlert(Alerts.title(at), GameEndType.Retired.CastToInt())).ShowDialog(wp);
          break;
        default:
          break;
      }
    }
    return dr;
  }
}
