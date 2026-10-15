package dev.eldensteve.mixin;

import dev.eldensteve.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CombatCameraMixin {
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void setPos(Vec3d pos);
    @Shadow protected abstract void moveBy(float forward,float up,float sideways);
    @Shadow private float clipToSpace(float distance){throw new AssertionError();}
    @Inject(method="update",at=@At("TAIL"))
    private void eldensteve$camera(BlockView world,Entity focus,boolean third,boolean inverse,float delta,CallbackInfo ci){
        var c=MinecraftClient.getInstance();
        if((!EldenSteveClient.enabled&&!CombatCamera.exiting())||focus!=c.player||!third||inverse){CombatCamera.reset();return;}
        CombatCamera.update(focus.getYaw(delta),focus.getPitch(delta),delta);
        setRotation(CombatCamera.yaw+CombatCamera.orbit(),CombatCamera.pitch*(float)Math.cos(Math.toRadians(CombatCamera.orbit())));
        // Raised pivot lets the player and the target stay visible; vanilla collision clips the boom.
        setPos(focus.getLerpedPos(delta).add(0,focus.getStandingEyeHeight()+CombatCamera.lift()+StepSmoothing.offset(delta),0));
        moveBy(-clipToSpace(CombatCamera.distance()),0,0);
        CombatCamera.finishTransition();
    }
}
