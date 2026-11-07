package com.shaoxinpipaqu.qa.mixin;

import com.shaoxinpipaqu.qa.PoseProbe;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 检查羊毛实际使用的独立模型，不能用羊皮肤主模型的姿势替代图层验收。 */
@Mixin(SheepFurLayer.class)
public abstract class SheepFurCaptureMixin {
    @Shadow @Final private SheepFurModel<Sheep> model;
    @Inject(method="render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Sheep;FFFFFF)V",at=@At("TAIL"))
    private void probe$fur(PoseStack pose,MultiBufferSource buffers,int light,Sheep e,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){
        if(!e.isSheared()&&!e.isInvisible())PoseProbe.fur(e,model,((SheepFurLayer)(Object)this).getParentModel());
    }
}
