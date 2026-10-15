package dev.eldensteve;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ExperienceOrbEntity;

/** Reuses the vanilla orb texture and animation; only vertex colors change. */
public final class SoulOrbRenderer extends ExperienceOrbEntityRenderer {
    public SoulOrbRenderer(EntityRendererFactory.Context context){super(context);}
    public void render(ExperienceOrbEntity orb,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider buffers,int light){
        super.render(orb,yaw,delta,matrices,layer->new Tint(buffers.getBuffer(layer)),light);
    }
    private record Tint(VertexConsumer delegate) implements VertexConsumer {
        public VertexConsumer vertex(float x,float y,float z){delegate.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){delegate.color(Math.max(65,r/2),Math.max(45,b/2),255,a);return this;}
        public VertexConsumer texture(float u,float v){delegate.texture(u,v);return this;}
        public VertexConsumer overlay(int u,int v){delegate.overlay(u,v);return this;}
        public VertexConsumer light(int u,int v){delegate.light(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){delegate.normal(x,y,z);return this;}
    }
}
