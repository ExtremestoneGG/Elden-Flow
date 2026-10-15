package dev.eldensteve.progression;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.EnumMap;

public final class ProgressionPackets {
    public record Intent(int action,int argument,BlockPos fire) implements CustomPayload {
        public static final Id<Intent> ID=new Id<>(Identifier.of("eldensteve","progression_intent"));
        public static final PacketCodec<RegistryByteBuf,Intent> CODEC=new PacketCodec<>(){
            public Intent decode(RegistryByteBuf b){return new Intent(b.readVarInt(),b.readVarInt(),b.readBlockPos());}
            public void encode(RegistryByteBuf b,Intent p){b.writeVarInt(p.action);b.writeVarInt(p.argument);b.writeBlockPos(p.fire);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Snapshot(PlayerProgress progress,boolean resting,int legacyStamina,boolean raisedSeat) implements CustomPayload {
        public static final Id<Snapshot> ID=new Id<>(Identifier.of("eldensteve","progression_snapshot"));
        public static final PacketCodec<RegistryByteBuf,Snapshot> CODEC=new PacketCodec<>(){
            public Snapshot decode(RegistryByteBuf b){
                long souls=b.readVarLong();int free=b.readVarInt();var stats=new EnumMap<Attribute,Integer>(Attribute.class);
                for(var a:Attribute.values())stats.put(a,b.readVarInt());
                return new Snapshot(new PlayerProgress(souls,free,stats),b.readBoolean(),Math.clamp(b.readVarInt(),0,10000),b.readBoolean());
            }
            public void encode(RegistryByteBuf b,Snapshot p){
                b.writeVarLong(p.progress.souls());b.writeVarInt(p.progress.unspent());
                for(var a:Attribute.values())b.writeVarInt(p.progress.level(a));b.writeBoolean(p.resting);b.writeVarInt(p.legacyStamina);b.writeBoolean(p.raisedSeat);
            }
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
}
