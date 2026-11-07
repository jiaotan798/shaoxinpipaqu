package com.shaoxinpipaqu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.friendswine.PartyGuestEntity;
import com.friendswine.client.PartyGuestRenderer;
import com.shaoxinpipaqu.MobFlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 提取阶段只停立板缩放；GIF 帧和朝向仍由前置计算。 */
@Mixin(value=PartyGuestRenderer.class,remap=false)
abstract class GuestRenderMixin {
    @WrapOperation(method="extractRenderState(Lcom/friendswine/PartyGuestEntity;Lcom/friendswine/client/PartyGuestRenderer$State;F)V",at=@At(value="INVOKE",target="Lcom/friendswine/GuestAnimation;scale(DDD)F"),remap=false)
    private float pipa$scale(double sec,double min,double max,Operation<Float> original,PartyGuestEntity entity,PartyGuestRenderer.State state,float partial) {
        return MobFlight.posed(entity)?1:original.call(sec,min,max);
    }
}
