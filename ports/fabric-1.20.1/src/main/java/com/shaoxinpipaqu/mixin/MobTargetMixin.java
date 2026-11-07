package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.PeaceAura;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 忘情保护在原版写入目标前生效，不保留出圈后可恢复的旧仇恨。 */
@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void pipa$target(LivingEntity target, CallbackInfo ci) {
        if (PeaceAura.cancelTarget((Mob)(Object)this, target)) ci.cancel();
    }
}
