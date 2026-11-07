package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import com.shaoxinpipaqu.SongState;
import com.shaoxinpipaqu.client.MemeTiming;
import com.shaoxinpipaqu.client.ClientConfig;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** 两端对实际绘制调用计数并截图，阶段文件仅作屏障；窗口和静音沿用已有后台入口。 */
@Mod(value="pipa_qa",dist=Dist.CLIENT)
public final class BaseMemeClient {
    private static int mj,pictures;
    private static MemeTiming.Cue drawn;
    private static float alpha;
    private final String role=System.getProperty("pipa.qa.role");
    private final Set<String> shots=new HashSet<>();
    private final StringBuilder rows=new StringBuilder("stage,phase_ms,cycle,cue,count,alpha\n");
    private int stage=-1,age;
    private boolean done;
    private DollBlockEntity layoutDoll;
    private long layoutStart,layoutCycle;
    private MemeTiming.MjPlacement[] previousLayout;
    private int layoutFrames;
    private final BaseConfigScreenCheck config=new BaseConfigScreenCheck();
    private boolean startupChecked,startupReady,configured,mjChecked,pictureChecked;
    public BaseMemeClient() {
        if(!Boolean.getBoolean("pipa.qa.memeOnly"))return;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->tick());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre e)->{mj=0;pictures=0;drawn=null;});
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e)->frame());
    }
    public static void mj() {mj++;drawn=MemeTiming.Cue.MJ;alpha=1;}
    public static void picture(MemeTiming.Cue cue,float opacity) {pictures++;drawn=cue;alpha=opacity;}
    private void fail(Throwable error) {error.printStackTrace();BaseCheck.write(role+"-result.txt","FAIL: "+error);done=true;Minecraft.getInstance().stop();}
    private void shot(String name) throws Exception {
        if(!shots.add(name))return;
        if(!Files.exists(BaseCheck.ROOT.resolve(role+"-shader.json")))shader();
        var mc=Minecraft.getInstance();try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(BaseCheck.ROOT.resolve(role+"-meme-"+name+".png"));}
        BaseCheck.write(role+"-meme-frames.csv",rows.toString());
    }
    /** 沿用完整回归的Iris实际状态核验，不能仅凭配置文件宣称光影已启用。 */
    private void shader() throws Exception {
        var data=new java.util.LinkedHashMap<String,Object>();
        try {
            var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=type.getMethod("getInstance").invoke(null);
            data.put("enabled",type.getMethod("isShaderPackInUse").invoke(api));
            var iris=Class.forName("net.irisshaders.iris.Iris");data.put("pack",iris.getMethod("getCurrentPackName").invoke(null));
            data.put("fallback",iris.getMethod("isFallback").invoke(null));data.put("error",iris.getMethod("getStoredError").invoke(null).toString());
            BaseCheck.require(Boolean.FALSE.equals(data.get("fallback"))&&data.get("error").equals("Optional.empty"),"光影不得回退或报错");
        }catch(ClassNotFoundException absent){data.put("enabled",false);data.put("pack","vanilla");}
        BaseCheck.require(data.get("enabled").equals(Boolean.getBoolean("pipa.qa.shader")),"光影实际启用状态");
        BaseCheck.write(role+"-shader.json",new com.google.gson.Gson().toJson(data));
    }
    private void tick() {
        if(done)return;var mc=Minecraft.getInstance();
        try {
            if(mc.getWindow()==null)return;
            if(!Boolean.getBoolean("pipa.qa.foreground"))BaseCheck.require(GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE&&GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(),GLFW.GLFW_FOCUSED)==GLFW.GLFW_FALSE&&!mc.mouseHandler.isMouseGrabbed(),"后台窗口约束");
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)throw new AssertionError("卡点专项断线");
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null)return;
            BaseCheck.require(mc.getSingleplayerServer()==null,"必须真实TCP连接");
            if(!startupChecked) {
                boolean expected=!Boolean.getBoolean("pipa.qa.configReload");
                BaseCheck.require(ClientConfig.SHOW_CUE_TEXT.get()==expected&&ClientConfig.SHOW_MEME_PICTURES.get()==expected,"首次启动默认开启；复用真实保存文件重启后保持关闭");
                BaseCheck.write(role+"-config-startup.txt","PASS loaded text="+expected+" pictures="+expected+"\n");startupChecked=true;
            }
            if(!startupReady) {startupReady=config.apply("startup",true,true);if(!startupReady)return;}
            if(!Files.exists(BaseCheck.ROOT.resolve(role+"-connected.ok")))BaseCheck.write(role+"-connected.ok","TCP connected\n");
            int next=Integer.parseInt(Files.readString(BaseCheck.ROOT.resolve("stage.txt")).trim());BaseCheck.require(next!=-1,"服务端失败");
            if(next==99){BaseCheck.write(role+"-window.txt","PASS hidden/unfocused/no mouse capture every tick\n");BaseCheck.write(role+"-result.txt","PASS actual render, native config UI, independent toggles, cache clear, save and reload\n");done=true;mc.stop();return;}
            if(stage!=next){stage=next;age=0;configured=false;mjChecked=false;pictureChecked=false;}age++;mc.options.hideGui=true;
            // 先验证原曲无卡点，再按住切歌键跨越切歌阶段，覆盖长按去抖。
            if(role.equals("actor"))mc.options.keyAttack.setDown(
                    (stage==3&&Files.exists(BaseCheck.ROOT.resolve("actor-3.ok"))||stage==4&&age<=20)
                    &&mc.player.getMainHandItem().is(FriendsWine.REMOTE.get()));
            if(stage>=6&&stage<=9) {
                // 6两端各关一项；7演员全关/观察者全开；8互换；9全关保存供新JVM读取。
                boolean actor=role.equals("actor");
                boolean text=stage==6?!actor:stage==7?!actor:stage==8?actor:false;
                boolean pictures=stage==6?actor:text;
                configured=config.apply("stage-"+stage,text,pictures);
                if(!configured)return;
                if(stage==9)BaseCheck.write(role+"-9.ok","PASS both disabled and saved for next process\n");
            }
            if(mc.screen!=null)return;
            var delta=new Vec3(0.5,83.7,0.5).subtract(mc.player.getEyePosition());
            mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        }catch(Throwable error){fail(error);}
    }
    private void frame() {
        if(done||stage<1||age<10)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getOverlay()!=null||mc.screen!=null)return;
        try {
            var entity=mc.level.getBlockEntity(new BlockPos(0,81,0));
            if(stage==2||stage==3||stage==5) {
                if(age<40||Files.exists(BaseCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
                BaseCheck.require(mj==0&&pictures==0,"停止/原曲/拆除后不得绘制卡点");
                BaseCheck.require(layouts().isEmpty(),"停止/原曲/拆除后须清理布局缓存");
                BaseCheck.write(role+"-layout-cache.txt","PASS reused frames="+layoutFrames+"; cleared stage="+stage+"\n");
                shot("cleared-"+stage);BaseCheck.write(role+"-"+stage+".ok","PASS no draw\n");return;
            }
            if(!(entity instanceof DollBlockEntity doll)||!SongState.of(doll).pipa$isSelected()||!doll.isPlaying())return;
            double ticks=doll.getElapsedTicks(mc.level.getGameTime())+mc.getTimer().getGameTimeDeltaPartialTick(false);
            long cycle=MemeTiming.cycle(ticks);double phase=MemeTiming.phaseMs(ticks);
            if(stage>=6&&stage<=8) {
                if(!configured)return;
                var expected=MemeTiming.active(ticks);
                if(expected==null)return;
                double progress=(phase-expected.startMs)/(expected.endMs-expected.startMs);
                if(progress<0.3||progress>0.7)return;
                boolean text=ClientConfig.SHOW_CUE_TEXT.get(),pics=ClientConfig.SHOW_MEME_PICTURES.get();
                if(expected==MemeTiming.Cue.MJ) {
                    BaseCheck.require(mj==(text?6:0),"文字开关控制实际MJ绘制");
                    if(!text)BaseCheck.require(layouts().isEmpty(),"关闭文字须释放布局缓存");
                    mjChecked=true;
                } else {
                    int n=MemeTiming.count(doll.getBlockPos().asLong(),doll.getStartTick(),cycle,expected);
                    BaseCheck.require(pictures==(pics?n:0),"图片开关控制实际表情包绘制");pictureChecked=true;
                }
                shot("settings-"+stage+"-"+expected);
                if(mjChecked&&pictureChecked)BaseCheck.write(role+"-"+stage+".ok","PASS live rendering text="+text+" pictures="+pics+"; independent local controls\n");
                return;
            }
            if(drawn!=null) {
                int n=drawn==MemeTiming.Cue.MJ?mj:pictures;
                int expected=MemeTiming.count(doll.getBlockPos().asLong(),doll.getStartTick(),cycle,drawn);
                BaseCheck.require(n==expected,"实际渲染数量必须匹配同步抽选："+drawn+" "+n+"/"+expected);
                rows.append(stage).append(',').append(phase).append(',').append(cycle).append(',').append(drawn).append(',').append(n).append(',').append(alpha).append('\n');
                if(drawn==MemeTiming.Cue.MJ){
                    // 读取成品真实缓存，不另造布局；同一触发的绘制帧必须复用同一数组。
                    var cache=layouts().get(doll);
                    BaseCheck.require(cache!=null,"实际MJ绘制必须使用布局缓存");
                    var layout=cache.get(doll.getBlockPos().asLong(),doll.getStartTick(),cycle);
                    if(layoutDoll==doll&&layoutStart==doll.getStartTick()&&layoutCycle==cycle) {
                        BaseCheck.require(layout==previousLayout,"同次触发不能逐帧重新计算布局");layoutFrames++;
                    }
                    layoutDoll=doll;layoutStart=doll.getStartTick();layoutCycle=cycle;previousLayout=layout;
                    shot(stage==4?"MJ-restart":cycle==0?"MJ":"MJ-loop");
                    // 捕获回弹起步与归位后的真实画面，不能只用第一帧证明动画完成。
                    double mjAge=phase-MemeTiming.Cue.MJ.startMs;
                    if(stage==1&&mjAge<60)shot("MJ-pop");
                    if(stage==1&&mjAge>=140)shot("MJ-settled");
                }
                else if(alpha>=0.99F)shot(drawn+"-peak");
                else if(alpha>0.15F&&alpha<0.4F) {
                    double progress=(phase-drawn.startMs)/(drawn.endMs-drawn.startMs);
                    if(progress<0.25)shot(drawn+"-in");else if(progress>0.75)shot(drawn+"-fade");
                }
            }
            if(stage==1&&shots.containsAll(Set.of("MJ","MJ-loop","MJ-pop","MJ-settled","DOG-in","DOG-peak","DOG-fade","CABBAGE-in","CABBAGE-peak","CABBAGE-fade","COURAGE-in","COURAGE-peak","COURAGE-fade")))BaseCheck.write(role+"-1.ok","PASS 4 cues, fade in/out and next PCM loop\n");
            if(stage==4&&shots.contains("MJ-restart"))BaseCheck.write(role+"-4.ok","PASS switched back to pipa and restarted cue\n");
        }catch(Throwable error){fail(error);}
    }

    /** 反射只存在于独立QA包，成品不为验收暴露内部缓存或计数器。 */
    @SuppressWarnings("unchecked")
    private static java.util.Map<DollBlockEntity,MemeTiming.LayoutCache> layouts() throws ReflectiveOperationException {
        var field=com.shaoxinpipaqu.client.MemeEffects.class.getDeclaredField("LAYOUTS");field.setAccessible(true);
        return (java.util.Map<DollBlockEntity,MemeTiming.LayoutCache>)field.get(null);
    }
}
