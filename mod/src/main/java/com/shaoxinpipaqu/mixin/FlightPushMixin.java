package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.MobFlight;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 实体互推发生在原版移动阶段；飞行控制必须同时拦截双向推力，避免悬停时挤走玩家。 */
@Mixin(Entity.class)
public abstract class FlightPushMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void pipa$noPush(Entity other,CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        if (self instanceof Mob a && (MobFlight.controlled(a) || MobFlight.posed(a))
                || other instanceof Mob b && (MobFlight.controlled(b) || MobFlight.posed(b))) ci.cancel();
    }
}
