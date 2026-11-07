package com.shaoxinpipaqu.qa;

import com.friendswine.*;
import com.shaoxinpipaqu.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 只在独立开发世界布置场景，通过真实客户端渲染和饮用操作核对；不打入成品。 */
@Mod(value="pipa_qa",dist=Dist.CLIENT)
public final class ClientCheck {
    private static final Path ROOT=Path.of("../../docs/validation/0.1.0").toAbsolutePath().normalize();
    private int age;
    private boolean setup, done;
    private double startY;
    private net.minecraft.client.resources.sounds.SoundInstance song;
    public ClientCheck() {
        if(System.getProperty("pipa.qa.root")!=null) { new MultiplayerClient(); return; }
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.sound.PlaySoundEvent e) -> {
            if (e.getOriginalSound() instanceof com.friendswine.client.DollMusicSound) song=e.getOriginalSound();
        });
    }
    private void require(boolean ok,String why) { if (!ok) throw new AssertionError(why); }
    private void shot(Minecraft mc,String name) throws Exception {
        try (var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(ROOT.resolve(name+".png")); }
    }
    private void tick(ClientTickEvent.Post e) {
        if (done) return;
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null || mc.getSingleplayerServer()==null || mc.getOverlay()!=null) return;
        try {
            Files.createDirectories(ROOT);
            mc.options.pauseOnLostFocus=false;
            if (!setup) {
                setup=true;
                Files.writeString(ROOT.resolve("client-result.txt"),"RUNNING\n");
                mc.getSingleplayerServer().execute(() -> {
                    var level=mc.getSingleplayerServer().overworld();
                    var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    p.setGameMode(GameType.SURVIVAL);
                    for (var mob:level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(-40,50,-40,40,100,40))) mob.discard();
                    for (BlockPos b:BlockPos.betweenClosed(-20,60,-20,20,60,20)) level.setBlockAndUpdate(b,Blocks.STONE.defaultBlockState());
                    level.setDayTime(6000);
                    p.teleportTo(0.5,61,9.5); p.setYRot(180); p.setXRot(10);
                    p.getInventory().setItem(0,new ItemStack(Pipa.DOG_MILK.get(),4));
                    p.getInventory().setItem(1,new ItemStack(Pipa.NOODLES.get(),4));
                    p.getInventory().setItem(2,new ItemStack(FriendsWine.REMOTE.get()));
                    var pos=new BlockPos(0,61,0);
                    level.setBlockAndUpdate(pos,FriendsWine.DOLL.get().defaultBlockState());
                    var doll=(DollBlockEntity)level.getBlockEntity(pos);
                    doll.stopPlaying(); if (!SongState.of(doll).pipa$isSelected()) SongState.of(doll).pipa$toggle(); doll.startPlaying();
                    for (int i=0;i<8;i++) {
                        var pig=EntityType.PIG.create(level); pig.moveTo(3+i%4*2,61,-3+i/4*3); level.addFreshEntity(pig);
                    }
                    var zombie=EntityType.HUSK.create(level); zombie.moveTo(-4,61,0); level.addFreshEntity(zombie);
                    var villager=EntityType.VILLAGER.create(level); villager.moveTo(-6,61,0); level.addFreshEntity(villager);
                });
                return;
            }
            if (mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen) return;
            age++;
            if (age==50) {
                mc.setScreen(null); mc.player.setYRot(180); mc.player.setXRot(12);
                // 重跑世界会保存上轮遥控器槽位；必须通过真实选槽包切回饮品，不能误把右键遥控当成喝奶。
                mc.player.getInventory().selected=0;
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(0));
            }
            if (age==100) {
                require(mc.level.getBlockEntity(new BlockPos(0,61,0)) instanceof DollBlockEntity,"客户端收到玩偶");
                var d=(DollBlockEntity)mc.level.getBlockEntity(new BlockPos(0,61,0));
                require(SongState.of(d).pipa$isSelected(),"选曲同步");
                require(song!=null && song.getLocation().toString().equals("shaoxinpipaqu:pipa") && mc.getSoundManager().isActive(song),"琵琶曲流式音频通道实际活动");
                require(mc.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,mc.player.getBoundingBox().inflate(40)).stream().anyMatch(m -> m.hasEffect(Pipa.AIRBORNE)),"真实客户端收到飞行姿势");
                shot(mc,"mobs-flight");
                mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                mc.options.keyUse.setDown(true);
            }
            if (age==140) {
                mc.options.keyUse.setDown(false);
                require(mc.player.hasEffect(Pipa.MILK),"完整饮用后同步狗奶效果");
                require(mc.player.getAbilities().mayfly,"获得临时飞行权限");
                mc.options.keyJump.setDown(true);
            }
            if (age==142) mc.options.keyJump.setDown(false);
            if (age==145) mc.options.keyJump.setDown(true);
            if (age==165) {
                require(mc.player.getAbilities().flying,"双击跳跃起飞");
                require(mc.player.getY()>62,"实际升空");
                mc.options.keyJump.setDown(false); startY=mc.player.getY();
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            if (age==185) { shot(mc,"player-flight"); require(Math.abs(mc.player.getY()-startY)<0.5,"松开跳跃后悬停不持续上升"); }
            if (age==200) mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().removeEffect(Pipa.MILK));
            if (age==210) require(!mc.player.getAbilities().flying && !mc.player.getAbilities().mayfly,"清除效果后回收本效果权限");
            if (age==300) {
                require(mc.player.onGround(),"解除后安全落地");
                require(!mc.player.hasEffect(Pipa.AIRBORNE),"落地清姿势");
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().teleportTo(0.5,61,3.5));
            }
            if (age==320) {
                mc.player.getInventory().selected=2;
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(2));
            }
            if (age==330) {
                var pos=new BlockPos(0,61,0);
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.SOUTH,pos,false));
            }
            if (age==340) mc.options.keyAttack.setDown(true);
            if (age==360) {
                mc.options.keyAttack.setDown(false);
                var d=(DollBlockEntity)mc.level.getBlockEntity(new BlockPos(0,61,0));
                require(!SongState.of(d).pipa$isSelected(),"左键长按只切一次原曲");
                require(song!=null && song.getLocation().toString().equals("friendswine:doll_music") && mc.getSoundManager().isActive(song),"切回原曲实际启用原音频通道");
            }
            if (age==370) mc.options.keyAttack.setDown(true);
            if (age==372) mc.options.keyAttack.setDown(false);
            if (age==390) {
                require(SongState.of(mc.level.getBlockEntity(new BlockPos(0,61,0))).pipa$isSelected(),"再次单击切回琵琶曲");
                require(song.getLocation().toString().equals("shaoxinpipaqu:pipa") && mc.getSoundManager().isActive(song),"重新切入琵琶曲通道");
                Files.writeString(ROOT.resolve("client-result.txt"),"PASS: 客户端启动、飞行姿势同步、物品真实饮用、双击起飞、悬停、清除后安全落地、遥控器实际绑定与左键切歌、长按防连切、两种流式音频通道。\n");
                done=true; mc.stop();
            }
        } catch(Throwable error) {
            error.printStackTrace();
            try { Files.writeString(ROOT.resolve("client-result.txt"),"FAIL: "+error); } catch(Exception ignored) {}
            mc.options.keyJump.setDown(false); mc.options.keyUse.setDown(false); done=true; mc.stop();
        }
    }
}
