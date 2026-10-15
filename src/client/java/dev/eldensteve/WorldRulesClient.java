package dev.eldensteve;
import dev.eldensteve.progression.*;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class WorldRulesClient {
    private static long revision=-1,incoming=-1;
    private static int expected,count;
    private static boolean editable,required;
    private static StringBuilder pending;
    private static String upload;
    private static long uploadRevision;
    private static int uploadIndex;
    public static boolean active(){return revision>=0;}
    public static boolean waiting(){return required&&!active();}
    public static boolean editable(){return editable;}
    public static void importPreset(String json)throws java.io.IOException{
        if(!active()||!editable||!ClientPlayNetworking.canSend(WeaponRulePackets.Upload.ID))throw new java.io.IOException("Somente o anfitriao ou administrador importa regras neste mundo.");
        if(upload!=null)throw new java.io.IOException("Uma importacao ja esta sendo enviada.");
        upload=json;uploadRevision=revision;uploadIndex=0;
    }
    public static void save(String item,String json)throws java.io.IOException{
        if(!editable||!ClientPlayNetworking.canSend(WeaponRulePackets.Edit.ID))throw new java.io.IOException("Somente o anfitriao ou administrador altera as regras deste mundo.");
        ClientPlayNetworking.send(new WeaponRulePackets.Edit(revision,item,json));
    }
    public static void register(){
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(upload==null)return;
            if(client.getNetworkHandler()==null||!ClientPlayNetworking.canSend(WeaponRulePackets.Upload.ID)){upload=null;return;}
            int chunks=(upload.length()+WeaponRulePackets.CHUNK-1)/WeaponRulePackets.CHUNK;
            for(int sent=0;sent<4&&uploadIndex<chunks;sent++,uploadIndex++){
                int start=uploadIndex*WeaponRulePackets.CHUNK;
                ClientPlayNetworking.send(new WeaponRulePackets.Upload(uploadRevision,uploadIndex,chunks,upload.substring(start,Math.min(upload.length(),start+WeaponRulePackets.CHUNK))));
            }
            if(uploadIndex==chunks)upload=null;
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{required=ClientPlayNetworking.canSend(WeaponRulePackets.Edit.ID);if(required)WeaponConfig.startWorld();});
        ClientPlayNetworking.registerGlobalReceiver(WeaponRulePackets.Part.ID,(part,ctx)->ctx.client().execute(()->receive(part)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{revision=incoming=-1;editable=required=false;pending=null;upload=null;WeaponConfig.clearWorld();});
    }
    private static void receive(WeaponRulePackets.Part p){
        if(p.count()<1||p.count()>(WorldWeaponRules.MAX_BYTES/WeaponRulePackets.CHUNK+1)||p.index()<0||p.index()>=p.count()||p.revision()<revision)return;
        if(p.index()==0){incoming=p.revision();expected=0;count=p.count();pending=new StringBuilder();}
        if(pending==null||p.revision()!=incoming||p.index()!=expected||p.count()!=count)return;
        pending.append(p.json());expected++;
        if(pending.length()>WorldWeaponRules.MAX_BYTES){pending=null;return;}
        if(expected==count){try{WeaponConfig.applyWorld(pending.toString());revision=incoming;editable=p.editable();}catch(Exception e){EldenSteveClient.LOG.error("Invalid world weapon rules",e);}finally{pending=null;}}
    }
}
