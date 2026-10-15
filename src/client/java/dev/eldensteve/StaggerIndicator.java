package dev.eldensteve;
import dev.eldensteve.combat.CombatPackets;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
/** Tiny vanilla-font marker; no custom shader or graphics backend dependency. */
public final class StaggerIndicator {
    private static final Map<Integer,Integer> remaining=new HashMap<>();private static Object world;
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(CombatPackets.Stagger.ID,(packet,context)->context.client().execute(()->{if(packet.ticks()>0)remaining.put(packet.entity(),packet.ticks());}));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{remaining.clear();world=null;});
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.world!=world){remaining.clear();world=c.world;}if(!c.isPaused()){remaining.replaceAll((id,ticks)->ticks-1);remaining.entrySet().removeIf(e->e.getValue()<=0||c.world==null||c.world.getEntityById(e.getKey())==null);}});
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
            var c=MinecraftClient.getInstance();if(c.world==null||c.options.hudHidden||ctx.matrixStack()==null||ctx.consumers()==null)return;
            var m=ctx.matrixStack();var camera=ctx.camera();float delta=ctx.tickCounter().getTickDelta(false);
            for(var id:remaining.keySet()){
                var e=c.world.getEntityById(id);if(e==null||e.isInvisible())continue;var pos=e.getLerpedPos(delta).add(0,e.getHeight()+.28,0).subtract(camera.getPos());
                m.push();m.translate(pos.x,pos.y,pos.z);m.multiply(camera.getRotation());m.scale(-.025f,-.025f,.025f);
                String marker="✦";c.textRenderer.draw(marker,-c.textRenderer.getWidth(marker)/2f,0,0xFFFFD36A,true,m.peek().getPositionMatrix(),ctx.consumers(),TextRenderer.TextLayerType.NORMAL,0,0xF000F0);m.pop();
            }
        });
    }
}
