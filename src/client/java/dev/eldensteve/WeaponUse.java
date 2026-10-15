package dev.eldensteve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;

/** Dispatch the item's own use callback, allowing other mods to own their abilities. */
public final class WeaponUse {
    private static boolean held,using;
    public static void tick(MinecraftClient c){
        boolean now=Combat.intercept()&&!Combat.modifier()&&!ShieldCombat.canParry()
            &&(Gamepad.skillHeld||EldenInput.down(EldenSteveClient.skill)||!Combat.twoHand&&(Gamepad.left||EldenInput.down(c.options.useKey)));
        if(now&&!held&&c.interactionManager!=null&&!c.player.isUsingItem()){
            var hand=Combat.twoHandLeft?Hand.OFF_HAND:Hand.MAIN_HAND;
            c.interactionManager.interactItem(c.player,hand);
            using=c.player.isUsingItem();
        }
        if(!now&&using&&c.player!=null&&c.interactionManager!=null)c.interactionManager.stopUsingItem(c.player);
        if(!now)using=false;
        held=now;
    }
}
