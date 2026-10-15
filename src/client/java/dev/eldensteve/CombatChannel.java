package dev.eldensteve;
import dev.eldensteve.combat.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import java.util.*;
/** Commands go to the server; returned frames contain authoritative combat and stamina. */
public final class CombatChannel {
    private static int sequence;
    private static Object world;
    private static final LinkedHashMap<Integer,Float> predicted=new LinkedHashMap<>();
    private static float correction;
    public static float correction(){float value=correction;correction=0;return value;}
    private static final Map<Integer,CombatPackets.Frame> frames=new HashMap<>();
    public static CombatPackets.Frame frame(int entity){return frames.get(entity);}
    public static boolean available(){return ClientPlayNetworking.canSend(CombatPackets.Input.ID);}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(CombatPackets.RestVisual.ID,(packet,context)->context.client().execute(()->{var view=RemoteCombat.get(packet.entity());if(view!=null)view.restVisual=packet;}));
        ClientPlayNetworking.registerGlobalReceiver(CombatPackets.Frame.ID,(packet,context)->context.client().execute(()->{
            var c=context.client();if(c.world==null)return;if(world!=c.world){frames.clear();predicted.clear();correction=0;DefenseVisuals.clear();RemoteCombat.clear();world=c.world;}
            if(packet.clip()<0)frames.remove(packet.entity());else frames.put(packet.entity(),packet);
            RemoteCombat.receive(packet);
            if(c.player!=null&&packet.entity()==c.player.getId()){
                Float old=predicted.remove(packet.sequence());
                if(old!=null&&packet.clip()>=0){float difference=packet.state()[6]-old;correction+=difference;predicted.replaceAll((key,value)->value+difference);}
            }
            var e=c.world.getEntityById(packet.entity());if(e!=null)DefenseVisuals.put(e.getUuid(),packet.clip()<0?0:packet.defense());
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{sequence=0;world=null;predicted.clear();correction=0;frames.clear();DefenseVisuals.clear();RemoteCombat.clear();});
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(c->{if(!c.isPaused()){RemoteCombat.tick();DefenseVisuals.prune();frames.entrySet().removeIf(e->c.world==null||c.world.getEntityById(e.getKey())==null);}});
    }
    public static void send(float right,float forward,float yaw,int buttons,int target,int grip,float stamina){
        if(!available())return;
        predicted.put(sequence,stamina);while(predicted.size()>80)predicted.remove(predicted.keySet().iterator().next());
        ClientPlayNetworking.send(new CombatPackets.Input(new CombatInput(sequence++,right,forward,yaw,buttons,target,grip)));
    }
}
