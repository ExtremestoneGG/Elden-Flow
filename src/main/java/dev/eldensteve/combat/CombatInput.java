package dev.eldensteve.combat;
/** Intent only. Positions, damage, stamina and hit results are never accepted from a client. */
public record CombatInput(int sequence,float right,float forward,float yaw,int buttons,int target,int grip) {
    public static final int ROLL=1,WALK=2,SPRINT=4,LIGHT=8,JUMP=16,HEAVY=32,GUARD=64,CROUCH=128,PARRY=256;
    public boolean down(int button){return (buttons&button)!=0;}
    public boolean valid(){return sequence>=0&&Float.isFinite(right)&&Float.isFinite(forward)&&Float.isFinite(yaw)&&Math.abs(right)<=1.001f&&Math.abs(forward)<=1.001f&&Math.abs(yaw)<=1000&&buttons>=0&&buttons<512&&target>=-1&&grip>=0&&grip<=2;}
    public static CombatInput idle(int sequence,float yaw,int grip){return new CombatInput(sequence,0,0,yaw,0,-1,grip);}
}
