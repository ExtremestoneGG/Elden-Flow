package dev.eldensteve;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;
public final class BodyCollision {
    public static Vec3d clip(Entity e,Vec3d movement){return e.getWorld().isClient&&e instanceof LivingEntity living&&EldenSteveClient.drives(living)?dev.eldensteve.combat.SolidBodies.clip(e,movement,true):movement;}
    public static Vec3d resolve(Box box,Vec3d movement,java.util.List<Box> bodies){return dev.eldensteve.combat.SolidBodies.resolve(box,movement,bodies);}
}
