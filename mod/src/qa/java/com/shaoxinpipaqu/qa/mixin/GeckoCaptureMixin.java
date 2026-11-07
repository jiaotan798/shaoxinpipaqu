package com.shaoxinpipaqu.qa.mixin;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.shaoxinpipaqu.qa.PoseProbe;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在真正的 Gecko 主模型绘制调用之前观察动画后的骨骼。 */
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer",remap=false)
public abstract class GeckoCaptureMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/AnimatableEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/geo/GeoReplacedEntityRenderer;render(Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/geo/animated/AnimatedGeoModel;Ljava/lang/Object;FLnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void probe$native(LivingEntity e,AnimatableEntity<?> animated,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){PoseProbe.gecko(e,animated.getCurrentModel());}
}
