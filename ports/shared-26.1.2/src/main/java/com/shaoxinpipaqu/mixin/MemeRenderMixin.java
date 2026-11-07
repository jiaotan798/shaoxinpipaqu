package com.shaoxinpipaqu.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.shaoxinpipaqu.client.MemeEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class MemeRenderMixin {
    // 世界帧图完成后再画卡点；此时云层已合成，世界投影和视图矩阵还未恢复。
    @Inject(method="renderLevel",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",shift=At.Shift.AFTER))
    private void pipa$cards(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outline,CameraRenderState camera,
                            Matrix4fc view,GpuBufferSlice fog,Vector4f fogColor,boolean sky,ChunkSectionsToRender chunks,CallbackInfo ci) {
        MemeEffects.render(camera,delta.getGameTimeDeltaPartialTick(false));
    }
}
