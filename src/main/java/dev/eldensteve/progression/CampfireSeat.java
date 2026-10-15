package dev.eldensteve.progression;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
public record CampfireSeat(Vec3d position,boolean raised) {
    public static CampfireSeat find(PlayerEntity p,BlockPos fire){
        var world=p.getWorld();var base=p.getBlockPos();var best=new CampfireSeat(p.getPos(),false);double nearest=1.8*1.8;
        for(var pos:BlockPos.iterate(base.add(-2,-1,-2),base.add(2,1,2))){
            var state=world.getBlockState(pos);if(!(state.getBlock() instanceof SlabBlock)||pos.getSquaredDistance(fire)>16)continue;
            var shape=state.getCollisionShape(world,pos);if(shape.isEmpty())continue;
            double top=pos.getY()+shape.getMax(Direction.Axis.Y),height=top-p.getY();if(height<-.1||height>1.05)continue;
            var seat=new Vec3d(pos.getX()+.5,top,pos.getZ()+.5);double distance=seat.subtract(p.getPos()).horizontalLengthSquared();if(distance>=nearest)continue;
            var box=p.getBoundingBox().offset(seat.subtract(p.getPos()));if(!world.isSpaceEmpty(p,box)||box.intersects(new Box(fire)))continue;
            var trace=world.raycast(new RaycastContext(p.getEyePos(),seat.add(0,p.getStandingEyeHeight(),0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));if(trace.getType()==HitResult.Type.BLOCK)continue;
            nearest=distance;best=new CampfireSeat(seat,true);
        }
        return best;
    }
}
