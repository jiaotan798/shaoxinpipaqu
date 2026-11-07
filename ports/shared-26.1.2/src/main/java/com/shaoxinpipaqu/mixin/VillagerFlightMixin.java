package com.shaoxinpipaqu.mixin;

import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** 在村民烘焙网格中加入飞行用肩关节，实际可见性由延迟姿势阶段切换。 */
@Mixin(VillagerModel.class)
public abstract class VillagerFlightMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void pipa$hideArms(ModelPart root,CallbackInfo ci) {
        // 两套网格共享渲染器；初始化时隐藏飞行臂，结束每次绘制后还原这个状态。
        if(root.hasChild("pipa_right_arm"))root.getChild("pipa_right_arm").visible=false;
        if(root.hasChild("pipa_left_arm"))root.getChild("pipa_left_arm").visible=false;
    }
    /** 袖段与手部共用肩关节，但分别使用原版8像素袖子与折叠手部纹理，不能把袖子UV直接拉长到12像素。 */
    @Inject(method="createBodyModel",at=@At("RETURN"))
    private static void pipa$arms(CallbackInfoReturnable<MeshDefinition> cir) {
        var root=cir.getReturnValue().getRoot();
        // 两段在Y=6处相接，总长仍为12；侧面UV不再进入原贴图V=34～37的透明带。
        root.addOrReplaceChild("pipa_right_arm",CubeListBuilder.create()
                .texOffs(44,22).addBox(-2,-2,-2,4,8,4)
                .texOffs(40,38).addBox(-2,6,-2,4,4,4),PartPose.offset(-6,3,-1));
        root.addOrReplaceChild("pipa_left_arm",CubeListBuilder.create().mirror()
                .texOffs(44,22).addBox(-2,-2,-2,4,8,4)
                .texOffs(40,38).addBox(-2,6,-2,4,4,4),PartPose.offset(6,3,-1));
    }
}

/** 26.1.2 幼年村民拥有独立网格，肩关节和 UV 对齐其原版的 2×4×2 袖段。 */
@Mixin(BabyVillagerModel.class)
abstract class BabyVillagerFlightMixin {
    @Inject(method="createBodyModel",at=@At("RETURN"))
    private static void pipa$arms(CallbackInfoReturnable<MeshDefinition> cir) {
        var root=cir.getReturnValue().getRoot();
        root.addOrReplaceChild("pipa_right_arm",CubeListBuilder.create().texOffs(36,15).addBox(-1,-1,-1,2,4,2),
                PartPose.offset(-3,17.5F,0));
        root.addOrReplaceChild("pipa_left_arm",CubeListBuilder.create().texOffs(16,15).addBox(-1,-1,-1,2,4,2),
                PartPose.offset(3,17.5F,0));
    }
}
