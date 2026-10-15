package dev.eldensteve;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;

/** A rest session owns no entity/item: closing it cannot duplicate or lose equipment. */
public final class Bonfire {
    private record Rest(UUID player,net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension,BlockPos fire,Vec3d seat){}
    private static volatile Rest rest;
    private static ItemStack displayed=ItemStack.EMPTY;
    private static float amount,previousAmount,lastYaw;
    private static boolean confirmed;
    private static int awaiting;
    private static Vec3d seatOffset=Vec3d.ZERO;
    private static BlockPos slabSeat;
    public static boolean raisedSeat(){return slabSeat!=null;}
    public static Vec3d seatOffset(float delta){return seatOffset.multiply(poseAmount(delta));}
    public static float poseAmount(float delta){float t=MathHelper.lerp(delta,previousAmount,amount);return t*t*(3-2*t);}
    public static boolean posing(){return active()||amount>0||previousAmount>0;}
    public static boolean active(){return rest!=null;}
    public static void stop(){if(rest!=null)ProgressionClient.intent(3,0,BlockPos.ORIGIN);rest=null;displayed=ItemStack.EMPTY;confirmed=false;awaiting=0;}
    public static void reset(){stop();amount=previousAmount=0;slabSeat=null;seatOffset=Vec3d.ZERO;}
    public static void serverResting(boolean resting,boolean raised){if(rest!=null){if(resting){
        var c=MinecraftClient.getInstance();if(c.player!=null&&!confirmed){rest=new Rest(rest.player,rest.dimension,rest.fire,c.player.getPos());slabSeat=raised?c.player.getBlockPos().down():null;seatOffset=raised?new Vec3d(0,-.5*c.player.getScale()*.9375,0):Vec3d.ZERO;}
        confirmed=true;}else if(confirmed)stop();}}
    private static void findSeat(MinecraftClient c,BlockPos fire){
        slabSeat=null;seatOffset=Vec3d.ZERO;double best=1.8*1.8;var base=c.player.getBlockPos();
        for(var pos:BlockPos.iterate(base.add(-2,-1,-2),base.add(2,1,2))){
            var state=c.world.getBlockState(pos);if(!(state.getBlock() instanceof SlabBlock)||!c.world.getBlockState(pos.up()).getCollisionShape(c.world,pos.up()).isEmpty())continue;
            var shape=state.getCollisionShape(c.world,pos);if(shape.isEmpty())continue;
            double top=pos.getY()+shape.getMax(Direction.Axis.Y),height=top-c.player.getY();
            if(height<-.1||height>1.05||pos.getSquaredDistance(fire)>16)continue;
            double dx=pos.getX()+.5-c.player.getX(),dz=pos.getZ()+.5-c.player.getZ(),distance=dx*dx+dz*dz;
            if(distance<best){best=distance;slabSeat=pos.toImmutable();seatOffset=new Vec3d(dx,height-.5*c.player.getScale()*.9375,dz);}
        }
    }
    private static boolean lit(net.minecraft.world.World world,BlockPos pos){var state=world.getBlockState(pos);return state.getBlock() instanceof CampfireBlock&&state.get(CampfireBlock.LIT);}
    public static boolean interact(BlockPos pos){
        var c=MinecraftClient.getInstance();if(c.player==null||c.world==null||!EldenSteveClient.enabled||!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(dev.eldensteve.progression.ProgressionPackets.Intent.ID)||!lit(c.world,pos)||c.player.squaredDistanceTo(Vec3d.ofCenter(pos))>16)return false;
        if(rest==null){
            if(!c.player.isOnGround()||c.player.isTouchingWater()||c.player.hasVehicle())return false;
            // Rest beside the fire, never in its damaging central block.
            if(c.player.getBoundingBox().intersects(new Box(pos)))return false;
            EldenSteveClient.restReset();
            slabSeat=null;seatOffset=Vec3d.ZERO;
            rest=new Rest(c.player.getUuid(),c.world.getRegistryKey(),pos.toImmutable(),c.player.getPos());
            ProgressionClient.intent(2,0,pos);
            displayed=c.player.getMainHandStack().copy();confirmed=false;awaiting=0;lastYaw=yaw();Combat.reset();ShieldCombat.reset();
        }
        c.setScreen(new BonfireScreen());return true;
    }
    public static boolean tick(MinecraftClient c){
        if(!c.isPaused()){previousAmount=amount;amount=Math.clamp(amount+(active()?1f/12:-1f/10),0,1);}
        if(!posing()){slabSeat=null;seatOffset=Vec3d.ZERO;}
        var r=rest;if(r==null)return false;
        if(!confirmed&&!c.isPaused()&&++awaiting>60){stop();if(c.currentScreen instanceof BonfireScreen)c.setScreen(null);return false;}

        if(c.player==null||c.world==null||!c.world.getRegistryKey().equals(r.dimension)||!c.player.isAlive()||!lit(c.world,r.fire)||confirmed&&c.player.squaredDistanceTo(r.seat)>1||c.player.hurtTime>0){stop();if(c.currentScreen instanceof BonfireScreen)c.setScreen(null);return false;}
        if(c.currentScreen==null&&(Gamepad.jump||Gamepad.roll||Math.abs(Gamepad.right)+Math.abs(Gamepad.forward)>.15||EldenInput.down(c.options.jumpKey)||EldenInput.down(c.options.forwardKey)||EldenInput.down(c.options.backKey)||EldenInput.down(c.options.leftKey)||EldenInput.down(c.options.rightKey))){stop();return false;}
        c.player.setVelocity(0,c.player.getVelocity().y,0);c.player.setSprinting(false);
        try{if(!c.isPaused())SteveAnimations.tick(0,0,false,0,true);}catch(Exception e){stop();EldenSteveClient.LOG.error("Rest pose",e);return false;}
        return true;
    }
    public static float yaw(){var c=MinecraftClient.getInstance();var r=rest;if(r==null||c.player==null)return lastYaw;var d=Vec3d.ofCenter(r.fire).subtract(c.player.getPos().add(seatOffset));return (float)Math.atan2(d.x,d.z);}
    public static void spawn(){if(active())ProgressionClient.intent(7,0,BlockPos.ORIGIN);}
    public static void dawn(){if(active())ProgressionClient.intent(8,0,BlockPos.ORIGIN);}
    public static void register(){

        UseBlockCallback.EVENT.register((player,world,hand,hit)->world.isClient&&hand==Hand.MAIN_HAND&&interact(hit.getBlockPos())?ActionResult.SUCCESS:ActionResult.PASS);
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
            var c=MinecraftClient.getInstance();if(ctx.matrixStack()==null||ctx.consumers()==null||c.world==null)return;
            var r=rest;if(r!=null&&!displayed.isEmpty())renderWeapon(ctx,c.player,r.fire,displayed);
            for(var player:c.world.getPlayers()){
                if(player==c.player)continue;var view=RemoteCombat.get(player.getId());
                if(view!=null&&view.frame.state()[3]==7&&view.restVisual!=null&&!view.restVisual.stack().isEmpty())renderWeapon(ctx,player,view.restVisual.fire(),view.restVisual.stack());
            }
        });
    }
    private static void renderWeapon(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx,net.minecraft.client.network.AbstractClientPlayerEntity player,BlockPos fire,ItemStack stack){
        var m=ctx.matrixStack();var cam=ctx.camera().getPos();m.push();m.translate(fire.getX()+.5-cam.x,fire.getY()+.7-cam.y,fire.getZ()+.5-cam.z);
        var mount=WeaponMount.of(player,stack,false,false);
        var axis=mount==null?new org.joml.Vector3f(1,1,0).normalize():new org.joml.Vector3f(mount.axis());
        m.multiply(new org.joml.Quaternionf().rotationTo(axis,new org.joml.Vector3f(0,-1,0)));
        HeldWeaponRenderer.render(player,stack,false,false,new WeaponConfig.Entry(),m,ctx.consumers(),0xF000F0,OverlayTexture.DEFAULT_UV);m.pop();
    }
}
