package com.shaoxinpipaqu.qa.mixin;
import com.shaoxinpipaqu.qa.PoseProbe;

import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.github.tartaricacid.simplebedrockmodel.client.bedrock.AbstractBedrockEntityModel",remap=false)
public abstract class MaidNativeCaptureMixin {
    @Inject(method="renderToBuffer",at=@At("HEAD"))
    private void probe$native(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color,CallbackInfo ci) {PoseProbe.maid(this);}
}
