package dev.eldensteve;
import java.util.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

/** Half of a twelve-pixel limb, with cut faces sampled at the actual skin seam. */
public final class SegmentCuboid extends ModelPart.Cuboid {
    private static final Map<ModelPart,SegmentCuboid> SEGMENTS=new IdentityHashMap<>();
    private final ModelPart.Quad[] faces;
    public SegmentCuboid(int u,int v,float width,float dilation,boolean lower,boolean mirror,int textureHeight) {
        super(u,v,-width/2,0,-2,width,6,4,dilation,dilation,dilation,mirror,64,textureHeight,Set.of());
        float x=-width/2-dilation,X=width/2+dilation,y=-dilation,Y=6+dilation,z=-2-dilation,Z=2+dilation;
        if(mirror){float swap=x;x=X;X=swap;}
        var a=new ModelPart.Vertex(x,y,z,0,0);var b=new ModelPart.Vertex(X,y,z,0,0);
        var c=new ModelPart.Vertex(X,Y,z,0,0);var d=new ModelPart.Vertex(x,Y,z,0,0);
        var e=new ModelPart.Vertex(x,y,Z,0,0);var f=new ModelPart.Vertex(X,y,Z,0,0);
        var g=new ModelPart.Vertex(X,Y,Z,0,0);var h=new ModelPart.Vertex(x,Y,Z,0,0);
        float top=v+4+(lower?6:0),bottom=top+6;
        // The inner elbow/knee takes a one-pixel strip of the adjacent outer face.
        float seamTop=v+9.05f,seamBottom=v+9.95f;
        faces=new ModelPart.Quad[]{
            new ModelPart.Quad(new ModelPart.Vertex[]{f,e,a,b},u+4,lower?seamTop:v,u+4+width,lower?seamBottom:v+4,64,textureHeight,mirror,Direction.DOWN),
            new ModelPart.Quad(new ModelPart.Vertex[]{c,d,h,g},lower?u+4+width:u+4,lower?v+4:seamBottom,lower?u+4+2*width:u+4+width,lower?v:seamTop,64,textureHeight,mirror,Direction.UP),
            new ModelPart.Quad(new ModelPart.Vertex[]{a,e,h,d},u,top,u+4,bottom,64,textureHeight,mirror,Direction.WEST),
            new ModelPart.Quad(new ModelPart.Vertex[]{b,a,d,c},u+4,top,u+4+width,bottom,64,textureHeight,mirror,Direction.NORTH),
            new ModelPart.Quad(new ModelPart.Vertex[]{f,b,c,g},u+4+width,top,u+8+width,bottom,64,textureHeight,mirror,Direction.EAST),
            new ModelPart.Quad(new ModelPart.Vertex[]{e,f,g,h},u+8+width,top,u+8+2*width,bottom,64,textureHeight,mirror,Direction.SOUTH)
        };
    }
    @Override public void renderCuboid(MatrixStack.Entry entry,VertexConsumer out,int light,int overlay,int color) {
        var normal=new Vector3f();var position=new Vector3f();
        for(var face:faces){
            entry.transformNormal(face.direction,normal);
            for(var vertex:face.vertices){entry.getPositionMatrix().transformPosition(vertex.pos.x/16,vertex.pos.y/16,vertex.pos.z/16,position);
                out.vertex(position.x,position.y,position.z,color,vertex.u,vertex.v,overlay,light,normal.x,normal.y,normal.z);}
        }
    }
    public static ModelPart part(int u,int v,float width,float dilation,boolean lower,boolean mirror,int textureHeight){
        var cube=new SegmentCuboid(u,v,width,dilation,lower,mirror,textureHeight);
        var part=new ModelPart(List.of(cube),Map.of());SEGMENTS.put(part,cube);return part;
    }
    /** VulkanMod bypasses Cuboid overrides when compiling ModelPart. Submit our faces directly. */
    public static void renderPart(ModelPart part,MatrixStack matrices,VertexConsumer out,int light,int overlay,int color){
        var segment=SEGMENTS.get(part);
        if(segment==null){part.render(matrices,out,light,overlay,color);return;}
        if(!part.visible||part.hidden)return;
        matrices.push();part.rotate(matrices);segment.renderCuboid(matrices.peek(),out,light,overlay,color);matrices.pop();
    }
}
