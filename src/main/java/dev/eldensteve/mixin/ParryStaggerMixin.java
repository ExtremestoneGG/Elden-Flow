package dev.eldensteve.mixin;
import dev.eldensteve.combat.ServerCombat;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MobEntity.class)
public class ParryStaggerMixin {
    @Inject(method="tickNewAi",at=@At("HEAD"),cancellable=true)
    private void eldensteve$stagger(CallbackInfo ci){if(dev.eldensteve.progression.ProgressionServer.repelMob((MobEntity)(Object)this)||ServerCombat.staggered((MobEntity)(Object)this))ci.cancel();}
}
