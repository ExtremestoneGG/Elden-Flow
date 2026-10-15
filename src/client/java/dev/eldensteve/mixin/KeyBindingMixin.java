package dev.eldensteve.mixin;

import dev.eldensteve.EldenInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class KeyBindingMixin {
    @Shadow private int timesPressed;
    @Shadow @Final private static Map<InputUtil.Key,KeyBinding> KEY_TO_BINDINGS;
    @Inject(method="onKeyPressed",at=@At("TAIL"))
    private static void eldensteve$sharedPress(InputUtil.Key input,CallbackInfo ci){
        var c=MinecraftClient.getInstance();if(c.options==null)return;
        var selected=KEY_TO_BINDINGS.get(input);
        if(selected==null||!selected.getCategory().equals("category.eldensteve"))return;
        for(var binding:c.options.allKeys)
            if(binding!=selected&&!binding.getCategory().equals("category.eldensteve")&&binding.equals(selected))
                ((KeyBindingMixin)(Object)binding).timesPressed++;
    }
    @Inject(method="isPressed",at=@At("HEAD"),cancellable=true)
    private void eldensteve$held(CallbackInfoReturnable<Boolean> ci) {
        if(EldenInput.active()&&dev.eldensteve.Gamepad.use&&(Object)this==MinecraftClient.getInstance().options.useKey){ci.setReturnValue(true);return;}
        if(EldenInput.blocks((KeyBinding)(Object)this))ci.setReturnValue(false);
    }
    @Inject(method="wasPressed",at=@At("HEAD"),cancellable=true)
    private void eldensteve$press(CallbackInfoReturnable<Boolean> ci) {
        if(EldenInput.blocks((KeyBinding)(Object)this)){timesPressed=0;ci.setReturnValue(false);}
    }
    // Vanilla's map keeps only one binding per key. Deliver hold/release to any
    // overlapping vanilla binding too, so F6-off restores crouch/sprint correctly.
    @Inject(method="setKeyPressed",at=@At("TAIL"))
    private static void eldensteve$sharedKey(InputUtil.Key input,boolean pressed,CallbackInfo ci) {
        var c=MinecraftClient.getInstance();if(c.options==null)return;
        var selected=KEY_TO_BINDINGS.get(input);
        if(selected==null || !selected.getCategory().equals("category.eldensteve"))return;
        for(var binding:c.options.allKeys) {
            if(binding!=selected && !binding.getCategory().equals("category.eldensteve") && binding.equals(selected))
                binding.setPressed(pressed);
        }
    }
}
