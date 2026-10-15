package dev.eldensteve.progression;

/** Pure authoritative formulas, shared with UI previews. No client or registry references. */
public final class ProgressionRules {
    private ProgressionRules(){}
    public static final int MAX_ATTRIBUTE=10_000;
    public static final long MAX_SOULS=9_000_000_000_000_000L;
    public static int level(int value){return Math.clamp(value,0,MAX_ATTRIBUTE);}
    public static long cost(int total){long n=Math.clamp(total,0,MAX_ATTRIBUTE*Attribute.values().length);return 100L+25L*n+5L*n*n;}
    public static long soulsForExperience(int xp){return Math.max(0L,(long)xp)*2L;}
    public static long addSouls(long current,long earned){return Math.min(MAX_SOULS,Math.clamp(current,0,MAX_SOULS)+Math.clamp(earned,0,MAX_SOULS));}
    public static double soft(int level){int n=level(level);return Math.min(n,20)+Math.clamp(n-20,0,30)*.5+Math.max(0,n-50)*.1;}
    public static double extraHealth(int vitality){return soft(vitality);}
    public static float maximumStamina(int endurance){return (float)(97+2*soft(endurance));}
    public static float relativeWeight(float rawWeight,int capacity){if(!Float.isFinite(rawWeight))return 0;return Math.max(0,rawWeight)/(float)(1+.025*soft(capacity));}
    public static double physicalMultiplier(WeaponFamily family,int strength,int dexterity){return 1+.025*(family.strength*soft(strength)+(1-family.strength)*soft(dexterity));}
    public static double passivePower(int affinity){return Math.min(2,1+.015*soft(affinity));}
    public static double stunSeconds(boolean large,int affinity){return Math.min(1.5,(large?.6:.35)*passivePower(affinity));}
    public static double stunChance(boolean large,boolean fullyCharged){return large?(fullyCharged?.25:.06):(fullyCharged?.30:0);}
    public static double rapierRefundChance(int affinity){return Math.min(.2,.04+.0015*soft(affinity));}
    public static double spearKnockback(int affinity,boolean heavy){return Math.min(.65,(heavy?.35:.15)*passivePower(affinity));}
    public static double bleedBurst(double maxHealth,boolean boss,int affinity){return Math.min(boss?12:24,2+Math.min(200,Math.max(0,maxHealth))*(boss?.025:.06)*passivePower(affinity));}
}
