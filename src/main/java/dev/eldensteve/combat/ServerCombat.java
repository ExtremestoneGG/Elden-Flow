package dev.eldensteve.combat;
import dev.eldensteve.*;
import dev.eldensteve.progression.*;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.fabricmc.fabric.api.networking.v1.*;

/** One isolated simulation per player. Network messages cannot advance the simulation clock. */
public final class ServerCombat {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("eldenflow-server-combat");
    private static final class Player implements AutoCloseable {
        final NativeCore core;final InputMailbox mailbox=new InputMailbox();final LinkedHashSet<Long> hits=new LinkedHashSet<>();
        float[] state=new float[12];int defense,grip,guardTicks,parryTicks;float refundBudget;Object dimension;
        Player()throws Exception{core=new NativeCore();}
        public void close(){core.close();}
    }
    private static final Map<MinecraftServer,Map<UUID,Player>> sessions=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Set<UUID>> failed=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Map<UUID,Integer>> staggers=new IdentityHashMap<>(),staggerResistance=new IdentityHashMap<>();
    public static void stop(MinecraftServer server){var map=sessions.remove(server);if(map!=null)map.values().forEach(Player::close);failed.remove(server);staggers.remove(server);staggerResistance.remove(server);CombatEffects.stop(server);}
    public static void leave(ServerPlayerEntity p){CombatEffects.leave(p);var map=sessions.get(p.getServer());if(map!=null){var state=map.remove(p.getUuid());if(state!=null)state.close();}var failures=failed.get(p.getServer());if(failures!=null)failures.remove(p.getUuid());}
    public static WeaponCatalog.Profile profile(ServerPlayerEntity p,ItemStack stack){var rules=WeaponRulesServer.rules(p.getServer());return rules==null?null:WeaponCatalog.get(stack,rules.mode(Registries.ITEM.getId(stack.getItem()).toString()),rules.automatic());}
    public static void receive(ServerPlayerEntity p,CombatInput input){
        if(!input.valid()||!ProgressionServer.enabled(p)||!p.isAlive()||p.isSpectator()||WeaponRulesServer.rules(p.getServer())==null)return;
        var failures=failed.computeIfAbsent(p.getServer(),s->new HashSet<>());if(failures.contains(p.getUuid()))return;
        var map=sessions.computeIfAbsent(p.getServer(),s->new HashMap<>());
        try{var state=map.get(p.getUuid());if(state==null){state=new Player();state.dimension=p.getWorld().getRegistryKey();map.put(p.getUuid(),state);}state.mailbox.offer(input,p.getServer().getTicks());}
        catch(Exception e){failures.add(p.getUuid());LOG.error("Could not start player combat controller",e);p.sendMessage(net.minecraft.text.Text.literal("Controlador de combate indisponivel neste servidor."),false);}
    }
    public static void tick(MinecraftServer server){CombatEffects.tick(server);
        var resistant=staggerResistance.get(server);if(resistant!=null)resistant.entrySet().removeIf(e->e.getValue()<=server.getTicks());
        var stagger=staggers.get(server);if(stagger!=null)stagger.entrySet().removeIf(e->e.getValue()<=server.getTicks());
        var map=sessions.get(server);if(map==null)return;
        for(var iterator=map.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();var p=server.getPlayerManager().getPlayer(entry.getKey());var s=entry.getValue();
            if(p==null||!p.isAlive()||!ProgressionServer.enabled(p)||!p.getWorld().getRegistryKey().equals(s.dimension)||p.isTouchingWater()||p.isSpectator()){
                if(p!=null)send(p,new CombatPackets.Frame(p.getId(),0,-1,0,0,new float[12]));s.close();iterator.remove();continue;
            }
            try{step(p,s,server.getTicks());}catch(Exception e){LOG.error("Server controller stopped safely",e);s.close();iterator.remove();failed.computeIfAbsent(server,x->new HashSet<>()).add(p.getUuid());}
        }
    }
    private static double floor(ServerPlayerEntity p){if(p.isOnGround())return p.getY();var start=p.getPos().add(0,.01,0);var hit=p.getWorld().raycast(new RaycastContext(start,start.add(0,-16,0),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));return hit.getType()==HitResult.Type.BLOCK?Math.min(p.getY(),hit.getPos().y):p.getY()-16;}
    private static void step(ServerPlayerEntity p,Player s,int tick){
        var input=s.mailbox.take(tick);boolean supported=!ProgressionServer.resting(p)&&!s.mailbox.stale(tick)&&!p.hasVehicle()&&!p.isInLava()&&!p.isClimbing()&&!p.isFallFlying()&&!p.getAbilities().flying&&!p.isUsingItem();
        var main=profile(p,p.getMainHandStack());var off=profile(p,p.getOffHandStack());s.grip=input.grip();if(s.grip==1&&main==null||s.grip==2&&off==null)s.grip=0;
        var active=s.grip==2?off:main;boolean armed=active!=null;boolean paired=s.grip==0&&main!=null&&off!=null&&main.nativeIndex()!=24&&main.nativeIndex()==off.nativeIndex();
        var stats=ProgressionServer.view(p);s.core.equip(main==null?1:main.nativeIndex(),s.grip);s.core.left(off==null?24:off.nativeIndex());
        if(active!=null)s.core.weight(ProgressionRules.relativeWeight(active.weight(),stats.level(Attribute.CAPACITY)));s.core.capacity(ProgressionServer.maximumStamina(p));
        LivingEntity target=null;var entity=p.getWorld().getEntityById(input.target());if(entity instanceof LivingEntity living&&living!=p&&living.isAlive()&&p.squaredDistanceTo(living)<=256&&p.canSee(living))target=living;
        if(supported&&input.down(CombatInput.CROUCH))s.core.crouch();
        if(s.parryTicks>0)s.parryTicks--;
        var blockStack=blockingStack(p,s);var rules=WeaponRulesServer.rules(p.getServer());
        if(supported&&input.down(CombatInput.PARRY)&&s.parryTicks==0&&s.state[3]==0&&s.state[6]>=12&&!blockStack.isEmpty()&&rules!=null&&rules.parry(Registries.ITEM.getId(blockStack.getItem()).toString())){s.parryTicks=18;s.core.spend(12);}
        if(!supported)s.parryTicks=0;
        boolean busy=s.parryTicks>0;
        boolean attack=supported&&!busy&&armed&&input.down(CombatInput.LIGHT);
        float oldStamina=s.state[6];boolean wasAttacking=s.state[3]==4||s.state[3]==6;
        s.state=s.core.tick(busy?0:input.right(),busy?0:input.forward(),input.yaw(),supported&&input.down(CombatInput.ROLL),input.down(CombatInput.WALK),supported,supported&&input.down(CombatInput.SPRINT),attack&&!paired,armed&&!busy,supported&&!busy&&input.down(CombatInput.JUMP),(float)p.getY(),(float)floor(p),p.isOnGround(),target==null?0:(float)(target.getX()-p.getX()),target==null?0:(float)(target.getZ()-p.getZ()),target!=null,supported&&!busy&&armed&&input.down(CombatInput.HEAVY),attack&&paired||supported&&!busy&&armed&&input.down(CombatInput.GUARD));
        boolean attacking=s.state[3]==4||s.state[3]==6;
        if(!wasAttacking)s.refundBudget=0;if(attacking)s.refundBudget+=Math.max(0,oldStamina-s.state[6]);
        s.defense=supported?s.core.defense():0;
        s.guardTicks=supported&&!busy&&input.down(CombatInput.GUARD)&&s.state[3]==0&&!blockStack.isEmpty()?s.guardTicks+1:0;
        if(supported&&!busy&&armed)contacts(p,s);
        var rest=ProgressionServer.rest(p);if(rest!=null){s.core.adjustStamina(s.state[7]/40);s.state[6]=Math.min(s.state[7],s.state[6]+s.state[7]/40);}
        if(rest!=null&&tick%10==0){var visual=new CombatPackets.RestVisual(p.getId(),rest.fire(),p.getMainHandStack().copy());for(var other:PlayerLookup.tracking(p))if(ServerPlayNetworking.canSend(other,CombatPackets.RestVisual.ID))ServerPlayNetworking.send(other,visual);}
        var frame=s.state.clone();if(rest!=null){frame[0]=frame[1]=0;var toward=Vec3d.ofCenter(rest.fire()).subtract(p.getPos());frame[2]=(float)Math.atan2(toward.x,toward.z);frame[3]=7;frame[4]=Math.min(12,tick-rest.since());frame[5]=12;frame[10]=rest.raised()?1:0;}
        if(Consumption.active(p)){frame[3]=8;frame[4]=p.getItemUseTime();}
        if(busy){frame[3]=3;frame[4]=(18-s.parryTicks)*2.5f;frame[5]=45;}
        send(p,new CombatPackets.Frame(p.getId(),input.sequence(),rest!=null?0:Consumption.active(p)?(p.getItemUseTime()<10?50110:50111):busy?692040040:s.core.animationId(),s.grip,s.defense,frame));
    }
    private static void contacts(ServerPlayerEntity p,Player s){
        var hits=s.core.contacts();var origin=p.getPos();var world=p.getServerWorld();
        var candidates=world.getEntitiesByClass(LivingEntity.class,p.getBoundingBox().expand(3.5),e->e!=p&&e.isAlive()&&e.isAttackable()&&p.squaredDistanceTo(e)<=12.25);
        for(int i=0;i<(int)hits[0];i++){
            int at=1+i*10,id=(int)hits[at],flags=(int)hits[at+1];double radius=Math.clamp(hits[at+2],.03,.35);
            var a=origin.add(hits[at+3],hits[at+4],hits[at+5]);var b=origin.add(hits[at+6],hits[at+7],hits[at+8]);
            for(var victim:candidates){long key=((long)id<<32)|(victim.getId()&0xffffffffL);if(s.hits.contains(key))continue;
                var box=victim.getBoundingBox().expand(radius);var contact=box.contains(a)?Optional.of(a):box.raycast(a,b);if(contact.isEmpty())continue;
                var wall=world.raycast(new RaycastContext(p.getEyePos(),contact.get(),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));if(wall.getType()==HitResult.Type.BLOCK)continue;
                s.hits.add(key);while(s.hits.size()>256)s.hits.remove(s.hits.iterator().next());
                if(ServerDamage.hit(p,victim,flags,profile(p,(flags&1)!=0?p.getOffHandStack():p.getMainHandStack())))s.core.impact(flags);
            }
        }
    }
    private static void send(ServerPlayerEntity p,CombatPackets.Frame frame){if(ServerPlayNetworking.canSend(p,CombatPackets.Frame.ID))ServerPlayNetworking.send(p,frame);for(var other:PlayerLookup.tracking(p))if(ServerPlayNetworking.canSend(other,CombatPackets.Frame.ID))ServerPlayNetworking.send(other,frame);}
    public static int defense(ServerPlayerEntity p){var map=sessions.get(p.getServer());var state=map==null?null:map.get(p.getUuid());return state==null||state.mailbox.stale(p.getServer().getTicks())?0:state.defense;}

    private static Player state(ServerPlayerEntity p){var map=sessions.get(p.getServer());return map==null?null:map.get(p.getUuid());}
    private static ItemStack blockingStack(ServerPlayerEntity p,Player s){
        if(s.grip==2)return p.getOffHandStack();if(s.grip==1)return p.getMainHandStack();
        var off=profile(p,p.getOffHandStack());if(off!=null&&off.nativeIndex()==24)return p.getOffHandStack();
        var main=profile(p,p.getMainHandStack());return main!=null&&main.nativeIndex()==24?p.getMainHandStack():ItemStack.EMPTY;
    }
    public static void refundAttack(ServerPlayerEntity p){var s=state(p);if(s!=null&&s.refundBudget>0){s.core.adjustStamina(s.refundBudget);s.state[6]=Math.min(s.state[7],s.state[6]+s.refundBudget);s.refundBudget=0;}}
    public static void stagger(LivingEntity e,int ticks){
        var server=e.getServer();if(server==null||ticks<=0)return;int now=server.getTicks();
        var resistance=staggerResistance.computeIfAbsent(server,x->new HashMap<>());if(resistance.getOrDefault(e.getUuid(),0)>now)return;
        ticks=Math.clamp(ticks,1,30);staggers.computeIfAbsent(server,x->new HashMap<>()).put(e.getUuid(),now+ticks);resistance.put(e.getUuid(),now+ticks+20);
        var packet=new CombatPackets.Stagger(e.getId(),ticks);for(var p:PlayerLookup.tracking(e))if(ServerPlayNetworking.canSend(p,CombatPackets.Stagger.ID))ServerPlayNetworking.send(p,packet);
        if(e instanceof ServerPlayerEntity p&&ServerPlayNetworking.canSend(p,CombatPackets.Stagger.ID))ServerPlayNetworking.send(p,packet);
    }
    public static boolean staggered(Entity entity){if(entity.getWorld().isClient||entity.getServer()==null)return false;var map=staggers.get(entity.getServer());return map!=null&&map.getOrDefault(entity.getUuid(),0)>entity.getServer().getTicks();}
    public static boolean avoids(LivingEntity entity,net.minecraft.entity.damage.DamageSource source){
        if(!(entity instanceof ServerPlayerEntity p)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        var direct=source.getSource();var attacker=source.getAttacker();if(direct==null||attacker==null||attacker==p)return false;
        int flags=defense(p);return (flags&1)!=0||(flags&2)!=0&&direct.getBoundingBox().maxY<p.getY()+.45;
    }
    public static boolean projectilePasses(Entity entity,net.minecraft.entity.projectile.ProjectileEntity projectile){
        if(projectile.getOwner()==entity)return false;
        int flags=entity.getWorld().isClient?DefenseVisuals.get(entity.getUuid()):entity instanceof ServerPlayerEntity p?defense(p):0;
        return (flags&1)!=0;
    }
    public static float guardDamage(LivingEntity entity,net.minecraft.entity.damage.DamageSource source,float amount){
        if(!(entity instanceof ServerPlayerEntity p)||amount<=0||!Float.isFinite(amount)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_SHIELD)||source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        var s=state(p);if(s==null||s.mailbox.stale(p.getServer().getTicks())||!ProgressionServer.enabled(p))return amount;
        var origin=source.getPosition();if(origin==null)return amount;var incoming=origin.subtract(p.getPos()).multiply(1,0,1).normalize();
        var facing=new Vec3d(Math.sin(s.state[2]),0,Math.cos(s.state[2]));if(facing.dotProduct(incoming)<.17)return amount;
        var stack=blockingStack(p,s);if(stack.isEmpty())return amount;
        var attacker=source.getAttacker();float frame=(18-s.parryTicks)*2.5f;
        if(s.parryTicks>0&&frame>=2.5f&&frame<17.5f&&source.getSource()==attacker&&attacker instanceof LivingEntity living&&living!=p){
            stagger(living,(int)(Math.min(30,20*ProgressionRules.passivePower(ProgressionServer.view(p).level(Attribute.AFFINITY)))*(CombatEffects.boss(living)?.3:1)));
            living.takeKnockback(.25,p.getX()-living.getX(),p.getZ()-living.getZ());blockSound(p,1.5f);return 0;
        }
        if(s.guardTicks<3)return amount;
        float cost=Math.max(4,Math.min(1000000,amount*2));float available=s.state[6];s.core.spend(cost);s.state[6]=Math.max(0,available-cost);
        if(available<cost){s.guardTicks=0;return amount;}
        var blockedProfile=profile(p,stack);boolean shield=blockedProfile!=null&&blockedProfile.nativeIndex()==24;
        stack.damage(Math.max(1,(int)Math.min(1000,amount)),p,stack==p.getOffHandStack()?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND);blockSound(p,1);
        return shield?0:amount*.35f;
    }
    private static void blockSound(ServerPlayerEntity p,float pitch){p.getWorld().playSound(null,p.getBlockPos(),net.minecraft.sound.SoundEvents.ITEM_SHIELD_BLOCK,net.minecraft.sound.SoundCategory.PLAYERS,1,pitch);}
}
