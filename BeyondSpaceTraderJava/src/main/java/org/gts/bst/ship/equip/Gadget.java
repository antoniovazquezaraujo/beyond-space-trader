package org.gts.bst.ship.equip;
import spacetrader.Strings;
import spacetrader.enums.SkillType;
import spacetrader.enums.TechLevel;
import spacetrader.util.Hashtable;
import spacetrader.util.Log;


public class Gadget extends Equipment {
  private GadgetType _type;
  private SkillType _skillBonus;

  public Gadget(GadgetType type, SkillType skillBonus, int price, TechLevel minTechLevel, int chance) {
    super(org.gts.bst.ship.equip.EquipmentType.Gadget, price, minTechLevel, chance);
    _type = type;
    _skillBonus = skillBonus;
  }

  public Gadget(Hashtable hash) {
    super(hash);
    _type = GadgetType.FromInt(GetValueFromHash(hash, "_type", Integer.class));
    _skillBonus = (GetValueFromHash(hash, "_skillBonus", SkillType.NA, SkillType.class));
  }

  @Override
  public Equipment Clone() {
    return new Gadget(_type, _skillBonus, _price, _minTech, _chance);
  }

  @Override
  public EquipmentSubType SubType() {
    return Type();
  }

  @Override
  public Hashtable Serialize() {
    Hashtable hash = super.Serialize();
    hash.add("_type", _type.asInteger());
    hash.add("_skillBonus", _skillBonus.CastToInt());
    return hash;
  }

  @Override
  public String Name() {
    return Strings.GadgetNames.get(_type.asInteger());
  }

  @Override
  public boolean TypeEquals(Object type) {
    return Type() == type;
  }

  public GadgetType Type() {
    return _type;
  }

  public SkillType SkillBonus() {
    return _skillBonus;
  }
}
