package com.shaoxinpipaqu.qa;

import com.elfmcys.yesstevemodel.O0OOo0OoOoo0o0o0oO000o00;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.BedrockModel;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.client.FlightVisuals;
import com.shaoxinpipaqu.compat.FlightBones;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import java.util.*;

/** 只在独立探针中观察真正送入绘制器的骨骼；终态断言等过渡结束，过程由 FlightMotionProbe 单独验收。 */
public final class PoseProbe {
    static volatile int stage;
    public static String failure;
    static final Map<String,Integer> samples=new HashMap<>();
    static final Map<String,Set<Object>> animalModels=new HashMap<>();
    private static final Map<String,List<Float>> animalJoints=new HashMap<>();
    public static final ThreadLocal<LivingEntity> ENTITY=new ThreadLocal<>();
    public static void ysm(O0OOo0OoOoo0o0o0oO000o00 definition,float[] data) {
        var entity=ENTITY.get();if(entity==null || !MobFlight.posed(entity) || FlightVisuals.weight(entity)<1)return;
        var bones=definition.oOo0OO0O0o000OO0O000oo0o();
        int mask=0;var detail=new StringBuilder();
        for(int i=0;i<bones.size();i++) {
            int role=FlightBones.role(bones.get(i).oOo0OO0O0o000OO0O000oo0o());if(role<2)continue;
            if(!angles("YSM",role,data[i*12],data[i*12+1],data[i*12+2],true))return;
            mask|=1<<(role-2);detail.append(role).append(':').append(data[i*12]).append(',').append(data[i*12+2]).append(' ');
        }
        record(entity,"YSM",mask,detail.toString());
    }
    public static void maid(Object model) {
        var entity=ENTITY.get();if(entity==null || !MobFlight.posed(entity) || FlightVisuals.weight(entity)<1 || !(model instanceof BedrockModel<?> b))return;
        if(com.shaoxinpipaqu.compat.MaidFlightPose.turtle(b)) {
            for(var name:List.of("legRight","legLeft","legRight2","legLeft2"))if(Math.abs(b.getModelMap().get(name).xRot-(name.endsWith("2")?Math.PI/2:-Math.PI/2))>0.01){failure="玄爷四肢未水平伸展";return;}
            String key=stage+":"+entity.getId();int count=samples.merge(key,1,Integer::sum);
            if(count==1) {
                var detail=new StringBuilder("Genjii actual native draw front="+b.getModelMap().get("legRight").xRot+" hind="+b.getModelMap().get("legRight2").xRot+" body="+b.getModelMap().get("body").xRot+"\n");
                for(var name:List.of("legRight","legLeft","legRight2","legLeft2"))detail.append(name).append(' ').append(NativeLegProbe.measure(b.getModelMap().get(name))).append('\n');
                MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-bones-"+stage+"-"+entity.getId()+".txt",detail.toString());
            }
            return;
        }
        int mask=0;var detail=new StringBuilder();
        for(var entry:b.getModelMap().entrySet()) {
            int role=FlightBones.role(entry.getKey());if(role<2)continue;var p=entry.getValue();
            if(!angles("Bedrock",role,p.xRot,p.yRot,p.zRot,false))return;
            mask|=1<<(role-2);detail.append(entry.getKey()).append(':').append(p.xRot).append(',').append(p.zRot).append(' ');
        }
        record(entity,"Bedrock",mask,detail.toString());
    }
    private static boolean angles(String renderer,int role,float x,float y,float z,boolean yUp) {
        double pitch=(role==4?Math.PI/2:Math.PI)*(yUp?1:-1),roll=role==2?-Math.PI/6:role==3?Math.PI/6:0;
        if(Math.abs(x-pitch)>0.01 || Math.abs(y)>0.01 || Math.abs(z-roll)>0.01) {failure=renderer+"头/双臂目标姿势不符：role="+role+" rotation="+x+","+y+","+z;return false;}
        return true;
    }
    private static void record(LivingEntity e,String renderer,int mask,String detail) {
        if(mask!=7){failure=renderer+"缺少实际Head或左右上臂：mask="+mask;return;}
        String key=stage+":"+e.getId();int count=samples.merge(key,1,Integer::sum);
        if(count==1 && MultiplayerCheck.ROOT!=null)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-bones-"+stage+"-"+e.getId()+".txt",renderer+" actual native draw: "+detail+" model="+(e instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid m?m.getModelId():e.getType())+"\n");
    }
    /** 在实际实体绘制结束后检查本次使用的共享模型；每种模型独立计数，不能借用其他实体的姿势结果。 */
    public static void standard(LivingEntity e,net.minecraft.client.model.EntityModel<?> model) {
        standard(e,model,false);
    }
    public static void animalDraw(Object model) {
        var entity=com.shaoxinpipaqu.client.AnimalFlightPose.entity();
        if(entity!=null&&stage>=221&&model instanceof HierarchicalModel<?> h
                && entity.hasCustomName()&&entity.getCustomName().getString().equals("PipaModel-"+MultiplayerCheck.modelIndex(stage))
                && MobFlight.posed(entity)&&FlightVisuals.weight(entity)>=1) {
            supplemental(entity,h);
        } else if(entity!=null&&MobFlight.posed(entity)&&FlightVisuals.weight(entity)>=1&&(model instanceof com.shaoxinpipaqu.client.AnimalModelRoot||model instanceof net.minecraft.client.model.PandaModel<?>||model instanceof net.minecraft.client.model.PolarBearModel<?>||model instanceof net.minecraft.client.model.TurtleModel<?>)) {
            if(model instanceof com.shaoxinpipaqu.client.AnimalModelRoot animal) {
                var signature=new ArrayList<Float>();var root=animal.pipa$animalRoot();
                for(var name:List.of("right_front_leg","left_front_leg","right_hind_leg","left_hind_leg","body","head","head_parts","neck"))if(root.hasChild(name)) {
                    var p=root.getChild(name);signature.addAll(List.of(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot));
                }
                var previous=animalJoints.putIfAbsent(stage+":"+entity.getId(),signature);
                // 对照真正绘制的本体与独立图层关节，不借本体角度替代护甲/项圈/装饰的结果。
                if(previous!=null)for(int i=0;i<signature.size();i++)if(Math.abs(previous.get(i)-signature.get(i))>0.001F){failure="四足独立图层关节与本体不对齐："+model.getClass().getName()+" component="+i;return;}
            }
            animalModels.computeIfAbsent(stage+":"+entity.getId(),k->Collections.newSetFromMap(new IdentityHashMap<>())).add(model);
            standard(entity,(net.minecraft.client.model.EntityModel<?>)model,true);
        }
    }
    /** 在真实网格入口读关节方向，带电外壳和鞍具需要独立模型采样。 */
    private static void supplemental(LivingEntity entity,HierarchicalModel<?> model) {
        var root=model.root();
        if(model instanceof CamelModel<?> || model instanceof CreeperModel<?>) {
            if(!horizontal(new ModelPart[]{root.getChild("right_front_leg"),root.getChild("left_front_leg"),
                    root.getChild("right_hind_leg"),root.getChild("left_hind_leg")}))return;
        } else if(model instanceof StriderModel<?>) {
            if(!direction(root.getChild("right_leg"),0,1,-0.5,0,-Math.sqrt(3)/2)
                    ||!direction(root.getChild("left_leg"),0,1,0.5,0,-Math.sqrt(3)/2))return;
        } else if(model instanceof SnowGolemModel<?>) {
            // 树枝沿+X延伸；变换后的本地-Y分量在俯身后指向前方。
            if(!direction(root.getChild("right_arm"),1,0,-0.5,-Math.sqrt(3)/2,0)
                    ||!direction(root.getChild("left_arm"),1,0,0.5,-Math.sqrt(3)/2,0)
                    ||!direction(root.getChild("head"),0,0,0,-1,0))return;
        } else if(model instanceof IronGolemModel<?> || model instanceof IllagerModel<?> || model instanceof WardenModel<?>) {
            if(model instanceof WardenModel<?>)root=root.getChild("bone").getChild("body");
            var right=root.getChild("right_arm");
            var left=root.getChild("left_arm");
            if(model instanceof IllagerModel<?> && (root.getChild("arms").visible||!right.visible||!left.visible)) {
                failure="灾厄村民未切换到独立手臂";
                return;
            }
            if(!humanParts(root.getChild("head"),right,left))return;
        } else {
            failure="新增生物未使用预期模型："+model.getClass().getName();
            return;
        }
        animalModels.computeIfAbsent(stage+":"+entity.getId(),k->Collections.newSetFromMap(new IdentityHashMap<>())).add(model);
        record(entity,model.getClass().getSimpleName(),7,"飞行关节轴向通过，实际模型绘制");
    }

    private static boolean direction(ModelPart part,float axisX,float axisY,double x,double y,double z) {
        var pose=new com.mojang.blaze3d.vertex.PoseStack();
        part.translateAndRotate(pose);
        var actual=pose.last().pose().transformDirection(new org.joml.Vector3f(axisX,axisY,axisX==0&&axisY==0?-1:0)).normalize();
        if(Math.abs(actual.x-x)>0.01||Math.abs(actual.y-y)>0.01||Math.abs(actual.z-z)>0.01) {
            failure="新增生物的关节轴向错误："+actual+" expected="+x+","+y+","+z;
            return false;
        }
        return true;
    }
    private static void standard(LivingEntity e,net.minecraft.client.model.EntityModel<?> model,boolean nativeDraw) {
        // 新增四足的姿势只在网格绘制期间生效，RenderLivingEvent.Post时已恢复，不能在那里读站姿当作失败。
        if(!nativeDraw&&stage>=221)return; // 新增模型只在实际绘制入口采样，避免主模型与装备层重复计数。
        if(!nativeDraw&&(model instanceof com.shaoxinpipaqu.client.AnimalModelRoot||model instanceof net.minecraft.client.model.PandaModel<?>||model instanceof net.minecraft.client.model.PolarBearModel<?>||model instanceof net.minecraft.client.model.TurtleModel<?>)) {
            if(model instanceof com.shaoxinpipaqu.client.AnimalModelRoot animal&&samples.containsKey(stage+":"+e.getId())) {
                var root=animal.pipa$animalRoot();boolean leaked=true;
                for(var name:List.of("right_front_leg","left_front_leg","right_hind_leg","left_hind_leg"))leaked&=Math.abs(root.getChild(name).xRot-(name.contains("front")?-Math.PI/2:Math.PI/2))<0.001;
                if(leaked)failure="非标准四足绘制结束后飞行关节未恢复";
            }
            return;
        }
        // YSM的getModel()可能返回未参与真实绘制的PlayerModel占位对象；原版外层只在明确关闭YSM的专项检查。
        if(Boolean.getBoolean("pipa.qa.vanillaPlayers")&&stage>=30&&stage<50&&e instanceof net.minecraft.world.entity.player.Player&&model instanceof net.minecraft.client.model.PlayerModel<?> player&&MobFlight.posed(e)&&FlightVisuals.weight(e)>=1) {
            if(!humanParts(player.head,player.rightArm,player.leftArm))return;
            var outer=new net.minecraft.client.model.geom.ModelPart[]{player.hat,player.rightSleeve,player.leftSleeve,player.rightPants,player.leftPants,player.jacket};
            var inner=new net.minecraft.client.model.geom.ModelPart[]{player.head,player.rightArm,player.leftArm,player.rightLeg,player.leftLeg,player.body};
            for(int i=0;i<outer.length;i++)if(outer[i].xRot!=inner[i].xRot||outer[i].yRot!=inner[i].yRot||outer[i].zRot!=inner[i].zRot) {failure="原版玩家帽层/袖子/裤腿/外衣姿势不同步："+i;return;}
            record(e,"Vanilla PlayerModel outer layers",7,"head, both arms, hat, sleeves, pants, jacket aligned");return;
        }
        if(stage<60 || !e.hasCustomName() || !e.getCustomName().getString().equals("PipaModel-"+MultiplayerCheck.modelIndex(stage)) || !MobFlight.posed(e) || FlightVisuals.weight(e)<1)return;
        if(model instanceof net.minecraft.client.model.HumanoidModel<?> h) {
            if(!humanParts(h.head,h.rightArm,h.leftArm))return;
            if(Math.abs(h.hat.xRot-h.head.xRot)>0.0001 || Math.abs(h.hat.zRot-h.head.zRot)>0.0001){failure="帽层未跟随抬头";return;}
            if(model instanceof net.minecraft.client.model.EndermanModel<?>) {
                for(var arm:List.of(h.rightArm,h.leftArm)) {
                    var rest=arm.getInitialPose();
                    if(arm.x!=rest.x||arm.y!=rest.y||arm.z!=rest.z){failure="末影人肩关节偏离原模型位置";return;}
                }
            }
            record(e,"Humanoid+equipment",7,"face forward, both arms 30deg outward; shoulders="+h.rightArm.x+","+h.rightArm.y+" / "+h.leftArm.x+","+h.leftArm.y);
        }
        else if(model instanceof net.minecraft.client.model.VillagerModel<?> v) {
            var r=v.root();
            if(r.getChild("arms").visible || !r.getChild("pipa_right_arm").visible || !r.getChild("pipa_left_arm").visible) {failure="村民未切换独立手臂";return;}
            if(!humanParts(r.getChild("head"),r.getChild("pipa_right_arm"),r.getChild("pipa_left_arm")))return;
            if(samples.getOrDefault(stage+":"+e.getId(),0)==0) {
                try {MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-villager-uv-"+stage+".txt",VillagerUvProbe.check(r.getChild("pipa_right_arm"),r.getChild("pipa_left_arm")));}
                catch(Exception|AssertionError problem){failure="村民网格/UV检查失败："+problem;return;}
            }
            record(e,"Villager+profession",7,"face forward, both independent arms 30deg outward");
        } else if(model instanceof com.shaoxinpipaqu.client.AnimalModelRoot animal) {
            var root=animal.pipa$animalRoot();
            var legs=new net.minecraft.client.model.geom.ModelPart[]{root.getChild("right_front_leg"),root.getChild("left_front_leg"),root.getChild("right_hind_leg"),root.getChild("left_hind_leg")};
            if(!horizontal(legs))return;
            // 幼年马实际绘制另一组腿，不能只验证隐藏的成年腿。
            if(e.isBaby()&&root.hasChild("right_front_baby_leg")&&!horizontal(new net.minecraft.client.model.geom.ModelPart[]{root.getChild("right_front_baby_leg"),root.getChild("left_front_baby_leg"),root.getChild("right_hind_baby_leg"),root.getChild("left_hind_baby_leg")}))return;
            String key=stage+":"+e.getId();int count=samples.merge(key,1,Integer::sum);
            if(count==1)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-bones-"+stage+".txt",model.getClass().getName()+" baby="+e.isBaby()+" all four actual leg axes horizontal; original body orientation retained");
        } else if(model instanceof net.minecraft.client.model.QuadrupedModel<?> q) {
            if(q instanceof net.minecraft.client.model.TurtleModel<?>?!turtle(q):!horizontal(q))return;
            String key=stage+":"+e.getId();int count=samples.merge(key,1,Integer::sum);
            if(count==1)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-bones-"+stage+".txt",model.getClass().getName()+" baby="+e.isBaby()+" modelYoung="+model.young+" all four leg directions horizontal; front outward 30deg, hind=+Z");
        } else failure="专项模型尚无骨骼检查："+model.getClass().getName();
    }
    /** 用俯身及原版坐标翻转后的实际方向验证：脸朝前，双臂在躯干平面内各外展30°。 */
    private static boolean humanParts(net.minecraft.client.model.geom.ModelPart head,net.minecraft.client.model.geom.ModelPart right,net.minecraft.client.model.geom.ModelPart left) {
        var parts=new net.minecraft.client.model.geom.ModelPart[]{head,right,left};
        for(int i=0;i<3;i++) {
            var pose=new com.mojang.blaze3d.vertex.PoseStack();
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));pose.scale(-1,-1,1);parts[i].translateAndRotate(pose);
            var d=pose.last().pose().transformDirection(i==0?new org.joml.Vector3f(0,0,-1):new org.joml.Vector3f(0,1,0)).normalize();
            double x=i==0?0:i==1?0.5:-0.5,z=i==0?-1:-Math.sqrt(3)/2;
            if(Math.abs(d.x-x)>0.01 || Math.abs(d.y)>0.01 || Math.abs(d.z-z)>0.01){failure="人形实际方向错误：part="+i+" direction="+d;return false;}
        }
        return true;
    }
    /** 立板实际渲染的缩放必须是单位比例，且同一专项中观察到 GIF 帧推进。 */
    public static void guest(com.friendswine.PartyGuestEntity e,float x,float y,float z,int frame) {
        if(stage<70 || !MobFlight.posed(e))return;
        if(x!=1 || y!=1 || z!=1) {failure="飞行立板仍被压扁";return;}
        String key=stage+":"+e.getId();int count=samples.getOrDefault(key,0);
        java.util.Set<Integer> frames=GUEST_FRAMES.computeIfAbsent(key,k->new HashSet<>());frames.add(frame);
        if(frames.size()<2)return;
        samples.put(key,count+1);
        if(count==0)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-guest-"+stage+".txt","actual render scale=1,1,1 frames="+frames);
    }
    private static final Map<String,java.util.Set<Integer>> GUEST_FRAMES=new HashMap<>();
    /** 每帧对照羊皮肤与羊毛的四条腿，过渡期也检查，终态再验证实际变换后的腿轴是否水平。 */
    public static void fur(LivingEntity e,net.minecraft.client.model.SheepFurModel<?> model,net.minecraft.client.model.SheepModel<?> main) {
        var fur=legs(model);var skin=legs(main);
        for(int i=0;i<4;i++) {
            var a=fur[i];var b=skin[i];
            if(Math.abs(a.xRot-b.xRot)>0.0001 || Math.abs(a.yRot-b.yRot)>0.0001 || Math.abs(a.zRot-b.zRot)>0.0001
                    || Math.abs(a.x-b.x)>0.0001 || Math.abs(a.y-b.y)>0.0001 || Math.abs(a.z-b.z)>0.0001) {
                failure="羊毛与皮肤腿部错位：leg="+i+" weight="+FlightVisuals.weight(e)+" fur="+a.xRot+" skin="+b.xRot;return;
            }
        }
        if(!MobFlight.posed(e) || FlightVisuals.weight(e)<1)return;
        if(!horizontal(model))return;
        String key=stage+":"+e.getId()+":fur";int count=samples.merge(key,1,Integer::sum);
        if(count==1)MultiplayerCheck.write(System.getProperty("pipa.qa.role")+"-fur-"+stage+".txt","actual SheepFurModel all four legs aligned with SheepModel; front="+fur[0].xRot+" hind="+fur[2].xRot+" directions horizontal");
    }
    private static net.minecraft.client.model.geom.ModelPart[] legs(net.minecraft.client.model.QuadrupedModel<?> model) {
        var p=(com.shaoxinpipaqu.mixin.QuadrupedAccessor)model;
        return new net.minecraft.client.model.geom.ModelPart[]{p.pipa$frontRight(),p.pipa$frontLeft(),p.pipa$hindRight(),p.pipa$hindLeft()};
    }
    /** 从实际ModelPart变换腿的长度方向，检查前腿水平外展及后腿后伸。 */
    private static boolean horizontal(net.minecraft.client.model.QuadrupedModel<?> model) {
        return horizontal(legs(model));
    }
    /** 海龟的前鳍长轴为±X，后鳍长轴为+Z；检查完整关节变换后的方向，不以普通腿轴冒充鳍轴。 */
    private static boolean turtle(net.minecraft.client.model.QuadrupedModel<?> model) {
        var parts=legs(model);
        for(int i=0;i<4;i++) {
            var pose=new com.mojang.blaze3d.vertex.PoseStack();parts[i].translateAndRotate(pose);
            var axis=i<2?new org.joml.Vector3f(i==0?-1:1,0,0):new org.joml.Vector3f(0,0,1);
            var direction=pose.last().pose().transformDirection(axis).normalize();
            float x=i==0?-0.5F:i==1?0.5F:0,z=i<2?-(float)Math.sqrt(3)/2:1;
            if(Math.abs(direction.y)>0.01||Math.abs(direction.x-x)>0.01||Math.abs(direction.z-z)>0.01){failure="海龟实际鳍轴错误：part="+i+" direction="+direction;return false;}
        }
        return true;
    }
    private static boolean horizontal(net.minecraft.client.model.geom.ModelPart[] parts) {
        for(int i=0;i<4;i++) {
            var pose=new com.mojang.blaze3d.vertex.PoseStack();parts[i].translateAndRotate(pose);
            var direction=pose.last().pose().transformDirection(new org.joml.Vector3f(0,1,0));
            double x=i==0?-0.5:i==1?0.5:0,z=i<2?-Math.sqrt(3)/2:1;
            if(Math.abs(direction.y)>0.01 || Math.abs(direction.x-x)>0.01 || Math.abs(direction.z-z)>0.01) {
                failure="四足腿轴未按前腿外展30°、后腿后伸：leg="+i+" direction="+direction;return false;
            }
        }
        return true;
    }
    public static void gecko(LivingEntity e,com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel model) {
        if(model==null||!MobFlight.posed(e) || FlightVisuals.weight(e)<1)return;
        int mask=0;var detail=new StringBuilder();
        for(var entry:model.bones().entrySet()) {
            var p=entry.getValue();int role=p==model.rightArm()?2:FlightBones.role(entry.getKey());if(role<2)continue;
            if(!angles("Gecko",role,p.getRotationX(),p.getRotationY(),p.getRotationZ(),true))return;
            mask|=1<<(role-2);detail.append(entry.getKey()).append(':').append(p.getRotationX()).append(',').append(p.getRotationZ()).append(' ');
        }
        record(e,"Gecko native",mask,detail.toString());
    }
}
