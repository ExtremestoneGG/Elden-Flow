package dev.eldensteve.progression;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Each world owns a preset. LAN visitors receive temporary snapshots, never host file access. */
public final class WeaponRulesServer {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("eldenflow-world-rules");
    private static final class Transfer {
        final String json;final long revision;int index;
        Transfer(WorldWeaponRules rules){json=rules.json();revision=rules.revision();}
        int count(){return Math.max(1,(json.length()+WeaponRulePackets.CHUNK-1)/WeaponRulePackets.CHUNK);}
    }
    private static final class Session {
        final WorldWeaponRules rules;
        final Map<UUID,Transfer> queue=new HashMap<>();
        final Map<UUID,Integer> edits=new HashMap<>();
        final Map<UUID,PresetUpload> uploads=new HashMap<>();
        final Map<UUID,Integer> uploadStarts=new HashMap<>();
        Session(MinecraftServer s)throws java.io.IOException{rules=new WorldWeaponRules(s.getSavePath(WorldSavePath.ROOT).resolve("eldenflow-weapons.json"),FabricLoader.getInstance().getConfigDir().resolve("eldensteve-weapons.json"));}
    }
    private static final Map<MinecraftServer,Session> sessions=new IdentityHashMap<>();
    public static void start(MinecraftServer s){try{sessions.put(s,new Session(s));}catch(java.io.IOException e){LOG.error("World weapon rules disabled; invalid original preserved",e);}}
    public static void stop(MinecraftServer s){sessions.remove(s);}
    public static void leave(ServerPlayerEntity p){var s=sessions.get(p.getServer());if(s!=null){s.queue.remove(p.getUuid());s.edits.remove(p.getUuid());s.uploads.remove(p.getUuid());s.uploadStarts.remove(p.getUuid());}}
    public static boolean editable(ServerPlayerEntity p){return p.getServer().isHost(p.getGameProfile())||p.hasPermissionLevel(2);}
    public static void sync(ServerPlayerEntity p){var s=sessions.get(p.getServer());if(s!=null&&ServerPlayNetworking.canSend(p,WeaponRulePackets.Part.ID))s.queue.put(p.getUuid(),new Transfer(s.rules));}
    public static void edit(ServerPlayerEntity p,WeaponRulePackets.Edit request){
        var s=sessions.get(p.getServer());if(s==null||!editable(p))return;
        int tick=p.getServer().getTicks();if(tick-s.edits.getOrDefault(p.getUuid(),-100)<10){p.sendMessage(Text.literal("Aguarde um instante antes de salvar novamente."),true);return;}s.edits.put(p.getUuid(),tick);
        if(request.revision()!=s.rules.revision()){p.sendMessage(Text.literal("As regras mudaram. Aguarde sincronizar e salve novamente."),false);sync(p);return;}
        try{s.rules.update(request.item(),request.json());for(var other:p.getServer().getPlayerManager().getPlayerList())sync(other);p.sendMessage(Text.literal("Encaixe salvo nas regras deste mundo."),true);}
        catch(Exception e){LOG.warn("Weapon rule edit rejected",e);p.sendMessage(Text.literal("Nao foi possivel salvar o encaixe no mundo."),false);}
    }
    public static WorldWeaponRules rules(MinecraftServer server){var s=sessions.get(server);return s==null?null:s.rules;}
    public static void upload(ServerPlayerEntity p,WeaponRulePackets.Upload request){
        var s=sessions.get(p.getServer());if(s==null||!editable(p))return;
        int tick=p.getServer().getTicks();var id=p.getUuid();
        try{
            if(request.revision()!=s.rules.revision())throw new IllegalArgumentException("As regras mudaram durante a importacao. Sincronize e tente novamente.");
            if(request.index()==0){
                if(tick-s.uploadStarts.getOrDefault(id,-1000)<100)throw new IllegalArgumentException("Aguarde alguns segundos antes de importar novamente.");
                if(s.uploads.size()>=4&&!s.uploads.containsKey(id))throw new IllegalArgumentException("Servidor ocupado com outras importacoes.");
                s.uploadStarts.put(id,tick);s.uploads.put(id,new PresetUpload(request.revision(),request.count(),tick));
            }
            var upload=s.uploads.get(id);if(upload==null)return;
            var document=upload.append(request.revision(),request.index(),request.count(),request.json(),tick);
            if(document!=null){
                s.uploads.remove(id);int imported=s.rules.importPreset(document);
                for(var other:p.getServer().getPlayerManager().getPlayerList())sync(other);
                p.sendMessage(Text.literal("Importados "+imported+" itens nas regras do mundo. Backup anterior salvo."),false);
            }
        }catch(Exception e){s.uploads.remove(id);LOG.warn("World preset import rejected",e);p.sendMessage(Text.literal("Importacao recusada; regras anteriores preservadas. "+e.getMessage()),false);sync(p);}
    }
    public static void tick(MinecraftServer server){
        var s=sessions.get(server);if(s==null)return;
        s.uploads.entrySet().removeIf(e->e.getValue().expired(server.getTicks()));
        for(var it=s.queue.entrySet().iterator();it.hasNext();){var entry=it.next();var p=server.getPlayerManager().getPlayer(entry.getKey());
            if(p==null||!ServerPlayNetworking.canSend(p,WeaponRulePackets.Part.ID)){it.remove();continue;}
            var t=entry.getValue();int count=t.count();
            for(int sent=0;sent<4&&t.index<count;sent++,t.index++){
                int start=t.index*WeaponRulePackets.CHUNK,end=Math.min(t.json.length(),start+WeaponRulePackets.CHUNK);
                ServerPlayNetworking.send(p,new WeaponRulePackets.Part(t.revision,t.index,count,editable(p),t.json.substring(start,end)));
            }if(t.index==count)it.remove();
        }
    }
}
