package com.shaoxinpipaqu.mixin;

import com.elfmcys.yesstevemodel.*;
import com.shaoxinpipaqu.compat.YsmFlightPose;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** YSM 玩家和女仆共用的完整渲染作用域；动画之后接管，全部图层之后恢复共享骨骼。 */
@Pseudo
@Mixin(targets="com.elfmcys.yesstevemodel.OO0OOoo0ooooOoO0O0o00Ooo",remap=false)
public abstract class YsmFlightMixin {
    @Unique private final ThreadLocal<YsmFlightPose[]> pipa$scope=new ThreadLocal<>();

    @WrapMethod(method="oOo0OO0O0o000OO0O000oo0o(Lcom/elfmcys/yesstevemodel/O0OOoooOOoOo0O00O0oOoo0O;Lnet/minecraft/resources/ResourceLocation;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void pipa$render(O0OOoooOOoOo0O00O0oOoo0O<?> actor,ResourceLocation texture,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,Operation<Void> original) {
        var previous=pipa$scope.get();var current=new YsmFlightPose[1];pipa$scope.set(current);
        try { original.call(actor,texture,yaw,partial,pose,buffers,light); }
        finally {
            if(current[0]!=null) current[0].close();
            if(previous==null) pipa$scope.remove(); else pipa$scope.set(previous);
        }
    }

    @WrapOperation(method="oOo0OO0O0o000OO0O000oo0o(Lcom/elfmcys/yesstevemodel/O0OOoooOOoOo0O00O0oOoo0O;Lnet/minecraft/resources/ResourceLocation;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lcom/elfmcys/yesstevemodel/O0OOoooOOoOo0O00O0oOoo0O;O00OOOo00Oo0OO0000oOo0oo()Lcom/elfmcys/yesstevemodel/o0ooO0ooO00oo0o00Oo00000;"))
    private o0ooO0ooO00oo0o00Oo00000 pipa$animate(O0OOoooOOoOo0O00O0oOoo0O<?> actor,Operation<o0ooO0ooO00oo0o00Oo00000> original,O0OOoooOOoOo0O00O0oOoo0O<?> argument,ResourceLocation texture,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var skeleton=original.call(actor);
        // 此 getter 位于动画更新、朝向计算之后，身体和手持层之前；提前写入会被 YSM 动画覆盖。
        var scope=pipa$scope.get();
        if(scope!=null) scope[0]=YsmFlightPose.apply(skeleton,actor,pose);
        return skeleton;
    }
}
