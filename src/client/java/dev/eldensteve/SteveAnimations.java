package dev.eldensteve;
/** Local pose facade; each remote player owns a separate MotionPlayer. */
public final class SteveAnimations {
    private static final MotionPlayer local=new MotionPlayer();
    public static void load()throws Exception{local.load();}
    public static void motion(float dx,float dz,float yaw){local.motion(dx,dz,yaw);}
    public static void consume(int ticks)throws Exception{local.consume(ticks);}
    public static void reset(){local.reset();}
    public static boolean guarding(){return local.guarding();}
    public static void tick(int id,float frame,boolean action,float speed,boolean supported)throws Exception{local.tick(id,frame,action,speed,supported);}
    public static float[] pose(float delta){return local.pose(delta);}
    public static float[] rest(){return local.rest();}
}
