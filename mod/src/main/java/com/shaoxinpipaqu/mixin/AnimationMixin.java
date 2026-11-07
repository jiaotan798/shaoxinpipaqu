package com.shaoxinpipaqu.mixin;

import com.friendswine.EntityAnimation;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 飞行姿势优先；不删除酒劲效果，落地后原倒计时继续生效。 */
@Mixin(value=EntityAnimation.class,remap=false)
public abstract class AnimationMixin {
    @Inject(method="forEntity",at=@At("HEAD"),cancellable=true)
    private static void pipa$priority(LivingEntity entity,float partial,CallbackInfoReturnable<EntityAnimation> cir) { if (MobFlight.posed(entity) || FlightVisuals.weight(entity)>0) cir.setReturnValue(null); }
}
