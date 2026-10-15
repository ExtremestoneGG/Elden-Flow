package dev.eldensteve;

/** Tick-based animation time and short cross-fades; render FPS never advances the simulation. */
public final class MotionPlayer {
    private static MotionLibrary library;
    private int clip=-1;
    private float frame,previousFrame,blend,previousBlend;
    private boolean loop,ready;
    private float[] from,display,target;
    private float[] directionSample;
    private float motionSide,motionForward;
    public void motion(float dx,float dz,float yaw){float length=(float)Math.hypot(dx,dz);if(length>.0001){motionSide=(dx*(float)Math.cos(yaw)-dz*(float)Math.sin(yaw))/length;motionForward=(dx*(float)Math.sin(yaw)+dz*(float)Math.cos(yaw))/length;}}
    public void load() throws Exception {
        if(from!=null)return;
        if(library==null){
        try(var in=MotionPlayer.class.getResourceAsStream("/assets/eldensteve/movement.bin")){library=new MotionLibrary(in);}
        }
        from=new float[library.stride];display=new float[library.stride];target=new float[library.stride];
        directionSample=new float[library.stride];
        EldenSteveClient.LOG.info("Loaded {} real Elden movement clips",library.clips.size());
    }
    public void consume(int ticks)throws Exception{
        load();int id=ticks<10?50110:50111;var clip=library.clips.get(id);if(clip==null){tick(0,0,false,0,true);return;}
        float frame=ticks<10?Math.clamp(ticks/10f,0,1)*(clip.frames()-1):((ticks-10)*1.5f)%(clip.frames()-1);
        tick(id,frame,true,0,true);
    }
    public void reset(){clip=-1;ready=false;}
    private boolean gait(int id){int b=id%1_000_000;return b>=20000&&b<=20003||b>=20100&&b<=20103||b>=21000&&b<=21003||b>=21100&&b<=21103;}
    public boolean guarding(){int b=clip%1_000_000;return b==100||b>=21000&&b<=21003||b>=21100&&b<=21103;}
    public void tick(int id,float actionFrame,boolean action,float speed,boolean supported) throws Exception {
        load();
        if(!supported){reset();return;}
        if(!library.clips.containsKey(id))id=0;
        if(id!=clip){
            if(ready)System.arraycopy(display,0,from,0,from.length);
            float nextFrame=0;
            if(ready&&gait(clip)&&gait(id)){
                float oldLength=library.clips.get(clip).frames()-1,newLength=library.clips.get(id).frames()-1;
                nextFrame=(frame%oldLength)/oldLength*newLength;
            }
            clip=id;frame=previousFrame=action?actionFrame:nextFrame;blend=previousBlend=ready?0:1;
            if(!ready){library.sample(id,frame,!action,display);System.arraycopy(display,0,from,0,from.length);}
        }else {previousFrame=frame;previousBlend=blend;}
        loop=!action;
        int base=id%1_000_000;
        if((base>=21000&&base<=21003)||(base>=21100&&base<=21103))base-=1000;
        frame=action?actionFrame:frame+1.5f*(base>=20000&&base<=20003?speed/1.5f:base>=20100&&base<=20103?speed/4.012f:base==20200?speed/6f:1);
        blend=Math.min(1,blend+(action?.35f:.22f));ready=true;
        pose(1);
    }
    public float[] pose(float delta) {
        if(!ready)return null;
        float f=previousFrame+(frame-previousFrame)*delta;
        if(guarding()&&gait(clip)&&Math.abs(motionSide)+Math.abs(motionForward)>.01f){
            java.util.Arrays.fill(target,0);int base=clip-clip%100;float duration=library.clips.get(clip).frames()-1;
            float sum=Math.abs(motionSide)+Math.abs(motionForward);
            for(int d=0;d<4;d++){float weight=switch(d){case 0->Math.max(0,motionForward);case 1->Math.max(0,-motionForward);case 2->Math.max(0,motionSide);default->Math.max(0,-motionSide);};if(weight==0)continue;
                var directional=library.clips.get(base+d);if(directional==null)continue;
                library.sample(base+d,f/duration*(directional.frames()-1),true,directionSample);
                for(int i=0;i<target.length;i++)target[i]+=directionSample[i]*weight/sum;
            }
        }else library.sample(clip,f,loop,target);
        float t=previousBlend+(blend-previousBlend)*delta;
        for(int i=0;i<display.length;i++)display[i]=from[i]+(target[i]-from[i])*t;
        return display;
    }
    public float[] rest(){float[] r=new float[library.stride];library.sample(0,0,false,r);return r;}
}
