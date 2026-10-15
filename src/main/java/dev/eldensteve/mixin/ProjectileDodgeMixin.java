package dev.eldensteve.mixin;
import dev.eldensteve.combat.ServerCombat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileEntity.class)
public class ProjectileDodgeMixin {
    @Inject(method="canHit(Lnet/minecraft/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
    private void eldensteve$passThroughRoll(Entity candidate,CallbackInfoReturnable<Boolean> ci){
        if(ServerCombat.projectilePasses(candidate,(ProjectileEntity)(Object)this))ci.setReturnValue(false);
    }
}
