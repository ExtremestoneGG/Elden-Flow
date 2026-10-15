package dev.eldensteve;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Type tags are selectors; item IDs are only used to populate the lab chest. */
public final class WeaponCatalog {
    public record Profile(String type,int nativeIndex,String style,float weight){}
    public static final List<Profile> ALL=List.of(
        new Profile("sai",0,"Daggers",1.5f),new Profile("cutlass",11,"Curved Swords",3.5f),
        new Profile("longsword",1,"Straight Swords",3.5f),new Profile("rapier",4,"Thrusting Swords",4.5f),
        new Profile("twinblade",13,"Twinblades",7),new Profile("katana",5,"Katanas",5.5f),
        new Profile("claymore",2,"Greatswords",9),new Profile("greathammer",14,"Great Hammers",8.5f),
        new Profile("greataxe",16,"Greataxes",13),new Profile("spear",8,"Spears",4),
        new Profile("glaive",9,"Halberds",8),new Profile("scythe",18,"Reapers",7.5f),
        new Profile("halberd",9,"Halberds",8));
    public static final Profile AXE=new Profile("axe",7,"Axes",4.5f),SHIELD=new Profile("shield",24,"Shield",3);
    public static final List<Profile> FAMILIES;
    private static final Map<Item,WeaponNames.Match> INDEX=new java.util.concurrent.ConcurrentHashMap<>();
    static {var all=new ArrayList<>(ALL);all.add(AXE);all.add(new Profile("hammer",6,"Hammers",4.5f));all.add(SHIELD);FAMILIES=List.copyOf(all);}
    public static Profile byType(String type){return FAMILIES.stream().filter(p->p.type.equals(type)).findFirst().orElse(null);}
    public static void invalidate(){INDEX.clear();}
    public static WeaponNames.Match detect(ItemStack stack){
        if(stack.getItem() instanceof ShieldItem||stack.getUseAction()==net.minecraft.util.UseAction.BLOCK)return new WeaponNames.Match("shield","Tipo / uso de escudo",100);
        return INDEX.computeIfAbsent(stack.getItem(),item->{
            var id=Registries.ITEM.getId(item);
            var match=WeaponNames.match(id.getPath()+" "+item.getTranslationKey());
            if(match.reason().startsWith("Classe excluida"))return match;
            if(match.type()!=null)return match;
            if(item instanceof SwordItem)return new WeaponNames.Match("longsword","Tipo SwordItem",100);
            if(item instanceof AxeItem)return new WeaponNames.Match("axe","Tipo AxeItem",100);
            return match;
        });
    }
    public static Profile get(ItemStack stack,String mode,boolean automatic){
        if(stack.isEmpty())return null;
        if(mode.equals("off"))return null;
        if(!mode.equals("auto"))return weighted(stack,byType(mode));
        var known=known(stack);if(known!=null)return weighted(stack,known);
        if(!automatic)return null;
        var id=Registries.ITEM.getId(stack.getItem());
        if(id.getNamespace().equals("simplyswords")&&(id.getPath().endsWith("_warglaive")||id.getPath().endsWith("_chakram")))return null;
        for(var profile:FAMILIES){
            if(stack.streamTags().anyMatch(t->{String path=t.id().getPath();return path.equals(profile.type)||path.equals(profile.type+"s")||path.endsWith("/"+profile.type+"s");}))return weighted(stack,profile);
        }
        return weighted(stack,byType(detect(stack).type()));
    }
    private static Profile weighted(ItemStack stack,Profile p){
        if(p==null)return null;
        var id=Registries.ITEM.getId(stack.getItem());if(!id.getNamespace().equals("minecraft"))return p;
        float material=id.getPath().startsWith("wooden_")?.65f:id.getPath().startsWith("stone_")?.9f:id.getPath().startsWith("golden_")?1.12f:id.getPath().startsWith("diamond_")?1.20f:id.getPath().startsWith("netherite_")?1.40f:1;
        return new Profile(p.type,p.nativeIndex,p.style,p.weight*material);
    }
    private static Profile known(ItemStack stack){
        if(stack.isOf(Items.IRON_SWORD))return ALL.get(2);
        var id=Registries.ITEM.getId(stack.getItem());if(!id.getNamespace().equals("simplyswords"))return null;
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplyswords")){
            var type=SimplySwordsProfile.type(stack);
            if(type!=null)return ALL.stream().filter(p->type.equals(Identifier.of("simplyswords",p.type))).findFirst().orElse(null);
        }
        for(var p:ALL){
            var tag=TagKey.of(RegistryKeys.ITEM,Identifier.of("simplyswords","implicit/"+p.type));
            if(stack.isIn(tag)||id.getPath().endsWith("_"+p.type))return p;
        }
        return null;
    }
    public static String load(Profile p){return p.weight<=4.5?"LEVE":p.weight<=8?"MEDIO":"PESADO";}
}
