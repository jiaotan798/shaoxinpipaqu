package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.friendswine.client.DollMusicSound;
import com.mojang.blaze3d.platform.NativeImage;
import com.shaoxinpipaqu.AudioInfo;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.RemoteControl;
import com.shaoxinpipaqu.SongState;
import com.shaoxinpipaqu.client.MemeTiming;
import com.shaoxinpipaqu.qa.mixin.PcmOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** 每个视角检查真实状态同步、声音实例、模型角度和 GPU 帧缓冲。 */
public final class PortClientCheck {
    public static final PortClientCheck INSTANCE=new PortClientCheck();
    private final String role=System.getProperty("pipa.qa.role");
    private final EnumSet<MemeTiming.Cue> shots=EnumSet.noneOf(MemeTiming.Cue.class);
    private final Map<MemeTiming.Cue,Set<Integer>> palettes=new EnumMap<>(MemeTiming.Cue.class);
    private DollMusicSound song;
    private CompletableFuture<Void> reload;
    private final AtomicInteger pcmSamples=new AtomicInteger();
    private volatile String pcmFailure;
    private int stage,age,usedAt,pcmAtReload;
    private boolean done,model,pending,joined,reloadedMj;
    private CloudPair cloudPair;
    private double previousCloudAttempt=-20;
    private boolean cloudProof;

    private PortClientCheck() {}
    public void track(DollMusicSound sound) { song=sound; }
    public void modelPose() { if(stage==2)model=true; }

    public void pcm(AudioStream stream,long elapsed) {
        try {
            long frame=((PcmOffset)stream).probe$skippedBytes()/stream.getFormat().getFrameSize()%AudioInfo.FRAMES;
            long expected=Math.floorMod(elapsed*(AudioInfo.SAMPLE_RATE/20),AudioInfo.FRAMES);
            long difference=Math.abs(frame-expected);
            difference=Math.min(difference,AudioInfo.FRAMES-difference);
            PortCheck.require(difference<=AudioInfo.SAMPLE_RATE/10,"PCM 定位偏差超过 100 ms："+difference);
            PortCheck.write(role+"-pcm-"+pcmSamples.incrementAndGet()+".txt","elapsedTicks="+elapsed+" actualFrame="+frame+" expectedFrame="+expected+" differenceFrames="+difference);
        } catch(Throwable failure) { pcmFailure=failure.toString(); }
    }

    void tick(Minecraft mc) {
        if(done)return;
        try {
            PortCheck.require(pcmFailure==null,pcmFailure);
            long window=mc.getWindow().handle();
            PortCheck.require(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE
                    &&GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)==GLFW.GLFW_FALSE
                    &&!mc.mouseHandler.isMouseGrabbed(),"后台窗口约束失效");
            if(mc.screen instanceof DisconnectedScreen&&joined)throw new AssertionError("联机期间断线");
            if(mc.level==null||mc.player==null)return;
            joined=true;
            PortCheck.require(mc.getSingleplayerServer()==null,"验收必须使用独立专服");
            String state=PortCheck.read("stage.txt");
            if(state.isEmpty())return;
            int next=Integer.parseInt(state);
            if(stage!=next){stage=next;age=0;usedAt=0;}
            if(stage==-1)throw new AssertionError("专服验收失败");
            if(stage==99){ finish(mc,"PASS 独立专服、隐藏窗口、音频、四个卡点和原版模型");return; }
            age++;
            boolean actor=role.equals("actor");
            if(stage<=3) {
                var delta=new Vec3(0.5,83.7,0.5).subtract(mc.player.getEyePosition());
                mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
                mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
            }
            if(actor)mc.options.keyAttack.setDown((stage==1&&PortCheck.read("viewer-1.ok").startsWith("PASS")
                    &&PortCheck.read("actor-1.ok").startsWith("PASS")||stage==2&&age<=20)
                    &&mc.screen==null&&mc.getOverlay()==null&&RemoteControl.held(mc.player));
            if(stage==1&&playing(mc,"friendswine","doll_music"))ack(1,"原曲声音实例已播放");
            if(stage==2&&model&&cloudProof&&shots.size()==MemeTiming.Cue.values().length&&pcmSamples.get()>0&&playing(mc,"shaoxinpipaqu","pipa"))
                ack(2,"琵琶曲音频、四张帧缓冲、前景图片遮住远云和实际飞行关节");
            if(stage==3) {
                if(reload==null) {
                    pcmAtReload=pcmSamples.get();
                    reload=mc.reloadResourcePacks();
                }
                if(!reload.isDone())return;
                reload.join();
                if(!reloadedMj||pcmSamples.get()<=pcmAtReload||!playing(mc,"shaoxinpipaqu","pipa"))return;
                use(mc,actor&&RemoteControl.held(mc.player));
                if(mc.level.getBlockEntity(PortCheck.DOLL) instanceof DollBlockEntity d&&d.getMode()==DollBlockEntity.ORBIT)
                    ack(3,"资源重载后重新绘制 MJ 并恢复音频，遥控改为第三档");
            }
            if(stage==4) {
                use(mc,actor&&mc.player.getMainHandItem().is(Pipa.DOG_MILK.get()));
                if(actor&&usedAt!=0) {
                    int elapsed=age-usedAt;
                    mc.options.keyJump.setDown(elapsed==45||elapsed>=50&&elapsed<60);
                }
                var subject=actor?mc.player:mc.level.players().stream()
                        .filter(player->player.getGameProfile().name().equals("PipaActor")).findFirst().orElse(null);
                if(subject!=null&&MobFlight.posed(subject))ack(4,"玩家狗奶飞行姿势同步");
            }
            if(stage==5) {
                var subject=actor?mc.player:mc.level.players().stream()
                        .filter(player->player.getGameProfile().name().equals("PipaActor")).findFirst().orElse(null);
                if(subject!=null&&!MobFlight.posed(subject)&&subject.onGround())ack(5,"飞行姿势清理且落地");
            }
            if(stage==6) {
                use(mc,actor&&mc.player.getMainHandItem().is(Pipa.NOODLES.get()));
                if(!actor||mc.player.hasEffect(Pipa.PEACE)&&mc.player.getMainHandItem().is(Items.BOWL))
                    ack(6,actor?"牛肉面效果和返碗已同步":"旁观者仍在真实专服");
            }
            if(actor&&stage!=3&&stage!=4&&stage!=6)mc.options.keyUse.setDown(false);
            if(actor&&stage!=4)mc.options.keyJump.setDown(false);
        } catch(Throwable failure) { finish(mc,"FAIL "+failure); }
    }

    private boolean playing(Minecraft mc,String namespace,String path) {
        return song!=null&&song.getIdentifier().equals(Identifier.fromNamespaceAndPath(namespace,path))
                &&mc.getSoundManager().isActive(song);
    }

    private void use(Minecraft mc,boolean heldItem) {
        if(usedAt==0&&heldItem&&mc.screen==null&&mc.getOverlay()==null) {
            mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            usedAt=age;
        }
        if(role.equals("actor"))mc.options.keyUse.setDown(usedAt!=0&&age-usedAt<40);
    }

    /** 抓图在 GPU 读回结束时才确认；不能把卡点时钟命中视为画面通过。 */
    public void frame(float partial) {
        var mc=Minecraft.getInstance();
        boolean afterReload=stage==3&&reload!=null&&reload.isDone()&&!reload.isCompletedExceptionally()&&!reloadedMj;
        if(done||stage!=2&&!afterReload||pending||mc.level==null||mc.player==null||mc.getOverlay()!=null)return;
        try {
            if(!(mc.level.getBlockEntity(PortCheck.DOLL) instanceof DollBlockEntity d)
                    ||!d.isPlaying()||!SongState.of(d).pipa$isSelected())return;
            double ticks=d.getElapsedTicks(mc.level.getGameTime())+partial;
            var cue=MemeTiming.active(ticks);
            double phase=MemeTiming.phaseMs(ticks);
            if(cue==null||(afterReload?cue!=MemeTiming.Cue.MJ:shots.contains(cue))||MemeTiming.alpha(cue,phase)<0.99
                    ||cue==MemeTiming.Cue.MJ&&phase<cue.startMs+300)return;
            pending=true;
            Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{
                try(image) {
                    int pixels=coloredPixels(mc,image,cue);
                    PortCheck.require(pixels>=30,cue+" 未在帧缓冲看到足够特征颜色："+pixels);
                    String suffix=cue.name().toLowerCase(java.util.Locale.ROOT)+(afterReload?"-reload":"");
                    image.writeToFile(PortCheck.ROOT.resolve(role+"-"+suffix+".png"));
                    if(afterReload)reloadedMj=true;else shots.add(cue);
                    PortCheck.write(role+"-pixels-"+suffix+".txt",Integer.toString(pixels));
                } catch(Throwable failure) { finish(mc,"FAIL "+failure); }
                finally { pending=false; }
            });
        } catch(Throwable failure) { finish(mc,"FAIL "+failure); }
    }

    /** 对齐同一帧拍摄，选完全不透明的单张图片，避免旋转、淡入淡出和视角移动干扰像素对照。 */
    public void beforeCards(float partial) {
        var mc=Minecraft.getInstance();
        if(done||stage!=2||cloudProof||cloudPair!=null||mc.level==null||mc.getOverlay()!=null)return;
        try {
            PortCheck.require(mc.options.getCloudStatus()!=CloudStatus.OFF,"云层对照需要开启云层");
            if(!(mc.level.getBlockEntity(PortCheck.DOLL) instanceof DollBlockEntity d)
                    ||!d.isPlaying()||!SongState.of(d).pipa$isSelected())return;
            double ticks=d.getElapsedTicks(mc.level.getGameTime())+partial;
            var cue=MemeTiming.active(ticks);
            if(cue==null||cue==MemeTiming.Cue.MJ||ticks-previousCloudAttempt<5
                    ||MemeTiming.alpha(cue,MemeTiming.phaseMs(ticks))<0.999
                    ||MemeTiming.count(d.getBlockPos().asLong(),d.getStartTick(),MemeTiming.cycle(ticks),cue)!=1)return;
            previousCloudAttempt=ticks;
            var pair=new CloudPair(cue);
            cloudPair=pair;
            Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{ pair.before=image; compareCloud(pair); });
        } catch(Throwable failure) { finish(mc,"FAIL "+failure); }
    }

    public void afterCards() {
        var pair=cloudPair;
        if(pair==null||pair.afterRequested)return;
        pair.afterRequested=true;
        var mc=Minecraft.getInstance();
        try {
            Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{ pair.after=image; compareCloud(pair); });
        } catch(Throwable failure) { finish(mc,"FAIL "+failure); }
    }

    private void compareCloud(CloudPair pair) {
        if(pair.before==null||pair.after==null)return;
        try(var before=pair.before;var after=pair.after) {
            int w=before.getWidth(),h=before.getHeight();
            PortCheck.require(w==after.getWidth()&&h==after.getHeight(),"同帧云层对照尺寸不一致");
            int[] scene=before.getPixels(),result=after.getPixels();
            int left=w,right=-1,top=h,bottom=-1;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int offset=y*w+x;
                if(colorDistance(scene[offset],result[offset])<8)continue;
                left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
            }
            if(right-left<45||bottom-top<45)return;
            // 只看矩形内部，四边抗锯齿及个别与背景同色的图片像素不应影响通过条件。
            int borderX=(right-left)/8,borderY=(bottom-top)/8;
            int overlap=0,covered=0,changed=0,total=0;
            for(int y=top+borderY;y<=bottom-borderY;y++)for(int x=left+borderX;x<=right-borderX;x++) {
                int offset=y*w+x,bg=scene[offset];
                boolean painted=colorDistance(bg,result[offset])>=8;
                total++;
                if(painted)changed++;
                int r=bg>>16&255,g=bg>>8&255,b=bg&255;
                // 白昼测试布景的云为低饱和蓝灰；排除淡蓝天空及灰色地板。
                if(r>=120&&g-r>=3&&b-g>=5&&b>=175&&b<=246&&b-r<=55) {
                    overlap++;
                    if(painted)covered++;
                }
            }
            String stats=pair.cue+" cloudPixels="+overlap+" coveredCloudPixels="+covered+" paintedInterior="+changed+"/"+total;
            PortCheck.write(role+"-cloud-last.txt",stats);
            if(overlap<80||covered*100<overlap*96||changed*100<total*96)return;
            before.writeToFile(PortCheck.ROOT.resolve(role+"-cloud-before.png"));
            after.writeToFile(PortCheck.ROOT.resolve(role+"-cloud-after.png"));
            PortCheck.write(role+"-cloud-proof.txt","PASS "+stats);
            cloudProof=true;
        } catch(Throwable failure) {
            finish(Minecraft.getInstance(),"FAIL "+failure);
        } finally { cloudPair=null; }
    }

    private static int colorDistance(int a,int b) {
        return Math.max(Math.abs((a>>16&255)-(b>>16&255)),
                Math.max(Math.abs((a>>8&255)-(b>>8&255)),Math.abs((a&255)-(b&255))));
    }

    private static final class CloudPair {
        final MemeTiming.Cue cue;
        NativeImage before,after;
        boolean afterRequested;
        CloudPair(MemeTiming.Cue cue) { this.cue=cue; }
    }

    private int coloredPixels(Minecraft mc,NativeImage image,MemeTiming.Cue cue) throws java.io.IOException {
        Set<Integer> palette;
        if(cue==MemeTiming.Cue.MJ) {
            palette=java.util.stream.IntStream.range(0,6).map(i->MemeTiming.mjColor(i)&0xFFFFFF).boxed().collect(Collectors.toSet());
        } else {
            palette=palettes.get(cue);
            if(palette==null) {
                String name=switch(cue){case DOG->"big_dog";case CABBAGE->"cabbage";case COURAGE->"courage";default->throw new IllegalStateException();};
                var id=Identifier.fromNamespaceAndPath("shaoxinpipaqu","textures/meme/"+name+".png");
                try(var input=mc.getResourceManager().getResourceOrThrow(id).open();var original=NativeImage.read(input)) {
                    Map<Integer,Integer> histogram=new java.util.HashMap<>();
                    for(int y=0;y<original.getHeight();y++)for(int x=0;x<original.getWidth();x++) {
                        int c=original.getPixel(x,y);
                        int r=c>>16&255,g=c>>8&255,b=c&255;
                        if((c>>>24)>240&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>35)
                            histogram.merge(c&0xF0F0F0,1,Integer::sum);
                    }
                    palette=histogram.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(16).map(Map.Entry::getKey).collect(Collectors.toSet());
                }
                palettes.put(cue,palette);
            }
        }
        int pixels=0,mask=cue==MemeTiming.Cue.MJ?0xFFFFFF:0xF0F0F0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(palette.contains(image.getPixel(x,y)&mask))pixels++;
        return pixels;
    }

    private void ack(int value,String text) throws java.io.IOException {
        String file=role+"-"+value+".ok";
        if(PortCheck.read(file).isEmpty())PortCheck.write(file,"PASS "+text);
    }

    private void finish(Minecraft mc,String result) {
        if(done)return;
        done=true;
        PortCheck.write(role+"-result.txt",result);
        mc.stop();
    }
}
