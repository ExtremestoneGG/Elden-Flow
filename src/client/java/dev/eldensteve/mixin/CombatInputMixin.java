package dev.eldensteve.mixin;
import dev.eldensteve.Combat;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MinecraftClient.class)
public class CombatInputMixin {
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true)
    private void eldensteve$attack(CallbackInfoReturnable<Boolean> ci){if(Combat.intercept()){Combat.queue();ci.setReturnValue(false);}}
    @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true)
    private void eldensteve$heavy(CallbackInfo ci){if(Combat.intercept()&&!dev.eldensteve.Gamepad.use)ci.cancel();}
    @Inject(method="handleBlockBreaking",at=@At("HEAD"),cancellable=true)
    private void eldensteve$break(boolean breaking,CallbackInfo ci){if(Combat.intercept())ci.cancel();}
}
