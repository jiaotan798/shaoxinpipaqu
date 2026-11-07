package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.EnderMan;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 双臂外展后两掌间距大于原方块；飞行时改由右手持块，保持块大小、肩位置与肢体比例。 */
@Mixin(CarriedBlockLayer.class)
public abstract class CarriedBlockMixin {
    @Shadow @Final private BlockRenderDispatcher blockRenderer;
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/monster/EnderMan;FFFFFF)V",at=@At("HEAD"),cancellable=true)
    private void pipa$hand(PoseStack pose,MultiBufferSource buffers,int light,EnderMan entity,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci) {
        float weight=FlightVisuals.weight(entity);var block=entity.getCarriedBlock();
        if(weight<=0||block==null)return;
        var arm=((CarriedBlockLayer)(Object)this).getParentModel().rightArm;
        net.minecraft.client.model.geom.ModelPart.Cube hand=null;
        for(var cube:((ModelPartAccessor)(Object)arm).pipa$cubes())if(hand==null||cube.maxY>hand.maxY)hand=cube;
        if(hand==null)return; // 无可测手臂网格的第三方模型保留原图层，不猜测其手掌位置。

        // 原版站姿图层的局部变换；作为起落过渡端点，不改变普通站姿。
        var chest=new PoseStack();chest.translate(0,0.6875,-0.75);
        chest.mulPose(Axis.XP.rotationDegrees(20));chest.mulPose(Axis.YP.rotationDegrees(45));
        chest.translate(0.25,0.1875,0.25);chest.scale(-0.5F,-0.5F,0.5F);chest.mulPose(Axis.YP.rotationDegrees(90));
        var held=new PoseStack();arm.translateAndRotate(held);
        // 模型单位是像素；以实际网格最远端为掌心，将方块的一面中心贴到手端，保留原版0.5倍方块。
        held.translate((hand.minX+hand.maxX)/32.0,hand.maxY/16.0,(hand.minZ+hand.maxZ)/32.0);
        held.scale(-0.5F,-0.5F,0.5F);held.translate(-0.5,-1,-0.5);
        var position=chest.last().pose().getTranslation(new Vector3f()).lerp(held.last().pose().getTranslation(new Vector3f()),weight);
        // 四元数插值避免直接混合旋转矩阵时在过渡中把方块压扁。
        var rotation=chest.last().pose().getUnnormalizedRotation(new Quaternionf()).slerp(held.last().pose().getUnnormalizedRotation(new Quaternionf()),weight);
        pose.pushPose();
        try {
            pose.translate(position.x,position.y,position.z);pose.mulPose(rotation);pose.scale(0.5F,0.5F,0.5F);
            blockRenderer.renderSingleBlock(block,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        }finally{pose.popPose();}
        ci.cancel();
    }
}
