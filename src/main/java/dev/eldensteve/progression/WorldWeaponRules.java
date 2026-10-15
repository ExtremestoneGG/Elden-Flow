package dev.eldensteve.progression;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Set;

/** Canonical world-owned presets; never aliases the host's personal file after creation. */
public final class WorldWeaponRules {
    public static final int MAX_BYTES=16*1024*1024;
    private static final Gson JSON=new Gson();
    private static final Set<String> TYPES=Set.of("auto","off","sai","cutlass","longsword","rapier","twinblade","katana","claymore","greathammer","greataxe","spear","glaive","scythe","halberd","axe","hammer","shield");
    private final Path path;
    private JsonObject document;
    private long revision;
    private String encoded;
    public WorldWeaponRules(Path path,Path seed)throws IOException{
        this.path=path;
        if(Files.exists(path))document=read(path);
        else {document=Files.isRegularFile(seed)?read(seed):empty();write(document);}
    }
    private static JsonObject empty(){var o=new JsonObject();o.addProperty("schema",3);o.addProperty("automatic",true);o.add("items",new JsonObject());return o;}
    private static JsonObject read(Path p)throws IOException{
        if(Files.size(p)>MAX_BYTES)throw new IOException("Weapon preset too large");
        try{return normalize(JsonParser.parseString(Files.readString(p)).getAsJsonObject());}
        catch(RuntimeException e){throw new IOException("Invalid world weapon preset; original preserved",e);}
    }
    public static JsonObject normalize(JsonObject source){
        if(source.has("schema")&&source.get("schema").getAsInt()>3)throw new IllegalArgumentException("Unsupported schema");
        var items=source.getAsJsonObject("items");if(items==null||items.size()>50000)throw new IllegalArgumentException("Invalid item count");
        var out=empty();if(source.has("automatic"))out.addProperty("automatic",source.get("automatic").getAsBoolean());
        for(var row:items.entrySet()){
            validateId(row.getKey());out.getAsJsonObject("items").add(row.getKey(),entry(row.getValue().getAsJsonObject()));
        }return out;
    }
    private static void validateId(String id){if(id.length()>256||!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid item ID");}
    private static float value(JsonObject o,String key,float fallback,float lo,float hi){if(!o.has(key))return fallback;float f=o.get(key).getAsFloat();if(!Float.isFinite(f))throw new IllegalArgumentException("Non-finite transform");return Math.clamp(f,lo,hi);}
    private static JsonObject transform(JsonObject source){
        var out=new JsonObject();for(String a:new String[]{"x","y","z"})out.addProperty(a,value(source,a,0,-8,8));
        for(String a:new String[]{"rx","ry","rz"})out.addProperty(a,value(source,a,0,-360,360));
        out.addProperty("scale",value(source,"scale",1,.05f,8));return out;
    }
    private static JsonObject entry(JsonObject source){
        var out=new JsonObject();String mode=source.has("mode")?source.get("mode").getAsString():"auto";
        if(!TYPES.contains(mode))throw new IllegalArgumentException("Unknown weapon class");
        out.addProperty("mode",mode);out.addProperty("parry",!source.has("parry")||source.get("parry").getAsBoolean());
        var legacy=source.deepCopy();if(source.has("rotation"))legacy.add("rz",source.get("rotation"));
        for(String hand:new String[]{"left","right"})out.add(hand,transform(source.has(hand)&&source.get(hand).isJsonObject()?source.getAsJsonObject(hand):legacy));
        var poses=new JsonObject();if(source.has("poses")&&source.get("poses").isJsonObject())for(var pose:source.getAsJsonObject("poses").entrySet()){
            if(!pose.getKey().matches("[123]_(left|right)"))continue;poses.add(pose.getKey(),transform(pose.getValue().getAsJsonObject()));
        }out.add("poses",poses);return out;
    }
    public String json(){if(encoded==null)encoded=JSON.toJson(document);return encoded;}
    public long revision(){return revision;}
    public boolean parry(String itemId){var item=document.getAsJsonObject("items").getAsJsonObject(itemId);return item==null||item.get("parry").getAsBoolean();}
    public boolean automatic(){return document.get("automatic").getAsBoolean();}
    public String mode(String itemId){var item=document.getAsJsonObject("items").getAsJsonObject(itemId);return item==null?"auto":item.get("mode").getAsString();}
    /** Validate the entire import before touching either the current document or its save. */
    public int importPreset(String json)throws IOException{
        if(json.length()>MAX_BYTES)throw new IllegalArgumentException("Preset too large");
        var incoming=normalize(JsonParser.parseString(json).getAsJsonObject());
        var next=document.deepCopy();var items=next.getAsJsonObject("items");
        incoming.getAsJsonObject("items").entrySet().forEach(e->items.add(e.getKey(),e.getValue().deepCopy()));
        if(items.size()>50000)throw new IllegalArgumentException("Too many items");
        next.add("automatic",incoming.get("automatic"));
        byte[] bytes=JSON.toJson(next).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Weapon preset too large");
        if(Files.exists(path))Files.copy(path,path.resolveSibling(path.getFileName()+".before-import-"+java.util.UUID.randomUUID()));
        write(next);document=next;encoded=null;revision++;
        return incoming.getAsJsonObject("items").size();
    }
    public void update(String itemId,String json)throws IOException{
        validateId(itemId);if(json.length()>8192)throw new IllegalArgumentException("Entry too large");
        var next=document.deepCopy();var items=next.getAsJsonObject("items");
        if(!items.has(itemId)&&items.size()>=50000)throw new IllegalArgumentException("Too many items");
        items.add(itemId,entry(JsonParser.parseString(json).getAsJsonObject()));write(next);document=next;encoded=null;revision++;
    }
    private void write(JsonObject next)throws IOException{
        byte[] bytes=JSON.toJson(next).getBytes(java.nio.charset.StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("Weapon preset too large");
        Files.createDirectories(path.getParent());var temp=path.resolveSibling(path.getFileName()+".tmp");Files.write(temp,bytes);
        try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
}
