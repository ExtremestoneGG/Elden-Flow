package dev.eldensteve;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class Combat {
    private static boolean queued;
    private static LivingEntity target;
    private static int serial=-1;
    private static boolean connected;
    private static final TargetFlick flick=new TargetFlick();
    public static String feedback="";
    private static int feedbackTicks;
    public static boolean twoHand,twoHandLeft;
    private static boolean rightWas,leftWas;
    private static boolean interactWas,interactUsed;
    private static final java.util.LinkedHashSet<Integer> landed=new java.util.LinkedHashSet<>();
    private static final EquipmentSelection equipment=new EquipmentSelection();
    public static void synchronizeEquipment(){
        var p=MinecraftClient.getInstance().player;if(p==null)return;
        var main=profile();var off=offhand();
        if(equipment.update(p.getMainHandStack().getItem(),p.getOffHandStack().getItem(),main==null?-1:main.nativeIndex(),off==null?-1:off.nativeIndex()))
            {twoHand=false;twoHandLeft=false;}
    }
    public static WeaponProfiles.Profile profile(){var p=MinecraftClient.getInstance().player;return p==null?null:WeaponProfiles.get(p.getMainHandStack());}
    public static WeaponProfiles.Profile offhand(){var p=MinecraftClient.getInstance().player;return p==null?null:WeaponProfiles.get(p.getOffHandStack());}
    public static boolean paired(){return !twoHand&&profile()!=null&&offhand()!=null&&profile().nativeIndex()!=24&&profile().nativeIndex()==offhand().nativeIndex();}
    public static boolean leftHeld(){return !modifier()&&(twoHand||ShieldCombat.equipped())&&(Gamepad.left||EldenInput.down(MinecraftClient.getInstance().options.useKey));}
    public static boolean modifier(){return Gamepad.modifier||EldenInput.down(EldenSteveClient.interact);}
    public static void pollGrip(){
        var c=MinecraftClient.getInstance();boolean right=EldenInput.down(c.options.attackKey),left=EldenInput.down(c.options.useKey);
        boolean key=EldenInput.down(EldenSteveClient.interact);
        if(key&&!interactWas)interactUsed=false;
        if(key&&(right||left))interactUsed=true;
        if(Gamepad.gripRight||modifier()&&right&&!rightWas)toggleGrip(false);
        else if(Gamepad.gripLeft||modifier()&&left&&!leftWas)toggleGrip(true);
        rightWas=right;leftWas=left;
        if(interactWas&&!key&&!interactUsed)interact();
        interactWas=key;
    }
    private static void toggleGrip(boolean left){
        if((left?offhand():profile())==null)return;
        boolean leave=twoHand&&twoHandLeft==left;twoHand=!leave;twoHandLeft=twoHand&&left;
    }
    public static WeaponProfiles.Profile activeProfile(){return twoHandLeft?offhand():profile();}
    public static boolean armed(){if(Bonfire.active())return false;var c=MinecraftClient.getInstance();return EldenSteveClient.enabled&&c.player!=null&&c.player.isAlive()&&activeProfile()!=null;}
    public static boolean intercept(){return armed()&&MinecraftClient.getInstance().currentScreen==null;}
    public static void queue(){if(modifier())return;if(!heavyHeld())queued=true;}
    public static boolean heavyHeld(){return !modifier()&&EldenInput.down(EldenSteveClient.heavy);}
    public static void interact(){
        var c=MinecraftClient.getInstance();if(!EldenSteveClient.enabled||c.player==null||c.currentScreen!=null||c.interactionManager==null)return;
        if(c.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult hit){if(!Bonfire.interact(hit.getBlockPos()))c.interactionManager.interactBlock(c.player,Hand.MAIN_HAND,hit);}
        else if(c.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult hit)c.interactionManager.interactEntity(c.player,hit.getEntity(),Hand.MAIN_HAND);
    }
    public static boolean consume(){boolean click=queued;queued=false;return click&&armed();}
    public static void reset(){queued=false;target=null;twoHand=false;twoHandLeft=false;landed.clear();rightWas=leftWas=interactWas=interactUsed=false;equipment.reset();CombatCamera.reset();CombatOverlay.blade=new float[107];serial=-1;connected=false;feedback="";feedbackTicks=0;}
    private static boolean candidate(LivingEntity e){var p=MinecraftClient.getInstance().player;return e!=p&&!(e instanceof PlayerEntity)&&e.isAlive()&&!e.isRemoved()&&e.isAttackable()&&p.squaredDistanceTo(e)<256&&p.canSee(e);}
    public static void toggleLock(){
        var c=MinecraftClient.getInstance();if(!EldenSteveClient.enabled||c.player==null||c.world==null)return;
        if(target!=null){target=null;return;}
        Vec3d eye=c.player.getEyePos(),look=c.player.getRotationVec(1);double best=Double.MAX_VALUE;
        for(var e:c.world.getEntitiesByClass(LivingEntity.class,c.player.getBoundingBox().expand(16),Combat::candidate)){
            var toward=e.getPos().add(0,e.getHeight()*.6,0).subtract(eye);
            double dot=toward.normalize().dotProduct(look);if(dot<.25)continue;
            double score=(1-dot)*12+toward.length()*.12;if(score<best){target=e;best=score;}
        }
    }
    public static LivingEntity target(){return target;}
    public static void cycleTarget(int direction){
        var c=MinecraftClient.getInstance();if(target==null||c.world==null||c.player==null||c.currentScreen!=null)return;
        var camera=c.gameRenderer.getCamera();double yaw=Math.toRadians(camera.getYaw());
        var forward=new Vec3d(-Math.sin(yaw),0,Math.cos(yaw));var right=new Vec3d(-Math.cos(yaw),0,-Math.sin(yaw));
        var origin=camera.getPos();var current=target.getPos().subtract(origin);
        double currentAngle=Math.atan2(current.dotProduct(right),current.dotProduct(forward));
        LivingEntity best=null;double score=Double.MAX_VALUE;
        for(var e:c.world.getEntitiesByClass(LivingEntity.class,c.player.getBoundingBox().expand(16),Combat::candidate)){
            if(e==target)continue;var d=e.getPos().subtract(origin);if(d.dotProduct(forward)<=0)continue;
            double angle=Math.atan2(d.dotProduct(right),d.dotProduct(forward));
            double difference=Math.atan2(Math.sin(angle-currentAngle),Math.cos(angle-currentAngle));
            if(difference*direction<.015)continue;
            double value=Math.abs(difference)+d.length()*.002;
            if(value<score){score=value;best=e;}
        }
        if(best!=null)target=best;
    }
    public static void tickCamera(){
        var c=MinecraftClient.getInstance();
        if(feedbackTicks>0&&--feedbackTicks==0)feedback="";
        if(target!=null && (c.player==null||target.getWorld()!=c.world||!candidate(target))){
            boolean died=!target.isAlive();target=null;
            if(died&&c.player!=null&&c.world!=null)target=c.world.getEntitiesByClass(LivingEntity.class,c.player.getBoundingBox().expand(16),Combat::candidate).stream().min(java.util.Comparator.comparingDouble(c.player::squaredDistanceTo)).orElse(null);
        }
        int direction=flick.tick(Gamepad.lookX,target!=null&&Gamepad.active&&c.currentScreen==null&&!c.isPaused());
        if(direction!=0)cycleTarget(direction);
        if(target==null||c.currentScreen!=null||c.isPaused())return;

    }
    public static double floor(){
        var c=MinecraftClient.getInstance();var p=c.player;
        if(p.isOnGround())return p.getY();
        var from=p.getPos().add(0,.01,0);var to=from.add(0,-16,0);
        var hit=c.world.raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
        return hit.getType()==HitResult.Type.BLOCK?Math.min(p.getY(),hit.getPos().y):p.getY()-16;
    }
    public static void applyHit(NativeCore core){
        var c=MinecraftClient.getInstance();if(!armed()||c.world==null)return;
        CombatOverlay.blade=core.blade();float[] hits=core.contacts();var origin=c.player.getPos();
        var candidates=c.world.getEntitiesByClass(LivingEntity.class,c.player.getBoundingBox().expand(3),Combat::candidate);
        for(int i=0;i<(int)hits[0];i++){
            int at=1+i*10,id=(int)hits[at],flags=(int)hits[at+1];if(landed.contains(id))continue;
            double radius=Math.clamp(hits[at+2],.03,.35);
            var a=origin.add(hits[at+3],hits[at+4],hits[at+5]);var b=origin.add(hits[at+6],hits[at+7],hits[at+8]);
            LivingEntity victim=null;double nearest=Double.MAX_VALUE;
            for(var e:candidates){
                double distance=c.player.squaredDistanceTo(e);if(distance>9)continue;
                Box box=e.getBoundingBox().expand(radius);var contact=box.contains(a)?java.util.Optional.of(a):box.raycast(a,b);
                if(contact.isEmpty())continue;
                var wall=c.world.raycast(new RaycastContext(c.player.getEyePos(),contact.get(),RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,c.player));
                if(wall.getType()==HitResult.Type.BLOCK)continue;
                if(distance<nearest){victim=e;nearest=distance;}
            }
            if(victim!=null){
                // Damage is resolved exclusively by the server contact simulation.
                landed.add(id);while(landed.size()>128)landed.remove(landed.iterator().next());
                core.impact(flags);
            }
        }
    }
}
