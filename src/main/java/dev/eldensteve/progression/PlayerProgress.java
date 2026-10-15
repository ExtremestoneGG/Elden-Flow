package dev.eldensteve.progression;

import java.util.EnumMap;
import java.util.Map;

/** Mutated only by server thread; immutable copies are sent to clients. */
public final class PlayerProgress {
    private long souls;
    private int unspent;
    private final EnumMap<Attribute,Integer> levels=new EnumMap<>(Attribute.class);
    public PlayerProgress(){this(0,0,Map.of());}
    public PlayerProgress(long souls,int unspent,Map<Attribute,Integer> levels){
        this.souls=Math.clamp(souls,0,ProgressionRules.MAX_SOULS);
        this.unspent=Math.clamp(unspent,0,ProgressionRules.MAX_ATTRIBUTE*Attribute.values().length);
        for(var a:Attribute.values())this.levels.put(a,ProgressionRules.level(levels.getOrDefault(a,0)));
        this.unspent=Math.min(this.unspent,ProgressionRules.MAX_ATTRIBUTE*Attribute.values().length-allocated());
    }
    public long souls(){return souls;}
    public int unspent(){return unspent;}
    public int level(Attribute a){return levels.get(a);}
    public int allocated(){return levels.values().stream().mapToInt(Integer::intValue).sum();}
    public int purchased(){return allocated()+unspent;}
    public long nextCost(){return unspent>0?0:ProgressionRules.cost(purchased());}
    public Map<Attribute,Integer> levels(){return Map.copyOf(levels);}
    public void earn(long value){souls=ProgressionRules.addSouls(souls,value);}
    public boolean buy(Attribute a){
        if(a==null||level(a)>=ProgressionRules.MAX_ATTRIBUTE)return false;
        long cost=nextCost();if(souls<cost)return false;
        if(unspent>0)unspent--;else souls-=cost;
        levels.put(a,level(a)+1);return true;
    }
    public boolean buyCreative(Attribute a){
        if(a==null||level(a)>=ProgressionRules.MAX_ATTRIBUTE)return false;
        levels.put(a,level(a)+1);return true;
    }
    /** Caller must validate rest and consume the reset ingredients transactionally. */
    public void respec(){unspent=purchased();levels.replaceAll((a,n)->0);}
    public PlayerProgress copy(){return new PlayerProgress(souls,unspent,levels);}
}
