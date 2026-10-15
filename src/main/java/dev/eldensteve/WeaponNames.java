package dev.eldensteve;
import java.text.Normalizer;
import java.util.*;

/** Shared deterministic name matching for client previews and authoritative server rules. */
public final class WeaponNames {
    public record Match(String type,String reason,int confidence){}
    private static final Map<String,String> WORDS=new LinkedHashMap<>();
    static {
        words("greathammer","greathammer","great_hammer","warhammer","marreta");words("greataxe","greataxe","great_axe","battleaxe");
        words("claymore","claymore","greatsword","great_sword");words("twinblade","twinblade","twin_blade");
        words("katana","katana","uchigatana","nagakiba");words("rapier","rapier","estoc","florete");
        words("cutlass","cutlass","scimitar","curved_sword","falchion");words("halberd","halberd","alabarda");
        words("glaive","glaive");words("scythe","scythe","foice","reaper");words("spear","spear","lanca","pike");
        words("sai","sai","dagger","knife","adaga","punhal");words("axe","axe","hatchet","machado");
        words("hammer","hammer","club","mace","martelo","clava");
        words("longsword","longsword","sword","espada");words("shield","shield","escudo","buckler");
    }
    private static void words(String type,String... names){for(String name:names)WORDS.put(name.replace('_',' '),type);}
    public static String normalize(String value){return Normalizer.normalize(value.replaceAll("([a-z])([A-Z])","$1 $2"),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();}
    public static Match match(String text){
        String normalized=" "+normalize(text)+" ";
        for(String excluded:new String[]{"pickaxe","picareta","swordfish","warglaive","chakram"})if(normalized.contains(" "+excluded+" "))return new Match(null,"Classe excluida ou item sem moveset",0);
        for(var entry:WORDS.entrySet())if(normalized.contains(" "+entry.getKey()+" "))return new Match(entry.getValue(),"Nome: "+entry.getKey(),90);
        String found=null;
        for(String token:normalize(text).split(" ")){
            if(token.length()<5)continue;
            for(var entry:WORDS.entrySet()){
                String name=entry.getKey();if(name.contains(" ")||name.length()<5||Math.abs(token.length()-name.length())>1)continue;
                if(distance(token,name)>1)continue;
                if(found!=null&&!found.equals(entry.getValue()))return new Match(null,"Nome ambiguo: escolha manualmente",0);
                found=entry.getValue();
            }
        }
        return new Match(found,found==null?"Sem correspondencia segura":"Nome aproximado (revise no editor)",found==null?0:65);
    }
    static int distance(String a,String b){int[] prev=new int[b.length()+1];for(int j=0;j<prev.length;j++)prev[j]=j;for(int i=1;i<=a.length();i++){int[] cur=new int[b.length()+1];cur[0]=i;for(int j=1;j<cur.length;j++)cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=cur;}return prev[b.length()];}
}
