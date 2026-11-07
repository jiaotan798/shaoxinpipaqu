package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 只捕获五参数真实绘制入口，避免其七参数委托重复计数。 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockDrawCaptureMixin {
    @Inject(method="renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",at=@At("HEAD"))
    private void probe$block(BlockState block,PoseStack pose,MultiBufferSource buffers,int light,int overlay,CallbackInfo ci){com.shaoxinpipaqu.qa.CarriedBlockProbe.block(pose);}
}
