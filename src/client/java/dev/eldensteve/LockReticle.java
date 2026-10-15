package dev.eldensteve;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector4f;

/** The vanilla-style cross becomes an X at the target's center, rendered above its mesh. */
public final class LockReticle {
    private static float targetX,targetY,x,y,angle;
    private static int width,height;
    private static boolean projected,ready;
    private static long last,projectionFrame;
    public static void register(){
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
            projected=false;var c=MinecraftClient.getInstance();var target=Combat.target();
            if(!EldenSteveClient.enabled||target==null||c.player==null)return;
            var point=target.getLerpedPos(ctx.tickCounter().getTickDelta(false)).add(0,target.getHeight()*.5,0).subtract(ctx.camera().getPos());
            var clip=new Vector4f((float)point.x,(float)point.y,(float)point.z,1);
            ctx.positionMatrix().transform(clip);ctx.projectionMatrix().transform(clip);
            if(!clip.isFinite()||clip.w<=.001f)return;
            float px=clip.x/clip.w,py=clip.y/clip.w;
            targetX=(px*.5f+.5f)*c.getWindow().getScaledWidth();targetY=(.5f-py*.5f)*c.getWindow().getScaledHeight();
            projected=true;projectionFrame=System.nanoTime();
        });
        HudRenderCallback.EVENT.register((ctx,counter)->{
            var c=MinecraftClient.getInstance();
            if(!EldenSteveClient.enabled||c.player==null||c.options.hudHidden||c.player.isSpectator()){ready=false;last=0;return;}
            int w=ctx.getScaledWindowWidth(),h=ctx.getScaledWindowHeight();long now=System.nanoTime();
            if(!ready||w!=width||h!=height){x=w*.5f;y=h*.5f;angle=0;ready=true;width=w;height=h;}
            float dt=last==0?0:(float)Math.clamp((now-last)*1e-9,0,.1);last=now;
            boolean marked=Combat.target()!=null&&projected&&now-projectionFrame<250_000_000L;
            float toX=marked?Math.clamp(targetX,8,w-8):w*.5f,toY=marked?Math.clamp(targetY,8,h-8):h*.5f;
            float blend=c.isPaused()?0:(float)(1-Math.exp(-18*dt));x+=(toX-x)*blend;y+=(toY-y)*blend;angle+=((marked?45:0)-angle)*blend;
            var matrices=ctx.getMatrices();matrices.push();matrices.translate(x,y,0);matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            ctx.fill(-4,0,5,1,0xFFF4F4F4);ctx.fill(0,-4,1,5,0xFFF4F4F4);matrices.pop();
        });
    }
}
