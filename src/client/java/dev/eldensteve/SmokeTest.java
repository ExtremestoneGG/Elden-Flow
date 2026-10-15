package dev.eldensteve;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

/** Opt-in in-game regression. Only writes to the explicitly named disposable lab world. */
public final class SmokeTest {
    public static boolean running(){return t>=0;}
    private static int t=-1,rolls,backsteps;
    private static double startZ;
    private static boolean sprint;
    private static final StringBuilder report=new StringBuilder();
    public static void start(MinecraftClient c){
        if(c.player==null||c.getServer()==null)return;
        if(!c.getServer().getSaveProperties().getLevelName().equals("Elden Prototype Lab")){
            c.player.sendMessage(Text.literal("F7: use um mundo descartavel chamado Elden Prototype Lab."),false);return;
        }
        if(t>=0)return;
        report.setLength(0);rolls=0;backsteps=0;sprint=false;t=0;
        var server=c.getServer();var uuid=c.player.getUuid();
        server.execute(()->{
            var p=server.getPlayerManager().getPlayer(uuid);if(p==null)return;
            var w=p.getServerWorld();
            for(int x=-8;x<=8;x++)for(int z=-8;z<=32;z++){
                w.setBlockState(new BlockPos(x,80,z),Blocks.STONE.getDefaultState());
                for(int y=81;y<=85;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
            }
            for(int x=-5;x<=5;x++)for(int y=81;y<=84;y++)w.setBlockState(new BlockPos(x,y,10),Blocks.STONE_BRICKS.getDefaultState());
            p.changeGameMode(GameMode.SURVIVAL);p.setHealth(20);p.getHungerManager().setFoodLevel(20);
            p.requestTeleport(0.5,81,0.5);p.setYaw(0);p.setPitch(10);
        });
        c.player.sendMessage(Text.literal("Teste da ponte iniciado no laboratorio (~15 segundos)."),false);
    }
    public static void beforeTick(MinecraftClient c){
        if(t<0)return;
        if(c.player==null||c.getServer()==null){t=-1;release(c);return;}
        if(c.isPaused()||c.currentScreen!=null)return;
        c.options.forwardKey.setPressed(false);c.options.backKey.setPressed(false);
        c.options.leftKey.setPressed(false);c.options.rightKey.setPressed(false);
        c.options.jumpKey.setPressed(false);c.options.sprintKey.setPressed(false);
        EldenSteveClient.dodge.setPressed(false);EldenSteveClient.walk.setPressed(false);
        c.player.setYaw(0);c.player.setPitch(12);
        if(EldenSteveClient.state[3]==1)rolls++;
        if(EldenSteveClient.state[3]==2)backsteps++;
        sprint|=EldenSteveClient.state[9]>0;
        if(t==35)EldenSteveClient.activate(c);
        if(t==39){
            c.options.sneakKey.setPressed(true);c.options.sprintKey.setPressed(true);
            check("vanilla sneak and sprint suppressed",!c.options.sneakKey.isPressed()&&!c.options.sprintKey.isPressed(),"mode="+EldenSteveClient.enabled);
            check("vanilla inventory suppressed",EldenInput.blocks(c.options.inventoryKey),"inventory routed to I");
            c.options.sneakKey.setPressed(false);c.options.sprintKey.setPressed(false);
        }
        if(t>=40&&t<105)c.options.forwardKey.setPressed(true);
        if(t>=82&&t<84)EldenSteveClient.dodge.setPressed(true);
        if(t==108){check("walk and roll stop at solid wall",c.player.getZ()>7&&c.player.getZ()<9.71,"z="+c.player.getZ());reset(c);}
        if(t==60)check("vanilla horizontal input suppressed",!c.options.forwardKey.isPressed(),"native movement active");
        if(t==130)startZ=c.player.getZ();
        if(t>=130&&t<134)c.options.forwardKey.setPressed(true);
        if(t>=130&&t<132)EldenSteveClient.dodge.setPressed(true);
        if(t==155){double d=c.player.getZ()-startZ;check("roll translates player",d>1&&d<7,"distance="+d);check("native roll state observed",rolls>0,"ticks="+rolls);reset(c);}
        if(t>=178&&t<205){c.options.forwardKey.setPressed(true);EldenSteveClient.dodge.setPressed(true);}
        if(t>=205&&t<210)c.options.forwardKey.setPressed(true);
        if(t==210){check("hold activates sprint",sprint,"sprint="+sprint);check("sprint release does not roll",EldenSteveClient.state[3]!=1,"state="+EldenSteveClient.state[3]);reset(c);}
        if(t==232)startZ=c.player.getZ();
        if(t>=232&&t<234)EldenSteveClient.dodge.setPressed(true);
        if(t==253){check("stationary dodge backsteps",backsteps>0&&c.player.getZ()<startZ-0.3,"delta="+(c.player.getZ()-startZ));}
        if(t==265){
            check("no native integration error",EldenSteveClient.enabled,"enabled="+EldenSteveClient.enabled);
            try{Files.writeString(FabricLoader.getInstance().getGameDir().resolve("eldensteve-smoke.txt"),report.toString());}catch(Exception e){EldenSteveClient.LOG.error("Cannot write test report",e);}
            c.player.sendMessage(Text.literal(report.indexOf("FAIL")<0?"Testes passaram. Veja eldensteve-smoke.txt":"Teste falhou: veja eldensteve-smoke.txt"),false);
            release(c);t=-1;return;
        }
        t++;
    }
    private static void reset(MinecraftClient c){var s=c.getServer();var id=c.player.getUuid();s.execute(()->{var p=s.getPlayerManager().getPlayer(id);if(p!=null)p.requestTeleport(0.5,81,0.5);});EldenSteveClient.activate(c);}
    private static void check(String label,boolean pass,String value){String line=(pass?"PASS ":"FAIL ")+label+" | "+value;report.append(line).append('\n');EldenSteveClient.LOG.info(line);}
    private static void release(MinecraftClient c){c.options.forwardKey.setPressed(false);if(EldenSteveClient.dodge!=null)EldenSteveClient.dodge.setPressed(false);}
}
