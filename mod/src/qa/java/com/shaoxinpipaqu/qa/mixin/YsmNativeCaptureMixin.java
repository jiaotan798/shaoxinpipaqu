package com.shaoxinpipaqu.qa.mixin;
import com.shaoxinpipaqu.qa.PoseProbe;

import com.elfmcys.yesstevemodel.O0OOo0OoOoo0o0o0oO000o00;
import com.mojang.blaze3d.vertex.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.elfmcys.yesstevemodel.OoooOoo0o0000O0o00o0O0Oo",remap=false)
public abstract class YsmNativeCaptureMixin {
    @Inject(method="oOo0OO0O0o000OO0O000oo0o(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/elfmcys/yesstevemodel/O0OOo0OoOoo0o0o0oO000o00;[F[FIIIIFFFF)V",at=@At("HEAD"))
    private static void probe$native(VertexConsumer vertices,PoseStack.Pose pose,O0OOo0OoOoo0o0o0oO000o00 definition,float[] data,float[] flags,int pass,int unused,int light,int overlay,float r,float g,float b,float a,CallbackInfo ci) {PoseProbe.ysm(definition,data);}
}
