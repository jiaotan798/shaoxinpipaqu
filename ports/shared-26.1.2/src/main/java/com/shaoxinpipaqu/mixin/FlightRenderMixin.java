package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shaoxinpipaqu.client.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 提交阶段应用整体倾斜，并保留当前骨架直到装备和持物层读取完毕。 */
@Mixin(LivingEntityRenderer.class)
abstract class FlightRenderMixin {
    @Shadow protected EntityModel<?> model;
    @Unique private AnimalFlightPose pipa$layerPose;

    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void pipa$extract(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci) {
        ((FlightRenderState)state).pipa$pose(FlightVisuals.weight(entity),FlightVisuals.bank(entity));
    }

    @Inject(method="setupRotations",at=@At("TAIL"))
    private void pipa$body(LivingEntityRenderState state,PoseStack pose,float yaw,float scale,CallbackInfo ci) {
        FlightRenderState flight=(FlightRenderState)state;
        float weight=flight.pipa$weight();
        if(weight<=0)return;
        pose.mulPose(Axis.ZP.rotationDegrees(-flight.pipa$bank()));
        if(AnimalFlightPose.humanoid(model)) {
            pose.mulPose(Axis.XP.rotationDegrees(-90*weight));
            pose.translate(0,-0.7*weight,0);
        }
    }

    @WrapMethod(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    private void pipa$submit(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,Operation<Void> original) {
        var previous=pipa$layerPose;
        pipa$layerPose=null;
        try {
            original.call(state,pose,collector,camera);
        } finally {
            if(pipa$layerPose!=null)pipa$layerPose.close();
            pipa$layerPose=previous;
        }
    }

    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V",shift=At.Shift.AFTER))
    private void pipa$layers(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) {
        float weight=((FlightRenderState)state).pipa$weight();
        if(weight>0)pipa$layerPose=new AnimalFlightPose(model,weight);
    }
}
