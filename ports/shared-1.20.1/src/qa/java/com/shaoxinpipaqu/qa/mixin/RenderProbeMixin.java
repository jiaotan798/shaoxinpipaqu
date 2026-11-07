package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 读取真正的世界帧缓冲，位置晚于两个加载器各自的卡点绘制入口。 */
@Mixin(GameRenderer.class)
public abstract class RenderProbeMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void probe$frame(float partial, long end, PoseStack pose, CallbackInfo ci) { PortClientCheck.afterLevel(partial); }
}
