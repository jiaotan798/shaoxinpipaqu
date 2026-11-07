package com.shaoxinpipaqu.qa.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class BackgroundMouseMixin {
    /** 按键由游戏内状态驱动，验收客户端禁止捕获或移动桌面鼠标。 */
    @Inject(method="grabMouse",at=@At("HEAD"),cancellable=true)
    private void probe$noCapture(CallbackInfo ci) { ci.cancel(); }
}
