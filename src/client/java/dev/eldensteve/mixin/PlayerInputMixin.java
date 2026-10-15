package dev.eldensteve.mixin;
import dev.eldensteve.EldenSteveClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayerEntity.class)
public class PlayerInputMixin {
    @Shadow private int ticksToNextAutojump;
    @Inject(method="tickMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/input/Input;tick(ZF)V",shift=At.Shift.AFTER))
    private void eldensteve$ownMovement(CallbackInfo ci){
        if(!EldenSteveClient.drive&&!dev.eldensteve.Bonfire.active())return;
        ticksToNextAutojump=0;
        var p=(ClientPlayerEntity)(Object)this;var input=p.input;
        input.movementForward=0;input.movementSideways=0;
        input.pressingForward=input.pressingBack=input.pressingLeft=input.pressingRight=false;
        input.jumping=input.sneaking=false;p.setSprinting(false);
    }
}
