package com.shaoxinpipaqu.qa.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** 在原方法及生产注入的外层先测量手掌；正常结束或异常时都清理期望值。 */
@Mixin(CarriedBlockLayer.class)
public abstract class CarriedBlockCaptureMixin {
    @WrapMethod(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/monster/EnderMan;FFFFFF)V")
    private void probe$hand(PoseStack pose,MultiBufferSource buffers,int light,EnderMan entity,float limb,float amount,float partial,float age,float yaw,float pitch,Operation<Void> original) {
        try {com.shaoxinpipaqu.qa.CarriedBlockProbe.layer(entity,((CarriedBlockLayer)(Object)this).getParentModel(),pose);original.call(pose,buffers,light,entity,limb,amount,partial,age,yaw,pitch);}
        finally{com.shaoxinpipaqu.qa.CarriedBlockProbe.clear();}
    }
}
