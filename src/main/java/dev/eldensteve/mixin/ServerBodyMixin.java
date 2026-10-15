package dev.eldensteve.mixin;
import dev.eldensteve.combat.SolidBodies;
import dev.eldensteve.progression.ProgressionServer;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public class ServerBodyMixin {
    @Inject(method="adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",at=@At("RETURN"),cancellable=true)
    private void bodies(Vec3d desired,CallbackInfoReturnable<Vec3d> ci){var e=(Entity)(Object)this;if(!e.getWorld().isClient)ci.setReturnValue(SolidBodies.clip(e,ci.getReturnValue(),e instanceof ServerPlayerEntity p&&ProgressionServer.enabled(p)));}
}
