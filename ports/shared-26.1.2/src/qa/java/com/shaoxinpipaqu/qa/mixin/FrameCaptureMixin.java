package com.shaoxinpipaqu.qa.mixin;

import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class FrameCaptureMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void probe$frame(DeltaTracker delta,boolean advance,CallbackInfo ci) {
        PortClientCheck.INSTANCE.frame(delta.getGameTimeDeltaPartialTick(false));
    }
}
