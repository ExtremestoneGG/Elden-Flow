package dev.eldensteve;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class AttributesScreen extends Screen {
    private final Screen parent;
    private final boolean atFire;
    private int x,y;
    public AttributesScreen(Screen parent,boolean atFire){super(Text.literal("Atributos"));this.parent=parent;this.atFire=atFire;}
    protected void init(){
        x=width-AttributePanel.WIDTH-8;y=Math.max(6,(height-AttributePanel.HEIGHT-48)/2);
        addDrawableChild(ButtonWidget.builder(Text.literal("Voltar"),b->close()).dimensions(x,y+AttributePanel.HEIGHT+4,AttributePanel.WIDTH,20).build());
        if(atFire)addDrawableChild(ButtonWidget.builder(Text.literal("Redistribuir pontos"),b->ProgressionClient.intent(5,0,BlockPos.ORIGIN)).dimensions(x,y+AttributePanel.HEIGHT+27,AttributePanel.WIDTH,20).tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Custa 1 estrela do Nether e 1 catalisador de sculk. Devolve os pontos para redistribuir."))).build());
    }
    public boolean shouldPause(){return false;}
    public void renderBackground(DrawContext c,int x,int y,float delta){}
    public void render(DrawContext c,int mx,int my,float delta){
        super.render(c,mx,my,delta);AttributePanel.render(c,x,y,mx,my,atFire);
        if(x>=180)AttributePanel.weapon(c,8,y,Math.min(190,x-16),mx,my);
    }
    public boolean mouseClicked(double mx,double my,int button){if(button==0&&atFire&&AttributePanel.click(x,y,mx,my))return true;return super.mouseClicked(mx,my,button);}
    public void close(){client.setScreen(atFire&&!Bonfire.active()?null:parent);}
}
