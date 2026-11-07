package com.shaoxinpipaqu.qa.mixin;

import com.friendswine.*;
import com.friendswine.client.PartyGuestRenderer;
import com.shaoxinpipaqu.qa.PoseProbe;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

/** 观察实际立板渲染参数，不更改帧号或缩放；只打入验收 JAR。 */
@Mixin(value=PartyGuestRenderer.class,remap=false)
public abstract class GuestCaptureMixin {
    @Unique private int probe$frame;
    @Redirect(method="render(Lcom/friendswine/PartyGuestEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lcom/friendswine/GuestAnimation;frame(Lcom/friendswine/GuestVariant;D)I"))
    private int probe$frame(GuestVariant variant,double seconds) {return probe$frame=GuestAnimation.frame(variant,seconds);}
    // 第一次缩放用于立板本身，后一次缩放用于角色携带的玩偶。
    @Redirect(method="render(Lcom/friendswine/PartyGuestEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",ordinal=0))
    private void probe$scale(PoseStack pose,float x,float y,float z,PartyGuestEntity e,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light) {
        PoseProbe.guest(e,x,y,z,probe$frame);pose.scale(x,y,z);
    }
}
