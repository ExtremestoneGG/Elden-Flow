package dev.eldensteve;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class CombatOverlay {
    public static boolean debug;
    public static float[] blade=new float[107];
    public static void register(){WorldRenderEvents.AFTER_ENTITIES.register(ctx->{
        var c=MinecraftClient.getInstance();if(!EldenSteveClient.enabled||c.player==null||ctx.matrixStack()==null||ctx.consumers()==null)return;
        var m=ctx.matrixStack();var cam=ctx.camera().getPos();m.push();m.translate(-cam.x,-cam.y,-cam.z);
        var v=ctx.consumers().getBuffer(RenderLayer.getLines());float delta=ctx.tickCounter().getTickDelta(false);
        var target=Combat.target();
        if(debug&&target!=null)WorldRenderer.drawBox(m,v,target.getBoundingBox().expand(.1),1,.7f,0,1);
        if(debug){
            var origin=c.player.getPos();
            for(int i=0;i<(int)blade[1];i++){
                int at=2+i*7;var a=origin.add(blade[at+1],blade[at+2],blade[at+3]);var b=origin.add(blade[at+4],blade[at+5],blade[at+6]);
                var normal=b.subtract(a).normalize();if(normal.lengthSquared()<.001)normal=new Vec3d(0,1,0);
                var e=m.peek();
                v.vertex(e,(float)a.x,(float)a.y,(float)a.z).color(0,255,100,255).normal(e,(float)normal.x,(float)normal.y,(float)normal.z);
                v.vertex(e,(float)b.x,(float)b.y,(float)b.z).color(0,255,100,255).normal(e,(float)normal.x,(float)normal.y,(float)normal.z);
            }
        }
        m.pop();
    });}
}
