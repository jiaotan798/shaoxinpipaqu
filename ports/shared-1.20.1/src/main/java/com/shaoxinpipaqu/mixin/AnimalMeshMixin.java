package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.AnimalFlightPose;
import net.minecraft.client.model.*;
import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** 独立护甲、猫项圈、羊驼装饰也会重跑动画；在每次真实网格绘制期间覆盖，随后恢复。 */
@Mixin({AgeableListModel.class,HierarchicalModel.class,LlamaModel.class})
public abstract class AnimalMeshMixin {
    @WrapMethod(method="renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V")
    private void pipa$mesh(PoseStack pose,VertexConsumer vertices,int light,int overlay,float red,float green,float blue,float alpha,Operation<Void> original) {
        var entity=AnimalFlightPose.entity();
        // 普通生物和非飞行帧不创建姿势快照，避免给所有Ageable/Hierarchical模型增加逐帧分配。
        if(entity==null||com.shaoxinpipaqu.client.FlightVisuals.weight(entity)<=0||!((Object)this instanceof com.shaoxinpipaqu.client.AnimalModelRoot||(Object)this instanceof PandaModel<?>||(Object)this instanceof PolarBearModel<?>||(Object)this instanceof TurtleModel<?>||(Object)this instanceof StriderModel<?>)) {original.call(pose,vertices,light,overlay,red,green,blue,alpha);return;}
        try(var scope=new AnimalFlightPose((EntityModel<?>)(Object)this,entity)) {original.call(pose,vertices,light,overlay,red,green,blue,alpha);}
    }
}
