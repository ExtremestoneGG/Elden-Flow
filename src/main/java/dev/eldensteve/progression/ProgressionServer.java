package dev.eldensteve.progression;

import java.util.*;
import java.io.IOException;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.LoggerFactory;

/** No MinecraftClient access: same service on integrated and dedicated servers. */
public final class ProgressionServer {
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("eldenflow-progression");
    private static final Identifier VITALITY=Identifier.of("eldensteve","vitality");
    public record Rest(RegistryKey<World> dimension,BlockPos fire,Vec3d seat,boolean raised,int since){}
    public static Rest rest(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());var r=s==null?null:s.rest.get(p.getUuid());return valid(p,r)?r:null;}
    private static final class Session {
        final ProgressionStore store;
        final Map<UUID,Integer> legacyStamina=new HashMap<>();
        final Set<UUID> enabled=new HashSet<>();
        final Map<UUID,Rest> rest=new HashMap<>();
        final Map<UUID,Integer> lastIntent=new HashMap<>();
        Session(MinecraftServer s)throws IOException{store=new ProgressionStore(s.getSavePath(WorldSavePath.ROOT).resolve("eldenflow-progression.json"));
            var legacy=s.getSavePath(WorldSavePath.ROOT).resolve("eldenflow-stamina.json");
            if(java.nio.file.Files.isRegularFile(legacy))try{var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(legacy)).getAsJsonObject();for(var entry:json.entrySet())legacyStamina.put(UUID.fromString(entry.getKey()),Math.clamp(entry.getValue().getAsInt(),0,10000));}catch(RuntimeException e){LOG.warn("Legacy stamina could not be read; original file preserved",e);}
        }
    }
    private static final Map<MinecraftServer,Session> SESSIONS=new IdentityHashMap<>();
    public static void start(MinecraftServer s){try{SESSIONS.put(s,new Session(s));}catch(IOException e){LOG.error("Progression disabled: cannot load data safely",e);}}
    public static void stop(MinecraftServer s){var session=SESSIONS.remove(s);if(session!=null)try{session.store.flush();}catch(IOException e){LOG.error("Could not save progression",e);}}
    public static void leave(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());if(s!=null){s.enabled.remove(p.getUuid());s.rest.remove(p.getUuid());s.lastIntent.remove(p.getUuid());}}
    public static boolean enabled(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());return s!=null&&s.enabled.contains(p.getUuid());}
    public static int legacyStamina(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());return s==null?0:s.legacyStamina.getOrDefault(p.getUuid(),0);}
    public static float maximumStamina(ServerPlayerEntity p){return Math.min(10097,legacyStamina(p)+ProgressionRules.maximumStamina(view(p).level(Attribute.ENDURANCE)));}
    public static boolean resting(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());return s!=null&&valid(p,s.rest.get(p.getUuid()));}
    public static PlayerProgress view(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());return s==null?new PlayerProgress():s.store.view(p.getUuid());}
    private static boolean valid(ServerPlayerEntity p,Rest r){
        if(r==null||!p.isAlive()||p.isSpectator()||!p.getWorld().getRegistryKey().equals(r.dimension)||p.squaredDistanceTo(r.seat)>1||p.squaredDistanceTo(Vec3d.ofCenter(r.fire))>16||p.hurtTime>0)return false;
        if(p.isTouchingWater()||p.hasVehicle())return false;
        if(r.raised){
            var support=BlockPos.ofFloored(r.seat.add(0,-.01,0));var state=p.getWorld().getBlockState(support);
            if(!(state.getBlock() instanceof net.minecraft.block.SlabBlock))return false;
            var shape=state.getCollisionShape(p.getWorld(),support);
            if(shape.isEmpty()||Math.abs(support.getY()+shape.getMax(net.minecraft.util.math.Direction.Axis.Y)-r.seat.y)>.02)return false;
        }
        var block=p.getWorld().getBlockState(r.fire);return block.getBlock() instanceof CampfireBlock&&block.get(CampfireBlock.LIT);
    }
    public static void sync(ServerPlayerEntity p){var s=SESSIONS.get(p.getServer());if(s!=null&&ServerPlayNetworking.canSend(p,ProgressionPackets.Snapshot.ID))ServerPlayNetworking.send(p,new ProgressionPackets.Snapshot(s.store.view(p.getUuid()),valid(p,s.rest.get(p.getUuid())),legacyStamina(p),rest(p)!=null&&rest(p).raised()));}
    public static boolean earn(ServerPlayerEntity p,long amount){var s=SESSIONS.get(p.getServer());if(s==null)return false;if(p.interactionManager.getGameMode()!=net.minecraft.world.GameMode.CREATIVE)s.store.earn(p.getUuid(),amount);sync(p);return true;}
    public static void handle(ServerPlayerEntity p,ProgressionPackets.Intent packet){
        var s=SESSIONS.get(p.getServer());if(s==null||!p.isAlive()||p.isSpectator())return;
        var id=p.getUuid();int action=packet.action();
        // Expensive writes are rate-limited; mode/rest cancellation is always processed.
        if(action>=4){int tick=p.getServer().getTicks();if(tick-s.lastIntent.getOrDefault(id,-100)<4)return;s.lastIntent.put(id,tick);}
        if(action==6){sync(p);return;}
        if(action==0){s.enabled.remove(id);s.rest.remove(id);applyHealth(p);sync(p);return;}
        if(action==1){if(p.isTouchingWater())return;s.enabled.add(id);applyHealth(p);sync(p);return;}
        if(action==3){s.rest.remove(id);sync(p);return;}
        if(!s.enabled.contains(id))return;
        if(action==2){var r=new Rest(p.getWorld().getRegistryKey(),packet.fire().toImmutable(),p.getPos(),false,p.getServer().getTicks());
            if(p.isOnGround()&&!p.isTouchingWater()&&!p.hasVehicle()&&valid(p,r)){
                var seat=CampfireSeat.find(p,packet.fire());p.requestTeleport(seat.position().x,seat.position().y,seat.position().z);
                s.rest.put(id,new Rest(r.dimension,r.fire,seat.position(),seat.raised(),r.since));repel(p,s.rest.get(id));
            }sync(p);return;
        }
        if(!valid(p,s.rest.get(id))){s.rest.remove(id);sync(p);return;}
        if(action==7){
            var r=s.rest.get(id);var pos=BlockPos.ofFloored(r.seat);var world=p.getServerWorld();
            if(world.isSpaceEmpty(p,new net.minecraft.util.math.Box(pos).stretch(0,1,0))&&world.getBlockState(pos.down()).isSolidBlock(world,pos.down())){
                p.setSpawnPoint(r.dimension,pos,p.getYaw(),true,true);p.sendMessage(Text.literal("Renascimento salvo ao lado da fogueira."),false);
            }else p.sendMessage(Text.literal("Escolha um lugar livre e firme ao lado da fogueira."),false);
            return;
        }
        if(action==8){
            var world=p.getServerWorld();if(!world.getRegistryKey().equals(World.OVERWORLD)){p.sendMessage(Text.literal("Nao ha amanhecer nesta dimensao."),false);return;}
            long time=world.getTimeOfDay();if(Math.floorMod(time,24000)>=12000)world.setTimeOfDay((Math.floorDiv(time,24000)+1)*24000);
            return;
        }
        try{
            if(action==4&&packet.argument()>=0&&packet.argument()<Attribute.values().length){if(p.interactionManager.getGameMode()==net.minecraft.world.GameMode.CREATIVE)s.store.buyCreative(id,Attribute.values()[packet.argument()]);else s.store.buy(id,Attribute.values()[packet.argument()]);}
            if(action==5){
                int star=-1,sculk=-1;var inv=p.getInventory();
                for(int i=0;i<inv.size();i++){if(inv.getStack(i).isOf(Items.NETHER_STAR))star=i;if(inv.getStack(i).isOf(Items.SCULK_CATALYST))sculk=i;}
                if(star>=0&&sculk>=0&&s.store.view(id).allocated()>0){s.store.respec(id);inv.getStack(star).decrement(1);inv.getStack(sculk).decrement(1);inv.markDirty();}
            }
            applyHealth(p);sync(p);
        }catch(IOException e){LOG.error("Purchase not saved; balance unchanged",e);p.sendMessage(Text.literal("Nao foi possivel salvar os atributos. Tente novamente."),false);}
    }
    private static void applyHealth(ServerPlayerEntity p){
        var a=p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);if(a==null)return;
        double bonus=enabled(p)?ProgressionRules.extraHealth(view(p).level(Attribute.VITALITY)):0;
        var existing=a.getModifier(VITALITY);if(existing!=null&&existing.value()==bonus)return;
        a.removeModifier(VITALITY);if(bonus>0)a.addTemporaryModifier(new EntityAttributeModifier(VITALITY,bonus,EntityAttributeModifier.Operation.ADD_VALUE));
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    public static void tick(MinecraftServer server){
        var s=SESSIONS.get(server);if(s==null)return;
        for(var p:server.getPlayerManager().getPlayerList()){
            if(p.isTouchingWater()&&s.enabled.remove(p.getUuid())){s.rest.remove(p.getUuid());applyHealth(p);sync(p);}
            var r=s.rest.get(p.getUuid());if(r!=null){
                if(!valid(p,r)){s.rest.remove(p.getUuid());sync(p);}
                else{
                    if(server.getTicks()%20==0)p.heal(2);
                    if(server.getTicks()%5==0)repel(p,r);
                }
            }
            if(server.getTicks()%20==0)applyHealth(p);
        }
        if(server.getTicks()%100==0)try{s.store.flush();}catch(IOException e){LOG.error("Autosave failed; keeping dirty state for retry",e);}
    }
    /** Active rest creates a wide exclusion zone; normal despawning remains vanilla-owned. */
    public static boolean repelMob(net.minecraft.entity.mob.MobEntity mob){
        if(!(mob.getWorld() instanceof net.minecraft.server.world.ServerWorld world))return false;
        var session=SESSIONS.get(world.getServer());if(session==null)return false;
        for(var player:world.getPlayers()){
            var rest=session.rest.get(player.getUuid());if(!valid(player,rest))continue;
            var center=Vec3d.ofCenter(rest.fire);var offset=mob.getPos().subtract(center).multiply(1,0,1);
            double distance=offset.length();if(distance>=64||Math.abs(mob.getY()-center.y)>24)continue;
            if(mob instanceof net.minecraft.entity.mob.Angerable angry)angry.stopAnger();
            mob.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET);
            mob.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT);
            mob.getBrain().forget(net.minecraft.entity.ai.brain.MemoryModuleType.HURT_BY_ENTITY);
            mob.setTarget(null);mob.setAttacker(null);mob.getNavigation().stop();
            var away=distance<.01?new Vec3d(1,0,0):offset.multiply(1/distance);
            mob.setVelocity(away.x*.65,mob.horizontalCollision?.35:mob.getVelocity().y,away.z*.65);mob.velocityModified=true;
            return true;
        }return false;
    }
    private static void repel(ServerPlayerEntity player,Rest rest){
        var world=player.getServerWorld();
        for(var mob:world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,new net.minecraft.util.math.Box(rest.fire).expand(64),e->e.isAlive()))repelMob(mob);
        // Already airborne hostile shots cannot interrupt the newly started rest.
        for(var shot:world.getEntitiesByClass(net.minecraft.entity.projectile.ProjectileEntity.class,new net.minecraft.util.math.Box(rest.fire).expand(32),e->e.getOwner() instanceof net.minecraft.entity.mob.MobEntity))shot.discard();
    }
}
