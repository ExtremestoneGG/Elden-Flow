package dev.eldensteve.combat;
/** A fixed-size input mailbox consumed at server tick rate, never at packet arrival rate. */
public final class InputMailbox {
    private CombatInput latest=CombatInput.idle(0,0,0);
    private int sequence=-1,lastTick=-100,receivedTick=-100,received,pulses;
    private boolean rollPress;
    public boolean offer(CombatInput input,int tick){
        if(!input.valid()||input.sequence()<=sequence)return false;
        if(receivedTick!=tick){receivedTick=tick;received=0;}
        if(++received>8)return false;
        if(input.down(CombatInput.ROLL)&&!latest.down(CombatInput.ROLL))rollPress=true;
        pulses|=input.buttons()&(CombatInput.LIGHT|CombatInput.JUMP|CombatInput.CROUCH|CombatInput.PARRY);
        latest=input;sequence=input.sequence();lastTick=tick;return true;
    }
    public boolean stale(int tick){return tick-lastTick>5;}
    public CombatInput take(int tick){
        if(stale(tick)){pulses=0;rollPress=false;return CombatInput.idle(Math.max(0,sequence),latest.yaw(),latest.grip());}
        int flags=latest.buttons()&~(CombatInput.LIGHT|CombatInput.CROUCH|CombatInput.PARRY);
        flags|=pulses;if(rollPress)flags|=CombatInput.ROLL;
        pulses=0;rollPress=false;
        return new CombatInput(sequence,latest.right(),latest.forward(),latest.yaw(),flags,latest.target(),latest.grip());
    }
}
