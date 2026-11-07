package com.shaoxinpipaqu.mixin;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.shaoxinpipaqu.compat.MaidFlightPose;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 只适配女仆 1.5.3 内置 Gecko 引擎；动画完成后覆盖真实骨骼，外层女仆作用域负责所有图层结束后的恢复。 */
@Pseudo
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer",remap=false)
public abstract class MaidGeckoMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/AnimatableEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/AnimatableEntity;setCustomAnimations(Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/molang/context/AnimationContext;Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/event/predicate/AnimationEvent;)Z",shift=At.Shift.AFTER))
    private void pipa$pose(LivingEntity entity,AnimatableEntity<?> animated,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        if(MaidFlightPose.applyGecko(animated.getCurrentModel(),entity)) {
            FlightVisuals.body(pose,entity,true);
        }
    }
}
