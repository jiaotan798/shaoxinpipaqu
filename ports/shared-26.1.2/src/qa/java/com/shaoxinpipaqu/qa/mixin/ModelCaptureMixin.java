package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shaoxinpipaqu.client.FlightRenderState;
import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在真正绘制的模型队列读取姿态，避免只检查服务器效果标记。 */
@Mixin(ModelFeatureRenderer.class)
abstract class ModelCaptureMixin {
    @Inject(method="renderModel",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",ordinal=0))
    private void probe$model(SubmitNodeStorage.ModelSubmit<?> submit,RenderType type,VertexConsumer consumer,
                             OutlineBufferSource outline,MultiBufferSource.BufferSource crumbling,CallbackInfo ci) {
        if (!(submit.state() instanceof FlightRenderState flight) || flight.pipa$weight()<0.98F || !(submit.model() instanceof PigModel pig)) return;
        var front=pig.root().getChild("right_front_leg");
        var rear=pig.root().getChild("right_hind_leg");
        if (Math.abs(front.xRot+Math.PI/2)<0.12 && Math.abs(front.yRot-Math.PI/6)<0.12 && Math.abs(rear.xRot-Math.PI/2)<0.12)
            PortClientCheck.INSTANCE.modelPose();
    }
}
