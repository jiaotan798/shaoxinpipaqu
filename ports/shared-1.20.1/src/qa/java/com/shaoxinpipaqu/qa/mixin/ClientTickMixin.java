package com.shaoxinpipaqu.qa.mixin;

import com.shaoxinpipaqu.qa.PortClientCheck;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 保留正常输入调度，结束每个客户端 tick 后设置下一刻的游戏按键状态。 */
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void probe$tick(CallbackInfo ci) { PortClientCheck.clientTick((Minecraft) (Object) this); }
}
