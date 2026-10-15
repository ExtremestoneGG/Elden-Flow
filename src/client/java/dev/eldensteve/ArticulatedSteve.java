package dev.eldensteve;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

/** Ten rigid skin pieces driven by locally extracted Elden joint samples. */
public final class ArticulatedSteve {
    private static final StevePose pose=new StevePose();
    private static final ModelPart[][] meshes={parts(false,false),parts(true,false),parts(false,true),parts(true,true)};
    private static float[] rest;
    private static org.joml.Quaternionf restWeaponArm,restLeftArm,leftWeaponRotation;
    private static org.joml.Quaternionf weaponRotation;
    private static ModelPart[] parts(boolean slim,boolean overlay) {
        ModelPart[] parts=new ModelPart[10];
        for(int i=0;i<10;i++) {
            int u,v;float width,height,depth,y;
            if(i==0){u=16;v=overlay?32:16;width=8;height=12;depth=4;y=0;}
            else if(i==1){u=overlay?32:0;v=0;width=8;height=8;depth=8;y=-8;}
            else {
                width=slim&&i<6?3:4;height=6;depth=4;y=0;
                if(i<4){u=overlay?48:32;v=48;}
                else if(i<6){u=40;v=overlay?32:16;}
                else if(i<8){u=overlay?0:16;v=48;}
                else {u=0;v=overlay?32:16;}
                parts[i]=SegmentCuboid.part(u,v,width,overlay?.25f:0,i%2==1,false,64);
                continue;
            }
            var data=new ModelData();
            data.getRoot().addChild("part",ModelPartBuilder.create().uv(u,v).cuboid(-width/2,y,-depth/2,width,height,depth,new Dilation(overlay?(i==1?.5f:.25f):0)),ModelTransform.NONE);
            parts[i]=TexturedModelData.of(data,64,64).createModel().getChild("part");
        }
        return parts;
    }
    public static boolean render(AbstractClientPlayerEntity player,float delta,MatrixStack matrices,VertexConsumerProvider consumers,int light) {
        boolean local=player==MinecraftClient.getInstance().player;
        var remote=local?null:RemoteCombat.get(player.getId());
        if((remote==null&&!EldenSteveClient.visuals(player)&&!(Bonfire.active()&&player==MinecraftClient.getInstance().player))||player.isInvisible())return false;
        float[] sample=local?SteveAnimations.pose(delta):remote==null?null:remote.motion.pose(delta);if(sample==null)return false;
        if(rest==null){rest=local?SteveAnimations.rest():remote.motion.rest();var restPose=new StevePose();restPose.set(rest,rest);restWeaponArm=new org.joml.Quaternionf(restPose.rotations[5]);restLeftArm=new org.joml.Quaternionf(restPose.rotations[3]);}
        pose.set(sample,rest);weaponRotation=StevePose.weaponDelta(sample,rest).mul(restWeaponArm);leftWeaponRotation=StevePose.weaponDelta(sample,rest,true).mul(restLeftArm);
        boolean twoHand=local?Combat.twoHand:remote.frame.grip()!=0;
        boolean twoHandLeft=local?Combat.twoHandLeft:remote.frame.grip()==2;
        float action=local?EldenSteveClient.state[3]:remote.frame.state()[3];
        boolean resting=local?Bonfire.posing():remote.resting();
        boolean raised=local?Bonfire.raisedSeat():remote.raised();
        float sitting=local?Bonfire.poseAmount(delta):remote.sitting(delta);
        boolean guarding=local?SteveAnimations.guarding():remote.motion.guarding();
        boolean rightMain=player.getMainArm()==Arm.RIGHT;
        boolean consuming=local?dev.eldensteve.combat.Consumption.active(player):action==8;
        if(consuming){boolean right=player.getActiveHand()==net.minecraft.util.Hand.MAIN_HAND?rightMain:!rightMain;if(!right)pose.mirror();weaponRotation=new org.joml.Quaternionf(pose.rotations[5]);leftWeaponRotation=new org.joml.Quaternionf(pose.rotations[3]);}
        var mainMount=WeaponMount.of(player,(twoHandLeft?player.getOffHandStack():player.getMainHandStack()),!rightMain,twoHand);
        boolean gripAction=!consuming&&(!local||!ShieldCombat.busy())&&!resting&&(action==0||action==4);
        if(twoHand&&rightMain&&mainMount!=null&&gripAction)
            GripSolver.fit(pose,weaponRotation.transform(new org.joml.Vector3f(mainMount.axis())),mainMount.support(),mainMount.length());
        if(resting)pose.sit(raised,sitting);
        pose.ground(!resting&&action==1);
        if(!resting&&action==1)pose.rollContact();
        boolean slim=player.getSkinTextures().model().getName().equals("slim");
        var vertices=consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(player.getSkinTextures().texture()));
        int overlay=LivingEntityRenderer.getOverlay(player,0);
        matrices.push();
        matrices.translate(0,local?StepSmoothing.offset(delta):0,0);
        if(resting){var seat=local?Bonfire.seatOffset(delta):new net.minecraft.util.math.Vec3d(0,raised?-.5*player.getScale()*.9375*sitting:0,0);matrices.translate(seat.x,seat.y,seat.z);}
        float bodyYaw=local?EldenSteveClient.renderYaw(delta):remote.yaw(delta);
        if(local&&resting){float diff=Bonfire.yaw()-bodyYaw;bodyYaw+=(float)Math.atan2(Math.sin(diff),Math.cos(diff))*Bonfire.poseAmount(delta);}
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(bodyYaw));
        float scale=player.getScale()*.9375f;matrices.scale(scale,scale,scale);
        for(int i=0;i<10;i++) {
            matrices.push();var p=pose.origins[i];matrices.translate(p.x,p.y,p.z);
            matrices.multiply(pose.rotations[i]);matrices.scale(1,-1,-1);
            SegmentCuboid.renderPart(meshes[slim?1:0][i],matrices,vertices,light,overlay,-1);
            PlayerModelPart layer=i==0?PlayerModelPart.JACKET:i==1?PlayerModelPart.HAT:i<4?PlayerModelPart.LEFT_SLEEVE:i<6?PlayerModelPart.RIGHT_SLEEVE:i<8?PlayerModelPart.LEFT_PANTS_LEG:PlayerModelPart.RIGHT_PANTS_LEG;
            if(player.isPartVisible(layer))SegmentCuboid.renderPart(meshes[slim?3:2][i],matrices,vertices,light,overlay,-1);
            matrices.pop();
        }
        SteveEquipment.render(player,pose,delta,matrices,consumers,light);
        if(!resting)item(player,twoHandLeft?player.getOffHandStack():(rightMain?player.getMainHandStack():player.getOffHandStack()),5,false,twoHand,guarding,matrices,consumers,light,overlay);
        if(!resting&&!twoHand)item(player,rightMain?player.getOffHandStack():player.getMainHandStack(),3,true,twoHand,guarding,matrices,consumers,light,overlay);
        matrices.pop();return true;
    }
    private static void item(AbstractClientPlayerEntity player,ItemStack stack,int forearm,boolean left,boolean twoHand,boolean guarding,MatrixStack m,VertexConsumerProvider consumers,int light,int overlay) {
        if(stack.isEmpty())return;
        var mount=WeaponMount.of(player,stack,left,twoHand);
        var rotation=ShieldCombat.isShield(stack)?pose.rotations[forearm]:(left?leftWeaponRotation:weaponRotation);
        m.push();var p=pose.hand(left);
        m.translate(p.x,p.y,p.z);m.multiply(rotation);
        int context=(twoHand?1:0)+(guarding?2:0);
        HeldWeaponRenderer.render(player,stack,left,twoHand,WeaponConfig.get(stack),context,m,consumers,light,overlay);
        m.pop();
    }
}
