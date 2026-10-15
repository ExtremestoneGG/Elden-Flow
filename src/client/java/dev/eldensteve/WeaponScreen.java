package dev.eldensteve;
import java.util.*;
import java.nio.file.Path;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Two independent grip spaces with a shared renderer, orbit camera and projected handles. */
public final class WeaponScreen extends Screen {
    private final Screen parent;
    private final List<Item> all=new ArrayList<>(),filtered=new ArrayList<>();
    private final List<ButtonWidget> rows=new ArrayList<>();
    private static final ModelPart RIGHT=SegmentCuboid.part(40,16,4,0,true,false,64),LEFT=SegmentCuboid.part(32,48,4,0,true,false,64);
    private ItemStack selected=ItemStack.EMPTY;
    private WeaponConfig.Entry edit=new WeaponConfig.Entry();
    private TextFieldWidget search;
    private ButtonWidget mode,hand,tool,axis,parry,context;
    private int page,listWidth,top=110,bottom,dragButton=-1,activeAxis=-1;
    private boolean left,dirty;
    private int operation,poseContext;
    private float dragAngle;
    private float yaw=25,pitch=-15,zoom=65,panX,panY;
    private String status="",query="";
    private boolean initialized;
    public WeaponScreen(Screen parent){super(Text.literal("Elden Flow · Editor 3D"));this.parent=parent;}
    private ButtonWidget button(String name,int x,int y,int w,Runnable action){return addDrawableChild(ButtonWidget.builder(Text.literal(name),b->action.run()).dimensions(x,y,w,20).build());}
    protected void init(){
        listWidth=Math.max(110,Math.min(210,width/3));bottom=height-78;
        if(!initialized){zoom=Math.max(20,(bottom-top)*.32f);initialized=true;}
        all.clear();Registries.ITEM.forEach(i->{if(i!=Items.AIR)all.add(i);});all.sort(Comparator.comparing(i->Registries.ITEM.getId(i).toString()));
        search=addDrawableChild(new TextFieldWidget(textRenderer,8,34,listWidth-16,20,Text.literal("Nome ou ID")));
        search.setPlaceholder(Text.literal("Buscar item…"));search.setText(query);search.setChangedListener(q->{query=q;filter(q);});
        rows.clear();int count=Math.max(1,(height-124)/22);
        for(int i=0;i<count;i++){final int row=i;rows.add(button("",8,58+i*22,listWidth-16,()->{int at=page*rows.size()+row;if(at<filtered.size())select(filtered.get(at));}));}
        button("<",8,height-60,24,()->{page=Math.max(0,page-1);refresh();});button(">",36,height-60,24,()->{if((page+1)*rows.size()<filtered.size())page++;refresh();});
        button("Na mao",64,height-60,listWidth-72,()->{if(client.player!=null)select((left?client.player.getOffHandStack():client.player.getMainHandStack()).getItem());});
        int x=listWidth+8,w=width-x-8,third=(w-8)/3;
        hand=button("",x,34,third,()->{left=!left;labels();});
        tool=button("",x+third+4,34,third,()->{operation=(operation+1)%3;labels();});
        axis=button("",x+2*(third+4),34,third,()->{activeAxis=activeAxis==2?-1:activeAxis+1;labels();});
        mode=button("",x,58,(w-4)/2,()->{var types=new ArrayList<>(List.of("auto","off"));WeaponProfiles.FAMILIES.forEach(p->types.add(p.type()));edit.mode=types.get((types.indexOf(edit.mode)+1)%types.size());dirty=true;labels();});
        parry=button("",x+(w+4)/2,58,(w-4)/2,()->{edit.parry=!edit.parry;dirty=true;labels();});
        context=button("",x,81,w,()->{poseContext=(poseContext+1)%4;labels();});
        button("Salvar",x,height-60,third,this::save);
        button("Zerar mao",x+third+4,height-60,third,()->{edit.resetPose(left,poseContext);dirty=true;});
        button("Centralizar",x+2*(third+4),height-60,third,()->{yaw=25;pitch=-15;zoom=Math.max(20,(bottom-top)*.32f);panX=panY=0;});
        button("Exportar",8,height-26,74,()->{if(dirty&&!save())return;try{var p=WeaponConfig.exportPreset();status="Exportado: "+p.getFileName();client.keyboard.setClipboard(p.toAbsolutePath().toString());}catch(Exception e){failure(e);}});
        button("Importar",86,height-26,74,()->importFile(WeaponConfig.importPath()));
        button("Concluir",width-88,height-26,80,this::close);
        filter(query);if(selected.isEmpty()&&client.player!=null)select(client.player.getMainHandStack().getItem());labels();
    }
    private void filter(String value){String q=WeaponNames.normalize(value);filtered.clear();for(var item:all)if(q.isEmpty()||WeaponNames.normalize(Registries.ITEM.getId(item)+" "+item.getName().getString()).contains(q))filtered.add(item);page=0;refresh();}
    private void refresh(){for(int i=0;i<rows.size();i++){int at=page*rows.size()+i;var row=rows.get(i);row.visible=at<filtered.size();if(row.visible)row.setMessage(Text.literal("      "+textRenderer.trimToWidth(filtered.get(at).getName().getString(),listWidth-48)));}}
    private void select(Item item){if(item==Items.AIR)return;if(dirty&&!save())return;selected=new ItemStack(item);edit=WeaponConfig.get(selected).copy();edit.validate();dirty=false;status="";labels();}
    private void labels(){if(mode==null||context==null)return;context.setMessage(Text.literal("Encaixe: "+new String[]{"Normal","Duas maos","Defendendo","Duas maos defendendo"}[poseContext]));mode.setMessage(Text.literal("Classe: "+edit.mode));parry.setMessage(Text.literal("Parry: "+(edit.parry?"sim":"nao")));hand.setMessage(Text.literal(left?"Mao esquerda":"Mao direita"));tool.setMessage(Text.literal(new String[]{"Mover","Girar","Escalar"}[operation]));axis.setMessage(Text.literal("Eixo: "+(activeAxis<0?"livre":new String[]{"X","Y","Z"}[activeAxis])));}
    private boolean save(){if(selected.isEmpty())return true;try{WeaponConfig.save(Registries.ITEM.getId(selected.getItem()).toString(),edit);dirty=false;status=WorldRulesClient.active()?"Enviado ao mundo; aguarde a confirmacao.":"Salvo: ajustes das duas maos.";return true;}catch(Exception e){failure(e);return false;}}
    private void failure(Exception e){status="Falha: "+e.getMessage();EldenSteveClient.LOG.error("Weapon editor",e);}
    private void importFile(Path p){if(dirty){if(!save())return;if(WorldRulesClient.active()){status="Encaixe enviado. Aguarde confirmar e importe novamente.";return;}}try{int n=WeaponConfig.importPreset(p);if(!selected.isEmpty())edit=WeaponConfig.get(selected).copy();labels();status=WorldRulesClient.active()?"Enviando "+n+" itens; aguarde confirmacao do mundo.":"Importados "+n+" itens. Backup anterior salvo.";}catch(Exception e){failure(e);}}
    @Override public void filesDragged(List<Path> paths){if(paths.size()==1)importFile(paths.getFirst());else status="Arraste um arquivo JSON por vez.";}
    private Quaternionf camera(){return new Quaternionf().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw));}
    private float cx(){return (listWidth+width)/2f+panX;}
    private float cy(){return (top+bottom)/2f+panY;}
    private Vector2f project(Vector3f v){camera().transform(v);return new Vector2f(cx()+v.x*zoom,cy()-v.y*zoom);}
    private Vector3f pivot(){var t=edit.pose(left,poseContext);return new Vector3f((left?.65f:-.65f)+t.x,t.y,t.z);}
    private boolean inside(double x,double y){return x>listWidth+4&&x<width-4&&y>=top&&y<bottom;}
    private void line(DrawContext ctx,Vector2f a,Vector2f b,int color){int n=Math.max(1,(int)a.distance(b));for(int i=0;i<=n;i++){float t=(float)i/n;int x=Math.round(a.x+(b.x-a.x)*t),y=Math.round(a.y+(b.y-a.y)*t);ctx.fill(x,y,x+1,y+1,color);}}
    private void gridLine(DrawContext ctx,float x1,float z1,float x2,float z2){var entry=ctx.getMatrices().peek();var v=ctx.getVertexConsumers().getBuffer(RenderLayer.getLines());var n=new Vector3f(x2-x1,0,z2-z1).normalize();v.vertex(entry,x1,-.5f,z1).color(0xFF46515E).normal(entry,n.x,n.y,n.z);v.vertex(entry,x2,-.5f,z2).color(0xFF46515E).normal(entry,n.x,n.y,n.z);}
    // Screen.render calls this again after our scene. Never blur the freshly drawn editor.
    @Override public void renderBackground(DrawContext ctx,int mx,int my,float delta){}
    public void render(DrawContext ctx,int mx,int my,float delta){
        ctx.fill(0,0,width,height,0xFF171A20);
        ctx.drawTexture(Identifier.of("eldensteve","textures/gui/logo.png"),8,3,0,0,54,26,54,26);
        ctx.drawTextWithShadow(textRenderer,title,70,12,0xFFE6AD);
        ctx.fill(listWidth+4,top,width-4,bottom,0xFF252B34);
        ctx.enableScissor(listWidth+4,top,width-4,bottom);
        if(client.player!=null){
            ctx.draw();com.mojang.blaze3d.systems.RenderSystem.clear(256,false);com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            var m=ctx.getMatrices();m.push();m.translate(cx(),cy(),180);m.scale(zoom,-zoom,zoom);m.multiply(camera());
            for(int i=-8;i<=8;i++){gridLine(ctx,i*.25f,-2,i*.25f,2);gridLine(ctx,-2,i*.25f,2,i*.25f);}
            for(boolean side:new boolean[]{false,true}){
                m.push();m.translate(side?.65:-.65,0,0);
                m.push();m.translate(0,.375,0);m.scale(1,-1,-1);
                SegmentCuboid.renderPart(side?LEFT:RIGHT,m,ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(client.player.getSkinTextures().texture())),0xF000F0,OverlayTexture.DEFAULT_UV,-1);m.pop();
                if(!selected.isEmpty())HeldWeaponRenderer.render(client.player,selected,side,(poseContext&1)!=0,edit,poseContext,m,ctx.getVertexConsumers(),0xF000F0,OverlayTexture.DEFAULT_UV);
                m.pop();
            }
            m.pop();ctx.getVertexConsumers().draw();
        }
        ctx.getMatrices().push();ctx.getMatrices().translate(0,0,800);
        var origin=project(pivot());int[] colors={0xFFFF6868,0xFF85E174,0xFF77AEFF};
        for(int a=0;a<3;a++){if(operation==1){for(int i=0;i<96;i++)line(ctx,ring(a,i*(float)Math.PI/48),ring(a,(i+1)*(float)Math.PI/48),colors[a]);continue;}var end=handle(a);line(ctx,origin,end,colors[a]);ctx.fill((int)end.x-5,(int)end.y-5,(int)end.x+6,(int)end.y+6,colors[a]);ctx.drawTextWithShadow(textRenderer,new String[]{"X","Y","Z"}[a],(int)end.x+7,(int)end.y-4,colors[a]);}
        if(operation!=1)ctx.fill((int)origin.x-5,(int)origin.y-5,(int)origin.x+6,(int)origin.y+6,0xFFFFDB78);
        ctx.draw();ctx.getMatrices().pop();
        ctx.disableScissor();super.render(ctx,mx,my,delta);
        for(int i=0;i<rows.size();i++){int at=page*rows.size()+i;if(at<filtered.size())ctx.drawItem(new ItemStack(filtered.get(at)),12,60+i*22);}
        var t=edit.pose(left,poseContext);
        String transform=String.format(Locale.ROOT,"XYZ %.2f %.2f %.2f | graus %.0f %.0f %.0f | %.2fx",t.x,t.y,t.z,t.rx,t.ry,t.rz,t.scale);
        ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(transform,width-listWidth-16),listWidth+8,bottom+2,0xD5E8EF);
        ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(status.isEmpty()?"Arraste: editar | direito: orbitar | roda: zoom | Shift: fino":status,width-16),8,height-37,0xB7D5A4);
        if(inside(mx,my))ctx.drawTextWithShadow(textRenderer,left?"ESQUERDA":"DIREITA",listWidth+12,top+5,0xFFE4A6);
        if(!selected.isEmpty())ctx.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(Registries.ITEM.getId(selected.getItem()).toString(),width-listWidth-16),listWidth+8,bottom-12,0xE2E2E2);
    }
    private Vector2f handle(int a){var d=new Vector3f();d.setComponent(a,1);var v=camera().transform(d);var axis2=new Vector2f(v.x,-v.y);if(axis2.lengthSquared()<.02f)axis2.set(a==0?1:0,a==0?0:-1);return project(pivot()).add(axis2.normalize().mul(48));}
    private Vector2f ring(int a,float angle){var v=pivot();v.setComponent((a+1)%3,v.get((a+1)%3)+(float)Math.cos(angle)*55/zoom);v.setComponent((a+2)%3,v.get((a+2)%3)+(float)Math.sin(angle)*55/zoom);return project(v);}
    private float ringAngle(int a,double x,double y){var o=project(pivot());var u=ring(a,0).sub(o);var v=ring(a,(float)Math.PI/2).sub(o);float dx=(float)x-o.x,dy=(float)y-o.y,det=u.x*v.y-u.y*v.x;if(Math.abs(det)<1)return (float)Math.atan2(dy,dx);return (float)Math.atan2((u.x*dy-u.y*dx)/det,(dx*v.y-dy*v.x)/det);}
    private int hitHandle(double x,double y){
        if(operation==1){int best=-2;float distance=10;for(int a=0;a<3;a++)for(int i=0;i<192;i++){float d=ring(a,i*(float)Math.PI/96).distance((float)x,(float)y);if(d<distance){distance=d;best=a;}}return best;}
        var p=project(pivot());if(p.distance((float)x,(float)y)<=10)return -1;
        int best=-2;float distance=13;
        for(int a=0;a<3;a++){var end=handle(a);float d=end.distance((float)x,(float)y);if(d<distance){best=a;distance=d;}}
        return best;
    }
    public boolean mouseClicked(double x,double y,int b){if(inside(x,y)){
        setFocused(null);search.setFocused(false);
        dragButton=b==0?-1:b;
        if(b==0){int hit=hitHandle(x,y);if(hit!=-2){activeAxis=hit;dragButton=0;if(operation==1)dragAngle=ringAngle(hit,x,y);labels();}}
        return true;
    }return super.mouseClicked(x,y,b);}
    public boolean mouseDragged(double x,double y,int b,double dx,double dy){
        if(dragButton<0||dragButton!=b)return super.mouseDragged(x,y,b,dx,dy);
        if(!inside(x,y)){dragButton=-1;return true;}
        if(b==1){yaw+=(float)dx*.6f;pitch=Math.clamp(pitch+(float)dy*.6f,-85,85);return true;}
        if(b==2){panX+=dx;panY+=dy;return true;}
        var t=edit.editPose(left,poseContext);float fine=hasShiftDown()?.1f:1;
        if(operation==0){
            Vector3f change;
            if(activeAxis<0)change=camera().invert().transform(new Vector3f((float)dx/zoom,-(float)dy/zoom,0));
            else{var axis3=new Vector3f();axis3.setComponent(activeAxis,1);var projected=camera().transform(new Vector3f(axis3));float denominator=projected.x*projected.x+projected.y*projected.y;change=axis3.mul(denominator<.02f?-(float)dy/zoom:((float)dx*projected.x-(float)dy*projected.y)/(zoom*denominator));}
            t.x+=change.x*fine;t.y+=change.y*fine;t.z+=change.z*fine;
        }else if(operation==1){float next=ringAngle(activeAxis,x,y);float angle=(float)Math.toDegrees(Math.atan2(Math.sin(next-dragAngle),Math.cos(next-dragAngle)))*fine;dragAngle=next;if(activeAxis==0)t.rx+=angle;else if(activeAxis==1)t.ry+=angle;else t.rz+=angle;}
        else t.scale*=(float)Math.exp((dx-dy)*.01*fine);
        t.validate();dirty=true;return true;
    }
    public boolean mouseReleased(double x,double y,int b){dragButton=-1;return super.mouseReleased(x,y,b);}
    public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(inside(x,y)){zoom=Math.clamp(zoom*(float)Math.exp(vertical*.12),15,240);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    public boolean keyPressed(int key,int scan,int modifiers){
        if(!search.isFocused()){
            if(key==GLFW.GLFW_KEY_S&&hasControlDown()){save();return true;}
            if(key==GLFW.GLFW_KEY_X||key==GLFW.GLFW_KEY_Y||key==GLFW.GLFW_KEY_Z){activeAxis=key==GLFW.GLFW_KEY_X?0:key==GLFW.GLFW_KEY_Y?1:2;labels();return true;}
            if(key==GLFW.GLFW_KEY_G||key==GLFW.GLFW_KEY_R||key==GLFW.GLFW_KEY_B){operation=key==GLFW.GLFW_KEY_G?0:key==GLFW.GLFW_KEY_R?1:2;labels();return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    public void close(){if(dirty&&!save())return;client.setScreen(parent);}
}
