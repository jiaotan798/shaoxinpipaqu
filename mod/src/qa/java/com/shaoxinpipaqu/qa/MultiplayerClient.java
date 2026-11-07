package com.shaoxinpipaqu.qa;

import com.google.gson.Gson;
import com.shaoxinpipaqu.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/** 双端只读取 TCP 同步与实际 GPU 画面；阶段文件用于屏障，不替代玩法网络。 */
final class MultiplayerClient {
    private final String role=System.getProperty("pipa.qa.role");
    private int stage=-1,age;
    private boolean done;
    private boolean disconnected;
    private long reconnectAt;
    private net.minecraft.client.multiplayer.ServerData reconnectServer;
    private java.util.concurrent.CompletableFuture<Void> reload;
    private com.friendswine.client.DollMusicSound song,beforeReload;
    private int audioSamples;
    private boolean respawnRequested;
    private boolean foodRequested,foodCancelled;
    private int foodCompletedAt;
    private final StringBuilder foodRows=new StringBuilder("stage,age,mode,using,remaining,noodles,peace\n");
    private final FlightMotionProbe motion=new FlightMotionProbe();
    private final EffectIconProbe effects=new EffectIconProbe();
    private final FrameTimingProbe frameTiming=new FrameTimingProbe();
    MultiplayerClient() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->tick());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderFrameEvent.Post e)->frameTiming.frame(stage==53&&age>100));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.sound.PlaySoundEvent e)->{
            if(e.getOriginalSound() instanceof com.friendswine.client.DollMusicSound d)song=d;
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderLivingEvent.Post<?,?> e)->PoseProbe.standard(e.getEntity(),e.getRenderer().getModel()));
    }
    private void release(Minecraft mc) {mc.options.keyJump.setDown(false);mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyUse.setDown(false);}
    private void look(Minecraft mc,Vec3 p) {var d=p.subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));}
    private void screenshot(Minecraft mc) throws Exception {try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {image.writeToFile(MultiplayerCheck.ROOT.resolve(role+"-stage-"+stage+".png"));}}
    private void shader() throws Exception {
        Map<String,Object> data=new LinkedHashMap<>();
        try {
            var type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");var api=type.getMethod("getInstance").invoke(null);
            data.put("enabled",type.getMethod("isShaderPackInUse").invoke(api));
            var iris=Class.forName("net.irisshaders.iris.Iris");data.put("pack",iris.getMethod("getCurrentPackName").invoke(null));data.put("fallback",iris.getMethod("isFallback").invoke(null));data.put("error",iris.getMethod("getStoredError").invoke(null).toString());
            MultiplayerCheck.require(Boolean.FALSE.equals(data.get("fallback")) && data.get("error").equals("Optional.empty"),"Iris无错误及回退："+data);
        }catch(ClassNotFoundException absent){data.put("enabled",false);data.put("pack","vanilla");}
        MultiplayerCheck.require(data.get("enabled").equals(Boolean.getBoolean("pipa.qa.shader")),"实际光影启用状态："+data);
        MultiplayerCheck.write(role+"-shader.json",new Gson().toJson(data));
    }
    private void tick() {
        if(done)return;var mc=Minecraft.getInstance();
        try {
            if(mc.player!=null&&!mc.player.isAlive()&&!respawnRequested&&Files.readString(MultiplayerCheck.ROOT.resolve("stage.txt")).trim().equals("58")) {
                respawnRequested=true;
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));return;
            }
            if(reconnectAt>0 && System.currentTimeMillis()>=reconnectAt) {
                reconnectAt=0;
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),mc,net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(reconnectServer.ip),reconnectServer,false,null);
                return;
            }
            if(mc.getWindow()==null || mc.getMainRenderTarget()==null)return;
            // 隐藏而非最小化，保留真实尺寸的帧缓冲；禁止靠弹出窗口修复渲染异常。
            long window=mc.getWindow().getWindow();
            if(!Boolean.getBoolean("pipa.qa.foreground")) {
                MultiplayerCheck.require(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)==GLFW.GLFW_FALSE,"后台窗口必须保持隐藏");
                MultiplayerCheck.require(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)==GLFW.GLFW_FALSE&&!mc.mouseHandler.isMouseGrabbed(),"后台客户端不得占用焦点或捕获鼠标");
                if(!Files.exists(MultiplayerCheck.ROOT.resolve(role+"-window.txt")))MultiplayerCheck.write(role+"-window.txt","PASS hidden, unfocused, mouse not grabbed; checked each client tick\n");
            }
            if(mc.getMainRenderTarget().width<900 || mc.getMainRenderTarget().height<650) {mc.getWindow().setWindowed(1000,760);GLFW.glfwSetWindowSize(window,1000,760);mc.resizeDisplay();return;}
            if(mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen&&reconnectAt==0&&stage!=51&&Files.exists(MultiplayerCheck.ROOT.resolve(role+"-connected.ok")))throw new AssertionError("客户端意外断线，见原始日志；不能停在阶段屏障等待到全局超时");
            if(mc.level==null || mc.player==null || mc.getOverlay()!=null || mc.screen!=null&&!effects.ownsScreen(mc))return;
            MultiplayerCheck.require(mc.getSingleplayerServer()==null,"必须是真实专服连接");
            if(!Files.exists(MultiplayerCheck.ROOT.resolve(role+"-connected.ok")))MultiplayerCheck.write(role+"-connected.ok","TCP connected");
            String marker=Files.readString(MultiplayerCheck.ROOT.resolve("stage.txt")).trim();if(marker.isEmpty())return;
            int next=Integer.parseInt(marker);
            MultiplayerCheck.require(next!=-1,"服务端断言失败");
            if(next==99){frameTiming.finish(role);if(!Boolean.getBoolean("pipa.qa.modelMatrix")&&!Boolean.getBoolean("pipa.qa.carriedOnly"))motion.finish(role);release(mc);MultiplayerCheck.write(role+"-result.txt",Boolean.getBoolean("pipa.qa.modelMatrix")?"PASS: model matrix execution completed; individual failures remain in matrix records\n":"PASS: actual TCP sync, native skeleton rendering, pose recovery, flight transitions and banking, both player viewpoints and GPU shader state\n");done=true;mc.stop();return;}
            if(next!=stage){if(stage==53)frameTiming.finish(role);release(mc);stage=next;age=0;PoseProbe.stage=stage;foodRequested=false;foodCancelled=false;foodCompletedAt=0;}
            if(stage==0)return;age++;
            if(stage>=1&&stage<=5){effects.tick(mc,stage,age,role);return;}
            if(stage>=1000){matrix(mc);return;}
            motion.sample(mc,stage);
            if(PoseProbe.failure!=null)throw new AssertionError(PoseProbe.failure);
            if(AudioProbe.failure!=null)throw new AssertionError(AudioProbe.failure);
            if(stage==72)return;
            if(stage>=80&&stage<=82){food(mc);return;}
            if(stage>=74 && stage<=77){gecko(mc);return;}
            if(stage>=60) {model(mc);return;}
            if(stage>=50) {lifecycle(mc);return;}
            boolean playerCase=stage>=30,owner=role.equals(stage<40?"actor":"viewer");
            LivingEntity target;
            if(!playerCase) target=mc.level.getEntitiesOfClass(EntityMaid.class,mc.player.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
            else target=mc.level.players().stream().filter(p->p.getGameProfile().getName().equals(stage<40?"PipaActor":"PipaViewer")).findFirst().orElse(null);
            if(target==null)return;
            if(playerCase && owner)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);else {mc.options.setCameraType(CameraType.FIRST_PERSON);look(mc,target.getEyePosition());}
            int step=stage%10;
            if(playerCase && step==1 && owner) {
                if(age==10){mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));}
                if(age==15){mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);mc.options.keyUse.setDown(true);}
                if(age==55){mc.options.keyUse.setDown(false);MultiplayerCheck.require(mc.player.hasEffect(Pipa.MILK),"真实完整饮用产生效果");mc.options.keyJump.setDown(true);}
                if(age==57)mc.options.keyJump.setDown(false);
                if(age==60)mc.options.keyJump.setDown(true);
                return;
            }
            if(playerCase && step==2 && owner)mc.options.keyJump.setDown(age<10);
            if(age<45 || Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
            if(step==2 || !playerCase && step==3) {
                if(!MobFlight.posed(target))return;
                String key=stage+":"+target.getId();
                if(PoseProbe.samples.getOrDefault(key,0)<3)return;
            }
            if((!playerCase && step==4 || playerCase && step==3) && (!target.onGround() || MobFlight.posed(target)))return;
            shader();screenshot(mc);
            MultiplayerCheck.write(role+"-"+stage+".ok","PASS target="+target.getId()+" position="+target.position()+" nativeFrames="+PoseProbe.samples.getOrDefault(stage+":"+target.getId(),0));
        }catch(Throwable error){error.printStackTrace();release(mc);MultiplayerCheck.write(role+"-result.txt","FAIL: "+error);done=true;mc.stop();}
    }
    /** 逐模型检查实际飞行绘制；失败保留为该ID的结果并继续，执行完毕不代表所有模型通过。 */
    private void matrix(Minecraft mc) throws Exception {
        if(age==1)PoseProbe.failure=null;
        if(Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        var target=mc.level.getEntitiesOfClass(EntityMaid.class,mc.player.getBoundingBox().inflate(60)).stream().filter(m->m.hasCustomName()&&m.getCustomName().getString().startsWith("PipaMatrix-")).findFirst().orElse(null);
        if(target==null)return;
        mc.options.setCameraType(CameraType.FIRST_PERSON);mc.options.fov().set(70);look(mc,target.position().add(0,0.7,0));
        int frames=PoseProbe.samples.getOrDefault(stage+":"+target.getId(),0);
        if(age<80 || frames<3&&PoseProbe.failure==null&&age<160)return;
        boolean passed=MobFlight.posed(target)&&frames>=3&&PoseProbe.failure==null;
        screenshot(mc);
        MultiplayerCheck.write(role+"-"+stage+".ok",(passed?"PASS":"FAIL")+" model="+target.getModelId()+" entity="+target.getId()+" nativeFrames="+frames+" pose="+MobFlight.posed(target)+" reason="+PoseProbe.failure+"\n");
    }
    /** 通过真实断线与资源重载核对前置音频重建；阶段文件只通知检查时机，不代传游戏状态。 */
    private void lifecycle(Minecraft mc) throws Exception {
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        var actor=mc.level.players().stream().filter(p->p.getGameProfile().getName().equals("PipaActor")).findFirst().orElse(null);
        if(actor!=null && !role.equals("actor"))look(mc,actor.getEyePosition());
        if(stage==50)return;
        if(stage==58)return;
        if(stage==56 && role.equals("viewer")) {
            if(age<100 || mc.level.getChunkSource().hasChunk(0,0) || song!=null&&mc.getSoundManager().isActive(song))return;
            MultiplayerCheck.write(role+"-56.ok","PASS doll client chunk unloaded and song stopped outside range");return;
        }
        if(stage==51 && role.equals("actor") && !disconnected && age>=10) {
            disconnected=true;reconnectServer=mc.getCurrentServer();release(mc);
            // 与原版暂停菜单一致：先关闭 ClientLevel 的网络连接，再清理 Minecraft 画面；仅 disconnect() 会留下旧 TCP 连接。
            mc.level.disconnect();mc.disconnect();
            reconnectAt=System.currentTimeMillis()+3000;return;
        }
        if(stage==52 && reload==null && age>=10) {beforeReload=song;reload=mc.reloadResourcePacks();return;}
        if(age<80 || Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        if(stage==52 && (reload==null || !reload.isDone() || song==beforeReload))return;
        if(stage==52)reload.join();
        var d=(com.friendswine.DollBlockEntity)mc.level.getBlockEntity(new net.minecraft.core.BlockPos(0,81,0));
        if(d==null || song==null || !mc.getSoundManager().isActive(song))return;
        String location=stage==54?"friendswine:doll_music":"shaoxinpipaqu:pipa";
        MultiplayerCheck.require(song.getLocation().toString().equals(location),"实际音频通道与所选歌曲一致");
        if(stage==51 || stage==52) {
            if(actor==null || !MobFlight.posed(actor))return;
            MultiplayerCheck.require(mc.level.getGameTime()-d.getStartTick()>80,"中途重建不得从头重启歌曲");
            if(role.equals("actor"))MultiplayerCheck.require(mc.player.hasEffect(Pipa.MILK)&&mc.player.hasEffect(Pipa.PEACE)&&mc.player.getAbilities().flying,"断线/重载保留独立食物与自主飞行");
        }
        if(stage==57) {
            var target=mc.level.getEntitiesOfClass(EntityMaid.class,mc.player.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
            if(target==null||!MobFlight.posed(target))return;
            MultiplayerCheck.require(d.isPlaying()&&com.shaoxinpipaqu.SongState.of(d).pipa$isSelected(),"重新加载区块后恢复歌曲与档位");
        }
        if(stage==59) {
            if(actor==null)return;
            MultiplayerCheck.require(!actor.hasEffect(Pipa.MILK)&&!MobFlight.posed(actor),"死亡重生双端清除玩家姿势与狗奶");
            if(role.equals("actor"))MultiplayerCheck.require(!mc.player.getAbilities().mayfly&&!mc.player.hasEffect(Pipa.PEACE),"死亡重生清理权限和忘情");
        }
        if(stage==53) {
            if(age%20==0)audioSamples++;
            if(age<Math.ceil(AudioInfo.LOOP_TICKS*3)+100)return;
            MultiplayerCheck.require(audioSamples>=70,"三遍循环期间持续观察活动音频通道");
        }
        shader();screenshot(mc);
        MultiplayerCheck.write(role+"-"+stage+".ok","PASS song="+song.getLocation()+" start="+d.getStartTick()+" elapsed="+d.getElapsedTicks(mc.level.getGameTime())+" activeSamples="+audioSamples+" actorPose="+(actor!=null&&MobFlight.posed(actor)));
    }
    private void model(Minecraft mc) throws Exception {
        // 常规模型使用70°避免裁掉长肢；村民另用30°近景核对手部纹理，均瞄准俯身后的身体。
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        var target=mc.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,mc.player.getBoundingBox().inflate(60),m->m.hasCustomName()&&m.getCustomName().getString().equals("PipaModel-"+MultiplayerCheck.modelIndex(stage))).stream().findFirst().orElse(null);
        if(target==null)return;
        mc.options.fov().set(target instanceof net.minecraft.world.entity.npc.Villager?30:70);
        var renderer=mc.getEntityRenderDispatcher().getRenderer(target);
        boolean prone=renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer<?,?> living
                && (living.getModel() instanceof net.minecraft.client.model.HumanoidModel<?> || living.getModel() instanceof net.minecraft.client.model.VillagerModel<?>
                || com.shaoxinpipaqu.client.AnimalFlightPose.humanoid(living.getModel()));
        look(mc,prone?target.position().add(0,0.15,0):target.getEyePosition());
        if(age<100 || !MobFlight.posed(target) || Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        if(PoseProbe.samples.getOrDefault(stage+":"+target.getId(),0)<3)return;
        if((stage==66||stage==67)&&PoseProbe.samples.getOrDefault(stage+":"+target.getId()+":fur",0)<3)return;
        if(stage==91&&CarriedBlockProbe.samples.getOrDefault(stage,0)<3)return;
        int layers=PoseProbe.animalModels.getOrDefault(stage+":"+target.getId(),java.util.Set.of()).size();
        if(java.util.Set.of(205,206,207,208,211,212,215,216,217,224,227).contains(stage)&&layers<2)return;
        shader();screenshot(mc);
        MultiplayerCheck.write(role+"-"+stage+".ok","PASS actual selected entity="+target.getType()+" baby="+target.isBaby()+" position="+target.position()+" independentAnimalModels="+layers+" nativeFrames="+PoseProbe.samples.get(stage+":"+target.getId()));
    }
    private void gecko(Minecraft mc) throws Exception {
        // 原版视场角下限为30；20会被拒绝，无法得到预期的近景验收画面。
        mc.options.fov().set(30);
        var target=mc.level.getEntitiesOfClass(EntityMaid.class,mc.player.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
        if(target==null)return;mc.options.setCameraType(CameraType.FIRST_PERSON);look(mc,target.getEyePosition());
        if(age<100 || Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        if(stage%2==0 && (!MobFlight.posed(target)||PoseProbe.samples.getOrDefault(stage+":"+target.getId(),0)<3))return;
        if(stage%2==1 && (!target.onGround()||MobFlight.posed(target)))return;
        shader();screenshot(mc);MultiplayerCheck.write(role+"-"+stage+".ok","PASS Gecko nativeFrames="+PoseProbe.samples.getOrDefault(stage+":"+target.getId(),0));
    }
    /** 只操纵真实使用键及放开包，服务端检查物品数量、返碗和效果，不能直接调用 finishUsingItem 代替。 */
    private void food(Minecraft mc) throws Exception {
        if(!role.equals("actor"))return;
        if(age==5){mc.player.getInventory().selected=0;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));}
        if(age%5==0){foodRows.append(stage).append(',').append(age).append(',').append(mc.gameMode.getPlayerMode()).append(',').append(mc.player.isUsingItem()).append(',').append(mc.player.getUseItemRemainingTicks()).append(',').append(mc.player.getInventory().countItem(Pipa.NOODLES.get())).append(',').append(mc.player.hasEffect(Pipa.PEACE)).append('\n');MultiplayerCheck.write(role+"-food-timing.csv",foodRows.toString());}
        if((stage==80||stage==81)&&!foodRequested&&age>=10&&mc.gameMode.getPlayerMode()==net.minecraft.world.level.GameType.SURVIVAL&&mc.player.getMainHandItem().is(Pipa.NOODLES.get())) {
            foodRequested=true;mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);mc.options.keyUse.setDown(true);
        }
        // 以实际使用进度取消，不能按阶段固定帧数松手，否则网络/客户端不同步时可能从未真正开始吃。
        if(stage==80&&!foodCancelled&&mc.player.isUsingItem()&&mc.player.getTicksUsingItem()>=12) {
            mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);foodCancelled=true;foodCompletedAt=age;
        }
        // 完成场景持续按住，直到服务端效果同步；旧夹具35个本地tick后松手，不能证明服务端已走完32tick使用。
        if(stage==81&&foodCompletedAt==0&&mc.player.hasEffect(Pipa.PEACE)) {
            mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);foodCompletedAt=age;
        }
        MultiplayerCheck.require(age<500,"真实食物使用及服务端同步超时");
        if(stage==80&&(!foodCancelled||age<foodCompletedAt+10)||stage==81&&(foodCompletedAt==0||age<foodCompletedAt+10))return;
        if(age<55||Files.exists(MultiplayerCheck.ROOT.resolve(role+"-"+stage+".ok")))return;
        if(stage==80)MultiplayerCheck.require(!mc.player.hasEffect(Pipa.PEACE),"客户端中途取消不生效");
        else MultiplayerCheck.require(mc.player.hasEffect(Pipa.PEACE),"客户端完整使用后独立效果仍有效");
        screenshot(mc);MultiplayerCheck.write(role+"-"+stage+".ok","PASS actual food use client state");
    }
}
