package com.shaoxinpipaqu.client;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import com.shaoxinpipaqu.mixin.QuadrupedAccessor;
import net.minecraft.world.entity.LivingEntity;
import java.util.IdentityHashMap;
import net.minecraft.client.model.geom.PartPose;

/** 为原版特殊骨架叠加飞行姿势，绘制结束时恢复共享模型。 */
public final class AnimalFlightPose implements AutoCloseable {
    private static final ThreadLocal<LivingEntity> ENTITY=new ThreadLocal<>();
    private final IdentityHashMap<ModelPart,PartPose> saved=new IdentityHashMap<>();
    private ModelPart foldedArms, rightArm, leftArm;
    private boolean foldedVisible, rightVisible, leftVisible;
    public static LivingEntity entity(){return ENTITY.get();}
    public static LivingEntity swap(LivingEntity entity){var old=ENTITY.get();if(entity==null)ENTITY.remove();else ENTITY.set(entity);return old;}

    public static boolean humanoid(EntityModel<?> model) {
        return model instanceof IronGolemModel<?> || model instanceof SnowGolemModel<?>
                || model instanceof IllagerModel<?> || model instanceof WardenModel<?>;
    }

    /** 每次主模型、护甲或装饰绘制单独覆盖并恢复，避免同一帧重复插值和共享模型污染。 */
    public AnimalFlightPose(EntityModel<?> model,LivingEntity entity) {
        float weight=entity==null?0:FlightVisuals.weight(entity);
        if(weight<=0)return;
        if(humanoid(model)) {
            humanoidPose(((HierarchicalModel<?>)model).root(),model,weight);
        } else if(model instanceof StriderModel<?> strider) {
            var root=strider.root();
            // 炽足兽两条腿都向前伸，鞍具也用相同骨架单独绘制。
            joint(root.getChild("right_leg"),-90,30,0,weight);
            joint(root.getChild("left_leg"),-90,-30,0,weight);
            joint(root.getChild("body"),0,0,0,weight);
        } else if(model instanceof AnimalModelRoot || model instanceof CamelModel<?>) {
            var root=model instanceof AnimalModelRoot animal?animal.pipa$animalRoot():((CamelModel<?>)model).root();
            for(String name:new String[]{"right_front_leg","left_front_leg","right_hind_leg","left_hind_leg",
                    "right_front_baby_leg","left_front_baby_leg","right_hind_baby_leg","left_hind_baby_leg"}) {
                if(!root.hasChild(name))continue;
                var leg=root.getChild(name);
                // 马的幼年腿是独立网格；隐藏腿也同步，避免幼年渲染切换后仍沿用站立姿势。
                restPosition(leg,weight);FlightVisuals.rotate(leg,name.contains("front")?-(float)Math.PI/2:(float)Math.PI/2,0,weight);
                // rotate已将原Y角乘以(1-weight)，这里只补目标项，避免起落时重复混合。
                if(name.contains("front"))leg.yRot+=(name.startsWith("right")?1:-1)*(float)Math.PI/6*weight;
            }
            for(String name:new String[]{"body","upper_body","head","head_parts","neck","tail","tail1","tail2"})if(root.hasChild(name)) {
                var part=root.getChild(name);restPosition(part,weight);
                // 猫、狼等躯干初始角为90°，马则为0°；保留初始轴向，尾部也恢复坐姿前的位置以免与躯干脱节。
                FlightVisuals.rotate(part,part.getInitialPose().xRot,part.getInitialPose().zRot,weight);
            }
            if(model instanceof CamelModel<?>) {
                // 骆驼坐姿会同时移动身体和腿；头、尾在身体下，鞍具也随身体回到起飞前的骨架基准。
                var body=root.getChild("body");
                joint(body.getChild("head"),0,0,0,weight);
                joint(body.getChild("tail"),0,0,0,weight);
            }
        } else if(model instanceof PandaModel<?>||model instanceof PolarBearModel<?>||model instanceof TurtleModel<?>) {
            // 这些子类会在父类setupAnim之后覆盖腿部；父类挂钩跳过，统一在子类动画结束后混合一次。
            var p=(QuadrupedAccessor)model;
            restPosition(p.pipa$frontRight(),weight);restPosition(p.pipa$frontLeft(),weight);restPosition(p.pipa$hindRight(),weight);restPosition(p.pipa$hindLeft(),weight);
            if(model instanceof TurtleModel<?>) {
                // 海龟前鳍网格沿±X、后鳍沿+Z；前鳍以水平外展30°前伸，后鳍保持后伸，不能套普通腿的+Y长轴。
                for(var part:new ModelPart[]{p.pipa$frontRight(),p.pipa$frontLeft()}) {
                    part.xRot=FlightVisuals.blend(part.xRot,0,weight);part.zRot=FlightVisuals.blend(part.zRot,0,weight);
                    part.yRot=FlightVisuals.blend(part.yRot,part==p.pipa$frontRight()?-(float)Math.PI/3:(float)Math.PI/3,weight);
                }
                FlightVisuals.rotate(p.pipa$hindRight(),0,0,weight);FlightVisuals.rotate(p.pipa$hindLeft(),0,0,weight);
            } else {
                FlightVisuals.rotate(p.pipa$frontRight(),-(float)Math.PI/2,0,weight);FlightVisuals.rotate(p.pipa$frontLeft(),-(float)Math.PI/2,0,weight);
                p.pipa$frontRight().yRot+=(float)Math.PI/6*weight;p.pipa$frontLeft().yRot-=(float)Math.PI/6*weight;
                FlightVisuals.rotate(p.pipa$hindRight(),(float)Math.PI/2,0,weight);FlightVisuals.rotate(p.pipa$hindLeft(),(float)Math.PI/2,0,weight);
            }
            for(var part:new ModelPart[]{p.pipa$body(),p.pipa$head()}) {restPosition(part,weight);FlightVisuals.rotate(part,part.getInitialPose().xRot,part.getInitialPose().zRot,weight);}
        }
    }

    /** 人形在俯身后抬头。监守者的腿在 bone 下，头和手臂还要经过躯干。 */
    private void humanoidPose(ModelPart root,EntityModel<?> model,float weight) {
        if(model instanceof SnowGolemModel<?>) {
            joint(root.getChild("head"),-90,0,0,weight);
            joint(root.getChild("upper_body"),0,0,0,weight);
            // 网易树枝相对绑定姿势转117.3°；Java网格沿+X，折算后用±60°，右枝保留180°镜向。
            joint(root.getChild("right_arm"),0,180,60,weight);
            joint(root.getChild("left_arm"),0,0,-60,weight);
            return;
        }
        var hips=root;
        var shoulders=root;
        if(model instanceof WardenModel<?>) {
            hips=root.getChild("bone");
            joint(hips,0,0,0,weight);
            shoulders=hips.getChild("body");
        }
        joint(hips.getChild("body"),0,0,0,weight);
        joint(shoulders.getChild("head"),-90,0,0,weight);
        var right=shoulders.getChild("right_arm");
        var left=shoulders.getChild("left_arm");
        if(model instanceof IllagerModel<?>) {
            // 原版已经提供展开臂，飞行期间切换可见性，持物层仍读取同一对关节。
            foldedArms=root.getChild("arms");
            rightArm=right;
            leftArm=left;
            foldedVisible=foldedArms.visible;
            rightVisible=right.visible;
            leftVisible=left.visible;
            foldedArms.visible=false;
            right.visible=left.visible=true;
        }
        joint(right,-180,0,-30,weight);
        joint(left,-180,0,30,weight);
        joint(hips.getChild("right_leg"),0,0,0,weight);
        joint(hips.getChild("left_leg"),0,0,0,weight);
    }

    /** 坐下、攻击等动画的位移也需淡出，否则原关节角正确时网格仍可能错位。 */
    private void joint(ModelPart part,float pitch,float yaw,float roll,float weight) {
        restPosition(part,weight);
        FlightVisuals.rotate(part,(float)Math.toRadians(pitch),(float)Math.toRadians(roll),weight);
        part.yRot+=(float)Math.toRadians(yaw)*weight;
    }

    private void restPosition(ModelPart part,float weight) {
        saved.putIfAbsent(part,part.storePose());
        var rest=part.getInitialPose();
        part.x=FlightVisuals.blend(part.x,rest.x,weight);part.y=FlightVisuals.blend(part.y,rest.y,weight);part.z=FlightVisuals.blend(part.z,rest.z,weight);
    }
    @Override public void close(){
        // 原版 loadPose 会将缩放重置为1；这里只恢复覆盖过的位置和角度。
        saved.forEach((part,pose)->{
            part.setPos(pose.x,pose.y,pose.z);
            part.setRotation(pose.xRot,pose.yRot,pose.zRot);
        });
        if(foldedArms!=null) {
            foldedArms.visible=foldedVisible;
            rightArm.visible=rightVisible;
            leftArm.visible=leftVisible;
        }
    }
}
