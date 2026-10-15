package dev.eldensteve;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import dev.eldensteve.mixin.HandledScreenAccess;

public final class InventoryAttributes {
    public static void register(){ScreenEvents.AFTER_INIT.register((client,screen,w,h)->{
        if(!(screen instanceof InventoryScreen)||!EldenSteveClient.enabled)return;
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Guia Elden"),b->client.setScreen(new EldenGuide(screen))).dimensions(4,4,86,20).build());
        var access=(HandledScreenAccess)screen;
        int side=access.eldenflow$x()+access.eldenflow$width()+4;
        if(access.eldenflow$x()>=174)ScreenEvents.afterRender(screen).register((s,ctx,mx,my,delta)->{if(EldenSteveClient.enabled)AttributePanel.weapon(ctx,access.eldenflow$x()-170,Math.max(2,access.eldenflow$y()),166,mx,my);});
        if(side+AttributePanel.WIDTH<=w){
            ScreenEvents.afterRender(screen).register((s,ctx,mx,my,delta)->{if(EldenSteveClient.enabled)AttributePanel.render(ctx,access.eldenflow$x()+access.eldenflow$width()+4,Math.max(2,access.eldenflow$y()),mx,my,false);});
        }else{
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Atributos"),b->client.setScreen(new AttributesScreen(screen,false))).dimensions(Math.max(4,w-82),4,78,20).build());
        }
    });}
}
