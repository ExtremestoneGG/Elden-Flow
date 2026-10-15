package dev.eldensteve;
/** One switch per deliberate stick flick; the stick must return to its dead zone. */
public final class TargetFlick {
    private boolean latched;
    private int cooldown;
    public void reset(){latched=false;cooldown=0;}
    public int tick(float axis,boolean locked){
        if(!locked){reset();return 0;}
        if(cooldown>0)cooldown--;
        if(Math.abs(axis)<.25f)latched=false;
        if(latched||cooldown>0||Math.abs(axis)<.65f)return 0;
        latched=true;cooldown=6;return axis>0?1:-1;
    }
}
