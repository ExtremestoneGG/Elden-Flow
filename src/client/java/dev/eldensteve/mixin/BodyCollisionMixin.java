package dev.eldensteve.mixin;
import dev.eldensteve.*;
import net.minecraft.entity.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class BodyCollisionMixin {
    @Unique private double eldensteve$beforeY;
    @Unique private boolean eldensteve$wasGrounded;
    @Inject(method="adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",at=@At("RETURN"),cancellable=true)
    private void eldensteve$bodies(Vec3d desired,CallbackInfoReturnable<Vec3d> ci){var e=(Entity)(Object)this;ci.setReturnValue(StepDown.adjust(e,BodyCollision.clip(e,ci.getReturnValue())));}
    @Inject(method="move",at=@At("HEAD"))
    private void eldensteve$before(MovementType type,Vec3d movement,CallbackInfo ci){var e=(Entity)(Object)this;eldensteve$beforeY=e.getY();eldensteve$wasGrounded=e.isOnGround();}
    @Inject(method="move",at=@At("RETURN"))
    private void eldensteve$after(MovementType type,Vec3d movement,CallbackInfo ci){
        var e=(Entity)(Object)this;
        if(e instanceof LivingEntity living&&EldenSteveClient.drives(living)&&EldenSteveClient.state[3]==0&&movement.y<=0){
            StepSmoothing.stepped(e.getY()-eldensteve$beforeY);
            if(eldensteve$wasGrounded&&e.isOnGround())StepSmoothing.descended(eldensteve$beforeY-e.getY());
        }
    }
}
