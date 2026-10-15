package dev.eldensteve;
import java.util.*;
import net.minecraft.item.ItemStack;

/** Client adapter; classification and material weight are shared with the server. */
public final class WeaponProfiles {
    public record Profile(String type,int nativeIndex,String style,float weight){}
    private static Profile adapt(WeaponCatalog.Profile p){return p==null?null:new Profile(p.type(),p.nativeIndex(),p.style(),p.weight());}
    public static final List<Profile> ALL=WeaponCatalog.ALL.stream().map(WeaponProfiles::adapt).toList();
    public static final Profile AXE=adapt(WeaponCatalog.AXE),SHIELD=adapt(WeaponCatalog.SHIELD);
    public static final List<Profile> FAMILIES=WeaponCatalog.FAMILIES.stream().map(WeaponProfiles::adapt).toList();
    public static Profile byType(String type){return adapt(WeaponCatalog.byType(type));}
    public static void invalidate(){WeaponCatalog.invalidate();}
    public static WeaponNames.Match detect(ItemStack stack){return WeaponCatalog.detect(stack);}
    public static Profile get(ItemStack stack){return get(stack,WeaponConfig.get(stack));}
    public static Profile get(ItemStack stack,WeaponConfig.Entry config){return adapt(WeaponCatalog.get(stack,config.mode,WeaponConfig.automatic()));}
    public static String load(Profile p){return p.weight<=4.5?"LEVE":p.weight<=8?"MEDIO":"PESADO";}
}
