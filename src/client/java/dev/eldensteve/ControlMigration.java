package dev.eldensteve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import org.lwjgl.glfw.GLFW;

/** Migrate former defaults once; unrelated/custom bindings are left intact. */
public final class ControlMigration {
    private static void change(KeyBinding key,int old,int replacement){if(KeyBindingHelper.getBoundKeyOf(key).getCode()==old)key.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(replacement));}
    public static void apply(MinecraftClient client){
        var marker=FabricLoader.getInstance().getConfigDir().resolve("eldenflow-controls-v1.txt");if(Files.exists(marker))return;
        change(EldenSteveClient.interact,GLFW.GLFW_KEY_E,GLFW.GLFW_KEY_V);
        change(EldenSteveClient.dodge,GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_LEFT_ALT);
        change(EldenSteveClient.walk,GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_UNKNOWN);
        change(EldenSteveClient.sprint,GLFW.GLFW_KEY_UNKNOWN,GLFW.GLFW_KEY_LEFT_SHIFT);
        // crouch is private; find only our registered action by its stable translation key.
        for(var key:client.options.allKeys)if(key.getTranslationKey().equals("key.eldensteve.crouch"))change(key,GLFW.GLFW_KEY_X,GLFW.GLFW_KEY_LEFT_CONTROL);
        KeyBinding.updateKeysByCode();client.options.write();
        try{Files.writeString(marker,"Minecraft inventory E; jump Space; sprint Shift; crouch Ctrl; roll Alt; heavy R; skill F; interact/grip V.\n");}catch(java.io.IOException e){EldenSteveClient.LOG.warn("Could not record control migration",e);}
    }
}
