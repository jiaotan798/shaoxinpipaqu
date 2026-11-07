package com.shaoxinpipaqu.qa.mixin;

import com.shaoxinpipaqu.client.MemeEffects;
import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 卡点前后的同帧 GPU 缓冲，不关云、不改变相机或游戏时钟。 */
@Mixin(value=MemeEffects.class,remap=false)
abstract class CloudCaptureMixin {
    @Inject(method="render",at=@At("HEAD"))
    private static void probe$before(CameraRenderState camera,float partial,CallbackInfo ci) {
        PortClientCheck.INSTANCE.beforeCards(partial);
    }

    @Inject(method="render",at=@At("RETURN"))
    private static void probe$after(CameraRenderState camera,float partial,CallbackInfo ci) {
        PortClientCheck.INSTANCE.afterCards();
    }
}
