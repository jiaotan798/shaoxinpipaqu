package com.shaoxinpipaqu.mixin;

import com.friendswine.*;
import com.friendswine.client.PartyGuestRenderer;
import com.shaoxinpipaqu.MobFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.*;

/** 立板只停缩放；GIF 帧和面向摄像机逻辑继续使用前置。 */
@Mixin(value=PartyGuestRenderer.class,remap=false)
public abstract class GuestRenderMixin {
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lcom/friendswine/GuestAnimation;scale(DDD)F"))
    private float pipa$scale(double sec,double min,double max,Operation<Float> original,PartyGuestEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) { return MobFlight.posed(entity)?1:original.call(sec,min,max); }
}
