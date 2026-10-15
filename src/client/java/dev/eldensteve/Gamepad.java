package dev.eldensteve;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class Gamepad {
    public static boolean active,roll,jump,sprint,heavy,attack,lock,inventory,toggle,modifier,crouch,left;
    public static float right,forward,lookX;
    public static boolean previousItem,nextItem,use,skill,skillHeld,gripRight,gripLeft,interact;
    private static final boolean installed=FabricLoader.getInstance().isModLoaded("controlify");
    public static void poll(){
        active=roll=jump=sprint=heavy=attack=lock=inventory=toggle=modifier=crouch=left=false;right=forward=lookX=0;
        previousItem=nextItem=use=skill=skillHeld=gripRight=gripLeft=interact=false;
        var c=MinecraftClient.getInstance();
        if(installed&&c.player!=null&&c.currentScreen==null&&c.isWindowFocused()&&!c.isPaused())ControlifyBridge.poll();
        else if(installed)ControlifyBridge.resetInput();
    }
    public static boolean suppress(Identifier id){
        var c=MinecraftClient.getInstance();
        if(c.currentScreen!=null||!id.getNamespace().equals("controlify"))return false;

        if(ControlifyBridge.suppressTransition()&&(id.getPath().equals("sneak")||id.getPath().equals("sprint")||id.getPath().equals("change_perspective")))return true;
        if(!EldenSteveClient.enabled)return false;
        return switch(id.getPath()){
            case "jump","sprint","sneak","inventory","next_slot","prev_slot","swap_hands","drop","radial_menu" -> true;
            case "attack","use","open_chat","pick_block","change_perspective" -> Combat.armed();
            default -> false;
        };
    }
}
