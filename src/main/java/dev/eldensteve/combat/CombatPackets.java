package dev.eldensteve.combat;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public final class CombatPackets {
    public record Input(CombatInput value) implements CustomPayload {
        public static final Id<Input> ID=new Id<>(Identifier.of("eldensteve","combat_input"));
        public static final PacketCodec<RegistryByteBuf,Input> CODEC=new PacketCodec<>(){
            public Input decode(RegistryByteBuf b){return new Input(new CombatInput(b.readVarInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readVarInt(),b.readVarInt(),b.readVarInt()));}
            public void encode(RegistryByteBuf b,Input p){var v=p.value;b.writeVarInt(v.sequence());b.writeFloat(v.right());b.writeFloat(v.forward());b.writeFloat(v.yaw());b.writeVarInt(v.buttons());b.writeVarInt(v.target());b.writeVarInt(v.grip());}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Frame(int entity,int sequence,int clip,int grip,int defense,float[] state) implements CustomPayload {
        public static final Id<Frame> ID=new Id<>(Identifier.of("eldensteve","combat_frame"));
        public static final PacketCodec<RegistryByteBuf,Frame> CODEC=new PacketCodec<>(){
            public Frame decode(RegistryByteBuf b){int e=b.readVarInt(),s=b.readVarInt(),c=b.readVarInt(),g=b.readVarInt(),d=b.readVarInt();float[] values=new float[12];for(int i=0;i<12;i++){values[i]=b.readFloat();if(!Float.isFinite(values[i]))throw new IllegalArgumentException("Nonfinite frame");}return new Frame(e,s,c,g,d,values);}
            public void encode(RegistryByteBuf b,Frame p){b.writeVarInt(p.entity);b.writeVarInt(p.sequence);b.writeVarInt(p.clip);b.writeVarInt(p.grip);b.writeVarInt(p.defense);for(int i=0;i<12;i++)b.writeFloat(p.state[i]);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record RestVisual(int entity,net.minecraft.util.math.BlockPos fire,net.minecraft.item.ItemStack stack) implements CustomPayload {
        public static final Id<RestVisual> ID=new Id<>(Identifier.of("eldensteve","rest_visual"));
        public static final PacketCodec<RegistryByteBuf,RestVisual> CODEC=new PacketCodec<>(){
            public RestVisual decode(RegistryByteBuf b){return new RestVisual(b.readVarInt(),b.readBlockPos(),net.minecraft.item.ItemStack.OPTIONAL_PACKET_CODEC.decode(b));}
            public void encode(RegistryByteBuf b,RestVisual p){b.writeVarInt(p.entity);b.writeBlockPos(p.fire);net.minecraft.item.ItemStack.OPTIONAL_PACKET_CODEC.encode(b,p.stack);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
    public record Stagger(int entity,int ticks) implements CustomPayload {
        public static final Id<Stagger> ID=new Id<>(Identifier.of("eldensteve","stagger"));
        public static final PacketCodec<RegistryByteBuf,Stagger> CODEC=new PacketCodec<>(){
            public Stagger decode(RegistryByteBuf b){return new Stagger(b.readVarInt(),Math.clamp(b.readVarInt(),0,40));}
            public void encode(RegistryByteBuf b,Stagger p){b.writeVarInt(p.entity);b.writeVarInt(p.ticks);}
        };
        public Id<? extends CustomPayload> getId(){return ID;}
    }
}
