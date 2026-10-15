package dev.eldensteve.progression;

/** One ordered, bounded upload. No partial document can become a live world preset. */
public final class PresetUpload {
    public static final int CHUNK=16000,MAX_PARTS=(WorldWeaponRules.MAX_BYTES+CHUNK-1)/CHUNK;
    private final long revision;
    private final int count,startTick;
    private final StringBuilder data=new StringBuilder();
    private int index,bytes;
    public PresetUpload(long revision,int count,int tick){
        if(revision<0||count<1||count>MAX_PARTS)throw new IllegalArgumentException("Invalid upload header");
        this.revision=revision;this.count=count;startTick=tick;
    }
    public boolean expired(int tick){return tick-startTick>1200;}
    public String append(long revision,int index,int count,String part,int tick){
        if(expired(tick)||this.revision!=revision||this.index!=index||this.count!=count||index>=count||part.isEmpty()||part.length()>CHUNK)
            throw new IllegalArgumentException("Invalid upload sequence");
        bytes+=part.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if(bytes>WorldWeaponRules.MAX_BYTES)throw new IllegalArgumentException("Upload too large");
        data.append(part);this.index++;
        return this.index==count?data.toString():null;
    }
}
