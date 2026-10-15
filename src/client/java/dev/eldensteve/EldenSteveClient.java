package dev.eldensteve;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class EldenSteveClient implements ClientModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("eldensteve");
    public static boolean enabled=false,drive=false,gameData=false;
    public static float[] state=new float[12];
    public static float previousFrame;
    private static float previousYaw;
    public static int previousAction;
    private static NativeCore core;
    private static Object world;
    static KeyBinding toggle,dodge,walk,sprint,inventory,lock,jump,interact,heavy,skill;
    private static KeyBinding test,lab,debug,mouseLock,stock,crouch,targetLeft,targetRight,editor;
    private static String error="";
    private static final boolean DEV_TOOLS=Boolean.getBoolean("eldensteve.devTools");
    private static Perspective normalPerspective;
    public void onInitializeClient(){WeaponConfig.load();Bonfire.register();ProgressionClient.register();WorldRulesClient.register();CombatChannel.register();
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(EldenFlow.SOUL_ORB,SoulOrbRenderer::new);
        InventoryAttributes.register();LockReticle.register();WeaponDescriptions.register();StaggerIndicator.register();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(c->{
            ControlMigration.apply(c);
            long start=System.nanoTime();int count=0;
            for(var item:net.minecraft.registry.Registries.ITEM){WeaponProfiles.detect(new net.minecraft.item.ItemStack(item));count++;}
            LOG.info("Indexed {} item names in {} ms",count,(System.nanoTime()-start)/1_000_000);
        });
        toggle=key("toggle",GLFW.GLFW_KEY_F6);dodge=key("dodge_elden",GLFW.GLFW_KEY_LEFT_ALT);walk=key("walk_elden",GLFW.GLFW_KEY_UNKNOWN);
        jump=key("jump_elden",GLFW.GLFW_KEY_SPACE);interact=key("interact_elden",GLFW.GLFW_KEY_V);
        heavy=key("heavy_elden",GLFW.GLFW_KEY_R);skill=key("skill_elden",GLFW.GLFW_KEY_F);
        test=key("test",DEV_TOOLS?GLFW.GLFW_KEY_F7:GLFW.GLFW_KEY_UNKNOWN);
        sprint=key("sprint_optional",GLFW.GLFW_KEY_LEFT_SHIFT);
        inventory=key("inventory",GLFW.GLFW_KEY_I);
        lock=key("lock_elden",GLFW.GLFW_KEY_Q);
        mouseLock=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.eldensteve.lock_mouse",InputUtil.Type.MOUSE,GLFW.GLFW_MOUSE_BUTTON_MIDDLE,"category.eldensteve"));
        lab=key("lab",GLFW.GLFW_KEY_F8);
        editor=key("editor",GLFW.GLFW_KEY_F4);
        crouch=key("crouch",GLFW.GLFW_KEY_LEFT_CONTROL);
        stock=key("stock",DEV_TOOLS?GLFW.GLFW_KEY_F10:GLFW.GLFW_KEY_UNKNOWN);
        targetLeft=key("target_left",GLFW.GLFW_KEY_LEFT_BRACKET);targetRight=key("target_right",GLFW.GLFW_KEY_RIGHT_BRACKET);
        debug=key("debug",DEV_TOOLS?GLFW.GLFW_KEY_F9:GLFW.GLFW_KEY_UNKNOWN);CombatOverlay.register();
        ClientTickEvents.START_CLIENT_TICK.register(EldenSteveClient::tick);
        HudRenderCallback.EVENT.register((ctx,counter)->{
            MinecraftClient c=MinecraftClient.getInstance();if(c.player==null||c.options.hudHidden)return;
            if(enabled){
                int length=(int)Math.clamp(120+(state[7]-97)*.7,120,180);
                int x=ctx.getScaledWindowWidth()/2-length/2,y=ctx.getScaledWindowHeight()-58;
                int w=state[7]>0?(int)(length*Math.clamp(state[6]/state[7],0,1)):0;
                ctx.fill(x,y,x+length+2,y+7,0xFF181B13);ctx.fill(x+1,y+1,x+length+1,y+6,0xFF34382B);
                ctx.fill(x+1,y+1,x+1+w,y+6,0xFF56883B);ctx.fill(x+1,y+1,x+1+w,y+2,0xFFACE46B);
                for(int i=18;i<length;i+=18)ctx.fill(x+1+i,y+2,x+2+i,y+6,0x55303721);
                String amount=Math.max(0,(int)state[6])+"/"+Math.max(0,(int)state[7]);
                ctx.drawTextWithShadow(c.textRenderer,amount,Math.min(ctx.getScaledWindowWidth()-c.textRenderer.getWidth(amount)-4,x+length+6),y-9,0xC5F39B);
                ctx.drawTextWithShadow(c.textRenderer,"Almas: "+(c.player.isCreative()?"∞":ProgressionClient.view().souls()),x,y-9,0xB6A0EA);
            }
        });
        LOG.info("Elden/Steve initialized. Upstream Rust controller; Minecraft collision.");
    }
    private static KeyBinding key(String name,int code){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.eldensteve."+name,InputUtil.Type.KEYSYM,code,"category.eldensteve"));}
    private static void tick(MinecraftClient c){
        drive=false;Gamepad.poll();StepSmoothing.tick();
        if(world!=c.world){release();world=c.world;WeaponProfiles.invalidate();enabled=false;error="";}
        while(editor.wasPressed())if(c.player!=null&&c.currentScreen==null)c.setScreen(new WeaponScreen(null));
        while(test.wasPressed())if(DEV_TOOLS)SmokeTest.start(c);
        while(debug.wasPressed())if(DEV_TOOLS)CombatOverlay.debug=!CombatOverlay.debug;
        CombatOverlay.blade=new float[107];
        while(stock.wasPressed())if(DEV_TOOLS)CombatLab.stock(c);
        while(lab.wasPressed())CombatLab.create(c);
        if(DEV_TOOLS)SmokeTest.beforeTick(c);
        boolean padToggle=Gamepad.toggle;
        while(toggle.wasPressed()||padToggle){
            padToggle=false;
            if(c.player==null)continue;
            if(!enabled&&WorldRulesClient.waiting()){c.player.sendMessage(Text.literal("Recebendo as regras de armas deste mundo..."),true);continue;}
            if(!CombatChannel.available()){c.player.sendMessage(Text.literal("Este servidor precisa do Elden Flow para ativar o combate."),false);continue;}
            if(enabled)deactivate(c);
            else activate(c);
            c.options.sneakKey.setPressed(false);c.options.sprintKey.setPressed(false);
            c.player.setSneaking(false);c.player.setSprinting(false);c.player.input.sneaking=false;
        }
        if(enabled&&c.player!=null&&c.player.isTouchingWater())deactivate(c);
        if(Bonfire.tick(c)){var authoritative=CombatChannel.frame(c.player.getId());if(authoritative!=null){state[6]=authoritative.state()[6];state[7]=authoritative.state()[7];}if(Gamepad.inventory)c.setScreen(new BonfireScreen());while(interact.wasPressed())if(Bonfire.active())c.setScreen(new BonfireScreen());return;}
        if(!enabled||core==null||c.player==null||c.world==null){while(inventory.wasPressed()){}return;}
        var p=c.player;
        if(c.isPaused())return; // Retain the last articulated pose while the world is paused.
        if(Gamepad.previousItem!=Gamepad.nextItem)
            p.getInventory().selectedSlot=Math.floorMod(p.getInventory().selectedSlot+(Gamepad.nextItem?1:-1),9);
        Combat.synchronizeEquipment();Combat.pollGrip();ShieldCombat.tick(c);WeaponUse.tick(c);
        if(Gamepad.interact)Combat.interact();
        if(Gamepad.lock)Combat.toggleLock();
        if(Gamepad.attack&&Combat.armed())Combat.queue();
        if(Gamepad.inventory&&c.currentScreen==null)c.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(p));
        while(interact.wasPressed()){}
        boolean crouchCommand=Gamepad.crouch;
        while(crouch.wasPressed())crouchCommand=true;
        if(crouchCommand)core.crouch();
        while(lock.wasPressed())Combat.toggleLock();
        while(mouseLock.wasPressed())if(!EldenInput.shift())Combat.toggleLock();
        Combat.tickCamera();
        while(targetLeft.wasPressed())Combat.cycleTarget(-1);
        while(targetRight.wasPressed())Combat.cycleTarget(1);
        while(inventory.wasPressed())if(c.currentScreen==null)c.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(p));
        boolean supported=p.isAlive()&&!p.isSpectator()&&!p.getAbilities().flying&&!p.hasVehicle()&&!p.isTouchingWater()&&!p.isInLava()&&!p.isClimbing()&&!p.isFallFlying()&&!p.isUsingItem()&&!Gamepad.use;
        float right=(EldenInput.down(c.options.rightKey)?1:0)-(EldenInput.down(c.options.leftKey)?1:0);
        float forward=(EldenInput.down(c.options.forwardKey)?1:0)-(EldenInput.down(c.options.backKey)?1:0);
        if(Gamepad.active){right=Gamepad.right;forward=Gamepad.forward;}
        previousFrame=state[4];previousAction=(int)state[3];
        previousYaw=state[2];
        try{
            var profile=Combat.profile();core.equip(profile==null?1:profile.nativeIndex(),Combat.twoHand?(Combat.twoHandLeft?2:1):0);core.left(Combat.offhand()==null?24:Combat.offhand().nativeIndex());var active=Combat.activeProfile();if(active!=null)core.weight(dev.eldensteve.progression.ProgressionRules.relativeWeight(active.weight(),ProgressionClient.view().level(dev.eldensteve.progression.Attribute.CAPACITY)));
            var target=Combat.target();
            boolean attack=Combat.consume()&&!ShieldCombat.busy();core.spend(ShieldCombat.drain());if(ShieldCombat.busy()){right=0;forward=0;}
            boolean pairAttack=attack&&Combat.paired();attack=attack&&!pairAttack;
            core.capacity(ProgressionClient.maximumStamina());core.adjustStamina(CombatChannel.correction());
            state=core.tick(right,forward,(float)Math.toRadians(-CombatCamera.inputYaw(p.getYaw())),supported&&(EldenInput.down(dodge)||Gamepad.roll),EldenInput.down(walk),supported,supported&&(EldenInput.down(sprint)||Gamepad.sprint),supported&&attack,Combat.armed()&&!ShieldCombat.busy(),supported&&!ShieldCombat.busy()&&(EldenInput.down(jump)||Gamepad.jump),(float)p.getY(),(float)Combat.floor(),p.isOnGround(),target==null?0:(float)(target.getX()-p.getX()),target==null?0:(float)(target.getZ()-p.getZ()),target!=null,supported&&Combat.armed()&&(Combat.heavyHeld()||Gamepad.heavy&&!Combat.modifier()),supported&&Combat.armed()&&(pairAttack||Combat.leftHeld()));
            int buttons=0;
            if(supported&&c.currentScreen==null&&c.isWindowFocused()){
                if(EldenInput.down(dodge)||Gamepad.roll)buttons|=dev.eldensteve.combat.CombatInput.ROLL;
                if(EldenInput.down(walk))buttons|=dev.eldensteve.combat.CombatInput.WALK;
                if(EldenInput.down(sprint)||Gamepad.sprint)buttons|=dev.eldensteve.combat.CombatInput.SPRINT;
                if(attack||pairAttack)buttons|=dev.eldensteve.combat.CombatInput.LIGHT;
                if(EldenInput.down(jump)||Gamepad.jump)buttons|=dev.eldensteve.combat.CombatInput.JUMP;
                if(Combat.heavyHeld()||Gamepad.heavy&&!Combat.modifier())buttons|=dev.eldensteve.combat.CombatInput.HEAVY;
                if(Combat.leftHeld())buttons|=dev.eldensteve.combat.CombatInput.GUARD;
                if(crouchCommand)buttons|=dev.eldensteve.combat.CombatInput.CROUCH;
                if(ShieldCombat.busy()&&ShieldCombat.frame()==0)buttons|=dev.eldensteve.combat.CombatInput.PARRY;
            }
            CombatChannel.send(supported&&c.currentScreen==null&&c.isWindowFocused()?right:0,supported&&c.currentScreen==null&&c.isWindowFocused()?forward:0,(float)Math.toRadians(-CombatCamera.inputYaw(p.getYaw())),buttons,target==null?-1:target.getId(),Combat.twoHand?(Combat.twoHandLeft?2:1):0,state[6]);
            drive=supported&&c.currentScreen==null&&c.isWindowFocused();
            if(drive){p.setSprinting(false);p.setSneaking(false);}
            int animation=ShieldCombat.busy()?692040040:core.animationId();
            SteveAnimations.motion(state[0],state[1],state[2]);
            if(dev.eldensteve.combat.Consumption.active(p))SteveAnimations.consume(p.getItemUseTime());
            else SteveAnimations.tick(animation,ShieldCombat.busy()?ShieldCombat.frame():state[4],ShieldCombat.busy()||state[3]>0&&core.animationId()!=202040,state[8],supported);
            if(supported){Combat.applyHit(core);dev.eldensteve.combat.DefenseVisuals.put(p.getUuid(),core.defense());}else clearDefense();
        }catch(Throwable ex){fail(ex);}
    }
    static void activate(MinecraftClient c){
        if(c.player==null||c.player.isTouchingWater())return;
        try{
            if(!enabled)normalPerspective=CombatCamera.returnPerspective(c.options.getPerspective());
            release();core=new NativeCore();state=new float[12];gameData=core.hasGameData();enabled=true;error="";
            CombatCamera.enter(normalPerspective);c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            LOG.info("Controller active: gameData={}",gameData);
        }catch(Throwable ex){fail(ex);}
    }
    private static void deactivate(MinecraftClient c){
        enabled=false;release();state=new float[12];
        if(normalPerspective!=null)CombatCamera.exit(normalPerspective);
        normalPerspective=null;
    }
    public static boolean drives(LivingEntity e){return drive&&e==MinecraftClient.getInstance().player;}
    public static boolean visuals(LivingEntity e){return enabled&&e==MinecraftClient.getInstance().player&&e.isAlive()&&!e.isSpectator()&&!e.hasVehicle()&&!e.isTouchingWater()&&!e.isInLava()&&!e.isClimbing()&&!e.isFallFlying();}
    static void restReset(){
        if(core!=null)core.reset();float maximum=state[7];state=new float[12];state[6]=state[7]=maximum;
        drive=false;clearDefense();ShieldCombat.reset();Combat.reset();
    }
    public static float renderYaw(float delta){float difference=state[2]-previousYaw;return previousYaw+(float)Math.atan2(Math.sin(difference),Math.cos(difference))*delta;}
    public static Vec3d travel(LivingEntity e,Vec3d input){if(!drives(e))return input;double y=state[10];if(e.isOnGround()&&y<=0)y=Math.min(-.08,y);e.setVelocity(state[0],y,state[1]);return Vec3d.ZERO;}
    public static float rollProgress(float d){float f=previousAction==(int)state[3]?previousFrame+(state[4]-previousFrame)*d:state[4];return state[5]>0?Math.clamp(f/state[5],0,1):0;}
    private static void fail(Throwable ex){LOG.error("Controller disabled safely",ex);error="falha; veja logs/latest.log";enabled=false;drive=false;release();}
    private static void clearDefense(){var p=MinecraftClient.getInstance().player;if(p!=null)dev.eldensteve.combat.DefenseVisuals.put(p.getUuid(),0);}
    private static void release(){drive=false;Bonfire.reset();StepSmoothing.reset();clearDefense();ShieldCombat.reset();Combat.reset();SteveAnimations.reset();if(core!=null){core.close();core=null;}}
}
