package dev.eldensteve;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

/** A manual combat playground; only the explicitly named disposable world is changed. */
public final class CombatLab {
    public static void stock(MinecraftClient c){
        if(c.player==null||c.getServer()==null)return;
        var server=c.getServer();var id=c.player.getUuid();
        if(!server.getSaveProperties().getLevelName().equals("Elden Prototype Lab")){
            c.player.sendMessage(Text.literal("F10: use o mundo Elden Prototype Lab."),false);return;
        }
        server.execute(()->{
            var p=server.getPlayerManager().getPlayer(id);if(p==null)return;var w=p.getServerWorld();
            net.minecraft.block.entity.ChestBlockEntity chest=null;
            for(int x=-2;x>=-8;x-=2){
                var pos=new BlockPos(x,81,-2);var existing=w.getBlockEntity(pos);
                if(existing instanceof net.minecraft.block.entity.ChestBlockEntity ch&&ch.hasCustomName()&&ch.getCustomName().getString().equals("Armas Elden / Steve")){chest=ch;break;}
                if(w.isAir(pos)&&w.isAir(pos.up())){
                    w.setBlockState(pos,Blocks.CHEST.getDefaultState());
                    chest=(net.minecraft.block.entity.ChestBlockEntity)w.getBlockEntity(pos);break;
                }
            }
            if(chest==null){p.sendMessage(Text.literal("Sem espaco livre para o bau entre x=-2 e -8, y=81, z=-2."),false);return;}
            var namedChest=new ItemStack(Items.CHEST);namedChest.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,Text.literal("Armas Elden / Steve"));chest.readComponents(namedChest);int slot=0;
            for(var profile:WeaponProfiles.ALL){
                var itemId=net.minecraft.util.Identifier.of("simplyswords","iron_"+profile.type());
                var item=net.minecraft.registry.Registries.ITEM.get(itemId);
                if(item==Items.AIR){p.sendMessage(Text.literal("Arma ausente: "+itemId),false);continue;}
                chest.setStack(slot,new ItemStack(item));chest.setStack(slot+13,new ItemStack(item));slot++;
            }
            chest.markDirty();
            stockArmor(w,p);
            stockExtras(w);
            for(int i=0;i<3;i++){
                double x=i==0?.5:i==1?5.5:-4.5,z=i==0?4.5:8.5;
                if(w.getEntitiesByClass(MobEntity.class,new Box(x-1,80,z-1,x+1,85,z+1),m->m.isAlive()&&m.getCommandTags().contains("eldensteve_lab_target")).isEmpty()){
                    var mob=EntityType.IRON_GOLEM.create(w);if(mob!=null){
                        mob.refreshPositionAndAngles(x,81,z,180,0);mob.setAiDisabled(true);mob.setPersistent();
                        mob.addCommandTag("eldensteve_lab_target");mob.setCustomNameVisible(true);w.spawnEntity(mob);
                    }
                }
            }
            for(var e:w.getEntitiesByClass(MobEntity.class,new Box(-10,80,-9,10,88,20),m->m.getCommandTags().contains("eldensteve_lab_target"))){e.setHealth(e.getMaxHealth());e.setCustomName(Text.literal("Alvo Elden - vida infinita"));}
            p.sendMessage(Text.literal(slot+" classes no bau "+chest.getPos().toShortString()+". E: abrir; E+clique: alternar 1/2 maos."),false);
        });
    }
    private static void stockArmor(net.minecraft.server.world.ServerWorld w,net.minecraft.server.network.ServerPlayerEntity p){
        String name="Armaduras Elden / Steve";
        net.minecraft.block.entity.ChestBlockEntity chest=null;
        for(int x=-2;x>=-8;x-=2){
            var pos=new BlockPos(x,81,-4);var existing=w.getBlockEntity(pos);
            if(existing instanceof net.minecraft.block.entity.ChestBlockEntity ch&&ch.hasCustomName()&&name.equals(ch.getCustomName().getString())){chest=ch;break;}
            if(w.isAir(pos)&&w.isAir(pos.up())){w.setBlockState(pos,Blocks.CHEST.getDefaultState());chest=(net.minecraft.block.entity.ChestBlockEntity)w.getBlockEntity(pos);break;}
        }
        if(chest==null){p.sendMessage(Text.literal("Sem espaco para o bau de armaduras em z=-4."),false);return;}
        var named=new ItemStack(Items.CHEST);named.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,Text.literal(name));chest.readComponents(named);
        int slot=0;
        for(String material:new String[]{"leather","chainmail","iron","golden","diamond","netherite"})
            for(String piece:new String[]{"helmet","chestplate","leggings","boots"}){
                var item=net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.ofVanilla(material+"_"+piece));
                chest.setStack(slot++,new ItemStack(item));
            }
        var dyed=new ItemStack(Items.LEATHER_CHESTPLATE);
        dyed.set(net.minecraft.component.DataComponentTypes.DYED_COLOR,new net.minecraft.component.type.DyedColorComponent(0xB32632,true));
        chest.setStack(24,dyed);chest.setStack(25,new ItemStack(Items.ELYTRA));chest.setStack(26,new ItemStack(Items.TURTLE_HELMET));chest.markDirty();
        p.sendMessage(Text.literal("Armaduras no bau "+chest.getPos().toShortString()+"; peso da armadura nao altera o roll."),false);
    }
    private static void stockExtras(net.minecraft.server.world.ServerWorld w){
        var pos=new BlockPos(4,81,-4);var existing=w.getBlockEntity(pos);
        String name="Escudos e armas vanilla";
        if(w.isAir(pos))w.setBlockState(pos,Blocks.CHEST.getDefaultState());
        if(w.getBlockEntity(pos) instanceof net.minecraft.block.entity.ChestBlockEntity chest
            &&(existing==null||chest.hasCustomName()&&name.equals(chest.getCustomName().getString()))){
            var named=new ItemStack(Items.CHEST);named.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,Text.literal(name));chest.readComponents(named);
            int slot=0;for(var item:new net.minecraft.item.Item[]{Items.SHIELD,Items.WOODEN_SWORD,Items.IRON_SWORD,Items.NETHERITE_SWORD,Items.WOODEN_AXE,Items.IRON_AXE,Items.NETHERITE_AXE,Items.COOKED_BEEF,Items.CAMPFIRE})chest.setStack(slot++,new ItemStack(item));chest.markDirty();
        }
        var fire=new BlockPos(0,81,-5);if(w.isAir(fire))w.setBlockState(fire,Blocks.CAMPFIRE.getDefaultState());
    }
    public static void create(MinecraftClient c){
        if(c.player==null||c.getServer()==null)return;
        if(!c.getServer().getSaveProperties().getLevelName().equals("Elden Prototype Lab")){
            c.player.sendMessage(Text.literal("F8: crie um mundo descartavel chamado Elden Prototype Lab."),false);return;
        }
        var server=c.getServer();var id=c.player.getUuid();
        server.execute(()->{
            var p=server.getPlayerManager().getPlayer(id);if(p==null)return;var w=p.getServerWorld();
            for(int x=-9;x<=9;x++)for(int z=-8;z<=18;z++){
                w.setBlockState(new BlockPos(x,80,z),Blocks.STONE.getDefaultState());
                for(int y=81;y<=86;y++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
            }
            // A low obstacle and a solid wall to check jumping and blocked hits.
            for(int x=4;x<=6;x++)w.setBlockState(new BlockPos(x,81,5),Blocks.STONE_BRICKS.getDefaultState());
            for(int x=-6;x<=-4;x++)for(int y=81;y<=84;y++)w.setBlockState(new BlockPos(x,y,5),Blocks.STONE_BRICKS.getDefaultState());
            for(var mob:w.getEntitiesByClass(MobEntity.class,new Box(-10,80,-9,10,88,20),e->e.getCommandTags().contains("eldensteve_lab_target")))mob.discard();
            for(int i=0;i<3;i++){
                var mob=EntityType.IRON_GOLEM.create(w);if(mob==null)continue;
                mob.refreshPositionAndAngles(i==0?.5:i==1?5.5:-4.5,81,i==0?4.5:8.5,180,0);
                mob.setAiDisabled(true);mob.setPersistent();mob.addCommandTag("eldensteve_lab_target");
                mob.setCustomName(Text.literal("Alvo Elden "+(i+1)));mob.setCustomNameVisible(true);w.spawnEntity(mob);
            }
            p.changeGameMode(GameMode.SURVIVAL);p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);
            var previous=p.getMainHandStack().copy();p.setStackInHand(Hand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            if(!previous.isEmpty())p.getInventory().offerOrDrop(previous);
            p.requestTeleport(.5,81,.5);p.setYaw(0);p.setPitch(5);
        });
        EldenSteveClient.activate(c);
        stock(c);
        c.player.sendMessage(Text.literal("Laboratorio preparado com armas, escudos, armaduras e fogueira."),false);
    }
}
