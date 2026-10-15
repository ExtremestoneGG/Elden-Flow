package dev.eldensteve;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import java.util.ArrayList;

/** Grounded locomotion follows a floor at most one block below the next position. */
public final class StepDown {
    public static double floorDrop(Box next,Iterable<VoxelShape> shapes){
        double drop=VoxelShapes.calculateMaxOffset(Direction.Axis.Y,next,shapes,-1.001);
        return drop<-.001&&drop>=-1.00001?drop:0;
    }
    public static Vec3d adjust(Entity e,Vec3d movement){
        if(!(e instanceof LivingEntity living)||!EldenSteveClient.drives(living)||!e.isOnGround()
            ||EldenSteveClient.state[3]!=0||movement.y>0||movement.horizontalLengthSquared()<1e-8)return movement;
        var next=e.getBoundingBox().offset(movement.x,0,movement.z);
        var swept=next.stretch(0,-1.001,0);var shapes=new ArrayList<VoxelShape>();
        e.getWorld().getBlockCollisions(e,swept).forEach(shapes::add);
        // Do not snap through a mob standing on the lower floor.
        for(var mob:e.getWorld().getEntitiesByClass(LivingEntity.class,swept,m->m!=e&&m.isAlive()&&!m.isSpectator()))
            shapes.add(VoxelShapes.cuboid(mob.getBoundingBox()));
        double drop=floorDrop(next,shapes);
        if(drop>=movement.y-.001)return movement;
        var candidate=new Vec3d(movement.x,drop,movement.z);
        if(!e.getWorld().isSpaceEmpty(e,e.getBoundingBox().offset(candidate)))return movement;
        var clipped=BodyCollision.clip(e,candidate);
        if(Math.abs(clipped.x-candidate.x)>1e-7||Math.abs(clipped.z-candidate.z)>1e-7)return movement;
        return candidate;
    }
}
