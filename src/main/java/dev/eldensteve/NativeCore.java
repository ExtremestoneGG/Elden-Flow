package dev.eldensteve;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
/** One native state per owner thread/player. Library loading is shared and synchronized. */
public final class NativeCore implements AutoCloseable {
    public interface Api extends Library {
        int es_abi_version(); int es_has_game_data();
        int es_animation(Pointer handle);
        int es_equip(Pointer handle,int weapon,int twoHand);
        int es_defense(Pointer handle);
        int es_left(Pointer handle,int weapon);
        int es_contacts(Pointer handle,float[] output,int length);
        int es_weight(Pointer handle,float weight);
        void es_impact(Pointer handle);
        void es_hit_hand(Pointer handle,int flags);
        void es_spend(Pointer handle,float amount);
        void es_adjust_stamina(Pointer handle,float delta);
        void es_capacity(Pointer handle,float maximum);
        void es_crouch(Pointer handle);
        int es_blade(Pointer handle,float[] output,int length);
        void es_confirm_hit(Pointer handle);
        Pointer es_create(); void es_destroy(Pointer handle);
        int es_tick(Pointer handle,float[] input,int inputLength,float[] output,int outputLength);
    }
    private static volatile Api api;
    private Pointer handle;
    public NativeCore() throws Exception {
        loadApi();
        handle=api.es_create();if(handle==null)throw new IllegalStateException("Falha no controlador");
    }
    private static synchronized void loadApi()throws Exception{
        if(api==null) {
            if(!System.getProperty("os.name").startsWith("Windows")||!System.getProperty("os.arch").equals("amd64"))throw new IllegalStateException("Este prototipo requer Windows x64.");
            Path dir=FabricLoader.getInstance().getGameDir().resolve("eldensteve/native");
            Files.createDirectories(dir);
            System.setProperty("jna.tmpdir",dir.toAbsolutePath().toString());
            byte[] data;
            try(InputStream in=NativeCore.class.getResourceAsStream("/natives/windows-x86_64/elden_steve_core.dll")) {
                if(in==null)throw new IllegalStateException("DLL ausente: execute Build.ps1.");
                data=in.readAllBytes();
            }
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));
            Path dll=dir.resolve("elden_steve_"+hash+".dll");
            if(!Files.exists(dll)){
                Path temp=Files.createTempFile(dir,"core-",".tmp");
                try{Files.write(temp,data);try{Files.move(temp,dll,StandardCopyOption.ATOMIC_MOVE);}catch(java.nio.file.FileAlreadyExistsException ignored){}}
                finally{Files.deleteIfExists(temp);}
            }
            Api loaded=Native.load(dll.toAbsolutePath().toString(),Api.class);
            if(loaded.es_abi_version()!=1)throw new IllegalStateException("ABI incompativel");api=loaded;
        }
    }
    public void equip(int weapon,boolean twoHand){if(api.es_equip(handle,weapon,twoHand?1:0)!=0)throw new IllegalStateException("Weapon profile");}
    public void equip(int weapon,int grip){if(api.es_equip(handle,weapon,grip)!=0)throw new IllegalStateException("Weapon grip");}
    public void weight(float weight){api.es_weight(handle,weight);}
    public float[] contacts(){float[] result=new float[961];if(api.es_contacts(handle,result,961)!=0)throw new IllegalStateException("Contact ABI");return result;}
    public void impact(){api.es_impact(handle);}
    public void impact(int flags){api.es_hit_hand(handle,flags);}
    public void spend(float amount){api.es_spend(handle,amount);}
    public void adjustStamina(float delta){api.es_adjust_stamina(handle,delta); }
    public void capacity(float maximum){api.es_capacity(handle,maximum);}
    public void reset(){if(handle!=null)api.es_destroy(handle);handle=api.es_create();}
    public void left(int weapon){if(api.es_left(handle,weapon)!=0)throw new IllegalStateException("Offhand profile");}
    public void crouch(){api.es_crouch(handle);}
    public int defense(){return api.es_defense(handle);}
    public boolean hasGameData(){return api.es_has_game_data()!=0;}
    public int animationId(){return api.es_animation(handle);}
    public float[] blade(){float[] out=new float[107];if(api.es_blade(handle,out,107)!=0)throw new IllegalStateException("Blade ABI");return out;}
    public void confirmHit(){api.es_confirm_hit(handle);}
    public float[] tick(float right,float forward,float yaw,boolean dodge,boolean walk,boolean supported,boolean sprint,boolean attack,boolean armed,boolean jump,float y,float floor,boolean grounded,float targetX,float targetZ,boolean locked,boolean heavy,boolean left) {
        float[] out=new float[12];
        float[] in={right,forward,yaw,dodge?1:0,walk?1:0,supported?1:0,sprint?1:0,attack?1:0,armed?1:0,jump?1:0,y,floor,grounded?1:0,targetX,targetZ,locked?1:0,heavy?1:0,left?1:0};
        int status=api.es_tick(handle,in,18,out,12);
        if(status!=0)throw new IllegalStateException("Controlador retornou "+status);
        for(float x:out)if(!Float.isFinite(x))throw new IllegalStateException("Saida invalida");
        if(Math.abs(out[0])>2||Math.abs(out[1])>2)throw new IllegalStateException("Deslocamento fora do limite");
        if(Math.abs(out[10])>4)throw new IllegalStateException("Deslocamento vertical fora do limite");
        return out;
    }
    public void close(){if(handle!=null){api.es_destroy(handle);handle=null;}}
}
