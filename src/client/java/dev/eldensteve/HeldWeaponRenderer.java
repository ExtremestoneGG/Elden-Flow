package dev.eldensteve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/** Shared by gameplay and editor: both use exactly the same grip-space transform. */
public final class HeldWeaponRenderer {
    public static void render(AbstractClientPlayerEntity player,ItemStack stack,boolean left,boolean twoHand,WeaponConfig.Entry entry,MatrixStack m,VertexConsumerProvider consumers,int light,int overlay){
        render(player,stack,left,twoHand,entry,0,m,consumers,light,overlay);
    }
    public static void render(AbstractClientPlayerEntity player,ItemStack stack,boolean left,boolean twoHand,WeaponConfig.Entry entry,int context,MatrixStack m,VertexConsumerProvider consumers,int light,int overlay){
        if(stack.isEmpty())return;
        var mount=WeaponMount.of(player,stack,left,twoHand,WeaponProfiles.get(stack,entry));m.push();entry.pose(left,context).apply(m);
        if(mount!=null){m.scale(mount.scale(),mount.scale(),mount.scale());m.translate(-mount.grip().x,-mount.grip().y,-mount.grip().z);}
        m.scale(1,-1,-1);m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        MinecraftClient.getInstance().getItemRenderer().renderItem(player,stack,left?ModelTransformationMode.THIRD_PERSON_LEFT_HAND:ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,left,m,consumers,player.getWorld(),light,overlay,player.getId());m.pop();
    }
}
