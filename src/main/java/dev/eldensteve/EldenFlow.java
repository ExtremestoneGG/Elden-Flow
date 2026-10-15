package dev.eldensteve;

import dev.eldensteve.progression.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;

public final class EldenFlow implements ModInitializer {
    public static final net.minecraft.entity.EntityType<SoulOrbEntity> SOUL_ORB=net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.ENTITY_TYPE,net.minecraft.util.Identifier.of("eldensteve","soul_orb"),net.minecraft.entity.EntityType.Builder.<SoulOrbEntity>create(SoulOrbEntity::new,net.minecraft.entity.SpawnGroup.MISC).dimensions(.5f,.5f).maxTrackingRange(6).trackingTickInterval(10).build("eldensteve:soul_orb"));
    public void onInitialize(){
        PayloadTypeRegistry.playS2C().register(dev.eldensteve.combat.CombatPackets.RestVisual.ID,dev.eldensteve.combat.CombatPackets.RestVisual.CODEC);
        PayloadTypeRegistry.playS2C().register(dev.eldensteve.combat.CombatPackets.Stagger.ID,dev.eldensteve.combat.CombatPackets.Stagger.CODEC);
        PayloadTypeRegistry.playC2S().register(dev.eldensteve.combat.CombatPackets.Input.ID,dev.eldensteve.combat.CombatPackets.Input.CODEC);
        PayloadTypeRegistry.playS2C().register(dev.eldensteve.combat.CombatPackets.Frame.ID,dev.eldensteve.combat.CombatPackets.Frame.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(dev.eldensteve.combat.ServerCombat::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(dev.eldensteve.combat.ServerCombat::stop);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->dev.eldensteve.combat.ServerCombat.leave(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(dev.eldensteve.combat.CombatPackets.Input.ID,(packet,context)->context.server().execute(()->dev.eldensteve.combat.ServerCombat.receive(context.player(),packet.value())));
        PayloadTypeRegistry.playC2S().register(ProgressionPackets.Intent.ID,ProgressionPackets.Intent.CODEC);
        PayloadTypeRegistry.playS2C().register(ProgressionPackets.Snapshot.ID,ProgressionPackets.Snapshot.CODEC);
        PayloadTypeRegistry.playC2S().register(WeaponRulePackets.Edit.ID,WeaponRulePackets.Edit.CODEC);
        PayloadTypeRegistry.playC2S().register(WeaponRulePackets.Upload.ID,WeaponRulePackets.Upload.CODEC);
        PayloadTypeRegistry.playS2C().register(WeaponRulePackets.Part.ID,WeaponRulePackets.Part.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(ProgressionServer::start);
        ServerLifecycleEvents.SERVER_STARTED.register(WeaponRulesServer::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(ProgressionServer::stop);
        ServerLifecycleEvents.SERVER_STOPPING.register(WeaponRulesServer::stop);
        ServerTickEvents.END_SERVER_TICK.register(ProgressionServer::tick);
        ServerTickEvents.END_SERVER_TICK.register(WeaponRulesServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->ProgressionServer.sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->ProgressionServer.leave(handler.player));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->WeaponRulesServer.sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->WeaponRulesServer.leave(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(ProgressionPackets.Intent.ID,(packet,context)->context.server().execute(()->ProgressionServer.handle(context.player(),packet)));
        ServerPlayNetworking.registerGlobalReceiver(WeaponRulePackets.Edit.ID,(packet,context)->context.server().execute(()->WeaponRulesServer.edit(context.player(),packet)));
        ServerPlayNetworking.registerGlobalReceiver(WeaponRulePackets.Upload.ID,(packet,context)->context.server().execute(()->WeaponRulesServer.upload(context.player(),packet)));
    }
}
