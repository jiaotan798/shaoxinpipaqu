package com.shaoxinpipaqu.qa.mixin;

import com.shaoxinpipaqu.qa.PoseProbe;
import net.minecraft.client.model.*;
import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 原方法HEAD位于生产WrapMethod作用域内，此时读到真正送往网格的临时姿势。 */
@Mixin({AgeableListModel.class,HierarchicalModel.class,LlamaModel.class,CamelModel.class})
public abstract class AnimalCaptureMixin {
    @Inject(method="renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",at=@At("HEAD"))
    private void probe$animal(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color,CallbackInfo ci){PoseProbe.animalDraw(this);}
}
