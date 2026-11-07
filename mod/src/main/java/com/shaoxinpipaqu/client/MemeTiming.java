package com.shaoxinpipaqu.client;

import com.shaoxinpipaqu.AudioInfo;

/** 卡点与稳定抽选的纯数学规则；毫秒取自已确认歌词轴，不加载或显示歌词正文。 */
public final class MemeTiming {
    public enum Cue {
        MJ(4720,6720), DOG(14720,15920), CABBAGE(17780,18800), COURAGE(23760,26540);
        public final int startMs,endMs;
        Cue(int startMs,int endMs) { this.startMs=startMs;this.endMs=endMs; }
    }
    private MemeTiming() {}

    /** 相对玩偶方块中心的世界坐标（格）及字体比例；固定三维点使两端一致，移动观察时保留视差。 */
    public record MjPlacement(double x,double y,double z,float scale) {}
    private static final java.util.List<MjPlacement> MJ_PLACEMENTS=java.util.List.of(
        new MjPlacement(-1.30,2.04,1.10,0.030F),
        new MjPlacement(0.90,1.74,1.40,0.045F),
        new MjPlacement(-1.45,2.87,-0.60,0.036F),
        new MjPlacement(0.20,1.98,-1.25,0.032F),
        new MjPlacement(1.50,0.85,-0.40,0.041F),
        new MjPlacement(-0.10,3.03,1.60,0.038F));
    public static MjPlacement mjPlacement(int index) { return MJ_PLACEMENTS.get(index); }
    private static final double[] VIEW_X={0,0.70710678,1,0.70710678,0,-0.70710678,-1,-0.70710678};
    private static final double[] VIEW_Z={1,0.70710678,0,-0.70710678,-1,-0.70710678,0,0.70710678};

    /** 每次触发在基准点附近稳定抽选；最多128组候选，整体比较最拥挤的一对，避免贪心挤压最后一个字。 */
    public static MjPlacement[] mjPlacements(long position,long startTick,long cycle) {
        var random=new java.util.Random(position^Long.rotateLeft(startTick,21)^(cycle*0x9E3779B97F4A7C15L));
        var best=MJ_PLACEMENTS.toArray(MjPlacement[]::new);double bestScore=2;
        for(int attempt=0;attempt<128;attempt++) {
            var candidate=new MjPlacement[6];double score=0;
            for(int i=0;i<6;i++) {
                var anchor=mjPlacement(i);
                candidate[i]=new MjPlacement(anchor.x()+(random.nextDouble()*2-1)*0.25,
                    anchor.y()+(random.nextDouble()*2-1)*0.30,anchor.z()+(random.nextDouble()*2-1)*0.35,anchor.scale());
                score=Math.max(score,placementOverlap(candidate[i],candidate,i));
                if(score>=bestScore)break;
            }
            if(score<bestScore){best=candidate;bestScore=score;}
            if(bestScore<=0.15)break;
        }
        // 有限抽样找不到低重叠布局时回退已核验基准，不把拥挤候选当作最终随机结果。
        return bestScore<=0.15?best:MJ_PLACEMENTS.toArray(MjPlacement[]::new);
    }

    /** 单玩偶只保留当前触发的布局；返回数组只读，镜头朝向和轻抖仍在渲染时计算。 */
    public static final class LayoutCache {
        private long position,startTick,cycle;
        private MjPlacement[] layout;
        public MjPlacement[] get(long position,long startTick,long cycle) {
            if(layout==null || this.position!=position || this.startTick!=startTick || this.cycle!=cycle) {
                layout=mjPlacements(position,startTick,cycle);
                this.position=position;this.startTick=startTick;this.cycle=cycle;
            }
            return layout;
        }
    }

    /** 八个水平观察方向、7格距离估计字芯投影重叠比例；含最大回弹和倾斜余量，不声称所有视角均无遮挡。 */
    private static double placementOverlap(MjPlacement a,MjPlacement[] placed,int count) {
        double worst=0;
        for(int i=0;i<count;i++) {
            var b=placed[i];double dx=a.x()-b.x(),dy=a.y()-b.y(),dz=a.z()-b.z();
            if(dx*dx+dy*dy+dz*dz<0.81)return 2; // 世界中心间距至少0.9格，不能仅依赖投影判断。
            for(int k=0;k<8;k++) {
                double ad=7-a.x()*VIEW_X[k]-a.z()*VIEW_Z[k],bd=7-b.x()*VIEW_X[k]-b.z()*VIEW_Z[k];
                double ax=(a.x()*VIEW_Z[k]-a.z()*VIEW_X[k])/ad,bx=(b.x()*VIEW_Z[k]-b.z()*VIEW_X[k])/bd;
                // 眼高相对玩偶中心1.12格；字芯中心在顶部下方3.5像素，使用保守的1.26倍最大尺寸。
                double ay=(a.y()-1.12-3.5*a.scale())/ad,by=(b.y()-1.12-3.5*b.scale())/bd;
                double aw=7.5*a.scale()*1.26/ad,bw=7.5*b.scale()*1.26/bd;
                double ah=6*a.scale()*1.26/ad,bh=6*b.scale()*1.26/bd;
                double intersection=Math.max(0,Math.min(ax+aw,bx+bw)-Math.max(ax-aw,bx-bw))
                    *Math.max(0,Math.min(ay+ah,by+bh)-Math.max(ay-ah,by-bh));
                worst=Math.max(worst,intersection/Math.min(4*aw*ah,4*bw*bh));
            }
        }
        return worst;
    }
    private static final int[] MJ_COLORS={0xFFC6DE,0xC8C1FF,0xA9E8EE,0xFFE1B2,0xBFECC9,0xB9D8FF};
    public static int mjColor(int index) { return MJ_COLORS[index]; }
    /** 入场120毫秒内从1.22倍回到原字号，随后保持在两秒显示区间内。 */
    public static float mjPop(double ageMs) {
        double rest=1-Math.max(0,Math.min(1,ageMs/120));return (float)(1+0.22*rest*rest);
    }
    /** 8Hz低频小幅变化，读取歌曲年龄而非本地墙钟，暂停与两端均保持一致。 */
    public static long mjWiggleFrame(double ageMs) { return (long)Math.floor(Math.max(0,ageMs)/125); }

    /** 每次触发稳定抽取±8°小幅倾斜，按玩偶、起播、循环和文字编号同步；不能逐帧随机。 */
    public static float mjTilt(long position,long startTick,long cycle,int index) {
        long seed=position^Long.rotateLeft(startTick,21)^(cycle*0x9E3779B97F4A7C15L)^((index+1L)*0xD1B54A32D192ED03L);
        seed=(seed^(seed>>>30))*0xBF58476D1CE4E5B9L;
        seed=(seed^(seed>>>27))*0x94D049BB133111EBL;
        return ((seed^(seed>>>31))>>>40)/16777215F*16F-8F;
    }

    /** 以真实PCM长度循环，不能将26.541814秒截成整数tick，否则逐遍漂移。 */
    public static double phaseMs(double elapsedTicks) {
        return elapsedTicks*(AudioInfo.SAMPLE_RATE/20.0)%AudioInfo.FRAMES*1000/AudioInfo.SAMPLE_RATE;
    }
    public static long cycle(double elapsedTicks) { return (long)Math.floor(elapsedTicks/AudioInfo.LOOP_TICKS); }
    public static Cue active(double elapsedTicks) {
        if(!Double.isFinite(elapsedTicks)||elapsedTicks<0)return null;
        double phase=phaseMs(elapsedTicks);
        for(Cue cue:Cue.values())if(phase>=cue.startMs&&phase<cue.endMs)return cue;
        return null;
    }

    /** 前后各25%平滑缓入/缓出，中间50%保持；动画全部包含在对应歌词区间内。 */
    public static float alpha(Cue cue,double phaseMs) {
        if(phaseMs<cue.startMs||phaseMs>=cue.endMs)return 0;
        if(cue==Cue.MJ)return 1;
        double progress=(phaseMs-cue.startMs)/(cue.endMs-cue.startMs);
        double weight=Math.max(0,Math.min(1,Math.min(progress/0.25,(1-progress)/0.25)));
        return (float)(weight*weight*(3-2*weight));
    }

    /** 一张正上方，两张仅位置绕玩偶中心分别左右偏转45°；此角度不用于图片自身旋转。 */
    public static float pictureAngle(int count,int index) { return count==2?(index==0?45:-45):0; }
    /** 图像自身倾斜与位置偏转分开：双张左右各15°，单张保持竖直。 */
    public static float pictureTilt(int count,int index) { return count==2?(index==0?15:-15):0; }
    public static double pictureX(int count,int index) { return -2.4*Math.sin(Math.toRadians(pictureAngle(count,index))); }
    public static double pictureY(int count,int index) { return 2.4*Math.cos(Math.toRadians(pictureAngle(count,index))); }

    /** 同一玩偶、起播时刻、循环及句子得到同一数量；不逐帧抽样，两端可独立复现。 */
    public static int count(long position,long startTick,long cycle,Cue cue) {
        if(cue==Cue.MJ)return 6;
        long seed=position^Long.rotateLeft(startTick,21)^(cycle*0x9E3779B97F4A7C15L)^cue.ordinal();
        seed=(seed^(seed>>>30))*0xBF58476D1CE4E5B9L;
        seed=(seed^(seed>>>27))*0x94D049BB133111EBL;
        return 1+(int)((seed^(seed>>>31))&1);
    }

    /** 边界、循环、缓出及稳定抽选自检，不依赖游戏窗口或客户端类。 */
    public static void main(String[] args) {
        var cache=new LayoutCache();
        var cached=cache.get(1,100,0);
        check(cached==cache.get(1,100,0),"同次触发复用数组，不逐帧重新搜索布局");
        for(long[] key:new long[][]{{2,100,0},{2,101,0},{2,101,1},{1,100,0}}) {
            var next=cache.get(key[0],key[1],key[2]);
            check(next!=cached && java.util.Arrays.equals(next,mjPlacements(key[0],key[1],key[2])),"位置、重启与循环变化均重新计算且保持原布局");
            cached=next;
        }
        for(Cue cue:Cue.values()) {
            check(active(cue.startMs/50.0+0.00001)==cue,"开始边界");
            check(active(cue.startMs/50.0-0.00001)!=cue,"开始前不显示");
            check(active(cue.endMs/50.0+0.00001)!=cue,"结束不拖尾");
            check(alpha(cue,cue.startMs)==(cue==Cue.MJ?1:0)&&alpha(cue,cue.endMs)==0,"入场与收尾");
            if(cue!=Cue.MJ) {
                double duration=cue.endMs-cue.startMs;
                check(alpha(cue,cue.startMs+duration*0.5)==1,"句中完整显示");
                check(Math.abs(alpha(cue,cue.startMs+duration*0.1)-alpha(cue,cue.startMs+duration*0.9))<0.0001,"缓入缓出对称");
                check(alpha(cue,cue.startMs+duration*0.1)>0&&alpha(cue,cue.startMs+duration*0.1)<1,"缓入不能闪现");
            }
            for(int loop=0;loop<1000;loop++)check(active(AudioInfo.LOOP_TICKS*loop+(cue.startMs+20)/50.0)==cue,"循环不漂移");
        }
        boolean one=false,two=false;
        for(int i=0;i<100;i++) {
            int n=count(i,100,4,Cue.DOG);one|=n==1;two|=n==2;
            check(n==count(i,100,4,Cue.DOG)&&n>=1&&n<=2,"两端抽选稳定且有界");
        }
        check(one&&two&&count(0,0,0,Cue.MJ)==6,"数量分支与MJ独立");
        check(MJ_PLACEMENTS.size()==count(0,0,0,Cue.MJ),"MJ布局必须覆盖全部绘制项");
        var scales=new java.util.HashSet<Float>();
        for(var point:MJ_PLACEMENTS) {
            check(point.scale()>=0.030F&&point.scale()<=0.045F,"MJ字号限制为旧版的40%至60%");
            scales.add(point.scale());
        }
        check(scales.size()==6,"六个MJ字号错落");
        double worstOverlap=0;int randomized=0;
        for(int sample=0;sample<1000;sample++) {
            var layout=mjPlacements(sample,100,sample%17);
            if(!java.util.Arrays.equals(layout,MJ_PLACEMENTS.toArray(MjPlacement[]::new)))randomized++;
            check(java.util.Arrays.equals(layout,mjPlacements(sample,100,sample%17)),"随机位置同一触发稳定且两端可复现");
            for(int i=0;i<6;i++) {
                var point=layout[i];var anchor=mjPlacement(i);
                check(Math.abs(point.x()-anchor.x())<=0.250001&&Math.abs(point.y()-anchor.y())<=0.300001&&Math.abs(point.z()-anchor.z())<=0.350001,"位置随机范围有界");
                double overlap=placementOverlap(point,layout,i);check(overlap<2,"候选必须保留0.9格世界间距");worstOverlap=Math.max(worstOverlap,overlap);
            }
        }
        check(!java.util.Arrays.equals(mjPlacements(1,100,0),mjPlacements(1,100,1)),"下一轮位置可变化");
        check(worstOverlap<=0.15&&randomized>900,"代表方向低重叠且随机布局不能普遍退化为基准");
        System.out.println("1000组布局随机成功数："+randomized+"；八方向最大字芯投影重叠比例："+worstOverlap);
        check(MJ_COLORS.length==6&&java.util.Arrays.stream(MJ_COLORS).distinct().count()==6,"六种柔和配色");
        check(Math.abs(mjPop(0)-1.22)<0.0001&&mjPop(60)>1&&mjPop(120)==1&&mjPop(240)==1,"短促回弹必须在卡点内归位");
        check(mjWiggleFrame(0)==0&&mjWiggleFrame(124)==0&&mjWiggleFrame(125)==1,"轻抖8Hz分段稳定");
        var tilts=new java.util.HashSet<Float>();
        for(int i=0;i<6;i++) {
            float tilt=mjTilt(10,100,2,i);tilts.add(tilt);
            check(tilt>=-8&&tilt<=8&&tilt==mjTilt(10,100,2,i),"MJ倾斜有界且同一触发稳定");
        }
        check(tilts.size()>1&&mjTilt(10,100,2,0)!=mjTilt(10,100,3,0),"各MJ与下一轮可有不同倾斜");
        // 取四点的混合积确认布局有体积，防止再次退化为同一镜头平面的文字环。
        var a=mjPlacement(0);var b=mjPlacement(1);var c=mjPlacement(2);var d=mjPlacement(4);
        double volume=(b.x()-a.x())*((c.y()-a.y())*(d.z()-a.z())-(c.z()-a.z())*(d.y()-a.y()))
            -(b.y()-a.y())*((c.x()-a.x())*(d.z()-a.z())-(c.z()-a.z())*(d.x()-a.x()))
            +(b.z()-a.z())*((c.x()-a.x())*(d.y()-a.y())-(c.y()-a.y())*(d.x()-a.x()));
        check(Math.abs(volume)>0.1,"MJ世界位置不能共面");
        check(pictureAngle(1,0)==0&&pictureAngle(2,0)==45&&pictureAngle(2,1)==-45,"单图正上方与双图对称旋转");
        check(pictureTilt(1,0)==0&&pictureTilt(2,0)==15&&pictureTilt(2,1)==-15,"图片自身仅双张倾斜15度，位置规则独立");
        check(Cue.MJ.startMs==4720&&Cue.MJ.endMs-Cue.MJ.startMs==2000,"MJ保持原触发时刻，持续恰好两秒");
        check(active(6719/50.0)==Cue.MJ&&active(6720/50.0)==null,"MJ在两秒结束边界消失");
        check(pictureX(1,0)==0&&pictureY(1,0)==2.4,"单图必须位于世界竖直上方");
        check(pictureX(2,0)<0&&pictureX(2,1)>0&&Math.abs(pictureX(2,0)+pictureX(2,1))<1e-9&&pictureY(2,0)==pictureY(2,1),"双图位置绕中心左右对称");
        check(active(-1)==null&&active(Double.NaN)==null&&active(0)==null,"无效时钟与无卡点区间");
        System.out.println("MJ/表情包卡点、精确循环、淡出和稳定数量检查通过");
    }
    private static void check(boolean valid,String message) { if(!valid)throw new AssertionError(message); }
}
