package dev.eldensteve;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/** Model-space grip anchors, transformed by the item's own third-person display transform. */
public record WeaponMount(Vector3f grip,Vector3f axis,float scale,float support,float length) {
    public static WeaponMount of(AbstractClientPlayerEntity player,ItemStack stack,boolean left,boolean twoHand){
        return of(player,stack,left,twoHand,WeaponProfiles.get(stack));
    }
    public static WeaponMount of(AbstractClientPlayerEntity player,ItemStack stack,boolean left,boolean twoHand,WeaponProfiles.Profile profile){
        if(profile==null)return null;
        String type=profile.type();float x=5.5f,y=27.5f,size=32;
        switch(type){
            case "sai","cutlass"->{x=3.5f;y=12.5f;size=16;}
            case "twinblade"->{x=16;y=16;}
            case "longsword","katana"->{x=9.5f;y=22.5f;}
            case "claymore"->{x=6.5f;y=26.5f;}
            case "rapier"->{x=9.5f;y=22.5f;}
            case "greataxe","spear"->{x=5.5f;y=27.5f;}
            case "glaive","scythe"->{x=4.5f;y=27.5f;}
            case "halberd"->{x=8.5f;y=42.5f;size=48;}
        }
        var itemId=net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        boolean known=itemId.getNamespace().equals("simplyswords"),vanilla=itemId.getNamespace().equals("minecraft");
        if(vanilla){x=3.5f;y=12.5f;size=16;}
        var m=new MatrixStack();m.scale(1,-1,-1);m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        var mode=left?ModelTransformationMode.THIRD_PERSON_LEFT_HAND:ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        var model=MinecraftClient.getInstance().getItemRenderer().getModel(stack,player.getWorld(),player,player.getId());
        model.getTransformation().getTransformation(mode).apply(left,m);
        var matrix=m.peek().getPositionMatrix();
        var grip=matrix.transformPosition(new Vector3f(x/size-.5f,.5f-y/size,0));
        var axis=matrix.transformDirection(new Vector3f(1,1,0)).normalize();
        // The hammer's shaft is actual geometry (x+y=12), not its texture atlas diagonal.
        if(known&&type.equals("greathammer")){grip=matrix.transformPosition(new Vector3f(7f/16-.5f,5f/16-.5f,0));axis=matrix.transformDirection(new Vector3f(-1,1,0)).normalize();}
        // Unknown 3D models own their display/grip transform; never borrow a Simply Swords atlas anchor.
        if(!known&&!vanilla||type.equals("shield"))grip.zero();
        boolean pole=type.equals("spear")||type.equals("glaive")||type.equals("halberd")||type.equals("scythe")||type.equals("twinblade");
        float spacing=switch(type){case "spear","glaive","halberd","scythe"->.55f;case "greathammer","greataxe"->.48f;case "twinblade"->.40f;case "claymore"->-.24f;default->-.18f;};
        return new WeaponMount(grip,axis,twoHand?1.25f:1.15f,spacing,pole?1.7f:type.equals("sai")?.45f:1.2f);
    }
}
