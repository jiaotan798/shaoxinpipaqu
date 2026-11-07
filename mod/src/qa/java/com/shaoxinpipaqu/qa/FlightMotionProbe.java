package com.shaoxinpipaqu.qa;

import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** 观察真实客户端的同步起落和转弯，防止只测纯数学却没有接入实际实体。 */
final class FlightMotionProbe {
    private final StringBuilder rows=new StringBuilder("stage,id,tick,flying,weight,bank\n");
    private int takeoff,landing;
    private float maxBank;
    void sample(Minecraft mc,int stage) {
        for(var entity:mc.level.entitiesForRendering()) {
            if(!(entity instanceof LivingEntity living))continue;
            float weight=FlightVisuals.weight(living),bank=FlightVisuals.bank(living);
            MultiplayerCheck.require(Float.isFinite(weight)&&Float.isFinite(bank)&&weight>=0&&weight<=1&&Math.abs(bank)<=25,"实际姿态数值有界");
            if(weight<=0)continue;
            boolean flying=MobFlight.posed(living);
            if(weight<1) {if(flying)takeoff++;else landing++;}
            maxBank=Math.max(maxBank,Math.abs(bank));
            rows.append(stage).append(',').append(entity.getId()).append(',').append(entity.tickCount).append(',').append(flying).append(',').append(weight).append(',').append(bank).append('\n');
        }
    }
    void finish(String role) {
        MultiplayerCheck.write(role+"-flight-motion.csv",rows.toString());
        MultiplayerCheck.write(role+"-flight-motion.txt","takeoffSamples="+takeoff+" landingSamples="+landing+" maxBankDegrees="+maxBank);
        // 模型专项按场景直接移除实体，不包含落地；完整回归及兼容专项仍必须观察到收姿。
        boolean modelOnly=Boolean.getBoolean("pipa.qa.modelOnly");
        MultiplayerCheck.require(takeoff>0&&(modelOnly||landing>0)&&maxBank>0.1F,"所选场景应观察到起飞过渡、转弯倾斜及适用的落地过渡");
    }
}
