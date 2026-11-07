package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightVisuals;
import com.shaoxinpipaqu.compat.MaidFlightPose;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 女仆原生 Bedrock 路径；YSM 女仆另走最后的 YSM 骨骼绘制入口，避免重复俯身。 */
@Pseudo
@Mixin(targets="com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer",remap=false)
public abstract class MaidRendererMixin {
    @WrapMethod(method="render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void pipa$scope(Mob entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,Operation<Void> original) {
        // 无姿势权重时没有骨骼覆盖，无需建立保存表；保留收姿阶段以及飞行者的嵌套恢复作用域。
        if(FlightVisuals.weight(entity)<=0) { original.call(entity,yaw,partial,pose,buffers,light);return; }
        try(var scope=new MaidFlightPose(entity)) { original.call(entity,yaw,partial,pose,buffers,light); }
    }
    @Inject(method="setupRotations(Lnet/minecraft/world/entity/Mob;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",at=@At("RETURN"))
    private void pipa$body(Mob entity,PoseStack pose,float bob,float yaw,float partial,float scale,CallbackInfo ci) {
        var model=((LivingEntityRenderer<?,?>)(Object)this).getModel();
        if(FlightVisuals.weight(entity)>0 && MaidFlightPose.supports(model))
            FlightVisuals.body(pose,entity,MaidFlightPose.humanoid(model));
    }
}
