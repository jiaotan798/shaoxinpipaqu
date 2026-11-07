package com.shaoxinpipaqu.qa.mixin;
import com.shaoxinpipaqu.qa.PoseProbe;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer",remap=false)
public abstract class MaidCaptureMixin {
    @WrapMethod(method="render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void probe$scope(Mob e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,Operation<Void> original) {
        var previous=PoseProbe.ENTITY.get();PoseProbe.ENTITY.set(e);
        try {original.call(e,yaw,partial,pose,buffers,light);}finally{if(previous==null)PoseProbe.ENTITY.remove();else PoseProbe.ENTITY.set(previous);}
    }
}
