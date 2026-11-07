package com.shaoxinpipaqu.qa;

import com.friendswine.*;
import com.shaoxinpipaqu.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 独立专服只布置夹具并核对权威状态；客户端通过真实 TCP、物品使用及按键执行玩法。 */
@Mod("pipa_qa")
public final class MultiplayerCheck {
    static final Path ROOT=System.getProperty("pipa.qa.root")==null?null:Path.of(System.getProperty("pipa.qa.root"));
    private int stage,age,total;
    private EntityMaid maid;
    private DollBlockEntity doll;
    private double minY=1000,maxY=-1000;
    private int logoutMilk,logoutPeace;
    private long logoutTick;
    private long loginTick;
    private int loginMilk,loginPeace;
    private Mob standard;
    private com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy fairy;
    private com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityDanmaku bullet;
    private float protectedHealth;
    private boolean departed;
    private java.util.List<String> matrixModels;
    private final java.util.List<String> matrixResults=new java.util.ArrayList<>();
    private final StringBuilder foodRows=new StringBuilder("stage,age,using,remaining,noodles,bowls,peace\n");
    private static final EntityType<?>[] MODELS={EntityType.HUSK,EntityType.SKELETON,EntityType.VILLAGER,EntityType.VILLAGER,EntityType.COW,EntityType.COW,EntityType.SHEEP,EntityType.SHEEP,EntityType.PIG,EntityType.PIG};
    // 独立使用90起的阶段，保留已有模型与生命周期的证据编号；末影人含搬方块动画覆盖场景。
    private static final EntityType<?>[] SPECIAL_MODELS={EntityType.ENDERMAN,EntityType.ENDERMAN,EntityType.WITHER_SKELETON,EntityType.ZOMBIE,EntityType.PIGLIN_BRUTE,EntityType.ZOMBIFIED_PIGLIN};
    private static final EntityType<?>[] EXTRA_MODELS={EntityType.GOAT,EntityType.POLAR_BEAR,EntityType.PANDA,EntityType.TURTLE,EntityType.MOOSHROOM,EntityType.WOLF,EntityType.WOLF,EntityType.CAT,EntityType.CAT,EntityType.OCELOT,EntityType.FOX,EntityType.HORSE,EntityType.HORSE,EntityType.DONKEY,EntityType.MULE,EntityType.LLAMA,EntityType.LLAMA,EntityType.TRADER_LLAMA,EntityType.HOGLIN,EntityType.ZOGLIN,EntityType.RAVAGER,
            EntityType.CAMEL,EntityType.CAMEL,EntityType.CREEPER,EntityType.CREEPER,EntityType.IRON_GOLEM,EntityType.SNOW_GOLEM,
            EntityType.STRIDER,EntityType.EVOKER,EntityType.PILLAGER,EntityType.VINDICATOR,EntityType.WARDEN};
    static int modelIndex(int stage){return stage>=200?stage-160:stage-60;}
    public MultiplayerCheck() { if(ROOT!=null) {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->{
            if(stage==51 && e.getEntity().getGameProfile().getName().equals("PipaActor")) {
                logoutMilk=e.getEntity().getEffect(Pipa.MILK).getDuration();logoutPeace=e.getEntity().getEffect(Pipa.PEACE).getDuration();
                logoutTick=e.getEntity().level().getGameTime();write("logout.txt","milk="+logoutMilk+" peace="+logoutPeace+" tick="+logoutTick);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->{
            if(stage==51 && e.getEntity().getGameProfile().getName().equals("PipaActor")) {
                loginTick=e.getEntity().level().getGameTime();loginMilk=e.getEntity().getEffect(Pipa.MILK).getDuration();loginPeace=e.getEntity().getEffect(Pipa.PEACE).getDuration();
                write("login.txt","milk="+loginMilk+" peace="+loginPeace+" tick="+loginTick+" offlineTicks="+(loginTick-logoutTick));
            }
        });
    } }
    static void write(String name,String data) {
        try {
            Path temporary=ROOT.resolve(name+".tmp");Files.writeString(temporary,data);
            // Windows 上其他 JVM 的短暂读取可能阻止替换；保留旧完整文件并限时重试，不能退回截断写入。
            for(int attempt=0;;attempt++) {
                try {Files.move(temporary,ROOT.resolve(name),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);break;}
                catch(AccessDeniedException busy) {if(attempt>=9)throw busy;Thread.sleep(10);}
            }
        }catch(Exception e){throw new RuntimeException(e);}
    }
    static void require(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    private boolean ready(String role) { return Files.exists(ROOT.resolve(role+"-"+stage+".ok")); }
    private void next(int stage) { this.stage=stage;age=0;write("stage.txt",Integer.toString(stage)); }
    private void tick(ServerTickEvent.Post e) {
        if(!(e.getServer() instanceof net.minecraft.server.dedicated.DedicatedServer server) || stage==99 || stage==-1) return;
        try {
            if(++total>(Boolean.getBoolean("pipa.qa.modelMatrix")?72000:18000))throw new AssertionError("双客户端验收超时");
            var level=server.overworld();
            ServerPlayer actor=server.getPlayerList().getPlayerByName("PipaActor"),viewer=server.getPlayerList().getPlayerByName("PipaViewer");
            if(actor==null || viewer==null || !Files.exists(ROOT.resolve("actor-connected.ok")) || !Files.exists(ROOT.resolve("viewer-connected.ok")))return;
            age++;
            if(stage>=80&&stage<=82&&age%10==0) {
                foodRows.append(stage).append(',').append(age).append(',').append(actor.isUsingItem()).append(',').append(actor.getUseItemRemainingTicks()).append(',').append(actor.getInventory().countItem(Pipa.NOODLES.get())).append(',').append(actor.getInventory().countItem(net.minecraft.world.item.Items.BOWL)).append(',').append(actor.hasEffect(Pipa.PEACE)).append('\n');
                write("server-food-timing.csv",foodRows.toString());
            }
            if(stage==0) {
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);level.setDayTime(6000);
                for(BlockPos p:BlockPos.betweenClosed(-30,80,-30,30,80,30))level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
                for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getInventory().setItem(0,new ItemStack(Pipa.DOG_MILK.get(),16));}
                actor.connection.teleport(0.5,81,10.5,180,10); viewer.connection.teleport(-7,81,10,150,10);
                var pos=new BlockPos(0,81,0);level.setBlockAndUpdate(pos,FriendsWine.DOLL.get().defaultBlockState());doll=(DollBlockEntity)level.getBlockEntity(pos);
                if(!SongState.of(doll).pipa$isSelected())SongState.of(doll).pipa$toggle();
                maid=(EntityMaid)BuiltInRegistries.ENTITY_TYPE.get(net.minecraft.resources.ResourceLocation.parse("touhou_little_maid:maid")).create(level);
                require(maid!=null,"真实女仆实体存在");maid.moveTo(4.5,81,0.5);maid.setNoAi(true);maid.setPersistenceRequired();maid.setCustomName(Component.literal("BedrockMaid"));
                level.addFreshEntity(maid);
                // 图标先在两端实际同步效果下验收，结束后清理夹具，避免干扰真实饮用用例。
                for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,3600));
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.MILK,1200));
                }
                next(1);return;
            }
            if(stage>=1&&stage<=5) {
                require(age<1200,"效果图标双端界面/资源重载验收超时："+stage);
                if(age<60||!ready("actor")||!ready("viewer"))return;
                if(stage<5){next(stage+1);return;}
                for(ServerPlayer p:new ServerPlayer[]{actor,viewer})p.removeAllEffects();
                if(Boolean.getBoolean("pipa.qa.modelMatrix")) {
                    var manifest=com.google.gson.JsonParser.parseString(Files.readString(ROOT.resolve("model-resources.json"))).getAsJsonObject();
                    matrixModels=new java.util.ArrayList<>();for(var item:manifest.getAsJsonArray("models"))matrixModels.add(item.getAsJsonObject().get("id").getAsString());
                    require(matrixModels.size()==234,"逐模型清单必须完整，不能只抽取已知可用模型");
                    matrixModel(level,0);next(1000);return;
                }
                if(Boolean.getBoolean("pipa.qa.compatOnly")){gecko(level);next(74);}
                else if(Boolean.getBoolean("pipa.qa.carriedOnly")){maid.discard();server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);model(level,31);next(91);}
                else if(Boolean.getBoolean("pipa.qa.modelOnly")){maid.discard();server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);model(level,0);next(60);}
                else next(11);return;
            }
            if(stage>=1000) {
                if(age%20==0)for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {
                    p.setGameMode(GameType.SPECTATOR);p.connection.teleport(maid.getX()+(p==actor?3:-3),maid.getY()+0.5,maid.getZ()+5,180,0);
                }
                if(age<100||(!ready("actor")||!ready("viewer"))&&age<240)return;
                int index=stage-1000;boolean passed=MobFlight.controlled(maid);
                for(String role:new String[]{"actor","viewer"}) {
                    String outcome=ready(role)?Files.readString(ROOT.resolve(role+"-"+stage+".ok")):"FAIL client model draw timeout";
                    passed&=outcome.startsWith("PASS");
                }
                matrixResults.add((passed?"PASS ":"FAIL ")+matrixModels.get(index));
                write("model-matrix.json",new com.google.gson.Gson().toJson(matrixResults));
                if(index+1<matrixModels.size()){matrixModel(level,index+1);next(stage+1);}
                else {doll.stopPlaying();maid.discard();write("server-result.txt","PASS: 234 model checks executed; individual FAIL entries remain in model-matrix.json\n");next(99);}
                return;
            }
            if(stage==11 || stage==21) {
                if(age<60 || !ready("actor") || !ready("viewer"))return;
                doll.startPlaying();for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();
                next(stage+1);return;
            }
            if(stage==12 || stage==22) {
                require(age<800,"真实女仆起飞或双端姿势验收超时");
                if(age>60 && ready("actor") && ready("viewer")) { require(MobFlight.controlled(maid) && maid.getY()>83,"真实女仆被抽中并起飞");doll.cycleRemote();minY=1000;maxY=-1000;next(stage+1); } return;
            }
            if(stage==13 || stage==23) {
                minY=Math.min(minY,maid.getY());maxY=Math.max(maxY,maid.getY());
                if(age>=180 && ready("actor") && ready("viewer")) {write("wave-"+stage+".txt","minY="+minY+" maxY="+maxY+" ticks="+age);require(maxY-minY>0.6,"三档有实际高度波浪，范围="+(maxY-minY));doll.stopPlaying();next(stage+1);}return;
            }
            if(stage==14 || stage==24) {
                require(age<800,"女仆停止后应安全落地");
                if(MobFlight.controlled(maid) || maid.hasEffect(Pipa.AIRBORNE) || !ready("actor") || !ready("viewer"))return;
                if(stage==14&&!Boolean.getBoolean("pipa.qa.vanillaPlayers")) {maid.setYsmModel("default","default",Component.literal("Default"));maid.setIsYsmModel(true);maid.setCustomName(Component.literal("YsmMaid"));next(21);}
                else {maid.discard();actor.connection.teleport(0.5,81,5,180,0);viewer.connection.teleport(-5,81,7,150,0);next(31);}return;
            }
            if(stage==31 || stage==41) {
                require(age<800,"狗奶真实饮用、双击起飞超时");
                var owner=stage==31?actor:viewer;
                if(owner.hasEffect(Pipa.MILK) && owner.getAbilities().flying && owner.getY()>83)next(stage+1);
                return;
            }
            if(stage==32 || stage==42) {
                require(age<800,"双端 YSM 玩家姿势验收超时");
                var owner=stage==32?actor:viewer;
                if(age>=70 && ready("actor") && ready("viewer")) {owner.removeEffect(Pipa.MILK);next(stage+1);}return;
            }
            if(stage==33 || stage==43) {
                var owner=stage==33?actor:viewer;
                require(age<800,"玩家清除效果后应安全落地");
                if(!owner.onGround() || owner.hasEffect(Pipa.AIRBORNE) || !ready("actor") || !ready("viewer"))return;
                require(!owner.getAbilities().mayfly && !owner.getAbilities().flying,"落地恢复生存权限");
                if(stage==33){actor.connection.teleport(5,81,7,210,0);viewer.connection.teleport(0.5,81,5,180,0);next(41);}
                else {
                    actor.connection.teleport(0.5,81,5,180,0);viewer.connection.teleport(-5,81,7,150,0);
                    actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.MILK,1200));
                    actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,3600));
                    if(!SongState.of(doll).pipa$isSelected())SongState.of(doll).pipa$toggle();doll.startPlaying();
                    for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();
                    next(50);
                }
                return;
            }
            if(stage==50) {
                if(age<50)return;
                actor.getAbilities().flying=true;actor.onUpdateAbilities();actor.connection.teleport(0.5,85,5,180,0);
                write("song-start.txt",Long.toString(doll.getStartTick()));next(51);return;
            }
            if(stage==51) {
                require(age<1200,"真实断线重进超时");
                if(logoutTick==0 || !ready("actor") || !ready("viewer"))return;
                require(loginTick-logoutTick>=40 && loginMilk==logoutMilk && loginPeace==logoutPeace,"必须存在真实离线间隔，且离线效果时长精确暂停");
                int milk=actor.getEffect(Pipa.MILK).getDuration(),peace=actor.getEffect(Pipa.PEACE).getDuration();
                require(milk<=logoutMilk && milk>=logoutMilk-160 && peace<=logoutPeace && peace>=logoutPeace-160,"重进恢复剩余时间，不能重置完整时长");
                require(actor.getAbilities().mayfly && actor.getAbilities().flying,"重进恢复飞行权限与姿势");
                write("rejoin.txt","milk="+milk+" peace="+peace+" tick="+level.getGameTime()+" logoutTick="+logoutTick);next(52);return;
            }
            if(stage==52) {
                require(age<1600,"资源重载后恢复音频与姿势超时");
                if(ready("actor")&&ready("viewer")){write("reload.txt","start="+doll.getStartTick()+" elapsed="+doll.getElapsedTicks(level.getGameTime()));next(53);}return;
            }
            if(stage==53) {
                require(age<2400,"三遍循环观测超时");
                if(age>=Math.ceil(AudioInfo.LOOP_TICKS*3)+100 && ready("actor")&&ready("viewer")) {
                    require(!actor.hasEffect(Pipa.MILK)&&!actor.getAbilities().mayfly&&!actor.hasEffect(Pipa.AIRBORNE)&&actor.onGround(),"真实时效到期并安全落地");
                    require(actor.hasEffect(Pipa.PEACE),"狗奶到期不能误清忘情");SongState.of(doll).pipa$toggle();next(54);
                }return;
            }
            if(stage==54 || stage==55) {
                require(age<600,"音频切换后实际通道超时");
                if(!ready("actor")||!ready("viewer"))return;
                if(stage==54){SongState.of(doll).pipa$toggle();next(55);}
                else {
                    maid=new EntityMaid(level);maid.moveTo(4.5,81,0.5);maid.setNoAi(true);maid.setPersistenceRequired();level.addFreshEntity(maid);
                    // 歌曲继续播放，只让一个旁观者真正离开客户端区块跟踪范围，再回来检查补发。
                    next(56);
                }
                return;
            }
            if(stage==56) {
                require(age<800,"客户端离开区块超时");
                if(age>=40 && !departed){viewer.setGameMode(GameType.SPECTATOR);viewer.connection.teleport(250,81,7,150,0);departed=true;}
                if(age>=120&&ready("actor")&&ready("viewer")){viewer.setGameMode(GameType.SURVIVAL);viewer.connection.teleport(-5,81,7,150,0);next(57);}return;
            }
            if(stage==57) {
                require(age<800,"客户端区块重载/重新跟踪超时");
                if(age>=100&&ready("actor")&&ready("viewer")) {
                    actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.MILK,1200));actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,3600));next(58);
                }return;
            }
            if(stage==58) {
                require(age<800,"真实死亡与重生超时");
                if(age==20){actor.hurt(actor.damageSources().genericKill(),Float.MAX_VALUE);write("death.txt","dead="+!actor.isAlive()+" milk="+actor.hasEffect(Pipa.MILK)+" tick="+level.getGameTime());}
                if(age>40 && actor.isAlive()) {
                    require(!actor.hasEffect(Pipa.MILK)&&!actor.hasEffect(Pipa.PEACE)&&!actor.hasEffect(Pipa.AIRBORNE)&&!actor.getAbilities().mayfly,"真实死亡重生不得继承本来源效果/权限");
                    actor.connection.teleport(0.5,81,7,180,0);next(59);
                }return;
            }
            if(stage==59) {
                require(age<800,"死亡重生后双端音乐/姿势恢复超时");
                if(ready("actor")&&ready("viewer")){maid.discard();server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);model(level,0);next(60);}return;
            }
            if(stage>=60 && stage<72) {
                if(age%20==0) {
                    // 专项画面让两个旁观镜头靠近实际飞行者，不能只留下远处十几个像素的缩略图。
                    for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {
                        p.setGameMode(GameType.SPECTATOR);p.connection.teleport(standard.getX()+(p==actor?3:-3),standard.getY()+0.5,standard.getZ()+5,180,0);
                    }
                }
                require(age<600,"原版/立板专项起飞和双端画面超时："+stage);
                if(age<120 || !ready("actor") || !ready("viewer"))return;
                require(MobFlight.controlled(standard)&&standard.hasEffect(Pipa.AIRBORNE),"专项模型实际被唯一抽中并飞行");
                standard.discard();
                if(stage<71){int index=stage-59;model(level,index);next(stage+1);}
                else if(Boolean.getBoolean("pipa.qa.modelOnly")){model(level,30);next(90);}
                else {
                    doll.stopPlaying();
                    fairy=new com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy(level);fairy.moveTo(10,83,0.5);fairy.setNoAi(true);level.addFreshEntity(fairy);
                    require(!MobFlight.eligible(fairy),"车万妖精实现原生飞行接口，不接管轨迹");
                    standard=EntityType.PIG.create(level);standard.moveTo(4.5,81,0.5);standard.setNoAi(true);level.addFreshEntity(standard);protectedHealth=standard.getHealth();
                    actor.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,300));viewer.addEffect(new net.minecraft.world.effect.MobEffectInstance(Pipa.PEACE,300));next(72);
                }
                return;
            }
            if(stage==72) {
                if(age==10 || age==40 || age==70) {
                    bullet=new com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityDanmaku(level,fairy);
                    bullet.setPos(7.5,81.5,0.5);bullet.setGravityVelocity(0);bullet.setDamage(3);bullet.setDeltaMovement(-0.5,0,0);level.addFreshEntity(bullet);
                }
                if(age==30 || age==60) {
                    require(bullet.isRemoved()&&standard.getHealth()==protectedHealth,"真实车万弹幕命中时，多来源忘情拦截伤害");
                    if(age==30)actor.removeEffect(Pipa.PEACE);else viewer.removeEffect(Pipa.PEACE);
                }
                if(age==90) {
                    require(bullet.isRemoved()&&standard.getHealth()<protectedHealth,"最后一个来源解除后，同模组同参数弹幕命中恢复伤害");
                    write("danmaku.txt","PASS actual EntityDanmaku collision; protected="+protectedHealth+" unprotected="+standard.getHealth()+" nativeFairyEligible="+MobFlight.eligible(fairy));
                    standard.discard();fairy.discard();gecko(level);next(74);
                }
                return;
            }
            if(stage>=74 && stage<=77) {
                if(stage==75 && age%40==0)write("gecko-landing-"+age+".txt","position="+maid.position()+" ground="+maid.onGround()+" control="+MobFlight.controlled(maid)+" gravity="+maid.isNoGravity()+" downSafe="+MobFlight.safe(level,maid,new net.minecraft.world.phys.Vec3(0,-0.1,0),false)+" nearby="+level.getEntities(maid,maid.getBoundingBox().inflate(0.2),e2->e2 instanceof LivingEntity));
                require(age<800,"Gecko实际骨骼与恢复验收超时："+maid.position()+" ground="+maid.onGround()+" control="+MobFlight.controlled(maid));
                if(age<120 || !ready("actor")||!ready("viewer"))return;
                if(stage==74 || stage==76){require(MobFlight.controlled(maid),"特殊模型女仆实际起飞");doll.stopPlaying();next(stage+1);}
                else {
                    require(!MobFlight.controlled(maid)&&!maid.hasEffect(Pipa.AIRBORNE),"Gecko女仆恢复原姿势");
                    if(stage==75){maid.discard();maid=new EntityMaid(level);maid.moveTo(4.5,81,0.5);maid.setNoAi(true);maid.setPersistenceRequired();maid.setModelId("touhou_little_maid_old:genjii");maid.setCustomName(Component.literal("GenjiiMaid"));level.addFreshEntity(maid);doll.startPlaying();for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();next(76);}
                    else if(Boolean.getBoolean("pipa.qa.compatOnly")){write("server-result.txt","PASS: Gecko/玄爷专项（非完整回归）\n");next(99);}
                    else {
                        actor.setGameMode(GameType.SURVIVAL);actor.connection.teleport(0.5,81,7,180,0);actor.removeAllEffects();actor.getInventory().clearContent();actor.getInventory().setItem(0,new ItemStack(Pipa.NOODLES.get(),2));next(80);
                    }
                }
                return;
            }
            if(stage==80) {
                require(age<600,"真实中途取消吃面超时");
                if(age>=60&&ready("actor")) {
                    require(!actor.hasEffect(Pipa.PEACE)&&actor.getInventory().countItem(Pipa.NOODLES.get())==2&&actor.getInventory().countItem(net.minecraft.world.item.Items.BOWL)==0,"中途取消不能生效、消耗或返碗");next(81);
                }return;
            }
            if(stage==81) {
                require(age<600,"真实完整吃面超时");
                if(age>=80&&ready("actor")) {
                    require(actor.hasEffect(Pipa.PEACE)&&actor.getInventory().countItem(Pipa.NOODLES.get())==1&&actor.getInventory().countItem(net.minecraft.world.item.Items.BOWL)==1,"真实完整吃面只消耗一份并返一个碗");
                    doll.startPlaying();SongState.of(doll).pipa$toggle();doll.stopPlaying();level.destroyBlock(doll.getBlockPos(),false);next(82);
                }return;
            }
            if(stage==82) {
                require(age<600,"真实食物效果独立性超时");
                if(age>=60&&ready("actor")) {
                    require(actor.hasEffect(Pipa.PEACE),"切歌/停止/拆除玩偶后实际吃面来源仍有效");
                    write("food-use.txt","PASS actual cancel, full use, single consumption, bowl, independent source");
                    // 前一专项的玄爷仍在场；先移除，否则会与新模型争抢保底一个飞行名额。
                    if(maid!=null)maid.discard();
                    level.setBlockAndUpdate(new BlockPos(0,81,0),FriendsWine.DOLL.get().defaultBlockState());
                    doll=(DollBlockEntity)level.getBlockEntity(new BlockPos(0,81,0));
                    if(!SongState.of(doll).pipa$isSelected())SongState.of(doll).pipa$toggle();
                    model(level,30);next(90);
                }
            }
            if(stage>=90 && stage<90+SPECIAL_MODELS.length) {
                if(age%20==0) for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {
                    // 右手持块比原胸前锚点更远；携物场景退后取景，确保两端都能拍到掌心与完整方块。
                    double side=stage==91?6:3,back=stage==91?10:5;
                    p.setGameMode(GameType.SPECTATOR);p.connection.teleport(standard.getX()+(p==actor?side:-side),standard.getY()+0.5,standard.getZ()+back,180,0);
                }
                require(age<600,"特殊比例人形起飞/双端画面超时："+stage);
                if(age<120 || !ready("actor") || !ready("viewer"))return;
                require(MobFlight.controlled(standard)&&standard.hasEffect(Pipa.AIRBORNE),"特殊人形实际被抽中飞行");standard.discard();
                if(Boolean.getBoolean("pipa.qa.carriedOnly")){doll.stopPlaying();write("server-result.txt","PASS: carried block actual hand contact and size only; not full regression\n");next(99);return;}
                if(stage<89+SPECIAL_MODELS.length){model(level,stage-59);next(stage+1);}
                else {model(level,40);next(200);}
            }
            if(stage>=200&&stage<=200+EXTRA_MODELS.length) {
                if(age%20==0)for(ServerPlayer p:new ServerPlayer[]{actor,viewer}) {
                    p.setGameMode(GameType.SPECTATOR);p.connection.teleport(standard.getX()+(p==actor?3:-3),standard.getY()+0.5,standard.getZ()+5,180,0);
                }
                require(age<600,"新增四足实际起飞/绘制超时："+stage);
                if(age<120||!ready("actor")||!ready("viewer"))return;
                require(MobFlight.controlled(standard)&&standard.hasEffect(Pipa.AIRBORNE),"新增四足实际被抽中飞行");standard.discard();
                if(stage<200+EXTRA_MODELS.length){model(level,modelIndex(stage)+1);next(stage+1);}
                else {doll.stopPlaying();write("server-result.txt",Boolean.getBoolean("pipa.qa.modelOnly")?"PASS: 原版/立板、人形及新增四足模型专项（非完整回归）\n":"PASS: 完整双端联机、模型、生命周期、音频、食物及新增四足。\n");next(99);}
            }
        }catch(Throwable failure){failure.printStackTrace();write("server-result.txt","FAIL: "+failure);next(-1);}
    }
    /** 每次只留一个实际候选，避免依赖随机抽中；覆盖三种立板及成年/幼年模型。 */
    private void model(net.minecraft.server.level.ServerLevel level,int index) {
        // 前置 DeferredHolder 只能在注册完成后的测试阶段解引用，不能在验收模组类初始化时取值。
        EntityType<?> type;
        if (index==40+EXTRA_MODELS.length) type=FriendsWine.AMIYA.get();
        else type=index>=40?EXTRA_MODELS[index-40]:index>=30?SPECIAL_MODELS[index-30]:index<MODELS.length?MODELS[index]:index==10?FriendsWine.KASUMI.get():FriendsWine.EMMA.get();
        doll.stopPlaying();standard=(Mob)type.create(level);require(standard!=null,"模型实体可创建");
        standard.moveTo(4.5,81,0.5);standard.setNoAi(true);standard.setPersistenceRequired();
        standard.setCustomName(Component.literal("PipaModel-"+index));
        if(index>=2&&index<=9&&index%2==1)((AgeableMob)standard).setAge(-24000);
        if(java.util.Set.of(46,48,52,56).contains(index))((AgeableMob)standard).setAge(-24000);
        if(index==62)((AgeableMob)standard).setAge(-24000);
        if(index==64) {
            // 仅设置测试实体的带电标记，不生成真实闪电干扰候选或场地。
            var tag=standard.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.putBoolean("powered",true);
            standard.load(tag);
        }
        if(index==65)standard.setHealth(standard.getMaxHealth()/2);
        if(standard instanceof net.minecraft.world.entity.monster.Strider strider)strider.equipSaddle(new ItemStack(net.minecraft.world.item.Items.SADDLE),null);
        if(index==69)standard.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.CROSSBOW));
        // 同时覆盖会独立重跑动画的真实护甲、项圈和羊驼装饰，不能只检查裸模型。
        if(index>=40&&standard instanceof net.minecraft.world.entity.TamableAnimal tame)tame.setTame(true,false);
        if(index>=40&&standard instanceof net.minecraft.world.entity.animal.Wolf)standard.setItemSlot(EquipmentSlot.BODY,new ItemStack(net.minecraft.world.item.Items.WOLF_ARMOR));
        if(index>=40&&standard instanceof net.minecraft.world.entity.animal.horse.Horse)standard.setItemSlot(EquipmentSlot.BODY,new ItemStack(net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR));
        if(index>=40&&standard instanceof net.minecraft.world.entity.animal.horse.Llama)standard.setItemSlot(EquipmentSlot.BODY,new ItemStack(net.minecraft.world.item.Items.RED_CARPET));
        if(index==31)((net.minecraft.world.entity.monster.EnderMan)standard).setCarriedBlock(Blocks.GRASS_BLOCK.defaultBlockState());
        if(index==33) {
            ((net.minecraft.world.entity.monster.Zombie)standard).setBaby(true);
            // 日晒火焰会遮挡脸与双臂；头盔仅用于本轮夹具遮阳，保留幼年僵尸的真实比例与动画。
            standard.setItemSlot(EquipmentSlot.HEAD,new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        }
        if(index==32)standard.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
        if(standard instanceof net.minecraft.world.entity.npc.Villager v)v.setVillagerData(v.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.FARMER));
        if(index<2) {
            standard.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            standard.setItemSlot(EquipmentSlot.HEAD,new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
            standard.setItemSlot(EquipmentSlot.CHEST,new ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        }
        level.addFreshEntity(standard);doll.startPlaying();
        for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))if(guest!=standard)guest.discard();
    }
    private void gecko(net.minecraft.server.level.ServerLevel level) {
        doll.stopPlaying();if(maid!=null)maid.discard();
        maid=new EntityMaid(level);maid.moveTo(4.5,81,0.5);maid.setNoAi(true);maid.setPersistenceRequired();
        maid.setModelId("geckolib:winefox");maid.setCustomName(Component.literal("GeckoMaid"));
        maid.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        level.addFreshEntity(maid);doll.startPlaying();
        for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();
    }
    /** 每个ID使用新女仆和独立飞行抽选，避免共享模型状态或前一模型样本冒充本模型通过。 */
    private void matrixModel(net.minecraft.server.level.ServerLevel level,int index) {
        doll.stopPlaying();if(maid!=null)maid.discard();
        maid=new EntityMaid(level);maid.moveTo(4.5,81,0.5);maid.setNoAi(true);maid.setPersistenceRequired();
        maid.setModelId(matrixModels.get(index));maid.setCustomName(Component.literal("PipaMatrix-"+index));
        maid.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        level.addFreshEntity(maid);doll.startPlaying();
        for(var guest:level.getEntitiesOfClass(PartyGuestEntity.class,new net.minecraft.world.phys.AABB(-60,60,-60,60,110,60)))guest.discard();
    }
}
