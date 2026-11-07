package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.MobFlight;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 临时暂停飞行个体 AI，不改 NoAI 存档位，落地自动交回原生控制。 */
@Mixin(Mob.class)
public abstract class MobAiMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void pipa$pause(CallbackInfo ci) { if (MobFlight.pauseAi((Mob)(Object)this)) ci.cancel(); }
}
