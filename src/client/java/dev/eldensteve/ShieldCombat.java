package dev.eldensteve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
/** Local animation prediction only; guard, stamina costs and parry rewards belong to the server. */
public final class ShieldCombat {
    private static int guardTicks,parryTicks,drain;
    private static boolean skillWas;
    public static boolean isShield(ItemStack s){var p=WeaponProfiles.get(s);return p!=null&&p.type().equals("shield");}
    private static ItemStack blockingStack(){
        var c=MinecraftClient.getInstance();if(c.player==null)return ItemStack.EMPTY;
        if(Combat.twoHand)return Combat.twoHandLeft?c.player.getOffHandStack():c.player.getMainHandStack();
        if(isShield(c.player.getOffHandStack()))return c.player.getOffHandStack();
        return isShield(c.player.getMainHandStack())?c.player.getMainHandStack():ItemStack.EMPTY;
    }
    public static boolean equipped(){return isShield(blockingStack());}
    public static boolean canParry(){var stack=blockingStack();return !stack.isEmpty()&&WeaponProfiles.get(stack)!=null&&WeaponConfig.get(stack).parry;}
    public static boolean busy(){return parryTicks>0;}
    public static float frame(){return (18-parryTicks)*2.5f;}
    public static int drain(){int n=drain;drain=0;return n;}
    public static void reset(){guardTicks=parryTicks=drain=0;skillWas=false;}
    public static void tick(MinecraftClient c){
        if(!EldenSteveClient.enabled||c.player==null||c.currentScreen!=null||c.isPaused()||!c.isWindowFocused()||blockingStack().isEmpty()){reset();return;}
        boolean skill=EldenInput.down(EldenSteveClient.skill);
        boolean pressed=Gamepad.skill||skill&&!skillWas;skillWas=skill;
        if(parryTicks>0)parryTicks--;
        if(pressed&&canParry()&&!Combat.modifier()&&parryTicks==0&&EldenSteveClient.state[3]==0&&EldenSteveClient.state[6]>=12){parryTicks=18;drain+=12;}
        boolean guard=!skill&&!Combat.modifier()&&!busy()&&(Gamepad.left||EldenInput.down(c.options.useKey))&&EldenSteveClient.state[3]==0;
        guardTicks=guard?guardTicks+1:0;
    }
    public static boolean guarding(){return guardTicks>=3;}
}
