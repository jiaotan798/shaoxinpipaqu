package com.shaoxinpipaqu.client;

/** 纯客户端姿态时钟：每 tick 更新一次，渲染只插值，阴影和多材质绘制不会重复推进。 */
public final class FlightPoseMotion {
    private float previousWeight, weight, previousBank, bank;
    private double heading;
    private boolean moving;

    /** 位移单位为格/tick；低速回正，传送不产生急转倾斜，航向差取最短角。 */
    public void tick(boolean flying, double dx, double dz) {
        previousWeight=weight; previousBank=bank;
        // 半秒收放姿势；这是本模组的表现参数，不影响服务端飞行速度或碰撞箱。
        weight=flying?Math.min(1,weight+0.1F):Math.max(0,weight-0.1F);
        double distance=Math.hypot(dx,dz), target=0;
        boolean travelling=flying && Double.isFinite(distance) && distance>=0.03 && distance<4;
        if(travelling) {
            double next=Math.toDegrees(Math.atan2(-dx,dz));
            if(moving) target=Math.max(-25,Math.min(25,wrap(next-heading)*2.5));
            heading=next;
        }
        moving=travelling;
        bank+=(float)(target-bank)*0.25F;
        if(Math.abs(bank)<0.01F && target==0) bank=0;
    }
    private static double wrap(double angle) { return angle-360*Math.floor((angle+180)/360); }
    public float weight(float partial) { return previousWeight+(weight-previousWeight)*Math.max(0,Math.min(1,partial)); }
    public float bank(float partial) { return (previousBank+(bank-previousBank)*Math.max(0,Math.min(1,partial)))*weight(partial); }
    public boolean finished() { return weight==0 && previousWeight==0; }

    /** 无游戏依赖的自检，覆盖起落、反向、跨 ±180°、悬停和传送。 */
    public static void main(String[] args) {
        var s=new FlightPoseMotion();
        for(int i=0;i<12;i++) s.tick(true,0,0.2);
        check(s.weight(1)==1 && s.bank(1)==0,"直飞收敛");
        s.tick(true,-0.02,0.2);
        check(s.bank(1)>0 && s.bank(1)<=25,"正向转弯倾斜");
        check(s.weight(0)==1 && s.bank(0.5F)>s.bank(0),"帧间插值");
        for(int i=0;i<40;i++) s.tick(true,0,0);
        check(s.bank(1)==0,"悬停回正");
        s.tick(true,0,-0.2);s.tick(true,0.003,-0.2);
        check(Math.abs(s.bank(1))<1,"航向跨周不应猛倾");
        s.tick(true,20,20);
        check(Math.abs(s.bank(1))<1,"传送不产生倾斜");
        for(int i=0;i<12;i++) s.tick(false,0,0);
        check(s.finished() && s.weight(1)==0 && s.bank(1)==0,"落地释放");
        s.tick(true,0,0.2);s.tick(false,0,0);s.tick(true,0,0.2);
        check(s.weight(1)>0 && s.weight(1)<1,"快速重复起落保持连续");
        s.tick(true,Double.NaN,Double.POSITIVE_INFINITY);
        check(Float.isFinite(s.bank(1)),"无效位移不能污染姿态");
        System.out.println("飞行姿态过渡与转弯倾斜检查通过");
    }
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
