package dev.eldensteve.mixin;
import dev.eldensteve.progression.*;
import net.minecraft.entity.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(LivingEntity.class)
public class SoulExperienceMixin {
    @WrapOperation(method="dropXp",at=@At(value="INVOKE",target="Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"))
    private void eldenflow$souls(ServerWorld world,Vec3d pos,int xp,Operation<Void> original,Entity attacker){
        if(attacker instanceof ServerPlayerEntity p&&ProgressionServer.enabled(p))SoulOrbEntity.spawn(world,pos,xp,p.getUuid());
        else original.call(world,pos,xp);
    }
}
