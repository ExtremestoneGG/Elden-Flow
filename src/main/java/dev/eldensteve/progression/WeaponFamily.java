package dev.eldensteve.progression;

/** Effects belong to the selected animation family, not item names or materials. */
public enum WeaponFamily {
    DAGGER(.15,"sequence"), STRAIGHT_SWORD(.5,"combo"), CURVED_SWORD(.2,"evade"),
    THRUSTING_SWORD(.15,"economy"), KATANA(.2,"bleed"), TWINBLADE(.3,"sharpness"),
    GREATSWORD(.8,"charged_fury"), HAMMER(.8,"charged_stun"), GREAT_HAMMER(.95,"stun"),
    AXE(.8,"fracture"), GREAT_AXE(.9,"opener"), SPEAR(.35,"repel"),
    HALBERD(.6,"reach"), REAPER(.25,"poison"), SHIELD(1,"parry");
    public final double strength;
    public final String passive;
    WeaponFamily(double strength,String passive){this.strength=strength;this.passive=passive;}
    public static WeaponFamily fromType(String type){
        if(type==null)return null;
        return switch(type){
            case "sai","dagger"->DAGGER;case "longsword"->STRAIGHT_SWORD;case "cutlass"->CURVED_SWORD;
            case "rapier"->THRUSTING_SWORD;case "katana"->KATANA;case "twinblade"->TWINBLADE;
            case "claymore"->GREATSWORD;case "hammer"->HAMMER;case "greathammer"->GREAT_HAMMER;
            case "axe"->AXE;case "greataxe"->GREAT_AXE;case "spear"->SPEAR;case "glaive","halberd"->HALBERD;
            case "scythe"->REAPER;case "shield"->SHIELD;default->null;
        };
    }
}
