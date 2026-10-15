package dev.eldensteve.combat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.UseAction;
public final class Consumption {
    public static boolean active(LivingEntity e){if(!e.isUsingItem())return false;var action=e.getActiveItem().getUseAction();return action==UseAction.EAT||action==UseAction.DRINK;}
}
