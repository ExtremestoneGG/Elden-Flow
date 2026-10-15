package dev.eldensteve.mixin;
import dev.eldensteve.EldenSteveClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class TravelMixin {
    @Inject(method="getStepHeight",at=@At("RETURN"),cancellable=true)
    private void eldensteve$step(CallbackInfoReturnable<Float> ci){
        var e=(LivingEntity)(Object)this;
        if(EldenSteveClient.drives(e)&&e.isOnGround()&&EldenSteveClient.state[3]==0)
            ci.setReturnValue(1.0f);
    }
    @ModifyVariable(method="travel",at=@At("HEAD"),argsOnly=true)
    private Vec3d eldensteve$horizontalIntent(Vec3d input){return EldenSteveClient.travel((LivingEntity)(Object)this,input);}
}
