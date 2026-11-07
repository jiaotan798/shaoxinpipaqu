package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 羊毛、鞍具等独立图层会重新执行 setupAnim；在共享四足动画入口覆盖，避免只改变主模型而让外层四肢错位。 */
@Mixin(QuadrupedModel.class)
public abstract class QuadrupedFlightMixin {
    @Inject(method="setupAnim",at=@At("RETURN"))
    private void pipa$pose(Entity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        if(!(entity instanceof LivingEntity living))return;
        float weight=FlightVisuals.weight(living);if(weight<=0)return;
        if((Object)this instanceof net.minecraft.client.model.PandaModel<?>||(Object)this instanceof net.minecraft.client.model.PolarBearModel<?>||(Object)this instanceof net.minecraft.client.model.TurtleModel<?>)return; // 子类还会覆盖腿角，改在最终网格绘制时处理，避免重复插值。
        var parts=(QuadrupedAccessor)this;
        // 原版腿沿局部+Y向下，绕X轴转±90°才与躯干平行；前腿向前、后腿向后，羊毛独立模型也走此入口。
        parts.pipa$frontRight().xRot=FlightVisuals.blend(parts.pipa$frontRight().xRot,-(float)Math.PI/2,weight);
        parts.pipa$frontLeft().xRot=FlightVisuals.blend(parts.pipa$frontLeft().xRot,-(float)Math.PI/2,weight);
        // 前伸后绕局部Y轴各外展30°，右腿为正、左腿为负；羊毛和鞍具沿用同一权重。
        parts.pipa$frontRight().yRot=FlightVisuals.blend(parts.pipa$frontRight().yRot,(float)Math.PI/6,weight);
        parts.pipa$frontLeft().yRot=FlightVisuals.blend(parts.pipa$frontLeft().yRot,-(float)Math.PI/6,weight);
        parts.pipa$hindRight().xRot=FlightVisuals.blend(parts.pipa$hindRight().xRot,(float)Math.PI/2,weight);
        parts.pipa$hindLeft().xRot=FlightVisuals.blend(parts.pipa$hindLeft().xRot,(float)Math.PI/2,weight);
    }
}
