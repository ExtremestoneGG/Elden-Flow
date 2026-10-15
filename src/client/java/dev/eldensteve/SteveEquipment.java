package dev.eldensteve;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;

/** Vanilla materials, dye, trim and glint on armor split at the rig's joints. */
public final class SteveEquipment {
    private static final ModelPart[] OUTER=armor(1),INNER=armor(.5f);
    private static final ModelPart CAPE=cape();
    private static ElytraEntityModel<AbstractClientPlayerEntity> wings;
    private static net.minecraft.client.render.entity.model.BipedEntityModel<net.minecraft.entity.LivingEntity> helmetContext;

    private static ModelPart[] armor(float dilation) {
        ModelPart[] result=new ModelPart[10];
        for(int i=0;i<10;i++) {
            float width=i<2?8:4, height=i==0?12:i==1?8:6,depth=i==1?8:4;
            int u=i==0?16:i==1?0:i<6?40:0, v=i==1?0:16;
            if(i>=2){result[i]=SegmentCuboid.part(u,v,width,dilation,i%2==1,i==2||i==3||i==6||i==7,32);continue;}
            var data=new ModelData();
            data.getRoot().addChild("part",ModelPartBuilder.create().uv(u,v).mirrored(i==2||i==3||i==6||i==7)
                .cuboid(-width/2,i==1?-8:0,-depth/2,width,height,depth,new Dilation(dilation)),ModelTransform.NONE);
            result[i]=TexturedModelData.of(data,64,32).createModel().getChild("part");
        }
        return result;
    }
    private static ModelPart cape() {
        var data=new ModelData();
        data.getRoot().addChild("cape",ModelPartBuilder.create().uv(0,0).cuboid(-5,0,-1,10,16,1),ModelTransform.NONE);
        return TexturedModelData.of(data,64,32).createModel().getChild("cape");
    }
    private static boolean includes(EquipmentSlot slot,int part) {
        return switch(slot){case HEAD->part==1;case CHEST->part==0||part>=2&&part<=5;case LEGS->part==0||part>=6;case FEET->part>=6;default->false;};
    }
    static void attach(StevePose pose,int part,MatrixStack m) {
        var p=pose.origins[part];m.translate(p.x,p.y,p.z);m.multiply(pose.rotations[part]);m.scale(1,-1,-1);
    }
    private static void draw(StevePose pose,EquipmentSlot slot,ModelPart[] models,MatrixStack m,VertexConsumer buffer,int light,int color) {
        for(int i=0;i<10;i++)if(includes(slot,i)) {
            m.push();attach(pose,i,m);SegmentCuboid.renderPart(models[i],m,buffer,light,OverlayTexture.DEFAULT_UV,color);m.pop();
        }
    }
    public static void render(AbstractClientPlayerEntity player,StevePose pose,float delta,MatrixStack m,VertexConsumerProvider consumers,int light) {
        var client=MinecraftClient.getInstance();
        for(var slot:new EquipmentSlot[]{EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.FEET,EquipmentSlot.HEAD}) {
            var stack=player.getEquippedStack(slot);
            if(slot==EquipmentSlot.HEAD&&!stack.isEmpty()){
                var renderer=net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl.get(stack.getItem());
                if(renderer!=null){
                    if(helmetContext==null)helmetContext=new net.minecraft.client.render.entity.model.BipedEntityModel<>(client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR));
                    helmetContext.setVisible(false);helmetContext.head.resetTransform();helmetContext.hat.resetTransform();helmetContext.head.visible=helmetContext.hat.visible=true;
                    m.push();attach(pose,1,m);
                    renderer.render(m,consumers,stack,player,slot,light,helmetContext);
                    m.pop();continue;
                }
            }
            if(!(stack.getItem() instanceof ArmorItem item)||item.getSlotType()!=slot)continue;
            boolean inner=slot==EquipmentSlot.LEGS;var models=inner?INNER:OUTER;
            int color=stack.isIn(ItemTags.DYEABLE)?ColorHelper.Argb.fullAlpha(DyedColorComponent.getColor(stack,-6265536)):-1;
            for(var layer:item.getMaterial().value().layers())
                draw(pose,slot,models,m,consumers.getBuffer(RenderLayer.getArmorCutoutNoCull(layer.getTexture(inner))),light,layer.isDyeable()?color:-1);
            var trim=stack.get(DataComponentTypes.TRIM);
            if(trim!=null) {
                var atlas=client.getBakedModelManager().getAtlas(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE);
                var sprite=atlas.getSprite(inner?trim.getLeggingsModelId(item.getMaterial()):trim.getGenericModelId(item.getMaterial()));
                var buffer=sprite.getTextureSpecificVertexConsumer(consumers.getBuffer(TexturedRenderLayers.getArmorTrims(trim.getPattern().value().decal())));
                draw(pose,slot,models,m,buffer,light,-1);
            }
            if(stack.hasGlint())draw(pose,slot,models,m,consumers.getBuffer(RenderLayer.getArmorEntityGlint()),light,-1);
        }
        back(player,pose,delta,m,consumers,light);
        headItem(player,pose,m,consumers,light);
    }
    private static void back(AbstractClientPlayerEntity p,StevePose pose,float delta,MatrixStack m,VertexConsumerProvider consumers,int light) {
        var chest=p.getEquippedStack(EquipmentSlot.CHEST);var skin=p.getSkinTextures();
        m.push();attach(pose,0,m);m.translate(0,0,.14);
        if(chest.isOf(Items.ELYTRA)) {
            if(wings==null)wings=new ElytraEntityModel<>(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(EntityModelLayers.ELYTRA));
            Identifier texture=skin.elytraTexture();
            if(texture==null&&p.isPartVisible(PlayerModelPart.CAPE))texture=skin.capeTexture();
            if(texture==null)texture=Identifier.ofVanilla("textures/entity/elytra.png");
            wings.setAngles(p,0,0,p.age+delta,0,0);
            wings.render(m,ItemRenderer.getArmorGlintConsumer(consumers,RenderLayer.getArmorCutoutNoCull(texture),chest.hasGlint()),light,OverlayTexture.DEFAULT_UV);
        }else if(skin.capeTexture()!=null&&p.isPartVisible(PlayerModelPart.CAPE)) {
            // Small velocity-driven lift, attached to the animated chest even while rolling.
            float lift=6+Math.min(35,(float)p.getVelocity().horizontalLength()*90);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lift));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            CAPE.render(m,consumers.getBuffer(RenderLayer.getEntitySolid(skin.capeTexture())),light,OverlayTexture.DEFAULT_UV);
        }
        m.pop();
    }
    private static void headItem(AbstractClientPlayerEntity p,StevePose pose,MatrixStack m,VertexConsumerProvider consumers,int light) {
        var stack=p.getEquippedStack(EquipmentSlot.HEAD);
        if(stack.isEmpty()||stack.getItem() instanceof ArmorItem||net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl.get(stack.getItem())!=null)return;
        m.push();attach(pose,1,m);m.translate(0,-.25,0);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));m.scale(.625f,-.625f,-.625f);
        MinecraftClient.getInstance().getItemRenderer().renderItem(p,stack,ModelTransformationMode.HEAD,false,m,consumers,p.getWorld(),light,OverlayTexture.DEFAULT_UV,p.getId());
        m.pop();
    }
}
