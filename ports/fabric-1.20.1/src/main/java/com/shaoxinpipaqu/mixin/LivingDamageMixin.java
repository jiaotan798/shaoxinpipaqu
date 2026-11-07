package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.PeaceAura;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 同步拦截生物或其弹射物伤害，伤害来源例外由共用逻辑处理。 */
@Mixin(LivingEntity.class)
public abstract class LivingDamageMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void pipa$hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (PeaceAura.cancelDamage(source, (LivingEntity)(Object)this)) cir.setReturnValue(false);
    }
}
