package com.shaoxinpipaqu.mixin;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.shaoxinpipaqu.compat.MaidFlightPose;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel",remap=false)
public abstract class MaidModelMixin {
    // 女仆动画分支提前 return，不能用仅命中最后返回的 TAIL，否则只会覆盖椅子分支。
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("RETURN"))
    private void pipa$pose(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        MaidFlightPose.apply((BedrockModel<?>)(Object)this,entity);
    }
}
