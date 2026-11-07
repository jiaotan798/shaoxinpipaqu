package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shaoxinpipaqu.client.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 渲染队列会重新执行原版动画；本体与独立装备网格在真正绘制前再次叠加姿势。 */
@Mixin(ModelFeatureRenderer.class)
abstract class FlightModelRenderMixin {
    @Unique private AnimalFlightPose pipa$drawPose;

    @WrapMethod(method="renderModel")
    private void pipa$draw(SubmitNodeStorage.ModelSubmit<?> submit,RenderType type,VertexConsumer buffer,OutlineBufferSource outline,MultiBufferSource.BufferSource crumbling,Operation<Void> original) {
        var previous=pipa$drawPose;
        pipa$drawPose=null;
        try {
            original.call(submit,type,buffer,outline,crumbling);
        } finally {
            if(pipa$drawPose!=null)pipa$drawPose.close();
            pipa$drawPose=previous;
        }
    }

    @Inject(method="renderModel",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V",shift=At.Shift.AFTER))
    private void pipa$pose(SubmitNodeStorage.ModelSubmit<?> submit,RenderType type,VertexConsumer buffer,OutlineBufferSource outline,MultiBufferSource.BufferSource crumbling,CallbackInfo ci) {
        if(submit.state() instanceof FlightRenderState flight && flight.pipa$weight()>0 && submit.model() instanceof EntityModel<?> model)
            pipa$drawPose=new AnimalFlightPose(model,flight.pipa$weight());
    }
}
