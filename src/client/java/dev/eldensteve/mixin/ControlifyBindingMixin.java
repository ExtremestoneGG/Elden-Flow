package dev.eldensteve.mixin;
import dev.eldensteve.Gamepad;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.isxander.controlify.bindings.InputBindingImpl",remap=false)
public abstract class ControlifyBindingMixin {
    @Shadow public abstract Identifier id();
    // Key emulation reads state history directly, bypassing digitalNow/justPressed.
    // Suppress before history/callbacks are updated so RT cannot also fire vanilla attack.
    @ModifyVariable(method="pushState",at=@At("STORE"),ordinal=0)
    private float eldensteve$state(float value){return Gamepad.suppress(id())?0:value;}
    @Inject(method={"digitalNow","digitalPrev","justPressed","justReleased","justTapped"},at=@At("HEAD"),cancellable=true)
    private void eldensteve$reserved(CallbackInfoReturnable<Boolean> ci){if(Gamepad.suppress(id()))ci.setReturnValue(false);}
}
