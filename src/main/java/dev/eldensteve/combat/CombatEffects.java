package dev.eldensteve.combat;
import dev.eldensteve.progression.*;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
/** Bounded, transient combat bonuses. They never modify items or survive a logout. */
public final class CombatEffects {
    private static final class Chain {int dagger,twin,sword,fury,last,expires;UUID target;WeaponFamily family;}
    private static final class Bleed {double value;int tick;}
    private record Wound(UUID player,UUID victim,WeaponFamily family){}
    private static final Map<MinecraftServer,Map<UUID,Chain>> chains=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Map<Wound,Bleed>> wounds=new IdentityHashMap<>();
    private static Chain chain(ServerPlayerEntity p,WeaponFamily family){
        var s=chains.computeIfAbsent(p.getServer(),x->new HashMap<>()).computeIfAbsent(p.getUuid(),x->new Chain());int now=p.getServer().getTicks();
        if(s.family!=family||now-s.last>100){s.dagger=s.twin=s.sword=0;s.target=null;}if(now>=s.expires)s.fury=0;s.family=family;return s;
    }
    public static float multiplier(ServerPlayerEntity p,LivingEntity target,WeaponFamily family){
        var s=chain(p,family);double power=ProgressionRules.passivePower(ProgressionServer.view(p).level(Attribute.AFFINITY));
        return (float)(1+switch(family){
            case DAGGER->(target.getUuid().equals(s.target)?s.dagger:0)*.02*power;
            case TWINBLADE->s.twin*.01*power;
            case GREATSWORD->s.fury*.08*power;
            case GREAT_AXE->target.getHealth()>=target.getMaxHealth()*.9?.06*power:0;
            case HALBERD->p.distanceTo(target)>2.2?.05*power:0;
            default->0;
        });
    }
    public static boolean boss(LivingEntity e){return e instanceof net.minecraft.entity.boss.WitherEntity||e instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity||e instanceof net.minecraft.entity.mob.WardenEntity;}
    public static void landed(ServerPlayerEntity p,LivingEntity victim,WeaponFamily family,int flags){
        var s=chain(p,family);int tick=p.getServer().getTicks(),affinity=ProgressionServer.view(p).level(Attribute.AFFINITY);double power=ProgressionRules.passivePower(affinity);boolean heavy=(flags&2)!=0,charged=(flags&4)!=0;var random=p.getRandom();
        switch(family){
            case DAGGER->s.dagger= Math.min(10,victim.getUuid().equals(s.target)?s.dagger+1:1);
            case TWINBLADE->s.twin=Math.min(20,s.twin+1);
            case GREATSWORD->{if(charged&&random.nextFloat()<.30){s.fury=Math.min(2,s.fury+1);s.expires=tick+160;spark(p,victim);}}
            case HAMMER,GREAT_HAMMER->{boolean large=family==WeaponFamily.GREAT_HAMMER;if(random.nextFloat()<ProgressionRules.stunChance(large,charged)){int duration=(int)(20*ProgressionRules.stunSeconds(large,affinity)*(boss(victim)?.35:1));ServerCombat.stagger(victim,Math.max(1,duration));spark(p,victim);}}
            case THRUSTING_SWORD->{if(random.nextFloat()<ProgressionRules.rapierRefundChance(affinity)){ServerCombat.refundAttack(p);spark(p,p);}}
            case SPEAR->victim.takeKnockback(ProgressionRules.spearKnockback(affinity,heavy),p.getX()-victim.getX(),p.getZ()-victim.getZ());
            case KATANA->{var map=wounds.computeIfAbsent(p.getServer(),x->new HashMap<>());var key=new Wound(p.getUuid(),victim.getUuid(),family);var w=map.computeIfAbsent(key,x->new Bleed());w.value=Math.max(0,w.value-Math.max(0,tick-w.tick)*.1)+(heavy?24:16)*power;w.tick=tick;
                if(w.value>=100){w.value=0;victim.damage(p.getDamageSources().playerAttack(p),(float)ProgressionRules.bleedBurst(victim.getMaxHealth(),boss(victim),affinity));p.getServerWorld().spawnParticles(ParticleTypes.DAMAGE_INDICATOR,victim.getX(),victim.getBodyY(.5),victim.getZ(),8,.2,.3,.2,.05);}}
            case REAPER->{var map=wounds.computeIfAbsent(p.getServer(),x->new HashMap<>());var key=new Wound(p.getUuid(),victim.getUuid(),family);var w=map.computeIfAbsent(key,x->new Bleed());if(tick-w.tick>60*power)w.value=0;w.value=Math.min(6,w.value+1);w.tick=tick;
                int amplifier=Math.min(2,(int)(w.value/3)+(power>=1.8?1:0));victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,(int)(60*power),amplifier),p);}
            case AXE->{if(charged)victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,(int)(30*power),0),p);}
            case STRAIGHT_SWORD->{if(++s.sword>=3){s.sword=0;p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,(int)(20*power),0),p);}}
            case CURVED_SWORD->{if(charged)victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,(int)(20*power),0),p);}
            default->{}
        }
        s.last=tick;s.target=victim.getUuid();
    }
    private static void spark(ServerPlayerEntity p,LivingEntity victim){p.getServerWorld().spawnParticles(ParticleTypes.CRIT,victim.getX(),victim.getBodyY(1)+.2,victim.getZ(),5,.15,.08,.15,0);}
    public static void hurt(LivingEntity e){if(!(e instanceof ServerPlayerEntity p))return;var map=chains.get(p.getServer());var s=map==null?null:map.get(p.getUuid());if(s!=null){s.twin/=2;s.dagger=s.sword=0;}}
    public static void tick(MinecraftServer server){var ws=wounds.get(server);if(ws!=null)ws.entrySet().removeIf(e->server.getTicks()-e.getValue().tick>200);var cs=chains.get(server);if(cs!=null)cs.entrySet().removeIf(e->server.getTicks()-e.getValue().last>400);}
    public static void leave(ServerPlayerEntity p){var cs=chains.get(p.getServer());if(cs!=null)cs.remove(p.getUuid());var ws=wounds.get(p.getServer());if(ws!=null)ws.keySet().removeIf(k->k.player.equals(p.getUuid()));}
    public static void stop(MinecraftServer server){chains.remove(server);wounds.remove(server);}
}
