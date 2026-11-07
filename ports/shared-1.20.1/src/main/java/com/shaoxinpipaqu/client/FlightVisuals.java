package com.shaoxinpipaqu.client;

import com.friendswine.PartyGuestEntity;
import com.shaoxinpipaqu.MobFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** 共用飞行表现；只消费同步标记和客户端位移，不修改镜头、权限、实体朝向或运动。 */
public final class FlightVisuals {
    private static final Map<LivingEntity,FlightPoseMotion> STATES=new WeakHashMap<>();
    private static ClientLevel level;
    private FlightVisuals() {}

    /** 换世界立即清理；立板维持前置显示，不加入骨骼倾斜和姿态过渡。 */
    public static void tick() {
        var current=Minecraft.getInstance().level;
        if(current!=level) { STATES.clear();level=current; }
        if(current==null || Minecraft.getInstance().isPaused())return;
        STATES.keySet().removeIf(e->e.isRemoved() || !e.isAlive());
        for(var entity:current.entitiesForRendering()) {
            if(!(entity instanceof LivingEntity living) || entity instanceof PartyGuestEntity || !living.isAlive())continue;
            boolean flying=MobFlight.posed(living);
            var state=STATES.get(living);
            if(state==null && !flying)continue;
            if(state==null) { state=new FlightPoseMotion();STATES.put(living,state); }
            state.tick(flying,living.getX()-living.xo,living.getZ()-living.zo);
            if(state.finished())STATES.remove(living);
        }
    }
    public static float weight(LivingEntity entity) {
        var state=STATES.get(entity);
        return state==null?0:state.weight(partial());
    }
    public static float bank(LivingEntity entity) {
        var state=STATES.get(entity);
        return state==null?0:state.bank(partial());
    }
    private static float partial() { return Minecraft.getInstance().getFrameTime(); }
    public static float blend(float original,float target,float weight) { return original+(target-original)*weight; }

    /** 角度均为弧度，只转动原有关节，不移动肩膀；每次从本帧动画混合，起落时保留连续性。 */
    public static void rotate(ModelPart part,float pitch,float roll,float weight) {
        part.xRot=blend(part.xRot,pitch,weight);part.yRot*=1-weight;
        part.zRot=blend(part.zRot,roll,weight);
    }

    /** 侧滚先绕水平前向轴，再俯身；反过来会把倾斜变成转向。四足只侧滚，保留横向躯干。 */
    public static void body(PoseStack pose,LivingEntity entity,boolean humanoid) {
        float weight=weight(entity);
        pose.mulPose(Axis.ZP.rotationDegrees(-bank(entity)));
        if(humanoid) {
            pose.mulPose(Axis.XP.rotationDegrees(-90*weight));
            pose.translate(0,-0.7*weight,0);
        }
    }
}
