package dev.eldensteve.mixin;
import dev.eldensteve.progression.ProgressionServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Vanilla entity attack packets cannot add extra hits between authored windows. */
@Mixin(PlayerEntity.class)
public class ServerAttackMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void attack(Entity target,CallbackInfo ci){if((Object)this instanceof ServerPlayerEntity p&&ProgressionServer.enabled(p))ci.cancel();}
}
