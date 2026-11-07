package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.PeaceAura;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
abstract class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void pipa$target(LivingEntity target, CallbackInfo ci) {
        if (PeaceAura.cancelTarget((Mob)(Object)this, target)) ci.cancel();
    }
}
