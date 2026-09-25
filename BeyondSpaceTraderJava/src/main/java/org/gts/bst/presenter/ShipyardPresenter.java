package org.gts.bst.presenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.gts.bst.ship.ShipSize;
import spacetrader.ShipTemplate;
import org.gts.bst.ship.ShipType;
import org.gts.bst.view.ShipyardView;
import org.gts.bst.view.ShipyardDesignerViewModel;
import org.gts.bst.view.ShipyardDesignerViewModel.Numeric;
import spacetrader.Commander;
import spacetrader.Consts;
import spacetrader.Functions;
import spacetrader.Game;
import spacetrader.ShipSpec;
import spacetrader.Shipyard;
import spacetrader.SpecialEvent;
import spacetrader.Strings;
import spacetrader.enums.AlertType;
import spacetrader.util.Util;
import spacetrader.util.Hashtable;


/**
 * Fills the shipyard designer and mediates its commands. No front-end types involved:
 * the view applies the model, performs the file dialogs and owns the custom bitmaps.
 */
public class ShipyardPresenter {
  private static final ShipType[] IMAGE_TYPES = new ShipType[]{
    ShipType.Flea, ShipType.Gnat, ShipType.Firefly, ShipType.Mosquito, ShipType.Bumblebee, ShipType.Beetle,
    ShipType.Hornet, ShipType.Grasshopper, ShipType.Termite, ShipType.Wasp, ShipType.Custom
  };

  private final Game game;
  private final Commander cmdr;
  private final Shipyard yard;
  private final ShipyardView view;
  private final List<ShipSize> sizes = new ArrayList<>();
  private final List<String> sizeNames = new ArrayList<>();
  private final List<Object> templates = new ArrayList<>();
  private boolean loading = false;
  private int sizeIndex = 0;
  private int templateIndex = 0;
  private int imageIndex = 0;
  private String name = "";

  public ShipyardPresenter(Game game, ShipyardView view) {
    this.game = game;
    this.cmdr = game.Commander();
    this.yard = cmdr.CurrentSystem().Shipyard();
    this.view = view;
  }

  public void start() {
    loadSizes();
    loadTemplateList();
    applySelectedTemplate();
    update();
  }

  public void update() {
    updateShip();
    view.render(new ShipyardDesignerViewModel(
        Functions.StringVars(Strings.ShipyardTitle, yard.Name()),
        yard.Id().CastToInt(),
        Functions.StringVars(Strings.ShipyardWelcome, yard.Name(), yard.Engineer()),
        Strings.Sizes[yard.SpecialtySize().CastToInt()],
        Strings.ShipyardSkills[yard.Skill().CastToInt()],
        Strings.ShipyardSkillDescriptions[yard.Skill().CastToInt()],
        Functions.StringVars(Strings.ShipyardWarning, "" + Shipyard.PENALTY_FIRST_PCT, "" + Shipyard.PENALTY_SECOND_PCT),
        sizeNames, sizeIndex, templateNames(), templateIndex, name, numerics(),
        "" + yard.UnitsUsed(), Functions.FormatPercent(yard.PercentOfMaxUnits()), percentLevel(),
        Functions.FormatMoney(yard.AdjustedPrice()), Functions.FormatMoney(yard.AdjustedDesignFee()),
        Functions.FormatMoney(yard.AdjustedPenaltyCost()), Functions.FormatMoney(-yard.TradeIn(cmdr)),
        Functions.FormatMoney(yard.TotalCost(cmdr)), constructEnabled(name), saveEnabled(name),
        imageIndex, customImage(), customImage() ? Strings.ShipNameCustomShip
            : Consts.ShipSpecs.get(IMAGE_TYPES[imageIndex].CastToInt()).Name()));
  }

  public void onSizeChanged(int index) {
    if(index < 0 || index >= sizes.size()) {
      return;
    }
    setTemplateModified();
    sizeIndex = index;
    yard.ShipSpec().setSize(sizes.get(index));
    updateCalculatedFigures();
    update();
  }

  public void loadSelectedTemplate(int index) {
    templateIndex = index;
    applySelectedTemplate();
    update();
  }

  public void onValuesChanged(int cargo, int fuel, int hull, int weapon, int shield, int gadget, int crew) {
    setTemplateModified();
    applyValues(cargo, fuel, hull, weapon, shield, gadget, crew);
    updateCalculatedFigures();
    update();
  }

  public void onNameChanged(String name) {
    this.name = name;
    setTemplateModified();
    update();
  }

  public void nextImage() {
    setTemplateModified();
    imageIndex = (imageIndex + 1) % IMAGE_TYPES.length;
    update();
  }

  public void previousImage() {
    setTemplateModified();
    imageIndex = (imageIndex + IMAGE_TYPES.length - 1) % IMAGE_TYPES.length;
    update();
  }

  public void customImageLoaded() {
    setTemplateModified();
    imageIndex = IMAGE_TYPES.length - 1;
    update();
  }

  public void saveTemplate(String name) {
    if(!saveEnabled(name)) {
      return;
    }
    String fileName = view.askSaveTemplateFile();
    if(fileName == null) {
      return;
    }
    ShipTemplate template = new ShipTemplate(yard.ShipSpec(), name);
    if(customImage()) {
      template.ImageIndex(ShipType.Custom.CastToInt());
      view.applyCustomImages(template);
    } else {
      template.ImageIndex(imageIndex);
    }
    Functions.SaveFile(fileName, template.Serialize(), game.Dialogs());
    loadTemplateList();
    update();
  }

  public void construct(String name) {
    if(!constructEnabled(name)) {
      return;
    }
    if(cmdr.TradeShip(yard.ShipSpec(), yard.TotalCost(cmdr), name)) {
      Strings.ShipNames[ShipType.Custom.CastToInt()] = name;
      if(game.getQuestStatusScarab() == SpecialEvent.StatusScarabDone) {
        game.setQuestStatusScarab(SpecialEvent.StatusScarabNotStarted);
      }
      if(cmdr.getShip().ImageIndex() == ShipType.Custom.CastToInt()) {
        view.applyCustomShipImages();
        cmdr.getShip().UpdateCustomImageOffsetConstants();
      }
      game.Dialogs().alert(AlertType.ShipDesignThanks, yard.Name());
      view.close();
    }
  }

  private void loadSizes() {
    sizes.clear();
    sizeNames.clear();
    for(ShipSize size : yard.AvailableSizes()) {
      sizes.add(size);
      sizeNames.add(Functions.StringVars(Strings.ShipyardSizeItem, Strings.Sizes[size.CastToInt()],
          Functions.Multiples(Shipyard.MaxUnits(size), Strings.ShipyardUnit)));
    }
  }

  private void loadTemplateList() {
    templates.clear();
    templates.add(new ShipTemplate(cmdr.getShip(), Strings.ShipNameCurrentShip));
    templates.add(Consts.ShipTemplateSeparator);
    for(ShipSize size : sizes) {
      templates.add(new ShipTemplate(size, Strings.Sizes[size.CastToInt()] + Strings.ShipNameTemplateSuffixMinimum));
    }
    templates.add(Consts.ShipTemplateSeparator);
    for(ShipSpec spec : Consts.ShipSpecs) {
      if(sizes.contains(spec.getSize()) && spec.Type().CastToInt() <= Consts.MaxShip) {
        templates.add(new ShipTemplate(spec, spec.Name() + Strings.ShipNameTemplateSuffixDefault));
      }
    }
    templates.add(Consts.ShipTemplateSeparator);
    List<ShipTemplate> userTemplates = new ArrayList<>();
    for(String fileName : Util.GetFiles(Consts.CustomTemplatesDirectory, ".sst")) {
      ShipTemplate template = new ShipTemplate((Hashtable)Functions.LoadFile(fileName, true, game.Dialogs()));
      if(sizes.contains(template.Size())) {
        userTemplates.add(template);
      }
    }
    Collections.sort(userTemplates);
    templates.addAll(userTemplates);
    templateIndex = 0;
  }

  private void applySelectedTemplate() {
    if(templateIndex < 0 || templateIndex >= templates.size() || !(templates.get(templateIndex) instanceof ShipTemplate)) {
      return;
    }
    loading = true;
    ShipTemplate template = (ShipTemplate)templates.get(templateIndex);
    if(template.Name().equals(Strings.ShipNameCurrentShip)) {
      name = cmdr.getShip().Name();
    } else if(template.Name().endsWith(Strings.ShipNameTemplateSuffixDefault)
        || template.Name().endsWith(Strings.ShipNameTemplateSuffixMinimum)) {
      name = "";
    } else {
      name = template.Name();
    }
    sizeIndex = Math.max(0, sizes.indexOf(template.Size()));
    yard.ShipSpec().setSize(template.Size());
    imageIndex = template.ImageIndex() == ShipType.Custom.CastToInt() ? IMAGE_TYPES.length - 1 : template.ImageIndex();
    view.adoptTemplateImages(template);
    applyValues(template.CargoBays(), template.FuelTanks(), template.HullStrength(),
        template.WeaponSlots(), template.ShieldSlots(), template.GadgetSlots(), template.CrewQuarters());
    updateCalculatedFigures();
    if(!templates.isEmpty() && templates.get(0).toString().equals(Strings.ShipNameModified)) {
      templates.remove(0);
      templateIndex = 0;
    }
    loading = false;
  }

  private void applyValues(int cargo, int fuel, int hull, int weapon, int shield, int gadget, int crew) {
    yard.ShipSpec().CargoBays(cargo);
    yard.ShipSpec().FuelTanks(Math.min(Math.max(yard.BaseFuel(), fuel), Consts.MaxFuelTanks));
    yard.ShipSpec().HullStrength(Math.max(yard.BaseHull(), hull));
    yard.ShipSpec().setWeaponSlots(weapon);
    yard.ShipSpec().setShieldSlots(shield);
    yard.ShipSpec().setGadgetSlots(gadget);
    yard.ShipSpec().setCrewQuarters(Math.max(1, crew));
  }

  private void updateCalculatedFigures() {
    ShipSpec spec = yard.ShipSpec();
    spec.FuelTanks(Math.max(yard.BaseFuel(), spec.FuelTanks()));
    int extraFuel = spec.FuelTanks() - yard.BaseFuel();
    if(extraFuel % yard.PerUnitFuel() > 0 && spec.FuelTanks() < Consts.MaxFuelTanks) {
      spec.FuelTanks(Math.min(Consts.MaxFuelTanks,
          (extraFuel + yard.PerUnitFuel()) / yard.PerUnitFuel() * yard.PerUnitFuel() + yard.BaseFuel()));
    }
    spec.HullStrength(Math.max(yard.BaseHull(), spec.HullStrength()));
    int extraHull = spec.HullStrength() - yard.BaseHull();
    if(extraHull % yard.PerUnitHull() > 0) {
      spec.HullStrength((extraHull + yard.PerUnitHull()) / yard.PerUnitHull() * yard.PerUnitHull() + yard.BaseHull());
    }
    yard.CalculateDependantVariables();
  }

  private void setTemplateModified() {
    if(!loading && !templates.isEmpty() && !templates.get(0).toString().equals(Strings.ShipNameModified)) {
      templates.add(0, Strings.ShipNameModified);
      templateIndex = 0;
    }
  }

  private List<String> templateNames() {
    List<String> names = new ArrayList<>(templates.size());
    for(Object template : templates) {
      names.add(template.toString());
    }
    return names;
  }

  private List<Numeric> numerics() {
    ShipSpec spec = yard.ShipSpec();
    return Arrays.asList(
        new Numeric(spec.CargoBays(), null, null, null),
        new Numeric(spec.FuelTanks(), yard.BaseFuel(), Consts.MaxFuelTanks, yard.PerUnitFuel()),
        new Numeric(spec.HullStrength(), yard.BaseHull(), null, yard.PerUnitHull()),
        new Numeric(spec.getWeaponSlots(), null, Consts.MaxSlots, null),
        new Numeric(spec.getShieldSlots(), null, Consts.MaxSlots, null),
        new Numeric(spec.getGadgetSlots(), null, Consts.MaxSlots, null),
        new Numeric(spec.getCrewQuarters(), null, Consts.MaxSlots, null));
  }

  private int percentLevel() {
    if(yard.UnitsUsed() > yard.MaxUnits()) {
      return 3;
    }
    if(yard.PercentOfMaxUnits() >= Shipyard.PENALTY_SECOND_PCT) {
      return 2;
    }
    if(yard.PercentOfMaxUnits() >= Shipyard.PENALTY_FIRST_PCT) {
      return 1;
    }
    return 0;
  }

  private boolean constructEnabled(String name) {
    return yard.PercentOfMaxUnits() <= 100 && !name.isEmpty();
  }

  private boolean saveEnabled(String name) {
    return !name.isEmpty();
  }

  private boolean customImage() {
    return imageIndex > Consts.MaxShip;
  }

  private void updateShip() {
    yard.ShipSpec().ImageIndex(IMAGE_TYPES[imageIndex].CastToInt());
  }
}
