package dev.eldensteve.progression;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class WeaponRulePackets {
    public static final int CHUNK=16000;
    public record Upload(long revision,int index,int count,String json) implements CustomPayload {
        public static final Id<Upload> ID=new Id<>(Identifier.of("eldensteve","weapon_rules_upload"));
        public static final PacketCodec<RegistryByteBuf,Upload> CODEC=new PacketCodec<>(){
            public Upload decode(RegistryByteBuf b){return new Upload(b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readString(CHUNK));}
            public void encode(RegistryByteBuf b,Upload p){b.writeVarLong(p.revision);b.writeVarInt(p.index);b.writeVarInt(p.count);b.writeString(p.json,CHUNK);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Part(long revision,int index,int count,boolean editable,String json) implements CustomPayload {
        public static final Id<Part> ID=new Id<>(Identifier.of("eldensteve","weapon_rules_part"));
        public static final PacketCodec<RegistryByteBuf,Part> CODEC=new PacketCodec<>(){
            public Part decode(RegistryByteBuf b){return new Part(b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readString(CHUNK));}
            public void encode(RegistryByteBuf b,Part p){b.writeVarLong(p.revision);b.writeVarInt(p.index);b.writeVarInt(p.count);b.writeBoolean(p.editable);b.writeString(p.json,CHUNK);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Edit(long revision,String item,String json) implements CustomPayload {
        public static final Id<Edit> ID=new Id<>(Identifier.of("eldensteve","weapon_rules_edit"));
        public static final PacketCodec<RegistryByteBuf,Edit> CODEC=new PacketCodec<>(){
            public Edit decode(RegistryByteBuf b){return new Edit(b.readVarLong(),b.readString(256),b.readString(8192));}
            public void encode(RegistryByteBuf b,Edit p){b.writeVarLong(p.revision);b.writeString(p.item,256);b.writeString(p.json,8192);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
}
