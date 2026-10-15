package dev.eldensteve.mixin;
import dev.eldensteve.EldenSteveClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
public class ReticleMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void eldenflow$reticle(DrawContext context,RenderTickCounter counter,CallbackInfo ci){if(EldenSteveClient.enabled)ci.cancel();}
}
