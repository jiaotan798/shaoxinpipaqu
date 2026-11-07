package com.shaoxinpipaqu.mixin;

import com.friendswine.*;
import com.friendswine.client.PartyGuestRenderer;
import com.shaoxinpipaqu.MobFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** 立板只停缩放；GIF 帧和面向摄像机逻辑继续使用前置。 */
@Mixin(value=PartyGuestRenderer.class,remap=false)
public abstract class GuestRenderMixin {
    @Redirect(method="render",at=@At(value="INVOKE",target="Lcom/friendswine/GuestAnimation;scale(DDD)F"))
    private float pipa$scale(double sec,double min,double max,PartyGuestEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) { return MobFlight.posed(entity)?1:GuestAnimation.scale(sec,min,max); }
}
