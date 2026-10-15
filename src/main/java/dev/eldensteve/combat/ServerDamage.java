package dev.eldensteve.combat;
import dev.eldensteve.*;
import dev.eldensteve.progression.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.*;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.tag.*;
import net.minecraft.server.network.ServerPlayerEntity;
/** Called only by a server-validated contact window, with server inventory and world rules. */
public final class ServerDamage {
    private record Hit(LivingEntity victim,Entity attacker){}
    private static final ThreadLocal<Hit> ACTIVE=new ThreadLocal<>();
    public static boolean bypassCooldown(LivingEntity victim,DamageSource source,TagKey<DamageType> tag){var h=ACTIVE.get();return tag==DamageTypeTags.BYPASSES_COOLDOWN&&h!=null&&h.victim==victim&&h.attacker==source.getAttacker();}
    public static boolean hit(ServerPlayerEntity player,LivingEntity victim,int flags,WeaponCatalog.Profile profile){
        if(!ProgressionServer.enabled(player)||profile==null||!player.isAlive()||!victim.isAlive()||player.getWorld()!=victim.getWorld()||player.squaredDistanceTo(victim)>16||!player.canSee(victim))return false;
        if(victim instanceof net.minecraft.entity.player.PlayerEntity other&&!player.shouldDamagePlayer(other))return false;
        boolean left=(flags&1)!=0,heavy=(flags&2)!=0;var stack=left?player.getOffHandStack():player.getMainHandStack();if(stack.isEmpty())return false;
        var original=player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);if(original==null)return false;
        var damage=new EntityAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE,a->{});damage.setFrom(original);
        if(left){player.getMainHandStack().applyAttributeModifiers(EquipmentSlot.MAINHAND,(a,m)->{if(a.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE))damage.removeModifier(m.id());});stack.applyAttributeModifiers(EquipmentSlot.MAINHAND,(a,m)->{if(a.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE))damage.updateModifier(m);});}
        var source=player.getDamageSources().playerAttack(player);var world=player.getServerWorld();
        float amount=EnchantmentHelper.getDamage(world,stack,victim,source,(float)damage.getValue())*(heavy?2.5f:1.5f);
        var family=WeaponFamily.fromType(profile.type());var stats=ProgressionServer.view(player);
        if(family!=null)amount*=ProgressionRules.physicalMultiplier(family,stats.level(Attribute.STRENGTH),stats.level(Attribute.DEXTERITY));
        if(family!=null)amount*=CombatEffects.multiplier(player,victim,family);
        if(!Float.isFinite(amount)||amount<=0)return false;
        ACTIVE.set(new Hit(victim,player));
        try{if(!victim.damage(source,amount))return false;EnchantmentHelper.onTargetDamaged(world,victim,source,stack);stack.damage(1,player,left?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND);player.onAttacking(victim);if(family!=null&&victim.isAlive())CombatEffects.landed(player,victim,family,flags);return true;}
        finally{ACTIVE.remove();}
    }
}
