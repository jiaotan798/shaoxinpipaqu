package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shaoxinpipaqu.client.MemeEffects;
import com.shaoxinpipaqu.client.MemeTiming;
import com.shaoxinpipaqu.qa.BaseMemeClient;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 仅QA包捕获真实绘制入口，不向成品渲染器写入测试状态或伪造绘制。 */
@Mixin(value=MemeEffects.class,remap=false)
public abstract class BaseMemeCaptureMixin {
    @Inject(method="drawMj",at=@At("HEAD"))
    private static void probe$mj(PoseStack pose,MultiBufferSource buffers,int color,CallbackInfo ci) {if(Boolean.getBoolean("pipa.qa.memeOnly"))BaseMemeClient.mj();}
    @Inject(method="drawPicture",at=@At("HEAD"))
    private static void probe$picture(PoseStack pose,MultiBufferSource buffers,MemeTiming.Cue cue,float alpha,CallbackInfo ci) {if(Boolean.getBoolean("pipa.qa.memeOnly"))BaseMemeClient.picture(cue,alpha);}
}
