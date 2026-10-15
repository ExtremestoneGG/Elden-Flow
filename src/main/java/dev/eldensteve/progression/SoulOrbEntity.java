package dev.eldensteve.progression;

import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.EntityTrackerEntry;

/** Vanilla orb silhouette, distinct entity/reward. Never invokes vanilla XP pickup or merging. */
public final class SoulOrbEntity extends ExperienceOrbEntity {
    private static final TrackedData<Integer> VALUE=DataTracker.registerData(SoulOrbEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private UUID owner;
    private int lifetime;
    public SoulOrbEntity(EntityType<? extends ExperienceOrbEntity> type,World world){super(type,world);}
    protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(VALUE,1);}
    public static void spawn(ServerWorld world,Vec3d pos,int xp,UUID owner){
        int left=Math.max(0,xp);
        for(int count=0;left>0&&count<64;count++){
            int value=count==63?left:ExperienceOrbEntity.roundToOrbSize(left);left-=value;
            var orb=new SoulOrbEntity(dev.eldensteve.EldenFlow.SOUL_ORB,world);orb.owner=owner;orb.dataTracker.set(VALUE,value);
            orb.setPosition(pos);orb.setVelocity((world.random.nextDouble()-.5)*.2,world.random.nextDouble()*.2+.1,(world.random.nextDouble()-.5)*.2);world.spawnEntity(orb);
        }
    }
    @Override public int getExperienceAmount(){return dataTracker.get(VALUE);}
    @Override public int getOrbSize(){int v=getExperienceAmount();int[] cut={3,7,17,37,73,149,307,617,1237,2477};int i=0;while(i<cut.length&&v>=cut[i])i++;return i;}
    @Override public void tick(){
        baseTick();
        if(getWorld().isClient&&age%3==0){
            var color=(age/3+getId())%2==0?new org.joml.Vector3f(.25f,.6f,1f):new org.joml.Vector3f(.65f,.2f,1f);
            getWorld().addParticle(new net.minecraft.particle.DustParticleEffect(color,.65f),getX(),getY()+.15,getZ(),0,.025,0);
        }
        if(++lifetime>=6000){if(!getWorld().isClient)discard();return;}
        if(!getWorld().isClient){
            var p=owner==null?null:getWorld().getPlayerByUuid(owner);
            if(p!=null&&p.isAlive()&&!p.isSpectator()&&squaredDistanceTo(p)<64){var d=p.getPos().add(0,.5,0).subtract(getPos());double len=d.length();if(len>.05)setVelocity(getVelocity().add(d.multiply((1-len/8)*(1-len/8)*.1/len)));}
        }
        if(!hasNoGravity())setVelocity(getVelocity().add(0,isTouchingWater()?.005:-.03,0));
        move(MovementType.SELF,getVelocity());setVelocity(getVelocity().multiply(isOnGround()?.6:.98,.98,isOnGround()?.6:.98));
        if(isOnGround())setVelocity(getVelocity().multiply(1,-.6,1));
    }
    @Override public void onPlayerCollision(PlayerEntity player){
        if(!(player instanceof ServerPlayerEntity p)||!p.isAlive()||p.isSpectator()||owner!=null&&!owner.equals(p.getUuid())||isRemoved())return;
        if(!ProgressionServer.earn(p,ProgressionRules.soulsForExperience(getExperienceAmount())))return;
        p.sendPickup(this,1);getWorld().playSound(null,getX(),getY(),getZ(),net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,net.minecraft.sound.SoundCategory.PLAYERS,.2f,1.2f);discard();
    }
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);n.putInt("SoulXp",getExperienceAmount());n.putInt("SoulAge",lifetime);if(owner!=null)n.putUuid("SoulOwner",owner);}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);dataTracker.set(VALUE,Math.max(1,n.getInt("SoulXp")));lifetime=Math.clamp(n.getInt("SoulAge"),0,6000);owner=n.containsUuid("SoulOwner")?n.getUuid("SoulOwner"):null;}
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entry){return new EntitySpawnS2CPacket(this,entry);}
}
