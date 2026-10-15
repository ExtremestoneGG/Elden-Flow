package dev.eldensteve.combat;
import java.util.*;
/** Client-only cosmetic projectile prediction; server collision never consults this cache. */
public final class DefenseVisuals {
    private record Value(int flags,long expires){}
    private static final Map<UUID,Value> values=new HashMap<>();
    public static void put(UUID id,int flags){if(flags==0)values.remove(id);else values.put(id,new Value(flags,System.nanoTime()+200_000_000L));}
    public static void prune(){long now=System.nanoTime();values.entrySet().removeIf(e->e.getValue().expires<=now);}
    public static int get(UUID id){var v=values.get(id);return v!=null&&System.nanoTime()<v.expires?v.flags:0;}
    public static void clear(){values.clear();}
}
