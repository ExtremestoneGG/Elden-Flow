package dev.eldensteve.mixin;
import dev.eldensteve.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractInventoryScreen.class)
public class InventoryEffectsMixin {
 @Inject(method="drawStatusEffects",at=@At("HEAD"),cancellable=true)
 private void eldenflow$effects(DrawContext ctx,int mx,int my,CallbackInfo ci){
  if(!EldenSteveClient.enabled||!((Object)this instanceof InventoryScreen screen))return;
  var a=(HandledScreenAccess)this;int side=a.eldenflow$x()+a.eldenflow$width()+4;
  if(side+AttributePanel.WIDTH>screen.width)return;
  ci.cancel();var c=MinecraftClient.getInstance();if(c.player==null)return;
  int x=side+AttributePanel.WIDTH+3,y=Math.max(2,a.eldenflow$y());
  boolean outside=x+24<=screen.width;
  if(!outside){x=a.eldenflow$x();y=Math.max(2,a.eldenflow$y()-24);}
  int i=0;for(var effect:c.player.getStatusEffects()){
   int px=outside?x:x+(i%7)*24,py=outside?y+i*24:y-(i/7)*24;i++;
   AttributePanel.frame(ctx,px,py,23,23);
   ctx.drawSprite(px+2,py+2,0,18,18,c.getStatusEffectSpriteManager().getSprite(effect.getEffectType()));
   if(mx>=px&&mx<px+23&&my>=py&&my<py+23)ctx.drawTooltip(c.textRenderer,java.util.List.of(effect.getEffectType().value().getName(),net.minecraft.entity.effect.StatusEffectUtil.getDurationText(effect,1,c.world.getTickManager().getTickRate())),java.util.Optional.empty(),mx,my);
  }
 }
}
