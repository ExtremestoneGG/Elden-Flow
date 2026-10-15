package dev.eldensteve;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
public final class BonfireScreen extends Screen {
    private ButtonWidget upgrade;
    public BonfireScreen(){super(Text.literal("Descansar na fogueira"));}
    protected void init(){int x=width-214,y=Math.max(25,height/2-65);
        addDrawableChild(ButtonWidget.builder(Text.literal("Salvar renascimento"),b->Bonfire.spawn()).dimensions(x,y,200,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Dormir ate amanhecer"),b->Bonfire.dawn()).dimensions(x,y+26,200,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ficar sentado / observar"),b->client.setScreen(null)).dimensions(x,y+52,200,20).build());
        upgrade=addDrawableChild(ButtonWidget.builder(Text.literal("Atributos / almas"),b->client.setScreen(new AttributesScreen(this,true))).dimensions(x,y+78,200,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Levantar"),b->close()).dimensions(x,y+104,200,20).build());
    }
    public boolean shouldPause(){return false;}
    public void close(){Bonfire.stop();client.setScreen(null);}
    public void render(DrawContext ctx,int x,int y,float delta){ctx.fill(width-225,Math.max(0,height/2-102),width-5,height/2+88,0xAA16120E);super.render(ctx,x,y,delta);ctx.drawTextWithShadow(textRenderer,title,width-214,Math.max(6,height/2-90),0xFFE2A4);ctx.drawTextWithShadow(textRenderer,Text.literal("Almas: "+ProgressionClient.view().souls()),width-214,height/2+73,0xDDDDDD);}
}
