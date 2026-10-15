package dev.eldensteve;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Physical input is sampled separately from the vanilla actions it replaces. */
public final class EldenInput {
    private EldenInput() {}
    public static boolean active() {
        var c=MinecraftClient.getInstance();
        return EldenSteveClient.enabled && c.player!=null && c.currentScreen==null;
    }
    public static boolean down(KeyBinding binding) {
        var c=MinecraftClient.getInstance();
        if(c.currentScreen!=null || !c.isWindowFocused())return false;
        if(SmokeTest.running())return ((dev.eldensteve.mixin.KeyBindingAccess)(Object)binding).eldensteve$rawPressed();
        var key=KeyBindingHelper.getBoundKeyOf(binding);
        if(key.getCategory()==InputUtil.Type.KEYSYM && key.getCode()>=0)
            return InputUtil.isKeyPressed(c.getWindow().getHandle(),key.getCode());
        if(key.getCategory()==InputUtil.Type.MOUSE)
            return GLFW.glfwGetMouseButton(c.getWindow().getHandle(),key.getCode())==GLFW.GLFW_PRESS;
        return binding.isPressed();
    }
    public static boolean shift(){var c=MinecraftClient.getInstance();return InputUtil.isKeyPressed(c.getWindow().getHandle(),GLFW.GLFW_KEY_LEFT_SHIFT)||InputUtil.isKeyPressed(c.getWindow().getHandle(),GLFW.GLFW_KEY_RIGHT_SHIFT);}
    public static boolean blocks(KeyBinding key) {
        if(!active() || key.getCategory().equals("category.eldensteve"))return false;
        var o=MinecraftClient.getInstance().options;
        if(key==o.sneakKey || key==o.sprintKey ||key==o.dropKey||key==o.swapHandsKey||key==o.pickItemKey)return true;
        if(key==o.jumpKey)return EldenSteveClient.drive;
        // Horizontal movement has exactly one owner on the ground. Vanilla still handles air/water.
        if(key==o.forwardKey||key==o.backKey||key==o.leftKey||key==o.rightKey)
            return EldenSteveClient.drive;
        return key.equals(EldenSteveClient.dodge)||key.equals(EldenSteveClient.walk)||key.equals(EldenSteveClient.sprint);
    }
}
