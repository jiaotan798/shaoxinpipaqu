package com.shaoxinpipaqu.compat;

import java.util.Locale;

/** 只识别明确的躯干、肢体骨骼；饰品、头发与未知骨骼保留原动画，缺右上臂不冒充已适配。 */
public final class FlightBones {
    // 只按名称缓存，不假定第三方模型骨架不可变；弱键让已卸载资源的名称可回收，同步表兼容并发调用。
    private static final java.util.Map<String,Integer> ROLES=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private FlightBones() {}
    public static int role(String name) {
        return ROLES.computeIfAbsent(name,FlightBones::classify);
    }
    private static int classify(String name) {
        String n=name.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(" ","");
        return switch(n) {
            case "rightarm","rightupperarm","armright","armr","rarm","右腕" -> 2;
            case "leftarm","leftupperarm","armleft","arml","larm","左腕" -> 3;
            case "head" -> 4;
            case "rightforearm","rightlowerarm","rightelbow","righthand","leftforearm","leftlowerarm","leftelbow","lefthand",
                 "root","mallbody","allbody","upbody","upperbody","downbody","body","torso","arm","leg",
                 "allhead","mhead","rightleg","leftleg","rightlowerleg","leftlowerleg","rightfoot","leftfoot" -> 1;
            default -> 0;
        };
    }
    /** YSM／Gecko网格以Y向上，原版／Bedrock以Y向下；只转实际Head，不能连AllHead、MHead父节点一起转。 */
    public static float pitch(int role,boolean yUp) {
        float angle=role==2||role==3?(float)Math.PI:role==4?(float)Math.PI/2:0;
        return yUp?angle:-angle;
    }
    public static float roll(int role) { return role==2?-(float)Math.PI/6:role==3?(float)Math.PI/6:0; }
    public static void main(String[] args) {
        for(String name:new String[]{"Right_Arm","right-arm","arm right","Head","unknown","右腕"}) {
            if(role(name)!=classify(name) || role(new String(name))!=classify(name))throw new AssertionError("名称缓存不能改变角色识别："+name);
        }
        for (String name:new String[]{"RightArm","right_arm","arm_right","右腕"}) if(role(name)!=2) throw new AssertionError(name);
        if(role("RightArmRibbon")!=0 || role("RightHandLocator")!=0 || role("LeftForeArm")!=1) throw new AssertionError("骨骼分类误匹配");
        if(role("LeftArm")!=3 || role("Head")!=4 || role("MHead")!=1 || role("AllHead")!=1)throw new AssertionError("头部父节点不能重复抬头");
        if(pitch(4,false)!=-(float)Math.PI/2 || pitch(2,true)!=(float)Math.PI || roll(2)!=-roll(3))throw new AssertionError("抬头与对称外展角度");
        System.out.println("特殊模型骨骼角色检查通过");
    }
}
