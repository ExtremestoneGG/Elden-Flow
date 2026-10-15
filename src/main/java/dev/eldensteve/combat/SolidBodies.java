package dev.eldensteve.combat;
import dev.eldensteve.progression.ProgressionServer;
import net.minecraft.entity.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;
/** Shared swept body collision; server queries all active players independently. */
public final class SolidBodies {
    public static Vec3d clip(Entity entity,Vec3d movement,boolean controlled){
        if(!(entity instanceof LivingEntity)||entity.noClip||entity.isSpectator())return movement;
        var box=entity.getBoundingBox();
        var bodies=entity.getWorld().getEntitiesByClass(LivingEntity.class,box.stretch(movement).expand(.16),
            e->e!=entity&&e.isAlive()&&!e.isSpectator()&&!e.noClip&&!e.isConnectedThroughVehicle(entity)&&(controlled||e instanceof ServerPlayerEntity p&&ProgressionServer.enabled(p)));
        if(bodies.isEmpty())return movement;
        return resolve(box,movement,bodies.stream().map(e->e.getBoundingBox().expand(.10,0,.10)).toList());
    }
    public static Vec3d resolve(Box box,Vec3d movement,java.util.List<Box> bodies){
        var shapes=bodies.stream().map(VoxelShapes::cuboid).toList();
        // Keep the whole vertical sweep solid: a one-block auto-step must not climb through a mob.
        var swept=box.stretch(0,movement.y,0);
        double mx=movement.x,mz=movement.z;
        for(var body:bodies)if(swept.intersects(body)){
            double left=swept.maxX-body.minX,right=body.maxX-swept.minX;
            double back=swept.maxZ-body.minZ,front=body.maxZ-swept.minZ;
            // Existing shallow overlaps must permit retreat, but never a deeper lunge.
            if(Math.min(left,right)<=Math.min(back,front)){
                if(left<right&&mx>0||right<left&&mx<0)mx=0;
            }else if(back<front&&mz>0||front<back&&mz<0)mz=0;
        }
        double x=VoxelShapes.calculateMaxOffset(Direction.Axis.X,swept,shapes,mx);
        double z=VoxelShapes.calculateMaxOffset(Direction.Axis.Z,swept.offset(x,0,0),shapes,mz);
        return new Vec3d(x,movement.y,z);
    }
}
