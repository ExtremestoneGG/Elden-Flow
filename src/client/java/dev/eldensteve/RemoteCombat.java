package dev.eldensteve;
import dev.eldensteve.combat.CombatPackets;
import java.util.*;
import net.minecraft.client.MinecraftClient;
/** Per-entity animation clocks; no remote pose reads the local player's grip or action. */
public final class RemoteCombat {
    public static final class View {
        public final MotionPlayer motion=new MotionPlayer();
        public CombatPackets.Frame frame;
        public CombatPackets.RestVisual restVisual;
        private float previousYaw;
        private int lastTick;
        private float sitting,previousSitting;
        private boolean raised;
        public boolean resting(){return sitting>0||previousSitting>0;}
        public boolean raised(){return raised;}
        public float sitting(float delta){float t=net.minecraft.util.math.MathHelper.lerp(delta,previousSitting,sitting);return t*t*(3-2*t);}
        public float yaw(float delta){float d=frame.state()[2]-previousYaw;return previousYaw+(float)Math.atan2(Math.sin(d),Math.cos(d))*delta;}
    }
    private static final Map<Integer,View> players=new HashMap<>();
    private static int tick;
    public static void clear(){players.clear();tick=0;}
    public static View get(int id){return players.get(id);}
    public static void receive(CombatPackets.Frame packet){
        var c=MinecraftClient.getInstance();if(c.player==null||packet.entity()==c.player.getId())return;
        if(packet.clip()<0){players.remove(packet.entity());return;}
        var view=players.computeIfAbsent(packet.entity(),id->new View());view.previousYaw=view.frame==null?packet.state()[2]:view.frame.state()[2];view.frame=packet;view.lastTick=tick;if(packet.state()[3]!=7)view.restVisual=null;
        try{view.motion.motion(packet.state()[0],packet.state()[1],packet.state()[2]);if(packet.state()[3]==8)view.motion.consume((int)packet.state()[4]);else view.motion.tick(packet.clip(),packet.state()[4],packet.state()[3]>0&&packet.state()[3]!=7&&packet.clip()!=202040,packet.state()[8],true);}
        catch(Exception e){players.remove(packet.entity());EldenSteveClient.LOG.error("Remote animation unavailable",e);}
    }
    public static void tick(){tick++;var world=MinecraftClient.getInstance().world;players.entrySet().removeIf(e->tick-e.getValue().lastTick>40||world==null||world.getEntityById(e.getKey())==null);
        for(var view:players.values()){view.previousSitting=view.sitting;boolean rest=view.frame.state()[3]==7;if(rest)view.raised=view.frame.state()[10]>0;view.sitting=Math.clamp(view.sitting+(rest?1f/12:-1f/10),0,1);}
    }
}
