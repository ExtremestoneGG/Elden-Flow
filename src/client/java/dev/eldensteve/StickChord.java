package dev.eldensteve;
/** Consume the whole L3/R3 gesture, so toggling does not also crouch or lock. */
public final class StickChord {
    private boolean left,right,consumed;
    public void reset(){left=right=consumed=false;}
    public int tick(boolean l,boolean r){
        int events=0;
        if(l&&r&&!consumed){events=1;consumed=true;}
        if(!consumed){if(left&&!l)events|=2;if(right&&!r)events|=4;}
        left=l;right=r;
        if(!l&&!r)consumed=false;
        return events;
    }
}
