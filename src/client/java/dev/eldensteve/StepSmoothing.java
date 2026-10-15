package dev.eldensteve;
import net.minecraft.util.math.MathHelper;

/** Visual compensation for collision-safe steps in either direction. */
public final class StepSmoothing {
    private static float previous,current;
    public static void reset(){previous=current=0;}
    public static void tick(){previous=current;current*=.60f;if(Math.abs(current)<.002f)current=0;}
    public static void stepped(double rise){if(rise>.25&&rise<=1.001)current=Math.max(-1.25f,current-(float)rise);}
    public static void descended(double fall){if(fall>.001&&fall<=1.001)current=Math.min(1.25f,current+(float)fall);}
    public static float offset(float delta){return MathHelper.lerp(delta,previous,current);}
}
