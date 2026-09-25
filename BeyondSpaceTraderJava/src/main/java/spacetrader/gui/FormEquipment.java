package spacetrader.gui;
import java.awt.Color;
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
import jwinforms.PictureBox;
import jwinforms.WinformForm;
import jwinforms.enums.BorderStyle;
import jwinforms.enums.DialogResult;
import jwinforms.enums.FlatStyle;
import jwinforms.enums.FontStyle;
import jwinforms.enums.FormBorderStyle;
import jwinforms.enums.FormStartPosition;
import org.gts.bst.presenter.EquipmentPresenter;
import org.gts.bst.ship.equip.EquipmentType;
import org.gts.bst.view.EquipmentInfoViewModel;
import org.gts.bst.view.EquipmentView;
import org.gts.bst.view.EquipmentViewModel;
import org.gts.bst.view.MainWindow;
import spacetrader.Game;


public class FormEquipment extends WinformForm implements EquipmentView {
  private final Game game = Game.CurrentGame();
  private final MainWindow mainWindow;
  private final EquipmentPresenter presenter;
  private Button btnClose;
  private Button btnBuy;
  private Button btnSell;
  private GroupBox boxSell;
  private GroupBox boxBuy;
  private GroupBox boxShipInfo;
  private Label lblName;
  private Label lblDescription;
  private Label lblSellPrice;
  private Label lblBuyPrice;
  private Label lblSellGadgets;
  private Label lblSellShields;
  private Label lblSellWeapons;
  private Label lblBuyGadgets;
  private Label lblBuyShields;
  private Label lblBuyWeapons;
  private Label lblBuyPriceLabel;
  private Label lblSellPriceLabel;
  private Label lblNameLabel;
  private Label lblTypeLabel;
  private Label lblType;
  private Label lblPowerLabel;
  private Label lblChargeLabel;
  private Label lblPower;
  private Label lblCharge;
  private Label lblSellWeaponNoSlots;
  private Label lblSellShieldNoSlots;
  private Label lblSellGadgetNoSlots;
  private Label lblBuyWeaponNone;
  private Label lblBuyShieldNone;
  private Label lblBuyGadgetNone;
  private ListBox lstSellWeapon;
  private ListBox lstSellShield;
  private ListBox lstSellGadget;
  private ListBox lstBuyGadget;
  private ListBox lstBuyShield;
  private ListBox lstBuyWeapon;
  private PictureBox picEquipment;
  private boolean handlingSelect = false;

  public FormEquipment(MainWindow mainWindow) {
    InitializeComponent();
    this.mainWindow = mainWindow;
    presenter = new EquipmentPresenter(game, this);
    presenter.update();
  }

  // Required method for Designer support - do not modify the contents of this method with the code editor.
  private void InitializeComponent() {
    btnClose = new Button();
    boxSell = new GroupBox();
    lblSellGadgetNoSlots = new Label();
    lblSellShieldNoSlots = new Label();
    lblSellWeaponNoSlots = new Label();
    lblSellGadgets = new Label();
    lblSellShields = new Label();
    lblSellWeapons = new Label();
    lstSellGadget = new ListBox();
    lstSellShield = new ListBox();
    lstSellWeapon = new ListBox();
    boxBuy = new GroupBox();
    lblBuyGadgetNone = new Label();
    lblBuyShieldNone = new Label();
    lblBuyWeaponNone = new Label();
    lblBuyGadgets = new Label();
    lblBuyShields = new Label();
    lblBuyWeapons = new Label();
    lstBuyGadget = new ListBox();
    lstBuyShield = new ListBox();
    lstBuyWeapon = new ListBox();
    boxShipInfo = new GroupBox();
    lblCharge = new Label();
    lblPower = new Label();
    lblChargeLabel = new Label();
    lblPowerLabel = new Label();
    lblType = new Label();
    lblTypeLabel = new Label();
    lblNameLabel = new Label();
    btnSell = new Button();
    btnBuy = new Button();
    lblBuyPriceLabel = new Label();
    lblBuyPrice = new Label();
    lblSellPriceLabel = new Label();
    picEquipment = new PictureBox();
    lblSellPrice = new Label();
    lblDescription = new Label();
    lblName = new Label();
    boxSell.SuspendLayout();
    boxBuy.SuspendLayout();
    boxShipInfo.SuspendLayout();
    SuspendLayout();
    // btnClose
    btnClose.setDialogResult(DialogResult.Cancel);
    btnClose.setLocation(new Point(-32, -32));
    btnClose.setName("btnClose");
    btnClose.setSize(new FormSize(32, 32));
    btnClose.setTabIndex(32);
    btnClose.setTabStop(false);
    btnClose.setText("X");
    // boxSell
    boxSell.Controls.add(lblSellGadgetNoSlots);
    boxSell.Controls.add(lblSellShieldNoSlots);
    boxSell.Controls.add(lblSellWeaponNoSlots);
    boxSell.Controls.add(lblSellGadgets);
    boxSell.Controls.add(lblSellShields);
    boxSell.Controls.add(lblSellWeapons);
    boxSell.Controls.add(lstSellGadget);
    boxSell.Controls.add(lstSellShield);
    boxSell.Controls.add(lstSellWeapon);
    boxSell.setLocation(new Point(4, 2));
    boxSell.setName("boxSell");
    boxSell.setSize(new FormSize(144, 304));
    boxSell.setTabIndex(1);
    boxSell.setTabStop(false);
    boxSell.setText("Current Inventory");
    // lblSellGadgetNoSlots
    lblSellGadgetNoSlots.setLocation(new Point(24, 228));
    lblSellGadgetNoSlots.setName("lblSellGadgetNoSlots");
    lblSellGadgetNoSlots.setSize(new FormSize(104, 16));
    lblSellGadgetNoSlots.setTabIndex(149);
    lblSellGadgetNoSlots.setText("No slots");
    lblSellGadgetNoSlots.setVisible(false);
    // lblSellShieldNoSlots
    lblSellShieldNoSlots.setLocation(new Point(24, 132));
    lblSellShieldNoSlots.setName("lblSellShieldNoSlots");
    lblSellShieldNoSlots.setSize(new FormSize(104, 16));
    lblSellShieldNoSlots.setTabIndex(148);
    lblSellShieldNoSlots.setText("No slots");
    lblSellShieldNoSlots.setVisible(false);
    // lblSellWeaponNoSlots
    lblSellWeaponNoSlots.setLocation(new Point(24, 36));
    lblSellWeaponNoSlots.setName("lblSellWeaponNoSlots");
    lblSellWeaponNoSlots.setSize(new FormSize(104, 16));
    lblSellWeaponNoSlots.setTabIndex(147);
    lblSellWeaponNoSlots.setText("No slots");
    lblSellWeaponNoSlots.setVisible(false);
    // lblSellGadgets
    lblSellGadgets.setAutoSize(true);
    lblSellGadgets.setLocation(new Point(8, 212));
    lblSellGadgets.setName("lblSellGadgets");
    lblSellGadgets.setSize(new FormSize(47, 16));
    lblSellGadgets.setTabIndex(146);
    lblSellGadgets.setText("Gadgets");
    // lblSellShields
    lblSellShields.setAutoSize(true);
    lblSellShields.setLocation(new Point(8, 116));
    lblSellShields.setName("lblSellShields");
    lblSellShields.setSize(new FormSize(41, 16));
    lblSellShields.setTabIndex(145);
    lblSellShields.setText("Shields");
    // lblSellWeapons
    lblSellWeapons.setAutoSize(true);
    lblSellWeapons.setLocation(new Point(8, 20));
    lblSellWeapons.setName("lblSellWeapons");
    lblSellWeapons.setSize(new FormSize(52, 16));
    lblSellWeapons.setTabIndex(144);
    lblSellWeapons.setText("Weapons");
    // lstSellGadget
    lstSellGadget.setBorderStyle(BorderStyle.FixedSingle);
    lstSellGadget.setLocation(new Point(8, 228));
    lstSellGadget.setName("lstSellGadget");
    lstSellGadget.setSize(new FormSize(128, 67));
    lstSellGadget.setTabIndex(3);
    lstSellGadget.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SellClick();
      }
    });
    lstSellGadget.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // lstSellShield
    lstSellShield.setBorderStyle(BorderStyle.FixedSingle);
    lstSellShield.setLocation(new Point(8, 132));
    lstSellShield.setName("lstSellShield");
    lstSellShield.setSize(new FormSize(128, 67));
    lstSellShield.setTabIndex(2);
    lstSellShield.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SellClick();
      }
    });
    lstSellShield.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // lstSellWeapon
    lstSellWeapon.setBorderStyle(BorderStyle.FixedSingle);
    lstSellWeapon.setLocation(new Point(8, 36));
    lstSellWeapon.setName("lstSellWeapon");
    lstSellWeapon.setSize(new FormSize(128, 67));
    lstSellWeapon.setTabIndex(1);
    lstSellWeapon.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SellClick();
      }
    });
    lstSellWeapon.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // boxBuy
    boxBuy.Controls.add(lblBuyGadgetNone);
    boxBuy.Controls.add(lblBuyShieldNone);
    boxBuy.Controls.add(lblBuyWeaponNone);
    boxBuy.Controls.add(lblBuyGadgets);
    boxBuy.Controls.add(lblBuyShields);
    boxBuy.Controls.add(lblBuyWeapons);
    boxBuy.Controls.add(lstBuyGadget);
    boxBuy.Controls.add(lstBuyShield);
    boxBuy.Controls.add(lstBuyWeapon);
    boxBuy.setLocation(new Point(156, 2));
    boxBuy.setName("boxBuy");
    boxBuy.setSize(new FormSize(144, 304));
    boxBuy.setTabIndex(2);
    boxBuy.setTabStop(false);
    boxBuy.setText("Equipment For Sale");
    // lblBuyGadgetNone
    lblBuyGadgetNone.setLocation(new Point(24, 228));
    lblBuyGadgetNone.setName("lblBuyGadgetNone");
    lblBuyGadgetNone.setSize(new FormSize(104, 16));
    lblBuyGadgetNone.setTabIndex(150);
    lblBuyGadgetNone.setText("None for sale");
    lblBuyGadgetNone.setVisible(false);
    // lblBuyShieldNone
    lblBuyShieldNone.setLocation(new Point(24, 132));
    lblBuyShieldNone.setName("lblBuyShieldNone");
    lblBuyShieldNone.setSize(new FormSize(104, 16));
    lblBuyShieldNone.setTabIndex(149);
    lblBuyShieldNone.setText("None for sale");
    lblBuyShieldNone.setVisible(false);
    // lblBuyWeaponNone
    lblBuyWeaponNone.setLocation(new Point(24, 36));
    lblBuyWeaponNone.setName("lblBuyWeaponNone");
    lblBuyWeaponNone.setSize(new FormSize(104, 16));
    lblBuyWeaponNone.setTabIndex(148);
    lblBuyWeaponNone.setText("None for sale");
    lblBuyWeaponNone.setVisible(false);
    // lblBuyGadgets
    lblBuyGadgets.setAutoSize(true);
    lblBuyGadgets.setLocation(new Point(8, 212));
    lblBuyGadgets.setName("lblBuyGadgets");
    lblBuyGadgets.setSize(new FormSize(47, 16));
    lblBuyGadgets.setTabIndex(143);
    lblBuyGadgets.setText("Gadgets");
    // lblBuyShields
    lblBuyShields.setAutoSize(true);
    lblBuyShields.setLocation(new Point(8, 116));
    lblBuyShields.setName("lblBuyShields");
    lblBuyShields.setSize(new FormSize(41, 16));
    lblBuyShields.setTabIndex(142);
    lblBuyShields.setText("Shields");
    // lblBuyWeapons
    lblBuyWeapons.setAutoSize(true);
    lblBuyWeapons.setLocation(new Point(8, 20));
    lblBuyWeapons.setName("lblBuyWeapons");
    lblBuyWeapons.setSize(new FormSize(52, 16));
    lblBuyWeapons.setTabIndex(141);
    lblBuyWeapons.setText("Weapons");
    // lstBuyGadget
    lstBuyGadget.setBorderStyle(BorderStyle.FixedSingle);
    lstBuyGadget.setLocation(new Point(8, 228));
    lstBuyGadget.setName("lstBuyGadget");
    lstBuyGadget.setSize(new FormSize(128, 67));
    lstBuyGadget.setTabIndex(6);
    lstBuyGadget.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        BuyClick(sender, e);
      }
    });
    lstBuyGadget.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // lstBuyShield
    lstBuyShield.setBorderStyle(BorderStyle.FixedSingle);
    lstBuyShield.setLocation(new Point(8, 132));
    lstBuyShield.setName("lstBuyShield");
    lstBuyShield.setSize(new FormSize(128, 67));
    lstBuyShield.setTabIndex(5);
    lstBuyShield.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        BuyClick(sender, e);
      }
    });
    lstBuyShield.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // lstBuyWeapon
    lstBuyWeapon.setBorderStyle(BorderStyle.FixedSingle);
    lstBuyWeapon.setLocation(new Point(8, 36));
    lstBuyWeapon.setName("lstBuyWeapon");
    lstBuyWeapon.setSize(new FormSize(128, 67));
    lstBuyWeapon.setTabIndex(4);
    lstBuyWeapon.setDoubleClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        BuyClick(sender, e);
      }
    });
    lstBuyWeapon.setSelectedIndexChanged(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SelectedIndexChanged(sender, e);
      }
    });
    // boxShipInfo
    boxShipInfo.Controls.add(lblCharge);
    boxShipInfo.Controls.add(lblPower);
    boxShipInfo.Controls.add(lblChargeLabel);
    boxShipInfo.Controls.add(lblPowerLabel);
    boxShipInfo.Controls.add(lblType);
    boxShipInfo.Controls.add(lblTypeLabel);
    boxShipInfo.Controls.add(lblNameLabel);
    boxShipInfo.Controls.add(btnSell);
    boxShipInfo.Controls.add(btnBuy);
    boxShipInfo.Controls.add(lblBuyPriceLabel);
    boxShipInfo.Controls.add(lblBuyPrice);
    boxShipInfo.Controls.add(lblSellPriceLabel);
    boxShipInfo.Controls.add(picEquipment);
    boxShipInfo.Controls.add(lblSellPrice);
    boxShipInfo.Controls.add(lblName);
    boxShipInfo.Controls.add(lblDescription);
    boxShipInfo.setLocation(new Point(308, 2));
    boxShipInfo.setName("boxShipInfo");
    boxShipInfo.setSize(new FormSize(208, 304));
    boxShipInfo.setTabIndex(3);
    boxShipInfo.setTabStop(false);
    boxShipInfo.setText("Equipment Information");
    // lblCharge
    lblCharge.setLocation(new Point(80, 164));
    lblCharge.setName("lblCharge");
    lblCharge.setSize(new FormSize(116, 16));
    lblCharge.setTabIndex(67);
    lblCharge.setText("888");
    // lblPower
    lblPower.setLocation(new Point(80, 148));
    lblPower.setName("lblPower");
    lblPower.setSize(new FormSize(116, 16));
    lblPower.setTabIndex(66);
    lblPower.setText("888");
    // lblChargeLabel
    lblChargeLabel.setAutoSize(true);
    lblChargeLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblChargeLabel.setLocation(new Point(8, 164));
    lblChargeLabel.setName("lblChargeLabel");
    lblChargeLabel.setSize(new FormSize(46, 16));
    lblChargeLabel.setTabIndex(65);
    lblChargeLabel.setText("Charge:");
    // lblPowerLabel
    lblPowerLabel.setAutoSize(true);
    lblPowerLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblPowerLabel.setLocation(new Point(8, 148));
    lblPowerLabel.setName("lblPowerLabel");
    lblPowerLabel.setSize(new FormSize(41, 16));
    lblPowerLabel.setTabIndex(64);
    lblPowerLabel.setText("Power:");
    // lblType
    lblType.setLocation(new Point(80, 100));
    lblType.setName("lblType");
    lblType.setSize(new FormSize(116, 16));
    lblType.setTabIndex(63);
    lblType.setText("Weapon");
    // lblTypeLabel
    lblTypeLabel.setAutoSize(true);
    lblTypeLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblTypeLabel.setLocation(new Point(8, 100));
    lblTypeLabel.setName("lblTypeLabel");
    lblTypeLabel.setSize(new FormSize(34, 16));
    lblTypeLabel.setTabIndex(62);
    lblTypeLabel.setText("Type:");
    // lblNameLabel
    lblNameLabel.setAutoSize(true);
    lblNameLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblNameLabel.setLocation(new Point(8, 84));
    lblNameLabel.setName("lblNameLabel");
    lblNameLabel.setSize(new FormSize(39, 16));
    lblNameLabel.setTabIndex(61);
    lblNameLabel.setText("Name:");
    // btnSell
    btnSell.setFlatStyle(FlatStyle.Flat);
    btnSell.setLocation(new Point(103, 272));
    btnSell.setName("btnSell");
    btnSell.setSize(new FormSize(58, 22));
    btnSell.setTabIndex(8);
    btnSell.setText("Sell");
    btnSell.setClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        SellClick();
      }
    });
    // btnBuy
    btnBuy.setFlatStyle(FlatStyle.Flat);
    btnBuy.setLocation(new Point(31, 272));
    btnBuy.setName("btnBuy");
    btnBuy.setSize(new FormSize(58, 22));
    btnBuy.setTabIndex(7);
    btnBuy.setText("Buy");
    btnBuy.setClick(new EventHandler<Object, EventArgs>() {
      @Override
      public void handle(Object sender, EventArgs e) {
        BuyClick(sender, e);
      }
    });
    // lblBuyPriceLabel
    lblBuyPriceLabel.setAutoSize(true);
    lblBuyPriceLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblBuyPriceLabel.setLocation(new Point(8, 116));
    lblBuyPriceLabel.setName("lblBuyPriceLabel");
    lblBuyPriceLabel.setSize(new FormSize(58, 16));
    lblBuyPriceLabel.setTabIndex(57);
    lblBuyPriceLabel.setText("Buy Price:");
    // lblBuyPrice
    lblBuyPrice.setLocation(new Point(80, 116));
    lblBuyPrice.setName("lblBuyPrice");
    lblBuyPrice.setSize(new FormSize(116, 16));
    lblBuyPrice.setTabIndex(56);
    lblBuyPrice.setText("888,888 cr.");
    // lblSellPriceLabel
    lblSellPriceLabel.setAutoSize(true);
    lblSellPriceLabel.setFont(new Font("Microsoft Sans Serif", 8.25F, FontStyle.Bold, GraphicsUnit.Point, ((byte)(0))));
    lblSellPriceLabel.setLocation(new Point(8, 132));
    lblSellPriceLabel.setName("lblSellPriceLabel");
    lblSellPriceLabel.setSize(new FormSize(58, 16));
    lblSellPriceLabel.setTabIndex(55);
    lblSellPriceLabel.setText("Sell Price:");
    // picEquipment
    picEquipment.setBackColor(Color.white);
    picEquipment.setBorderStyle(BorderStyle.FixedSingle);
    picEquipment.setLocation(new Point(71, 20));
    picEquipment.setName("picEquipment");
    picEquipment.setSize(new FormSize(66, 54));
    picEquipment.setTabIndex(54);
    picEquipment.setTabStop(false);
    picEquipment.setVisible(false);
    // lblSellPrice
    lblSellPrice.setLocation(new Point(80, 132));
    lblSellPrice.setName("lblSellPrice");
    lblSellPrice.setSize(new FormSize(116, 16));
    lblSellPrice.setTabIndex(52);
    lblSellPrice.setText("888,888 cr.");
    // lblDescription
    lblDescription.setLocation(new Point(8, 188));
    lblDescription.setName("lblDescription");
    lblDescription.setSize(new FormSize(196, 75));
    lblDescription.setTabIndex(47);
    // lblName
    lblName.setLocation(new Point(80, 84));
    lblName.setName("lblName");
    lblName.setSize(new FormSize(116, 16));
    lblName.setTabIndex(35);
    lblName.setText("Auto-Repair System");
    // FormEquipment
    setAutoScaleBaseSize(new FormSize(5, 13));
    setCancelButton(btnClose);
    setClientSize(new FormSize(522, 311));
    Controls.add(boxShipInfo);
    Controls.add(boxBuy);
    Controls.add(boxSell);
    Controls.add(btnClose);
    setFormBorderStyle(FormBorderStyle.FixedDialog);
    setMaximizeBox(false);
    setMinimizeBox(false);
    setName("FormEquipment");
    setShowInTaskbar(false);
    setStartPosition(FormStartPosition.CenterParent);
    setText("Buy/Sell Equipment");
    boxSell.ResumeLayout(false);
    boxBuy.ResumeLayout(false);
    boxShipInfo.ResumeLayout(false);
    ResumeLayout(false);
  }

  @Override
  public void render(EquipmentViewModel model) {
    handlingSelect = true;
    try {
      fill(lstBuyWeapon, model.buyWeapons());
      fill(lstBuyShield, model.buyShields());
      fill(lstBuyGadget, model.buyGadgets());
      fill(lstSellWeapon, model.sellWeapons());
      fill(lstSellShield, model.sellShields());
      fill(lstSellGadget, model.sellGadgets());
    } finally {
      handlingSelect = false;
    }
    updateList(lstBuyWeapon, lblBuyWeaponNone);
    updateList(lstBuyShield, lblBuyShieldNone);
    updateList(lstBuyGadget, lblBuyGadgetNone);
    updateList(lstSellWeapon, lblSellWeaponNoSlots);
    updateList(lstSellShield, lblSellShieldNoSlots);
    updateList(lstSellGadget, lblSellGadgetNoSlots);
  }

  @Override
  public void renderInfo(EquipmentInfoViewModel info) {
    picEquipment.setVisible(info.visible());
    lblNameLabel.setVisible(info.visible());
    lblTypeLabel.setVisible(info.visible());
    lblBuyPriceLabel.setVisible(info.visible());
    lblSellPriceLabel.setVisible(info.visible());
    lblPowerLabel.setVisible(info.visible());
    lblChargeLabel.setVisible(info.visible());
    lblName.setText(info.name());
    lblType.setText(info.type());
    lblDescription.setText(info.description());
    lblBuyPrice.setText(info.buyPrice());
    lblSellPrice.setText(info.sellPrice());
    lblPower.setText(info.power());
    lblCharge.setText(info.charge());
    if(info.visible() && info.imageIndex() != null) {
      picEquipment.setImage(game.getParentWindow().EquipmentImages().getImages()[info.imageIndex()]);
    }
    btnBuy.setVisible(info.buyVisible());
    btnSell.setVisible(info.sellVisible());
  }

  private static void fill(ListBox list, java.util.List<String> entries) {
    list.Items.clear();
    for(String entry : entries) {
      list.Items.add(entry);
    }
  }

  private static void updateList(ListBox list, Label noneLabel) {
    boolean entries = list.Items.size() > 0;
    list.setVisible(entries);
    noneLabel.setVisible(!entries);
    if(entries) {
      list.setHeight(list.getItemHeight() * Math.min(list.Items.size(), 5) + 2);
    }
  }

  private void DeselectAll() {
    lstSellWeapon.clearSelected();
    lstSellShield.clearSelected();
    lstSellGadget.clearSelected();
    lstBuyWeapon.clearSelected();
    lstBuyShield.clearSelected();
    lstBuyGadget.clearSelected();
  }

  private void BuyClick(Object sender, EventArgs e) {
    if(presenter.buy()) {
      mainWindow.refresh();
    }
  }

  private void SellClick() {
    if(presenter.sell()) {
      mainWindow.refresh();
    }
  }

  private void SelectedIndexChanged(Object sender, EventArgs e) {
    if(!handlingSelect) {
      ListBox list = (ListBox)sender;
      Object obj = list.getSelectedItem();
      handlingSelect = true;
      DeselectAll();
      list.setSelectedItem(obj);
      handlingSelect = false;
      presenter.select(equipmentType(list), list.getName().indexOf("Sell") >= 0, list.getSelectedIndex());
    }
  }

  private static EquipmentType equipmentType(ListBox list) {
    String name = list.getName();
    if(name.endsWith("Weapon")) {
      return EquipmentType.Weapon;
    }
    if(name.endsWith("Shield")) {
      return EquipmentType.Shield;
    }
    return EquipmentType.Gadget;
  }
}
