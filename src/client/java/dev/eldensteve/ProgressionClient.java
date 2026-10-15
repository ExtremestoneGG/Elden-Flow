package dev.eldensteve;

import dev.eldensteve.progression.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.math.BlockPos;

/** Display-only snapshot; purchases and balances are always decided by the server. */
public final class ProgressionClient {
    private static PlayerProgress progress=new PlayerProgress();
    private static boolean resting,sentMode;
    private static int legacyStamina;
    public static float maximumStamina(){return Math.min(10097,legacyStamina+ProgressionRules.maximumStamina(progress.level(Attribute.ENDURANCE)));}
    private static Object world;
    public static PlayerProgress view(){return progress.copy();}
    public static boolean resting(){return resting;}
    public static void intent(int action,int argument,BlockPos fire){if(net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler()!=null&&ClientPlayNetworking.canSend(ProgressionPackets.Intent.ID))ClientPlayNetworking.send(new ProgressionPackets.Intent(action,argument,fire));}
    public static void buy(Attribute attribute){intent(4,attribute.ordinal(),BlockPos.ORIGIN);}
    public static void register(){
        ClientPlayNetworking.registerGlobalReceiver(ProgressionPackets.Snapshot.ID,(packet,context)->context.client().execute(()->{progress=packet.progress().copy();legacyStamina=packet.legacyStamina();resting=packet.resting();Bonfire.serverResting(resting,packet.raisedSeat());}));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{progress=new PlayerProgress();legacyStamina=0;resting=sentMode=false;world=null;});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.world!=world){world=c.world;sentMode=false;resting=false;if(c.player!=null)intent(6,0,BlockPos.ORIGIN);}
            if(c.player!=null&&sentMode!=EldenSteveClient.enabled){sentMode=EldenSteveClient.enabled;intent(sentMode?1:0,0,BlockPos.ORIGIN);}
        });
    }
}
