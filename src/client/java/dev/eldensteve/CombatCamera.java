package dev.eldensteve;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/** Camera state belongs to rendered frames, independently of the 20 Hz movement simulation. */
public final class CombatCamera {
    public static float yaw,pitch;
    private static long last;
    private static boolean tracking;
    private static final CameraTransition transition=new CameraTransition();
    private static net.minecraft.client.option.Perspective restore;
    public static net.minecraft.client.option.Perspective returnPerspective(net.minecraft.client.option.Perspective fallback){return restore==null?fallback:restore;}
    public static void enter(net.minecraft.client.option.Perspective previous){
        var start=restore!=null?transition.view():normal(previous);
        transition.begin(start,new CameraTransition.View(5.2f,.65f,0),System.nanoTime());restore=null;
    }
    public static void exit(net.minecraft.client.option.Perspective previous){
        transition.begin(transition.view(),normal(previous),System.nanoTime());restore=previous;
    }
    private static CameraTransition.View normal(net.minecraft.client.option.Perspective perspective){return new CameraTransition.View(perspective.isFirstPerson()?0:4,0,perspective==net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT?180:0);}
    public static boolean exiting(){return restore!=null;}
    public static float distance(){return transition.view().distance();}
    public static float lift(){return transition.view().lift();}
    public static float orbit(){return transition.view().orbit();}
    public static void finishTransition(){if(restore!=null&&transition.complete()){MinecraftClient.getInstance().options.setPerspective(restore);restore=null;}}
    public static void reset(){last=0;tracking=false;}
    public static float inputYaw(float fallback){return tracking?yaw:fallback;}
    public static void update(float vanillaYaw,float vanillaPitch,float delta){
        var c=MinecraftClient.getInstance();var target=Combat.target();
        long now=System.nanoTime();transition.advance(now,c.isPaused());double dt=last==0||c.isPaused()?0:Math.clamp((now-last)*1e-9,0,.1);last=now;
        if(Bonfire.active()&&(c.currentScreen instanceof BonfireScreen||c.currentScreen instanceof AttributesScreen)){
            yaw=vanillaYaw+(float)(dt*8);pitch=vanillaPitch+(12-vanillaPitch)*(float)(1-Math.exp(-dt*2));tracking=false;
            c.player.setYaw(yaw);c.player.prevYaw=yaw;c.player.setPitch(pitch);c.player.prevPitch=pitch;return;
        }
        if(target==null){yaw=vanillaYaw;pitch=vanillaPitch;tracking=false;return;}
        if(!tracking){yaw=vanillaYaw;pitch=vanillaPitch;tracking=true;}
        var from=c.player.getLerpedPos(delta).add(0,c.player.getStandingEyeHeight()+lift()+StepSmoothing.offset(delta),0);
        var to=target.getLerpedPos(delta).add(0,target.getHeight()*.6,0);
        var d=to.subtract(from);
        float desired=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        float desiredPitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.max(.5,d.horizontalLength())));
        float blend=(float)(1-Math.exp(-9*dt));
        yaw+=MathHelper.wrapDegrees(desired-yaw)*blend;
        pitch=MathHelper.lerp(blend,pitch,Math.clamp(desiredPitch,-55,60));
        // Keep free camera orientation continuous when the lock is released.
        c.player.setYaw(yaw);c.player.prevYaw=yaw;
        c.player.setPitch(pitch);c.player.prevPitch=pitch;
    }
}
