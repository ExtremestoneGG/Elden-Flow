package dev.eldensteve;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Locally baked, model-space Elden joints at 30 fps. No game engine needed. */
public final class MotionLibrary {
    public record Clip(int id,float blend,int frames,float[] samples) {}
    public final Map<Integer,Clip> clips=new LinkedHashMap<>();
    public final int stride;
    public MotionLibrary(InputStream input) throws IOException {
        if(input==null)throw new IOException("movement.bin ausente");
        var b=ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getInt()!=0x4e415245)throw new IOException("ERAN invalido");
        int joints=b.getInt(),oriented=b.getInt(),count=b.getInt();
        if(joints!=16||(oriented!=3&&oriented!=4&&oriented!=5)||count<10||count>2000)throw new IOException("Esqueleto inesperado");
        stride=joints*3+oriented*9;
        for(int i=0;i<joints+oriented;i++)string(b);
        for(int i=0;i<count;i++) {
            String name=string(b);int split=name.indexOf('_');int id=Integer.parseInt(name.substring(1,split))*1000000+Integer.parseInt(name.substring(split+1));
            float blend=b.getFloat();int frames=b.getInt();
            if(frames<2||frames>10000)throw new IOException("Clip invalido");
            float[] samples=new float[frames*stride];
            for(int k=0;k<samples.length;k++){samples[k]=b.getFloat();if(!Float.isFinite(samples[k]))throw new IOException("Pose invalida");}
            clips.put(id,new Clip(id,blend,frames,samples));
        }
        if(b.hasRemaining())throw new IOException("Dados excedentes");
        // Guard uses authored upper-body joints, while feet keep the directional gait.
        // Register virtual clips once: no per-frame allocations or extra asset extraction.
        for(Clip gait:new ArrayList<>(clips.values())) {
            int base=gait.id%1_000_000;
            if(!((base>=20000&&base<=20003)||(base>=20100&&base<=20103)))continue;
            int stance=gait.id-base;
            Clip guard=clips.get(stance+100);
            if(guard==null)continue;
            float[] samples=gait.samples.clone(),upper=new float[stride];
            for(int f=0;f<gait.frames;f++) {
                sample(guard.id,f,true,upper);
                int o=f*stride;
                // Translate the guard torso and arms with the gait pelvis without changing their pose.
                for(int j=1;j<10;j++)for(int axis=0;axis<3;axis++)
                    samples[o+j*3+axis]=upper[j*3+axis]+samples[o+axis]-upper[axis];
                System.arraycopy(upper,48,samples,o+48,stride-48);
            }
            clips.put(gait.id+1000,new Clip(gait.id+1000,gait.blend,gait.frames,samples));
        }
    }
    private static String string(ByteBuffer b){byte[] a=new byte[Byte.toUnsignedInt(b.get())];b.get(a);return new String(a,StandardCharsets.UTF_8);}
    public void sample(int id,float frame,boolean loop,float[] out) {
        Clip clip=clips.getOrDefault(id,clips.get(0));
        float last=clip.frames-1;
        float f=loop?Math.floorMod((long)Math.floor(frame*1024),(long)(last*1024))/1024f:Math.clamp(frame,0,last);
        int a=(int)f, next=Math.min(a+1,clip.frames-1);float t=f-a;
        for(int k=0;k<stride;k++){float x=clip.samples[a*stride+k];out[k]=x+(clip.samples[next*stride+k]-x)*t;}
    }
}
