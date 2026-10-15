package dev.eldensteve.mixin;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(KeyBinding.class)
public interface KeyBindingAccess {
    @Accessor("pressed") boolean eldensteve$rawPressed();
}
