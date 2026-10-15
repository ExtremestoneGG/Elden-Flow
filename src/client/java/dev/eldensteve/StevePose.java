package dev.eldensteve;

import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.joml.Matrix3f;

/** Retarget directions onto Steve's fixed lengths, keeping elbows and knees connected. */
public final class StevePose {
    // torso, head, left upper/lower arm, right upper/lower arm, left thigh/calf, right thigh/calf
    public final Vector3f[] origins=new Vector3f[10];
    public final Quaternionf[] rotations=new Quaternionf[10];
    private final float[] sitSource=new float[70];
    private final Quaternionf sitRotation=new Quaternionf();
    public StevePose(){for(int i=0;i<10;i++){origins[i]=new Vector3f();rotations[i]=new Quaternionf();}}
    public void mirror(){
        for(int i=2;i<=7;i++){if(i==4||i==5)continue;int j=i+2;var p=origins[i];origins[i]=origins[j];origins[j]=p;var q=rotations[i];rotations[i]=rotations[j];rotations[j]=q;}
        for(int i=0;i<10;i++){origins[i].x=-origins[i].x;rotations[i].y=-rotations[i].y;rotations[i].z=-rotations[i].z;}
    }
    private static Vector3f joint(float[] p,int i){return new Vector3f(p[i*3],p[i*3+1],p[i*3+2]);}
    private static Vector3f unit(Vector3f v,Vector3f fallback){return v.lengthSquared()>1e-8f?v.normalize():new Vector3f(fallback);}
    private static Quaternionf basis(Vector3f left,Vector3f up){
        up=unit(up,new Vector3f(0,1,0));
        Vector3f forward=unit(new Vector3f(left).cross(up),new Vector3f(0,0,1));
        left=unit(new Vector3f(up).cross(forward),new Vector3f(1,0,0));
        return new Quaternionf().setFromNormalized(new Matrix3f(left,up,forward));
    }
    private static Quaternionf head(float[] p){
        // Mirroring HKX axes changes handedness; negate Z before forming a rotation.
        int o=66;Vector3f x=new Vector3f(p[o],p[o+1],p[o+2]), y=new Vector3f(p[o+3],p[o+4],p[o+5]);
        return basis(x,y);
    }
    public static Quaternionf weaponDelta(float[] p,float[] rest){
        return weaponDelta(p,rest,false);
    }
    public static Quaternionf weaponDelta(float[] p,float[] rest,boolean left){
        int o=left?84:75;
        if(p.length<o+9||rest.length<o+9)return new Quaternionf();
        Quaternionf current=basis(new Vector3f(p[o],p[o+1],p[o+2]),new Vector3f(p[o+3],p[o+4],p[o+5]));
        Quaternionf initial=basis(new Vector3f(rest[o],rest[o+1],rest[o+2]),new Vector3f(rest[o+3],rest[o+4],rest[o+5]));
        return current.mul(initial.invert());
    }
    public void set(float[] p,float[] rest){
        Vector3f pelvis=joint(p,0).mul(0.80f);
        // The controller already applies locomotion/root motion. Keep visual sway inside the body.
        float horizontal=(float)Math.hypot(pelvis.x,pelvis.z);
        if(horizontal>.12f){pelvis.x*=.12f/horizontal;pelvis.z*=.12f/horizontal;}
        Vector3f up=unit(joint(p,2).sub(joint(p,0)),new Vector3f(0,1,0));
        Vector3f left=unit(joint(p,4).sub(joint(p,7)),new Vector3f(1,0,0));
        Quaternionf chest=basis(left,up);left=chest.transform(new Vector3f(1,0,0));
        Vector3f neck=new Vector3f(pelvis).fma(0.75f,up);
        origins[0].set(neck);rotations[0].set(chest);
        origins[1].set(neck);rotations[1].set(head(p)).mul(head(rest).invert());
        Vector3f hips=unit(joint(p,10).sub(joint(p,13)),left);
        chain(p,4,5,6,new Vector3f(pelvis).fma(.64f,up).fma(.375f,left),2,chest);
        chain(p,7,8,9,new Vector3f(pelvis).fma(.64f,up).fma(-.375f,left),4,chest);
        chain(p,10,11,12,new Vector3f(pelvis).fma(.125f,hips),6,chest);
        chain(p,13,14,15,new Vector3f(pelvis).fma(-.125f,hips),8,chest);
        ground(false);
    }
    public Vector3f hand(boolean left){int i=left?3:5;return rotations[i].transform(new Vector3f(0,-.375f,0)).add(origins[i]);}
    /** Stable seated rest, separate from the authored combat clips. */
    public void sit(){sit(false);}
    public void sit(boolean raised,float amount){
        if(amount<=0)return;
        if(amount>=1){sit(raised);return;}
        for(int i=0;i<10;i++){int at=i*7;var p=origins[i];var q=rotations[i];sitSource[at]=p.x;sitSource[at+1]=p.y;sitSource[at+2]=p.z;sitSource[at+3]=q.x;sitSource[at+4]=q.y;sitSource[at+5]=q.z;sitSource[at+6]=q.w;}
        sit(raised);
        for(int i=0;i<10;i++){int at=i*7;var p=origins[i];p.set(sitSource[at]+(p.x-sitSource[at])*amount,sitSource[at+1]+(p.y-sitSource[at+1])*amount,sitSource[at+2]+(p.z-sitSource[at+2])*amount);sitRotation.set(sitSource[at+3],sitSource[at+4],sitSource[at+5],sitSource[at+6]).slerp(rotations[i],amount);rotations[i].set(sitRotation);}
        // Rotations follow arcs: keep the child joint at the actual endpoint, not a linear chord.
        for(int i=2;i<10;i+=2)rotations[i].transform(origins[i+1].set(0,-.375f,0)).add(origins[i]);
    }
    public void sit(boolean raised){
        float hip=raised?.5f:.13f,torso=raised?1.25f:.75f;
        origins[0].set(0,torso,0);origins[1].set(0,torso,0);
        rotations[0].identity();rotations[1].identity();
        for(int side=0;side<2;side++){
            float x=side==0?.125f:-.125f;int leg=6+side*2,arm=2+side*2;
            float sign=side==0?1:-1;
            var thigh=raised?new Vector3f(0,0,1):new Vector3f(sign*.6f,0,.8f);
            var calf=raised?new Vector3f(0,-1,.1f).normalize():new Vector3f(-sign*.99f,0,-.14f).normalize();
            origins[leg].set(x,hip,0);rotations[leg].set(limb(thigh,new Vector3f(0,0,1)));
            origins[leg+1].set(origins[leg]).fma(.375f,thigh);rotations[leg+1].set(limb(calf,new Vector3f(0,0,1)));
            var upper=new Vector3f(sign*.35f,-.9f,.25f).normalize();
            var lower=new Vector3f(-sign*.23f,-.25f,.94f).normalize();
            origins[arm].set(x*3,torso-.11f,0);rotations[arm].set(limb(upper,new Vector3f(0,0,1)));
            origins[arm+1].set(origins[arm]).fma(.375f,upper);rotations[arm+1].set(limb(lower,new Vector3f(0,0,1)));
        }
    }
    /** Two-bone IK with fixed Steve lengths and the authored elbow as the bend hint. */
    public void reach(boolean leftHand,Vector3f target){
        int slot=leftHand?2:4;
        Vector3f start=origins[slot],axis=new Vector3f(target).sub(start);
        float distance=Math.clamp(axis.length(),.001f,.7498f);axis=unit(axis,new Vector3f(0,-1,0));
        Vector3f bend=new Vector3f(origins[slot+1]).sub(start);
        bend.fma(-bend.dot(axis),axis);
        if(bend.lengthSquared()<1e-6f){bend=rotations[0].transform(new Vector3f(0,0,1));bend.fma(-bend.dot(axis),axis);}
        bend=unit(bend,new Vector3f(leftHand?1:-1,0,0));
        var front=rotations[0].transform(new Vector3f(0,0,1));
        if(bend.dot(front)<0)bend.negate();
        float height=(float)Math.sqrt(.375f*.375f-distance*distance/4);
        Vector3f elbow=new Vector3f(start).fma(distance/2,axis).fma(height,bend);
        Vector3f endpoint=new Vector3f(start).fma(distance,axis);
        rotations[slot].set(limb(unit(new Vector3f(elbow).sub(start),axis),front));
        origins[slot+1].set(elbow);
        rotations[slot+1].set(limb(unit(endpoint.sub(elbow),axis),front));
    }
    public float lowest(){
        float min=Float.POSITIVE_INFINITY;
        for(int i=0;i<10;i++) {
            float w=i<2?.25f:.125f, depth=i==1?.25f:.125f;
            float low=i==1?0:i==0?-.75f:-.375f, high=i==1?.5f:0;
            for(float x:new float[]{-w,w})for(float y:new float[]{low,high})for(float z:new float[]{-depth,depth})
                min=Math.min(min,rotations[i].transform(new Vector3f(x,y,z)).add(origins[i]).y);
        }
        return min;
    }
    public void ground(boolean contact){
        float min=lowest();
        if(min<0||contact)for(var v:origins)v.y-=min;
    }
    /** Small contact allowance for the back when extremities determine the retargeted floor. */
    public void rollContact(){
        var back=rotations[0].transform(new Vector3f(0,0,-1));
        float contact=Math.clamp((-back.y-.25f)/.5f,0,1);
        float lowestBack=Float.POSITIVE_INFINITY;
        for(float x:new float[]{-.25f,.25f})for(float y:new float[]{-.75f,0})
            lowestBack=Math.min(lowestBack,rotations[0].transform(new Vector3f(x,y,-.125f)).add(origins[0]).y);
        float down=Math.min(.06f,Math.max(0,lowestBack))*contact;
        for(var v:origins)v.y-=down;
    }
    private void chain(float[] p,int a,int b,int c,Vector3f start,int slot,Quaternionf chest){
        Vector3f upper=unit(joint(p,b).sub(joint(p,a)),new Vector3f(0,-1,0));
        Vector3f lower=unit(joint(p,c).sub(joint(p,b)),upper);
        Vector3f front=chest.transform(new Vector3f(0,0,1));
        origins[slot].set(start);origins[slot+1].set(start).fma(.375f,upper);
        rotations[slot].set(limb(upper,front));rotations[slot+1].set(limb(lower,front));
    }
    private static Quaternionf limb(Vector3f down,Vector3f front){
        Vector3f up=new Vector3f(down).negate();
        Vector3f left=unit(new Vector3f(up).cross(front),new Vector3f(1,0,0));
        return basis(left,up);
    }
}
