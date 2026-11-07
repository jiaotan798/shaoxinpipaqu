package com.shaoxinpipaqu.compat;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.*;
import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** 女仆 Bedrock／内置 Gecko 渲染作用域；动画完成后覆盖骨骼，全部材质层结束后恢复共享模型。 */
public final class MaidFlightPose implements AutoCloseable {
    private static final ThreadLocal<MaidFlightPose> CURRENT=new ThreadLocal<>();
    private final MaidFlightPose previous;
    private final LivingEntity entity;
    private final Map<BedrockPart,float[]> saved=new IdentityHashMap<>();
    private final Map<AnimatedGeoBone,float[]> geckoSaved=new IdentityHashMap<>();
    public MaidFlightPose(LivingEntity entity) { this.entity=entity; previous=CURRENT.get(); CURRENT.set(this); }
    public static boolean supports(Object model) {
        return humanoid(model) || turtle(model);
    }
    public static boolean humanoid(Object model) {
        return model instanceof BedrockModel<?> bedrock && bedrock.getModelMap().keySet().stream().anyMatch(n -> FlightBones.role(n)==2);
    }
    /** 已核对的玄爷四足布局：egg belly、独立头部和四足，不能把无右臂当作人形缺骨骼。 */
    public static boolean turtle(Object model) {
        return model instanceof BedrockModel<?> b && b.getModelMap().keySet().containsAll(Set.of("eggbelly","head","legRight","legLeft","legRight2","legLeft2"));
    }
    public static void apply(BedrockModel<?> model,LivingEntity entity) {
        var scope=CURRENT.get();
        if(scope==null || scope.entity!=entity || FlightVisuals.weight(entity)<=0 || !supports(model)) return;
        float weight=FlightVisuals.weight(entity);
        if(turtle(model)) {
            // 龟壳原始旋转为90°；四肢网格各自绕Z转±90°后沿Y伸展，因此仍绕X转±90°使其水平。
            for(String name:List.of("legRight","legLeft","legRight2","legLeft2")) {
                var p=model.getModelMap().get(name);
                scope.saved.computeIfAbsent(p,k -> new float[]{p.xRot,p.yRot,p.zRot,p.offsetX,p.offsetY,p.offsetZ});
                p.xRot=FlightVisuals.blend(p.xRot,name.endsWith("2")?(float)Math.PI/2:-(float)Math.PI/2,weight);
                p.offsetX*=1-weight;p.offsetY*=1-weight;p.offsetZ*=1-weight;
            }
            return;
        }
        model.getModelMap().forEach((name,p) -> {
            int role=FlightBones.role(name); if(role==0) return;
            scope.saved.computeIfAbsent(p,k -> new float[]{p.xRot,p.yRot,p.zRot,p.offsetX,p.offsetY,p.offsetZ});
            p.xRot=FlightVisuals.blend(p.xRot,FlightBones.pitch(role,false),weight);p.yRot*=1-weight;
            p.zRot=FlightVisuals.blend(p.zRot,FlightBones.roll(role),weight);
            p.offsetX*=1-weight;p.offsetY*=1-weight;p.offsetZ*=1-weight;
        });
    }
    /** 女仆自带的 Gecko 分支不经过 Bedrock.setupAnim，必须在其动画结束后单独覆盖，保留到手持图层绘制结束。 */
    public static boolean applyGecko(AnimatedGeoModel model,LivingEntity entity) {
        var scope=CURRENT.get();
        if(scope==null || scope.entity!=entity || FlightVisuals.weight(entity)<=0 || model==null || model.rightArm()==null)return false;
        float weight=FlightVisuals.weight(entity);
        model.bones().forEach((name,p)->{
            int role=p==model.rightArm()?2:FlightBones.role(name);if(role==0)return;
            scope.geckoSaved.computeIfAbsent(p,k->new float[]{p.getRotationX(),p.getRotationY(),p.getRotationZ(),p.getPositionX(),p.getPositionY(),p.getPositionZ(),p.getScaleX(),p.getScaleY(),p.getScaleZ()});
            p.setRotation(FlightVisuals.blend(p.getRotationX(),FlightBones.pitch(role,true),weight),p.getRotationY()*(1-weight),FlightVisuals.blend(p.getRotationZ(),FlightBones.roll(role),weight));
            // 使用模型原始位移与比例，而非把动画缩放当成体型；饰品等未识别骨骼仍沿用自己的动画。
            var rest=p.getInitialSnapshot();
            p.setPosition(FlightVisuals.blend(p.getPositionX(),rest.positionOffsetX,weight),FlightVisuals.blend(p.getPositionY(),rest.positionOffsetY,weight),FlightVisuals.blend(p.getPositionZ(),rest.positionOffsetZ,weight));
            p.setScale(FlightVisuals.blend(p.getScaleX(),rest.scaleValueX,weight),FlightVisuals.blend(p.getScaleY(),rest.scaleValueY,weight),FlightVisuals.blend(p.getScaleZ(),rest.scaleValueZ,weight));
        });
        return true;
    }
    @Override public void close() {
        saved.forEach((p,s) -> {p.xRot=s[0];p.yRot=s[1];p.zRot=s[2];p.offsetX=s[3];p.offsetY=s[4];p.offsetZ=s[5];});
        geckoSaved.forEach((p,s)->{p.setRotation(s[0],s[1],s[2]);p.setPosition(s[3],s[4],s[5]);p.setScale(s[6],s[7],s[8]);});
        if(previous==null) CURRENT.remove(); else CURRENT.set(previous);
    }
}
