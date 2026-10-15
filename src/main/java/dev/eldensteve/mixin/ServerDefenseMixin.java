package dev.eldensteve.mixin;
import dev.eldensteve.combat.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.*;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(LivingEntity.class)
public class ServerDefenseMixin {
    @WrapOperation(method="damage",at=@At(value="INVOKE",target="Lnet/minecraft/entity/damage/DamageSource;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean contacts(DamageSource source,TagKey<DamageType> tag,Operation<Boolean> original){return original.call(source,tag)||ServerDamage.bypassCooldown((LivingEntity)(Object)this,source,tag);}
    @WrapMethod(method="damage")
    private boolean damage(DamageSource source,float amount,Operation<Boolean> original){
        var e=(LivingEntity)(Object)this;
        if(source.getAttacker()!=null&&ServerCombat.staggered(source.getAttacker())||ServerCombat.avoids(e,source))return false;
        float adjusted=ServerCombat.guardDamage(e,source,amount);if(amount>0&&adjusted<=0)return false;
        boolean lab=!e.getWorld().isClient&&e.getServer()!=null&&e.getCommandTags().contains("eldensteve_lab_target")&&e.getServer().getSaveProperties().getLevelName().equals("Elden Prototype Lab");
        if(lab)adjusted=Math.min(adjusted,Math.max(0,e.getHealth()-1));
        boolean result=original.call(source,adjusted);
        if(result&&adjusted>0)CombatEffects.hurt(e);
        if(lab&&e.isAlive()){if(result)e.setCustomName(net.minecraft.text.Text.literal("Alvo infinito | dano: "+String.format(java.util.Locale.ROOT,"%.1f",e.getMaxHealth()-e.getHealth())));e.setHealth(e.getMaxHealth());}
        return result;
    }
}
