package dev.eldensteve;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Secondary-hand assistance only. The authored dominant arm and weapon never move. */
public final class GripSolver {
    public static void fit(StevePose pose,Vector3f axis,float spacing,float bladeLength){
        var target=pose.hand(false).fma(spacing,new Vector3f(axis).normalize());
        var current=pose.hand(true);
        float distance=target.distance(pose.origins[2]),correction=target.distance(current);
        var front=pose.rotations[0].transform(new Vector3f(0,0,1));
        // Unreachable grips keep the authored pose instead of folding an arm through the body.
        if(distance>.735f||correction>.45f||new Vector3f(target).sub(pose.origins[0]).dot(front)<.04f)return;
        float weight=.65f*Math.clamp((.735f-distance)/.08f,0,1)*Math.clamp((.45f-correction)/.15f,0,1);
        var upper=new Quaternionf(pose.rotations[2]);var lower=new Quaternionf(pose.rotations[3]);
        var elbow=new Vector3f(pose.origins[3]);
        pose.reach(true,target);
        pose.rotations[2].set(new Quaternionf(upper).slerp(pose.rotations[2],weight));
        pose.rotations[3].set(new Quaternionf(lower).slerp(pose.rotations[3],weight));
        pose.origins[3].set(pose.rotations[2].transform(new Vector3f(0,-.375f,0))).add(pose.origins[2]);
        // Interpolated joint rotations can describe an arc larger than the requested hand correction.
        // Reject that result instead of dragging a supporting arm through the chest.
        if(pose.hand(true).distance(current)>.25f||new Vector3f(pose.hand(true)).sub(pose.origins[0]).dot(front)<.02f){
            pose.rotations[2].set(upper);pose.rotations[3].set(lower);pose.origins[3].set(elbow);
        }
    }
}
