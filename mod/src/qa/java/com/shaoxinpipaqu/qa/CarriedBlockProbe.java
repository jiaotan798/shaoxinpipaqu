package com.shaoxinpipaqu.qa;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.world.entity.monster.EnderMan;
import org.joml.Vector3f;
import java.util.*;

/** 对实际方块绘制矩阵检查掌心接触与尺寸，并保留旧胸前锚点的反例，避免只检查手臂角度。 */
public final class CarriedBlockProbe {
    private record Anchor(Vector3f palm,Vector3f size,float oldError){}
    private static final ThreadLocal<Anchor> EXPECTED=new ThreadLocal<>();
    static final Map<Integer,Integer> samples=new HashMap<>();
    public static void clear(){EXPECTED.remove();}
    public static void layer(EnderMan entity,EndermanModel<?> model,PoseStack pose) {
        if(PoseProbe.stage!=91||!com.shaoxinpipaqu.MobFlight.posed(entity)||com.shaoxinpipaqu.client.FlightVisuals.weight(entity)<1)return;
        var arm=model.rightArm;
        var cube=((com.shaoxinpipaqu.mixin.ModelPartAccessor)(Object)arm).pipa$cubes().stream().max(Comparator.comparingDouble(c->c.maxY)).orElseThrow();
        var hand=new PoseStack();hand.last().pose().set(pose.last().pose());arm.translateAndRotate(hand);
        var palm=hand.last().pose().transformPosition(new Vector3f((cube.minX+cube.maxX)/32,cube.maxY/16,(cube.minZ+cube.maxZ)/32));
        var old=new PoseStack();old.last().pose().set(pose.last().pose());old.translate(0,0.6875,-0.75);
        old.mulPose(Axis.XP.rotationDegrees(20));old.mulPose(Axis.YP.rotationDegrees(45));old.translate(0.25,0.1875,0.25);old.scale(-0.5F,-0.5F,0.5F);old.mulPose(Axis.YP.rotationDegrees(90));
        float oldError=old.last().pose().transformPosition(new Vector3f(0.5F,1,0.5F)).distance(palm);
        EXPECTED.set(new Anchor(palm,pose.last().pose().getScale(new Vector3f()).mul(0.5F),oldError));
    }
    public static void block(PoseStack pose) {
        var expected=EXPECTED.get();if(expected==null)return;EXPECTED.remove();
        float error=pose.last().pose().transformPosition(new Vector3f(0.5F,1,0.5F)).distance(expected.palm());
        float sizeError=pose.last().pose().getScale(new Vector3f()).distance(expected.size());
        if(error>0.0001F||sizeError>0.0001F||expected.oldError()<0.1F){PoseProbe.failure="末影人实际持块接触/尺寸或旧锚点反例错误：error="+error+" sizeError="+sizeError+" oldError="+expected.oldError();return;}
        int count=samples.merge(PoseProbe.stage,1,Integer::sum);
        if(count==1)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-carried-block-91.txt","PASS actual BlockRenderDispatcher matrix: palmError="+error+" sizeError="+sizeError+" oldChestError="+expected.oldError()+"\n");
    }
}
