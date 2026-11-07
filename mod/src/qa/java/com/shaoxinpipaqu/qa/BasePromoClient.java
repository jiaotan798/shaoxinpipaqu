package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.shaoxinpipaqu.*;
import com.shaoxinpipaqu.client.MemeTiming;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** 发布摄影只移动摄影机并驱动原版使用／跳跃输入；PNG直接取真实光影帧缓冲。 */
@Mod(value="pipa_qa",dist=Dist.CLIENT)
public final class BasePromoClient {
    private final String role=System.getProperty("pipa.qa.role");
    private final boolean viewer="viewer".equals(role);
    private int stage=-1,age;
    private boolean done,shaderChecked;
    private long segmentStart,videoStart,lastFrame;
    private int videoFrame;
    private final List<Map<String,Object>> photos=new ArrayList<>();
    private final StringBuilder times=new StringBuilder("frame,seconds,stage,phase_ms\n");
    private static final String[] NAMES={"","01-sakura-concert","02-mj-beat","03-big-dog","04-cabbage","05-courage","06-mob-orbit","07-lantern-night","08-wild-dog-milk","09-player-flight","10-food-stall","11-villager-trade","12-forgetful-noodles"};

    public BasePromoClient() {
        if(!Boolean.getBoolean("pipa.qa.promo"))return;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->tick());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e)->frame());
    }
    private Path folder(){return BaseCheck.ROOT.resolve("promo");}
    private void fail(Throwable error) {
        error.printStackTrace();BaseCheck.write(role+"-result.txt","FAIL: "+error);done=true;
        var mc=Minecraft.getInstance();mc.options.keyUse.setDown(false);mc.options.keyJump.setDown(false);mc.stop();
    }
    private void tick() {
        if(done)return;
        var mc=Minecraft.getInstance();
        try {
            if(mc.getWindow()==null)return;
            long window=mc.getWindow().getWindow();
            BaseCheck.require(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE
                    &&GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)==GLFW.GLFW_FALSE&&!mc.mouseHandler.isMouseGrabbed(),"发布摄影必须隐藏、无焦点且不捕获鼠标");
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)throw new AssertionError("摄影客户端断线");
            if(mc.level==null||mc.player==null||mc.getOverlay()!=null)return;
            Files.createDirectories(folder());
            if(!Files.exists(BaseCheck.ROOT.resolve(role+"-connected.ok")))BaseCheck.write(role+"-connected.ok","TCP connected; hidden frame buffer\n");
            int next=Integer.parseInt(Files.readString(BaseCheck.ROOT.resolve("stage.txt")).trim());
            BaseCheck.require(next!=-1,"摄影服务端失败");
            if(next==99) {
                mc.options.keyUse.setDown(false);mc.options.keyJump.setDown(false);
                BaseCheck.write(role+"-window.txt","PASS hidden/unfocused/no mouse capture throughout photography\n");
                BaseCheck.write(role+"-result.txt","PASS cherry garden photography; not a gameplay regression\n");done=true;mc.stop();return;
            }
            if(stage!=next) {
                stage=next;age=0;segmentStart=0;
                mc.options.keyUse.setDown(false);mc.options.keyJump.setDown(false);
                if(viewer&&stage!=11)mc.setScreen(null);
            }
            age++;
            if(stage<1)return;
            var scene=new com.google.gson.Gson().fromJson(Files.readString(BaseCheck.ROOT.resolve("promo-scene.json")),com.google.gson.JsonObject.class);
            BasePromoServer.X=scene.get("x").getAsInt();BasePromoServer.Y=scene.get("y").getAsInt();BasePromoServer.Z=scene.get("z").getAsInt();
            mc.options.hideGui=stage!=11;
            mc.options.fov().set(stage==11?70:58);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            // 两个客户端使用各自独立配置；仅摄影帧需要时隐藏卡点，不保存到其他实例。
            com.shaoxinpipaqu.client.ClientConfig.SHOW_CUE_TEXT.set(stage>=2&&stage<=5||stage==7||stage>=13);
            com.shaoxinpipaqu.client.ClientConfig.SHOW_MEME_PICTURES.set(stage>=2&&stage<=5||stage>=13);
            if(viewer) {
                if(stage==11) {
                    mc.options.guiScale().set(3);
                    if(mc.screen==null&&age%20==0) {
                        int id=Integer.parseInt(Files.readString(BaseCheck.ROOT.resolve("promo-trader.txt")).trim());
                        var trader=mc.level.getEntity(id);
                        if(trader!=null)mc.gameMode.interact(mc.player,trader,InteractionHand.MAIN_HAND);
                    }
                } else camera(mc);
            } else {
                // 使用完整原版食用时长，截图里的效果与飞行权限来自正式物品结算。
                boolean using=stage==8&&age>=140&&age<=176||stage==12&&(age>=70&&age<=106||age>=160&&age<=196);
                mc.options.keyUse.setDown(using);
                if(using&&(age==140||age==70||age==160))mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                if(stage==9||stage==15) {
                    if(mc.player.getAbilities().mayfly)mc.options.keyJump.setDown(age==30||age>=35&&age<65);
                }
                boolean ready=stage==8?age>190&&mc.player.hasEffect(Pipa.MILK):stage==12?age>205&&mc.player.hasEffect(Pipa.PEACE):age>40;
                if(ready)BaseCheck.write(role+"-"+stage+".ok","actor ready\n");
            }
        }catch(Throwable error){fail(error);}
    }
    private Player actor(Minecraft mc) {
        return mc.level.players().stream().filter(p->p.getGameProfile().getName().equals("PipaActor")).findFirst().orElse(null);
    }
    /** 使用原版旁观者摄影机；视频轨道按墙钟平滑前进，不能靠改动生物位置制造飞行。 */
    private void camera(Minecraft mc) throws Exception {
        double y=BasePromoServer.Y;
        Vec3 eye=new Vec3(-7.5,y+3.6,10),target=new Vec3(0.5,y+1.8,0.5);
        if(stage>=2&&stage<=5){eye=new Vec3(-5.5,y+3.5,8);target=new Vec3(.5,y+2.6,.5);}
        if(stage==6){eye=new Vec3(-11,y+9,13);target=new Vec3(.5,y+3,.5);}
        if(stage==7){eye=new Vec3(-7,y+3.3,10);target=new Vec3(.5,y+2,.5);}
        if(stage==8){eye=new Vec3(11,y+3.5,12);target=new Vec3(7,y+2.4,7);}
        if(stage==9||stage==15) {
            var player=actor(mc);if(player!=null) {
                double t=stage==15&&segmentStart!=0?(System.nanoTime()-segmentStart)/1e9:0;
                eye=player.position().subtract(BasePromoServer.X,0,BasePromoServer.Z).add(6*Math.cos(t*.045),2.1,7*Math.sin(t*.045)+7);
                target=player.position().subtract(BasePromoServer.X,0,BasePromoServer.Z).add(0,.8,0);
            }
        }
        if(stage==10){eye=new Vec3(-.7,y+2.5,2.8);target=new Vec3(-4.7,y+1.9,-4.4);}
        if(stage==12){eye=new Vec3(7,y+2.6,7);target=new Vec3(2.5,y+1.3,-.2);}
        if(stage==13){double t=segmentStart==0?0:(System.nanoTime()-segmentStart)/1e9;double a=.60+t*.025;eye=new Vec3(-16*Math.sin(a),y+4.2,16*Math.cos(a));target=new Vec3(.5,y+2,.5);}
        if(stage==14){eye=new Vec3(-5,y+3.4,8.5);target=new Vec3(.5,y+2.6,.5);}
        if(stage==16){double t=segmentStart==0?0:(System.nanoTime()-segmentStart)/1e9;eye=new Vec3(-10+t*.25,y+3.3,13);target=new Vec3(.5,y+2,.5);}
        // 本轮文件只调整摄影机；复拍不修改玩法，也不重新生成布景。
        Path controls=BaseCheck.ROOT.resolve("promo-camera.json");
        if(Files.exists(controls)) {
            var all=new com.google.gson.Gson().fromJson(Files.readString(controls),com.google.gson.JsonObject.class);
            if(all.has(Integer.toString(stage))) {
                var shot=all.getAsJsonObject(Integer.toString(stage));var e=shot.getAsJsonArray("eye");var t=shot.getAsJsonArray("target");
                eye=new Vec3(e.get(0).getAsDouble(),y+e.get(1).getAsDouble(),e.get(2).getAsDouble());
                target=new Vec3(t.get(0).getAsDouble(),y+t.get(1).getAsDouble(),t.get(2).getAsDouble());
                if(shot.has("endEye")&&segmentStart!=0) {
                    var end=shot.getAsJsonArray("endEye");double progress=Math.clamp((System.nanoTime()-segmentStart)/10e9,0,1);
                    eye=eye.lerp(new Vec3(end.get(0).getAsDouble(),y+end.get(1).getAsDouble(),end.get(2).getAsDouble()),progress);
                }
            }
        }
        eye=eye.add(BasePromoServer.X,0,BasePromoServer.Z);target=target.add(BasePromoServer.X,0,BasePromoServer.Z);
        mc.player.setPos(eye.x,eye.y-mc.player.getEyeHeight(),eye.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        Vec3 d=target.subtract(eye);
        float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
    }
    private double phase(Minecraft mc) {
        var be=mc.level.getBlockEntity(new BlockPos(BasePromoServer.X,BasePromoServer.Y+1,BasePromoServer.Z));
        if(!(be instanceof DollBlockEntity doll)||!doll.isPlaying())return -1;
        return MemeTiming.phaseMs(doll.getElapsedTicks(mc.level.getGameTime())+mc.getTimer().getGameTimeDeltaPartialTick(false));
    }
    private void shader() throws Exception {
        var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=type.getMethod("getInstance").invoke(null);
        var iris=Class.forName("net.irisshaders.iris.Iris");
        var data=new LinkedHashMap<String,Object>();
        data.put("enabled",type.getMethod("isShaderPackInUse").invoke(api));data.put("pack",iris.getMethod("getCurrentPackName").invoke(null));
        data.put("fallback",iris.getMethod("isFallback").invoke(null));data.put("error",iris.getMethod("getStoredError").invoke(null).toString());
        BaseCheck.require(Boolean.TRUE.equals(data.get("enabled"))&&Boolean.FALSE.equals(data.get("fallback"))&&"Optional.empty".equals(data.get("error")),"摄影必须实际启用光影且无回退");
        BaseCheck.write(role+"-shader.json",new com.google.gson.Gson().toJson(data));shaderChecked=true;
    }
    private void frame() {
        if(done||!viewer||stage<1||stage>16)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||mc.getOverlay()!=null||stage!=11&&mc.screen!=null)return;
        try {
            if(age<20&&stage<13)return;
            if(!shaderChecked)shader();
            BaseCheck.require(mc.getMainRenderTarget().width==1920&&mc.getMainRenderTarget().height==1080,"发布图必须为1920×1080真实帧缓冲");
            double phase=phase(mc);
            if(stage>=13) {
                // 保存逐帧实际呈现时间；后期按这些时间编码，掉帧不能通过加速游戏画面掩盖。
                long now=System.nanoTime();
                if(segmentStart==0)segmentStart=now;
                if(videoStart==0)videoStart=now;
                if(now-lastFrame>=66_000_000L) {
                    Path frames=folder().resolve("video-frames");Files.createDirectories(frames);
                    try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(frames.resolve(String.format(Locale.ROOT,"%06d.png",videoFrame)));}
                    times.append(videoFrame++).append(',').append((now-videoStart)/1e9).append(',').append(stage).append(',').append(phase).append('\n');
                    lastFrame=now;
                }
                if(now-segmentStart>=10_000_000_000L) {
                    Files.writeString(folder().resolve("video-frames.csv"),times);
                    BaseCheck.write(role+"-"+stage+".ok","10 seconds native rendered video\n");
                }
                return;
            }
            if(Files.exists(BaseCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
            boolean ready=switch(stage) {
                case 1 -> age>=240;
                case 2,7 -> phase>=4950&&phase<=6300&&age>=60;
                case 3 -> phase>=15050&&phase<=15550;
                case 4 -> phase>=18000&&phase<=18400;
                case 5 -> phase>=24600&&phase<=25600;
                case 6 -> age>=180;
                case 8 -> age>140&&actor(mc)!=null&&actor(mc).isUsingItem();
                case 9 -> age>=100&&actor(mc)!=null&&MobFlight.posed(actor(mc));
                case 10 -> age>=100;
                case 11 -> age>=60&&mc.screen instanceof MerchantScreen;
                case 12 -> age>=160&&actor(mc)!=null&&actor(mc).isUsingItem()&&Files.exists(BaseCheck.ROOT.resolve("promo-peace.txt"));
                default -> false;
            };
            if(!ready)return;
            String name=NAMES[stage]+".png";
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(folder().resolve(name));}
            var detail=new LinkedHashMap<String,Object>();detail.put("file",name);detail.put("stage",stage);detail.put("phaseMs",phase);detail.put("width",1920);detail.put("height",1080);
            detail.put("camera",mc.gameRenderer.getMainCamera().getPosition().toString());detail.put("dayTime",mc.level.getDayTime());photos.add(detail);
            Files.writeString(folder().resolve("screenshots.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(photos));
            BaseCheck.write(role+"-"+stage+".ok","native 1920x1080 shader screenshot "+name+"\n");
        }catch(Throwable error){fail(error);}
    }
}
