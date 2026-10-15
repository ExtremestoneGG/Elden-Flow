package dev.eldensteve.progression;

/** Stable serialized IDs; display names are translated by the client. */
public enum Attribute {
    VITALITY, ENDURANCE, CAPACITY, STRENGTH, DEXTERITY, AFFINITY;
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
}
