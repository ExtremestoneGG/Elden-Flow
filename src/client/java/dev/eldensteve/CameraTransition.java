package dev.eldensteve;

/** Render-clock easing, independent of simulation ticks; retargeting preserves continuity. */
public final class CameraTransition {
    public record View(float distance,float lift,float orbit){}
    private View from=new View(5.2f,.65f,0),to=from;
    private long last;
    private double elapsed=.4;
    public void begin(View start,View end,long now){from=start;to=end;elapsed=0;last=now;}
    public void advance(long now,boolean paused){
        if(last!=0&&!paused)elapsed=Math.min(.4,elapsed+Math.max(0,now-last)*1e-9);
        last=now;
    }
    public boolean complete(){return elapsed>=.4;}
    public View view(){float t=(float)Math.clamp(elapsed/.4,0,1);t=t*t*(3-2*t);return new View(mix(from.distance,to.distance,t),mix(from.lift,to.lift,t),mix(from.orbit,to.orbit,t));}
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
}
