package dev.eldensteve.progression;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Server-owned persistence. A failed purchase write leaves the live balance untouched. */
public final class ProgressionStore {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final Map<UUID,PlayerProgress> players=new HashMap<>();
    private boolean dirty;
    public ProgressionStore(Path path)throws IOException {
        this.path=path;
        if(!Files.exists(path))return;
        try {
            var doc=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if(doc.get("schema").getAsInt()!=1)throw new IOException("Unsupported progression schema");
            for(var row:doc.getAsJsonObject("players").entrySet()){
                var obj=row.getValue().getAsJsonObject();var stats=new EnumMap<Attribute,Integer>(Attribute.class);
                var levels=obj.getAsJsonObject("levels");
                for(var a:Attribute.values())stats.put(a,levels.has(a.key())?levels.get(a.key()).getAsInt():0);
                players.put(UUID.fromString(row.getKey()),new PlayerProgress(obj.get("souls").getAsLong(),obj.get("unspent").getAsInt(),stats));
            }
        }catch(RuntimeException e){throw new IOException("Invalid progression file; refusing to overwrite",e);}
    }
    private PlayerProgress get(UUID id){return players.computeIfAbsent(id,key->new PlayerProgress());}
    public PlayerProgress view(UUID id){return get(id).copy();}
    public void earn(UUID id,long souls){if(souls>0){get(id).earn(souls);dirty=true;}}
    public boolean buy(UUID id,Attribute attribute)throws IOException{
        var next=get(id).copy();if(!next.buy(attribute))return false;
        commit(id,next);return true;
    }
    public boolean buyCreative(UUID id,Attribute a)throws IOException{var next=get(id).copy();if(!next.buyCreative(a))return false;commit(id,next);return true;}
    public void respec(UUID id)throws IOException{var next=get(id).copy();next.respec();commit(id,next);}
    private void commit(UUID id,PlayerProgress next)throws IOException{
        var copy=new HashMap<>(players);copy.put(id,next);write(copy);players.put(id,next);dirty=false;
    }
    public void flush()throws IOException{if(dirty){write(players);dirty=false;}}
    private void write(Map<UUID,PlayerProgress> data)throws IOException{
        var doc=new JsonObject();doc.addProperty("schema",1);var rows=new JsonObject();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            var value=entry.getValue();var obj=new JsonObject();obj.addProperty("souls",value.souls());obj.addProperty("unspent",value.unspent());
            var levels=new JsonObject();for(var a:Attribute.values())levels.addProperty(a.key(),value.level(a));obj.add("levels",levels);rows.add(entry.getKey().toString(),obj);
        });doc.add("players",rows);
        Files.createDirectories(path.getParent());var temp=path.resolveSibling(path.getFileName()+".tmp");
        Files.writeString(temp,JSON.toJson(doc),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
}
