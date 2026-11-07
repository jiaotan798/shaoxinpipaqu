package com.shaoxinpipaqu.mixin;

import com.shaoxinpipaqu.client.FlightVisuals;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** 原村民双臂为折叠网格；飞行时用独立肩关节手臂实现双臂前伸外展，并抬头面朝前方。 */
@Mixin(VillagerModel.class)
public abstract class VillagerFlightMixin {
    @Shadow @Final private ModelPart root;
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
    @Inject(method="setupAnim",at=@At("TAIL"))
    private void pipa$pose(Entity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci) {
        // 第三方若提供不同 root，保留其模型，另列兼容缺口，不能硬取不存在的子骨骼。
        if (!root.hasChild("pipa_right_arm") || !root.hasChild("pipa_left_arm") || !root.hasChild("arms")) return;
        float weight=FlightVisuals.weight((net.minecraft.world.entity.LivingEntity)entity);
        boolean fly=weight>0;
        root.getChild("arms").visible=!fly;
        var right=root.getChild("pipa_right_arm"); var left=root.getChild("pipa_left_arm");
        right.visible=left.visible=fly;
        if (fly) {
            // 独立臂从原折叠臂的旋转起步；网格形状无法靠骨骼插值变形。
            var folded=root.getChild("arms");
            right.xRot=left.xRot=folded.xRot;right.yRot=left.yRot=folded.yRot;right.zRot=left.zRot=folded.zRot;
            FlightVisuals.rotate(right,-(float)Math.PI,-(float)Math.PI/6,weight);
            FlightVisuals.rotate(left,-(float)Math.PI,(float)Math.PI/6,weight);
            FlightVisuals.rotate(root.getChild("head"),-(float)Math.PI/2,0,weight);
            root.getChild("right_leg").xRot*=1-weight;root.getChild("left_leg").xRot*=1-weight;
        }
    }
}
