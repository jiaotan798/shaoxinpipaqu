package com.shaoxinpipaqu.qa.mixin;
import com.shaoxinpipaqu.qa.PoseProbe;

import com.elfmcys.yesstevemodel.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.elfmcys.yesstevemodel.ooo0000oO0O0ooOO0ooOO0o0",priority=2000,remap=false)
public interface YsmCaptureMixin {
    @WrapMethod(method="oOo0OO0O0o000OO0O000oo0o(Lcom/elfmcys/yesstevemodel/o0ooO0ooO00oo0o00Oo00000;Lcom/elfmcys/yesstevemodel/OoO0oo0o0o0oOoo0oOOO0Ooo;FLnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V")
    default void probe$scope(o0ooO0ooO00oo0o00Oo00000 skeleton,OoO0oo0o0o0oOoo0oOOO0Ooo<?> actor,float partial,RenderType type,PoseStack pose,MultiBufferSource buffers,int pass,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float a,Operation<Void> original) {
        var previous=PoseProbe.ENTITY.get();float[] data=skeleton.oOo0OO0O0o000OO0O000oo0o(),before=data.clone();
        if(actor.ooo00OoO00OOOO0oOooOo0Oo() instanceof LivingEntity e)PoseProbe.ENTITY.set(e);
        try {original.call(skeleton,actor,partial,type,pose,buffers,pass,vertices,light,overlay,r,g,b,a);}
        finally {if(!java.util.Arrays.equals(before,data))PoseProbe.failure="YSM绘制退出没有恢复骨骼缓冲";if(previous==null)PoseProbe.ENTITY.remove();else PoseProbe.ENTITY.set(previous);}
    }
}
