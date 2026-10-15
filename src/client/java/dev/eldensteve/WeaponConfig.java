package dev.eldensteve;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/** Only stable item IDs are persisted, never translated names or type-selector IDs. */
public final class WeaponConfig {
    public static final class Transform {
        public float x,y,z,rx,ry,rz,scale=1;
        public Transform copy(){var t=new Transform();t.x=x;t.y=y;t.z=z;t.rx=rx;t.ry=ry;t.rz=rz;t.scale=scale;return t;}
        private static float angle(float value){return Float.isFinite(value)?(value%360+540)%360-180:0;}
        public void validate(){x=Entry.bound(x,-8,8,0);y=Entry.bound(y,-8,8,0);z=Entry.bound(z,-8,8,0);rx=angle(rx);ry=angle(ry);rz=angle(rz);scale=Entry.bound(scale,.05f,8,1);}
        public void apply(net.minecraft.client.util.math.MatrixStack m){m.translate(x,y,z);m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(rx));m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(ry));m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rz));m.scale(scale,scale,scale);}
    }
    public static final class Entry {
        public String mode="auto";
        public float x,y,z,rotation,scale=1;
        public boolean parry=true;
        public Transform right,left;
        public Map<String,Transform> poses=new TreeMap<>();
        private static String poseKey(boolean left,int context){return context+"_"+(left?"left":"right");}
        public Transform pose(boolean left,int context){if(context==0||poses==null)return hand(left);return poses.getOrDefault(poseKey(left,context),hand(left));}
        public Transform editPose(boolean left,int context){if(context==0)return hand(left);if(poses==null)poses=new TreeMap<>();return poses.computeIfAbsent(poseKey(left,context),k->hand(left).copy());}
        public void resetPose(boolean left,int context){if(context==0){if(left)this.left=new Transform();else right=new Transform();}else {if(poses==null)poses=new TreeMap<>();poses.put(poseKey(left,context),new Transform());}}
        public Transform hand(boolean isLeft){var t=isLeft?left:right;if(t!=null)return t;t=new Transform();t.x=x;t.y=y;t.z=z;t.rz=rotation;t.scale=scale;if(isLeft)left=t;else right=t;return t;}
        public Entry copy(){var e=new Entry();e.mode=mode;e.x=x;e.y=y;e.z=z;e.rotation=rotation;e.scale=scale;e.parry=parry;e.right=right==null?null:right.copy();e.left=left==null?null:left.copy();if(poses!=null)poses.forEach((k,v)->{if(v!=null)e.poses.put(k,v.copy());});return e;}
        public void validate(){if(mode==null||!mode.equals("auto")&&!mode.equals("off")&&WeaponProfiles.byType(mode)==null)mode="auto";x=bound(x,-1,1,0);y=bound(y,-1,1,0);z=bound(z,-1,1,0);scale=bound(scale,.25f,3,1);rotation=bound(rotation,-180,180,0);hand(false).validate();hand(true).validate();if(poses==null)poses=new TreeMap<>();poses.entrySet().removeIf(e->!e.getKey().matches("[123]_(left|right)")||e.getValue()==null);poses.values().forEach(Transform::validate);}
        private static float bound(float f,float lo,float hi,float fallback){return Float.isFinite(f)?Math.clamp(f,lo,hi):fallback;}
    }
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String,Entry> entries=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Entry DEFAULT=new Entry();
    private static Map<String,Entry> worldEntries;
    private static boolean worldAutomatic;
    public static boolean automatic=true;
    public static boolean automatic(){return worldEntries==null?automatic:worldAutomatic;}
    public static void startWorld(){worldEntries=Map.of();worldAutomatic=false;WeaponProfiles.invalidate();}
    public static void applyWorld(String document){var obj=JsonParser.parseString(document).getAsJsonObject();var parsed=parse(obj);worldAutomatic=!obj.has("automatic")||obj.get("automatic").getAsBoolean();worldEntries=Map.copyOf(parsed);WeaponProfiles.invalidate();}
    public static void clearWorld(){worldEntries=null;WeaponProfiles.invalidate();}
    private static Path path(){return FabricLoader.getInstance().getConfigDir().resolve("eldensteve-weapons.json");}
    public static void load(){
        if(!Files.isRegularFile(path()))return;
        try{var obj=JsonParser.parseString(Files.readString(path())).getAsJsonObject();automatic=!obj.has("automatic")||obj.get("automatic").getAsBoolean();
            entries.putAll(parse(obj));
        }catch(Exception e){EldenSteveClient.LOG.error("Could not read weapon config; using defaults",e);}
    }
    public static Entry get(ItemStack stack){return (worldEntries==null?entries:worldEntries).getOrDefault(Registries.ITEM.getId(stack.getItem()).toString(),DEFAULT);}
    public static void save(String id,Entry entry)throws java.io.IOException{
        if(worldEntries!=null){entry.validate();WorldRulesClient.save(id,JSON.toJson(entry));return;}
        entry.validate();var copy=new TreeMap<>(entries);copy.put(id,entry.copy());
        write(copy);
    }
    private static JsonObject document(Map<String,Entry> copy){var obj=new JsonObject();obj.addProperty("schema",3);obj.addProperty("automatic",automatic);obj.add("items",JSON.toJsonTree(copy));return obj;}
    private static Map<String,Entry> parse(JsonObject obj){
        if(obj.has("schema")&&obj.get("schema").getAsInt()>3)throw new IllegalArgumentException("Formato mais recente");
        var result=new TreeMap<String,Entry>();var items=obj.getAsJsonObject("items");if(items==null)throw new IllegalArgumentException("Lista de itens ausente");
        if(items.size()>50000)throw new IllegalArgumentException("Limite de 50000 itens");
        for(var kv:items.entrySet()){if(net.minecraft.util.Identifier.tryParse(kv.getKey())==null)throw new IllegalArgumentException("ID invalido");var e=JSON.fromJson(kv.getValue(),Entry.class);if(e==null)throw new IllegalArgumentException("Item invalido");e.validate();result.put(kv.getKey(),e);}return result;
    }
    private static void write(Map<String,Entry> copy)throws java.io.IOException{
        var obj=document(copy);
        Files.createDirectories(path().getParent());var tmp=path().resolveSibling("eldensteve-weapons.json.tmp");Files.writeString(tmp,JSON.toJson(obj));
        try{Files.move(tmp,path(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path(),StandardCopyOption.REPLACE_EXISTING);}
        entries.clear();entries.putAll(copy);WeaponProfiles.invalidate();
    }
    public static Path exportPreset()throws java.io.IOException{
        var dir=path().getParent().resolve("eldenflow-presets");Files.createDirectories(dir);
        var dest=dir.resolve("eldenflow-"+System.currentTimeMillis()+".json");var snapshot=document(new TreeMap<>(worldEntries==null?entries:worldEntries));snapshot.addProperty("automatic",automatic());Files.writeString(dest,JSON.toJson(snapshot),StandardOpenOption.CREATE_NEW);return dest;
    }
    public static int importPreset(Path source)throws java.io.IOException{
        if(Files.size(source)>16*1024*1024)throw new java.io.IOException("Arquivo maior que 16 MB");
        var normalized=dev.eldensteve.progression.WorldWeaponRules.normalize(JsonParser.parseString(Files.readString(source)).getAsJsonObject());
        var incoming=parse(normalized);
        if(worldEntries!=null){WorldRulesClient.importPreset(new Gson().toJson(normalized));return incoming.size();}
        var merged=new TreeMap<>(entries);merged.putAll(incoming);
        if(Files.isRegularFile(path()))Files.copy(path(),path().resolveSibling("eldensteve-weapons-before-import-"+System.currentTimeMillis()+".json"));
        write(merged);return incoming.size();
    }
    public static Path importPath(){return path().getParent().resolve("eldenflow-presets/import.json");}
}
