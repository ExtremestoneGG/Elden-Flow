package dev.eldensteve;
import dev.eldensteve.progression.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.entity.attribute.EntityAttributes;

/** Vanilla-style bevels and quiet labels. The same panel is used in inventory and at campfires. */
public final class AttributePanel {
    public static final int WIDTH=166,HEIGHT=178;
    private static final String[] NAMES={"Vitalidade","Folego","Capacidade","Forca","Destreza","Afinidade"};
    private static final String[] DETAILS={"Aumenta a vida maxima no modo Elden.","Aumenta a estamina maxima.","Ajuda a rolar com armas mais pesadas.","Aumenta o dano das classes pesadas.","Aumenta o dano das classes leves e tecnicas.","Melhora as passivas da classe da arma."};
    private static final String[] ICONS={"heart","lungs","bag","strength","dexterity","affinity"};
    private static void icon(DrawContext c,String name,int x,int y){c.drawTexture(net.minecraft.util.Identifier.of("eldensteve","textures/gui/stats/"+name+".png"),x,y,16,16,0,0,32,32,32,32);}
    private static void line(DrawContext c,int x,int y,int width){c.fill(x+7,y,x+width-7,y+1,0xFF999999);c.fill(x+7,y+1,x+width-7,y+2,0xFFE0E0E0);}
    public static String name(Attribute a){return NAMES[a.ordinal()];}
    public static void frame(DrawContext c,int x,int y,int w,int h){
        c.fill(x,y,x+w,y+h,0xFF373737);c.fill(x+1,y+1,x+w-1,y+h-1,0xFFC6C6C6);
        c.fill(x+1,y+1,x+w-2,y+3,0xFFFFFFFF);c.fill(x+1,y+1,x+3,y+h-2,0xFFFFFFFF);
        c.fill(x+3,y+h-3,x+w-1,y+h-1,0xFF555555);c.fill(x+w-3,y+3,x+w-1,y+h-1,0xFF555555);
    }
    public static void render(DrawContext ctx,int x,int y,int mouseX,int mouseY,boolean allowBuy){
        var client=MinecraftClient.getInstance();var font=client.textRenderer;var p=ProgressionClient.view();frame(ctx,x,y,WIDTH,HEIGHT);
        icon(ctx,"stats",x+7,y+4);ctx.drawText(font,"Atributos",x+28,y+8,0x404040,false);line(ctx,x,y+18,WIDTH);
        ctx.drawText(font,font.trimToWidth("Almas: "+(client.player!=null&&client.player.isCreative()?"∞":p.souls()),WIDTH-16),x+8,y+21,0x514183,false);
        Text hover=null;
        for(var a:Attribute.values()){
            int row=y+37+a.ordinal()*17;icon(ctx,ICONS[a.ordinal()],x+7,row);ctx.drawText(font,name(a),x+27,row+4,0x404040,false);
            ctx.drawText(font,Integer.toString(p.level(a)),x+107,row+4,0x404040,false);
            boolean can=allowBuy&&ProgressionClient.resting()&&(client.player.isCreative()||p.souls()>=p.nextCost())&&p.level(a)<ProgressionRules.MAX_ATTRIBUTE;
            frame(ctx,x+140,row,17,15);ctx.drawText(font,"+",x+146,row+3,can?0x324B26:0x999999,false);
            if(mouseX>=x+7&&mouseX<x+WIDTH-7&&mouseY>=row&&mouseY<row+16)hover=Text.literal(DETAILS[a.ordinal()]+(allowBuy?" Custo: "+p.nextCost()+" almas.":" Melhore na fogueira."));
        }
        line(ctx,x,y+140,WIDTH);
        ctx.drawText(font,font.trimToWidth(p.unspent()>0?"Pontos livres: "+p.unspent():"Proximo: "+p.nextCost()+" almas",WIDTH-16),x+8,y+144,0x404040,false);
        ctx.drawText(font,allowBuy?"Melhorias na fogueira":"Consulte; melhore na fogueira",x+8,y+161,0x606060,false);
        if(hover!=null)ctx.drawOrderedTooltip(font,font.wrapLines(hover,200),mouseX,mouseY);
    }
    public static boolean click(int x,int y,double mouseX,double mouseY){
        if(!ProgressionClient.resting()||mouseX<x+140||mouseX>=x+157)return false;
        int row=(int)(mouseY-y-37);if(row<0||row>=102||row%17>=15)return false;
        ProgressionClient.buy(Attribute.values()[row/17]);return true;
    }
    public static void weapon(DrawContext ctx,int x,int y,int width){
        var c=MinecraftClient.getInstance();if(c.player==null)return;var stack=Combat.twoHandLeft?c.player.getOffHandStack():c.player.getMainHandStack();if(stack.isEmpty())return;
        var profile=WeaponProfiles.get(stack);if(profile==null)return;var family=WeaponFamily.fromType(profile.type());if(family==null)return;
        frame(ctx,x,y,width,112);line(ctx,x,y+26,width);line(ctx,x,y+55,width);line(ctx,x,y+91,width);ctx.drawItem(stack,x+7,y+7);
        ctx.drawText(c.textRenderer,c.textRenderer.trimToWidth(stack.getName().getString(),width-34),x+28,y+11,0x404040,false);
        var stats=ProgressionClient.view();double multiplier=ProgressionRules.physicalMultiplier(family,stats.level(Attribute.STRENGTH),stats.level(Attribute.DEXTERITY));
        double base=c.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if(Combat.twoHandLeft){var copy=new net.minecraft.entity.attribute.EntityAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE,a->{});copy.setFrom(c.player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE));c.player.getMainHandStack().applyAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND,(a,m)->{if(a.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE))copy.removeModifier(m.id());});stack.applyAttributeModifiers(net.minecraft.entity.EquipmentSlot.MAINHAND,(a,m)->{if(a.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE))copy.updateModifier(m);});base=copy.getValue();}
        ctx.drawText(c.textRenderer,String.format(java.util.Locale.ROOT,"Leve %.1f | Pesado %.1f",base*multiplier*1.5,base*multiplier*2.5),x+8,y+29,0x404040,false);
        ctx.drawText(c.textRenderer,family.strength>=.65?"Escala: Forca":family.strength<=.35?"Escala: Destreza":"Escala: Forca / Destreza",x+8,y+43,0x404040,false);
        ctx.drawText(c.textRenderer,c.textRenderer.trimToWidth(WeaponDescriptions.passiveName(family),width-16),x+8,y+59,0x514183,false);
        ctx.drawText(c.textRenderer,"Passiva: melhora com Afinidade",x+8,y+73,0x606060,false);
        ctx.drawText(c.textRenderer,"Dano antes da defesa do alvo",x+8,y+98,0x606060,false);
    }
    public static void weapon(DrawContext ctx,int x,int y,int width,int mx,int my){
        weapon(ctx,x,y,width);var c=MinecraftClient.getInstance();if(c.player==null||mx<x||mx>x+width||my<y+54||my>y+99)return;
        var stack=Combat.twoHandLeft?c.player.getOffHandStack():c.player.getMainHandStack();var profile=WeaponProfiles.get(stack);if(profile==null)return;
        var family=WeaponFamily.fromType(profile.type());if(family!=null)ctx.drawOrderedTooltip(c.textRenderer,c.textRenderer.wrapLines(Text.literal(WeaponDescriptions.passive(family,ProgressionClient.view().level(Attribute.AFFINITY))),230),mx,my);
    }
}
