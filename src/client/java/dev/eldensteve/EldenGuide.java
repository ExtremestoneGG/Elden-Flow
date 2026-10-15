package dev.eldensteve;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

/** Vanilla book pages, with the keyboard bindings selected by this player. */
public final class EldenGuide extends BookScreen {
    private final Screen parent;
    public EldenGuide(Screen parent){super(new Contents(pages()));this.parent=parent;}
    private static String key(KeyBinding binding){return binding==null?"Sem tecla":binding.getBoundKeyLocalizedText().getString();}
    private static List<Text> pages(){
        var o=MinecraftClient.getInstance().options;
        return List.of(
            Text.literal("ELDEN FLOW\n\nCombate e movimento no seu mundo.\n\nModo Elden: "+key(EldenSteveClient.toggle)+"\nInventário: "+key(o.inventoryKey)+"\n\nNo controle: clique os dois analógicos juntos para entrar ou sair."),
            Text.literal("TECLADO · MOVIMENTO\n\nPular: "+key(EldenSteveClient.jump)+"\nCorrer: "+key(EldenSteveClient.sprint)+"\nRolar: "+key(EldenSteveClient.dodge)+"\n\nAlvo: "+key(EldenSteveClient.lock)+"\nInteragir: "+key(EldenSteveClient.interact)+"\n\nMude as teclas nas opções do Minecraft."),
            Text.literal("TECLADO · COMBATE\n\nLeve: "+key(o.attackKey)+"\nPesado: "+key(EldenSteveClient.heavy)+" (segure)\nDefesa: "+key(o.useKey)+"\nHabilidade: "+key(EldenSteveClient.skill)+"\n\nSegure "+key(EldenSteveClient.interact)+" e ataque/defenda para empunhar com duas mãos."),
            Text.literal("CONTROLE · PADRÃO\n\nA / X: pular\nB / Círculo: rolar; segurar para correr\nL3: agachar\nR3: fixar alvo\n\nCom alvo fixo, toque o analógico direito para os lados para trocar de inimigo."),
            Text.literal("CONTROLE · COMBATE\n\nRB / R1: ataque leve\nRT / R2: pesado/carregar\nLB / L1: defender ou usar item\nLT / L2: habilidade/parry\n\nY / Triângulo: inventário. Segure + RB/LB para empunhar a arma com duas mãos."),
            Text.literal("CONTROLE · ITENS\n\nDirecional esquerdo/direito: mudar slot.\nBaixo: usar item; segure para comer.\nCima: interagir com blocos ou criaturas.\n\nEstes são os padrões. Reconfigure no Controlify."),
            Text.literal("EMPUNHADURAS\n\nArma na mão secundária: use o slot de escudo.\n\nDuas armas da mesma classe ativam o conjunto duplo no ataque normal.\n\nUma arma segurada com duas mãos é uma postura diferente."),
            Text.literal("FOGUEIRA E ALMAS\n\nInteraja com uma fogueira acesa para descansar. Você se cura, mas continua vulnerável.\n\nMortes no modo Elden geram almas em vez de XP. Fora dele, o XP continua normal."),
            Text.literal("ATRIBUTOS\n\nVeja seus pontos no inventário. Compre melhorias somente descansando na fogueira.\n\nVitalidade: vida.\nFôlego: estamina.\nCapacidade: carga.\nForça/Destreza: dano.\nAfinidade: passivas."),
            Text.literal("CONFIGURAR ARMAS\n\nUse o editor para escolher classe, tamanho e encaixe de cada mão e postura.\n\nEm LAN/servidor, valem as regras do mundo. Somente o anfitrião ou administrador pode alterá-las.")
        );
    }
    public boolean shouldPause(){return false;}
    public void close(){client.setScreen(parent);}
    protected void closeScreen(){close();}
}
