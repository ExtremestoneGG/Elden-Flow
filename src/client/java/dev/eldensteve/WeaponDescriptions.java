package dev.eldensteve;
import dev.eldensteve.progression.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
/** Concise inventory text; numerical rules are shared with the server. */
public final class WeaponDescriptions {
    public static String passiveName(WeaponFamily f){return switch(f){case DAGGER->"Sequencia";case STRAIGHT_SWORD->"Tenacidade";case CURVED_SWORD->"Corte limitante";case THRUSTING_SWORD->"Economia";case KATANA->"Sangramento";case TWINBLADE->"Afiacao";case GREATSWORD->"Furia carregada";case HAMMER,GREAT_HAMMER->"Atordoamento";case AXE->"Fratura";case GREAT_AXE->"Abertura";case SPEAR->"Repulsao";case HALBERD->"Distancia";case REAPER->"Veneno";case SHIELD->"Parry";};}
    public static String passive(WeaponFamily f,int affinity){double power=ProgressionRules.passivePower(affinity);return switch(f){
        case DAGGER->"Acertos no mesmo alvo: +"+pct(.02*power)+" dano por hit, ate 10. Reinicia apos 5s ou ao sofrer dano.";
        case TWINBLADE->"Acertos: +"+pct(.01*power)+" dano por hit, ate 20. Sofrer dano remove metade; expira apos 5s sem acertar.";
        case GREATSWORD->"Carregado completo: 30% de ganhar +"+pct(.08*power)+" dano por 8s. Acumula 2 vezes.";
        case HAMMER->"Somente carregado completo: 30% de atordoar por "+seconds(ProgressionRules.stunSeconds(false,affinity))+"s.";
        case GREAT_HAMMER->"6% de atordoar; carregado completo: 25%. Duracao: "+seconds(ProgressionRules.stunSeconds(true,affinity))+"s.";
        case THRUSTING_SWORD->"Acertos: "+pct(ProgressionRules.rapierRefundChance(affinity))+" de devolver a estamina paga pelo ataque.";
        case KATANA->"Acumula "+seconds(16*power)+" de sangramento (pesado: "+seconds(24*power)+"). Em 100, causa uma explosao de dano limitada; diminui com o tempo.";
        case SPEAR->"Empurra o alvo; ataques pesados empurram mais. Afinidade amplia o recuo, com limite.";
        case REAPER->"Renova veneno por "+seconds(3*power)+"s. Acertos seguidos aumentam a intensidade, ate veneno III. Afinidade melhora duracao e intensidade inicial.";
        case AXE->"Carregado completo enfraquece o alvo por "+seconds(1.5*power)+"s.";
        case GREAT_AXE->"+"+pct(.06*power)+" dano contra alvos com pelo menos 90% da vida.";
        case HALBERD->"+"+pct(.05*power)+" dano acertando a mais de 2,2 blocos.";
        case STRAIGHT_SWORD->"A cada 3 acertos, recebe resistencia I por "+seconds(power)+"s. Sofrer dano interrompe a sequencia.";
        case CURVED_SWORD->"Carregado completo reduz a velocidade do alvo por "+seconds(power)+"s.";
        case SHIELD->"Parry frontal atordoa por "+seconds(Math.min(1.5,power))+"s e afasta o inimigo. Chefes resistem ao atordoamento.";
    };}
    private static String pct(double d){return String.format(java.util.Locale.ROOT,"%.1f%%",d*100);}
    private static String seconds(double d){return String.format(java.util.Locale.ROOT,"%.1f",d);}
    public static void register(){net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,type,lines)->{
        if(!EldenSteveClient.enabled)return;var profile=WeaponProfiles.get(stack);if(profile==null)return;var family=WeaponFamily.fromType(profile.type());if(family==null)return;
        Text name=lines.isEmpty()?stack.getName():lines.getFirst();lines.clear();lines.add(name);
        lines.add(Text.literal("Elden Flow | "+profile.type()).formatted(Formatting.GOLD));
        lines.add(Text.literal("Escala: Forca "+pct(family.strength)+" / Destreza "+pct(1-family.strength)).formatted(Formatting.GRAY));
        lines.add(Text.literal("Leve: dano x1,5 | Pesado: dano x2,5").formatted(Formatting.GRAY));
        lines.add(Text.literal("Peso da arma: "+seconds(profile.weight())).formatted(Formatting.GRAY));
        lines.add(Text.literal(passiveName(family)+" · Afinidade").formatted(Formatting.LIGHT_PURPLE));
        var font=net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        // Keep tooltips narrow even for a long passive description.
        String text=passive(family,ProgressionClient.view().level(Attribute.AFFINITY));StringBuilder row=new StringBuilder();
        for(String word:text.split(" ")){if(font.getWidth(row+" "+word)>250&&!row.isEmpty()){lines.add(Text.literal(row.toString()).formatted(Formatting.GRAY));row.setLength(0);}if(!row.isEmpty())row.append(' ');row.append(word);}if(!row.isEmpty())lines.add(Text.literal(row.toString()).formatted(Formatting.GRAY));
    });}
}
