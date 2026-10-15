package dev.eldensteve;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
/** Read the mod's declared weapon type, not its item identifier. */
public final class SimplySwordsProfile {
    public static Identifier type(ItemStack stack){
        var data=stack.get(ComponentTypeRegistry.WEAPON_IMPLICIT.get());
        return data==null?null:data.weaponType();
    }
}
