package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.RemoteControl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 手持遥控器左键只用于切歌，包括对空气时也不播放攻击挥手。 */
@Mixin(Minecraft.class)
public abstract class ClientAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void pipa$skipAttack(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player != null && RemoteControl.held(player)) cir.setReturnValue(false);
    }
}
