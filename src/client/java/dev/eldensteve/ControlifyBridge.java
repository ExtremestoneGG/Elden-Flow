package dev.eldensteve;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.*;
import dev.isxander.controlify.api.entrypoint.*;
import dev.isxander.controlify.bindings.*;
import dev.isxander.controlify.bindings.input.*;
import dev.isxander.controlify.controller.ControllerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Only loaded if Controlify is installed. All defaults remain rebindable in its settings. */
public final class ControlifyBridge implements ControlifyEntrypoint {
    private static InputBindingSupplier roll,jump,sprint,heavy,attack,lock,inventory,toggle,crouch,left,previousItem,nextItem,useItem,skill,interactAction;
    private static final StickChord chord=new StickChord();
    private static boolean interactWas,interactUsed,waitNeutral=true;
    private static long stickSuppression;
    public static boolean suppressTransition(){return System.nanoTime()<stickSuppression;}
    public static void resetInput(){chord.reset();interactWas=interactUsed=false;waitNeutral=true;}
    public void onControllersDiscovered(ControlifyApi api){}
    public void onControlifyInit(InitContext context){}
    public void onControlifyPreInit(PreInitContext context){
        roll=bind("roll","Esquiva / segurar para correr","east");
        jump=bind("jump","Pulo","south");crouch=bind("crouch","Agachar","left_stick");
        left=bind("left_attack","Defender / usar habilidade","left_shoulder");
        attack=bind("light","Ataque leve","right_shoulder");
        lock=bind("lock","Fixar / soltar alvo","right_stick");
        inventory=bind("interact","Inventario / modificar empunhadura","north");
        interactAction=bind("interact_world","Interagir com o mundo","dpad_up");
        previousItem=bind("previous_item","Item anterior","dpad_left");
        nextItem=bind("next_item","Proximo item","dpad_right");
        useItem=bind("use_item","Usar item / segurar para comer","dpad_down");
        skill=ControlifyBindApi.get().registerBinding(b->b.id("eldensteve","skill").name(Text.literal("Habilidade / parry do escudo"))
            .category(Text.literal("Elden / Steve")).allowedContexts(BindContext.IN_GAME)
            .defaultInput(new AxisInput(Identifier.of("controlify","axis/left_trigger"))));
        heavy=ControlifyBindApi.get().registerBinding(b->b.id("eldensteve","heavy").name(Text.literal("Ataque pesado / carregar"))
            .category(Text.literal("Elden / Steve")).allowedContexts(BindContext.IN_GAME)
            .defaultInput(new AxisInput(Identifier.of("controlify","axis/right_trigger"))));
    }
    private static InputBindingSupplier bind(String id,String name,String button){
        return ControlifyBindApi.get().registerBinding(b->b.id("eldensteve",id).name(Text.literal(name))
            .category(Text.literal("Elden / Steve")).allowedContexts(BindContext.IN_GAME)
            .defaultInput(new ButtonInput(Identifier.of("controlify","button/"+button))));
    }
    private static boolean down(InputBindingSupplier b,ControllerEntity c){return b!=null&&b.onOrNull(c)!=null&&b.on(c).digitalNow();}
    private static boolean pressed(InputBindingSupplier b,ControllerEntity c){return b!=null&&b.onOrNull(c)!=null&&b.on(c).justPressed();}
    private static float axis(InputBindingSupplier b,ControllerEntity c){return b.onOrNull(c)==null?0:b.on(c).analogueNow();}
    public static void poll(){
        var api=ControlifyApi.get();if(api==null||!api.currentInputMode().isController()){resetInput();return;}
        var c=api.getCurrentController().orElse(null);if(c==null||roll==null)return;
        if(waitNeutral){if(down(inventory,c)||down(roll,c)||down(jump,c)||down(crouch,c)||down(lock,c)||down(attack,c)||down(heavy,c)||down(left,c)||down(skill,c))return;waitNeutral=false;}
        Gamepad.active=true;Gamepad.roll=down(roll,c);Gamepad.jump=down(jump,c);Gamepad.sprint=down(sprint,c);
        boolean l3=down(crouch,c),r3=down(lock,c);
        if(l3||r3)stickSuppression=System.nanoTime()+450_000_000L;
        int sticks=chord.tick(l3,r3);
        Gamepad.left=down(left,c);Gamepad.crouch=(sticks&2)!=0;Gamepad.heavy=down(heavy,c);Gamepad.attack=pressed(attack,c);Gamepad.lock=(sticks&4)!=0;
        Gamepad.modifier=down(inventory,c);Gamepad.toggle=(sticks&1)!=0;
        if(Gamepad.modifier&&!interactWas)interactUsed=false;
        Gamepad.gripRight=Gamepad.modifier&&(pressed(attack,c)||pressed(heavy,c));
        Gamepad.gripLeft=Gamepad.modifier&&(pressed(left,c)||pressed(skill,c));
        interactUsed|=Gamepad.gripRight||Gamepad.gripLeft;
        Gamepad.inventory=interactWas&&!Gamepad.modifier&&!interactUsed;
        interactWas=Gamepad.modifier;
        Gamepad.skill=!Gamepad.modifier&&pressed(skill,c);
        Gamepad.skillHeld=!Gamepad.modifier&&down(skill,c);
        if(Gamepad.modifier){Gamepad.attack=false;Gamepad.heavy=false;Gamepad.left=false;}
        Gamepad.interact=pressed(interactAction,c);
        Gamepad.previousItem=pressed(previousItem,c);Gamepad.nextItem=pressed(nextItem,c);Gamepad.use=down(useItem,c);
        Gamepad.right=axis(ControlifyBindings.WALK_RIGHT,c)-axis(ControlifyBindings.WALK_LEFT,c);
        Gamepad.forward=axis(ControlifyBindings.WALK_FORWARD,c)-axis(ControlifyBindings.WALK_BACKWARD,c);
        Gamepad.lookX=axis(ControlifyBindings.LOOK_RIGHT,c)-axis(ControlifyBindings.LOOK_LEFT,c);
    }
}
