package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightVisuals;
import com.shaoxinpipaqu.client.AnimalFlightPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 标准人形动画全部完成后抬头、双臂前伸并外展；末影人等子类的原动画不能在后面再次覆盖。 */
@Mixin(LivingEntityRenderer.class)
public abstract class FlightRenderMixin {
    @Shadow protected EntityModel model;
    @Unique private AnimalFlightPose pipa$renderPose;
    /** 嵌套绘制与异常退出都恢复原实体上下文，不能让下一个生物继承当前飞行状态。 */
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void pipa$scope(LivingEntity entity,float yaw,float partial,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var previous=AnimalFlightPose.swap(entity);
        var previousPose=pipa$renderPose;
        pipa$renderPose=null;
        try {
            original.call(entity,yaw,partial,pose,buffers,light);
        } finally {
            if(pipa$renderPose!=null)pipa$renderPose.close();
            pipa$renderPose=previousPose;
            AnimalFlightPose.swap(previous);
        }
    }
    @Inject(method="setupRotations",at=@At("TAIL"))
    private void pipa$body(LivingEntity entity,PoseStack pose,float bob,float yaw,float partial,CallbackInfo ci) {
        float weight=FlightVisuals.weight(entity);
        if (weight<=0) return;
        // YSM 的渲染器也继承 LivingEntityRenderer，但其真正骨架在可选桥接处理；否则身体会被转两次。
        if(getClass().getName().startsWith("com.elfmcys.yesstevemodel.")) return;
        // Gecko 渲染器中的 HumanoidModel 只是占位；真实骨骼与整体姿势交给动画后的女仆专用入口，防止双重俯身。
        if(getClass().getName().equals("com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer"))return;
        // 女仆 Bedrock 的整体变换由专用入口处理，不能在这里叠加侧滚。
        if(getClass().getName().startsWith("com.github.tartaricacid.touhoulittlemaid."))return;
        FlightVisuals.body(pose,entity,model instanceof HumanoidModel || model instanceof VillagerModel || AnimalFlightPose.humanoid(model));
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void pipa$limbs(LivingEntity entity,float yaw,float partial,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,CallbackInfo ci) {
        float weight=FlightVisuals.weight(entity);
        if (weight<=0) return;
        if (model instanceof HumanoidModel h) {
            // 整体俯身后头脸会朝下；头部反向抬90°，双臂各绕原肩关节向外展开30°。
            FlightVisuals.rotate(h.head,-(float)Math.PI/2,0,weight);
            FlightVisuals.rotate(h.rightArm,-(float)Math.PI,-(float)Math.PI/6,weight);
            FlightVisuals.rotate(h.leftArm,-(float)Math.PI,(float)Math.PI/6,weight);
            FlightVisuals.rotate(h.body,0,0,weight);
            FlightVisuals.rotate(h.leftLeg,0,0,weight);FlightVisuals.rotate(h.rightLeg,0,0,weight);
            // 帽层、玩家袖子和裤腿已在原setupAnim里复制过旧姿势，这里须再同步到本次新姿势。
            h.hat.copyFrom(h.head);
            if(h instanceof PlayerModel p) {
                p.rightSleeve.copyFrom(h.rightArm);p.leftSleeve.copyFrom(h.leftArm);
                p.rightPants.copyFrom(h.rightLeg);p.leftPants.copyFrom(h.leftLeg);p.jacket.copyFrom(h.body);
            }
        } else if(AnimalFlightPose.humanoid(model)||model instanceof CamelModel<?>) {
            // 南瓜、持物、裂纹和发光层复用主模型关节，姿势持续到整个实体绘制结束。
            pipa$renderPose=new AnimalFlightPose(model,entity);
        }
    }
}
