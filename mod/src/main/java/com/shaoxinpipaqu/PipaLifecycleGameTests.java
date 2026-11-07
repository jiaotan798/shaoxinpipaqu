package com.shaoxinpipaqu;

import com.friendswine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.gametest.*;

/** 生命周期与结束边界回归；直接调用事件入口的用例只验证服务端状态机，不替代真实重连。 */
@GameTestHolder(Pipa.ID)
@PrefixGameTestTemplate(false)
public final class PipaLifecycleGameTests {
    /** 快照查询须与实际前置一致，包含全部歌曲、50格闭区间及列表顺序无关的等距规则。 */
    @GameTest(template="range",batch="pipa-owner-snapshot")
    public static void ownerSnapshotMatchesBase(GameTestHelper h) {
        var leftPos=new BlockPos(54,2,54);var rightPos=new BlockPos(58,2,54);
        h.setBlock(leftPos,FriendsWine.DOLL.get());h.setBlock(rightPos,FriendsWine.DOLL.get());
        DollBlockEntity left=h.getBlockEntity(leftPos),right=h.getBlockEntity(rightPos);
        SongState.of(left).pipa$toggle();left.startPlaying();right.startPlaying();
        var dolls=DollBlockEntity.getActiveDolls(h.getLevel());
        var reversed=new java.util.ArrayList<>(dolls);java.util.Collections.reverse(reversed);
        var center=Vec3.atCenterOf(left.getBlockPos());
        for(int x:new int[]{-51,-50,-49,0,2,49,50,51})for(int y:new int[]{0,50,51}) {
            var point=center.add(x,y,0);
            var expected=DollBlockEntity.nearestActive(h.getLevel(),point,50);
            h.assertTrue(MobFlight.nearest(dolls,point)==expected && MobFlight.nearest(reversed,point)==expected,"快照最近来源与前置一致："+point);
        }
        h.assertTrue(MobFlight.nearest(dolls,center.add(-50,0,0))==left,"恰好50格仍属于来源");
        h.assertTrue(MobFlight.nearest(dolls,center.add(2,0,0))==(left.getBlockPos().asLong()<right.getBlockPos().asLong()?left:right),"等距按方块位置长整数择优");
        right.stopPlaying();
        h.assertTrue(MobFlight.nearest(DollBlockEntity.getActiveDolls(h.getLevel()),Vec3.atCenterOf(right.getBlockPos()))==left,"下一次快照立即排除停播来源");
        left.stopPlaying();h.succeed();
    }

    /** 标记开启时保留每秒续期，关闭时只发首次或状态变化，普通玩家不能持续产生移除包。 */
    @GameTest(template="range",batch="pipa-pose-sync-policy")
    public static void poseSyncOnlyWhenNeeded(GameTestHelper h) {
        for(int tick=0;tick<80;tick++) {
            h.assertFalse(PlayerFlight.shouldSync(false,false,tick),"稳定关闭状态不重发移除包");
            h.assertTrue(PlayerFlight.shouldSync(true,true,tick)==(tick%20==0),"开启状态每秒续期");
            h.assertTrue(PlayerFlight.shouldSync(null,false,tick)&&PlayerFlight.shouldSync(null,true,tick),"首次状态仍同步");
            h.assertTrue(PlayerFlight.shouldSync(true,false,tick)&&PlayerFlight.shouldSync(false,true,tick),"状态变化立即同步");
        }
        h.succeed();
    }

    /** 落点下方已有普通生物时，须选择旁边空位；只保证不向上爬还不足以避免永久悬停。 */
    @GameTest(template="range",batch="pipa-landing-occupied",timeoutTicks=160)
    public static void landingAvoidsOccupiedFloor(GameTestHelper h) {
        for(var pos:BlockPos.betweenClosed(50,1,50,60,1,60))h.setBlock(pos,Blocks.STONE);
        var blocker=h.spawn(EntityType.PIG,new Vec3(54.5,2,54.5));blocker.setNoAi(true);blocker.setNoGravity(true);
        var pig=h.spawn(EntityType.PIG,new Vec3(54.5,2+blocker.getBbHeight()+0.005,54.5));pig.setNoAi(true);pig.setNoGravity(true);
        pig.getPersistentData().putBoolean("PipaFlightRecovery",true);pig.getPersistentData().putBoolean("PipaOriginalNoGravity",false);
        double initialY=pig.getY();
        h.onEachTick(()->h.assertTrue(pig.getY()<=initialY+0.001,"被生物挡住时不能靠向上爬恢复下降"));
        h.runAfterDelay(100,()->{
            h.assertFalse(MobFlight.controlled(pig),"旁边有空位时必须完成横移落地，不能一直悬在另一只猪上方");
            h.assertTrue(pig.onGround()&&!pig.isNoGravity()&&!pig.hasEffect(Pipa.AIRBORNE),"落地后恢复重力、姿势及原控制");
            h.assertFalse(pig.getBoundingBox().inflate(0.03,0,0.03).intersects(blocker.getBoundingBox()),"不能通过压入另一生物完成落地");
            blocker.discard();pig.discard();h.succeed();
        });
    }
    /** 探地距离 0.12 格大于单 tick 下降上限 0.1 格，不能把“快碰地”当作“已落地”。 */
    @GameTest(template="range",batch="pipa-landing-gap")
    public static void landingMustReachGround(GameTestHelper h) {
        h.setBlock(new BlockPos(54,2,54),Blocks.STONE);
        var pig=h.spawn(EntityType.PIG,new Vec3(54.5,3.1118361358,54.5));pig.setNoAi(true);pig.setNoGravity(true);
        pig.getPersistentData().putBoolean("PipaFlightRecovery",true);pig.getPersistentData().putBoolean("PipaOriginalNoGravity",false);
        MobFlight.entityTick(pig);
        h.assertTrue(MobFlight.controlled(pig)&&!pig.onGround(),"离地约0.012格时仍须保留下降控制，不能提前释放");
        MobFlight.entityTick(pig);
        h.assertTrue(pig.onGround()&&!MobFlight.controlled(pig)&&!pig.isNoGravity(),"下一步实际接触地面才释放并恢复重力");
        pig.discard();h.succeed();
    }
    /** 下降路径被另一只生物阻挡时必须悬停或横移，不能重新爬升。 */
    @GameTest(template="range",batch="pipa-landing-blocked")
    public static void landingDoesNotClimb(GameTestHelper h) {
        var obstacle=h.spawn(EntityType.COW,new Vec3(54.5,3,54.5));obstacle.setNoAi(true);obstacle.setNoGravity(true);
        var pig=h.spawn(EntityType.PIG,new Vec3(54.5,6,54.5));pig.setNoAi(true);pig.setNoGravity(true);
        pig.setPos(pig.getX(),obstacle.getBoundingBox().maxY+0.005,pig.getZ());
        pig.getPersistentData().putBoolean("PipaFlightRecovery",true);
        pig.getPersistentData().putBoolean("PipaOriginalNoGravity",false);
        double before=pig.getY();
        MobFlight.entityTick(pig);
        h.assertTrue(pig.getY()<=before,"安全下降遇实体阻挡不得向上避障，dy="+(pig.getY()-before));
        pig.discard();obstacle.discard();h.succeed();
    }

    /** 用真实 ServerPlayer 逐步核对授权、刷新、结束和原有权限；不把模拟 tick 写成客户端操作已测。 */
    @GameTest(template="range",batch="pipa-player-lifecycle")
    public static void playerPermissionLifecycle(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);
        p.moveTo(h.absoluteVec(new Vec3(54,8,54)));p.setOnGround(false);
        float speed=p.getAbilities().getFlyingSpeed();
        p.addEffect(new MobEffectInstance(Pipa.MILK,100));tick(p);
        h.assertTrue(p.getAbilities().mayfly,"狗奶授予生存飞行权限");
        p.getAbilities().flying=true;tick(p);
        h.assertTrue(p.hasEffect(Pipa.AIRBORNE),"实际飞行才启用姿势");
        Pipa.DOG_MILK.get().finishUsingItem(new ItemStack(Pipa.DOG_MILK.get()),h.getLevel(),p);
        h.assertTrue(p.getEffect(Pipa.MILK).getDuration()==1200,"重复饮用刷新而非叠加");
        p.removeEffect(Pipa.MILK);p.setDeltaMovement(0,0.5,0);tick(p);
        h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying && PlayerFlight.landing(p),"清除效果回收本来源并进入下降");
        h.assertTrue(p.getDeltaMovement().y<=0 && p.getDeltaMovement().y>=-0.1,"下降阶段不能继续上升或超速坠落");
        p.setOnGround(true);tick(p);
        h.assertFalse(PlayerFlight.landing(p)||p.hasEffect(Pipa.AIRBORNE),"首次落地结束保护和姿势");
        h.assertTrue(p.getAbilities().getFlyingSpeed()==speed,"归还原飞行速度");
        // 进入效果前已有 mayfly 的来源不归本模组拥有，结束后必须原样交还。
        p.getAbilities().mayfly=true;p.getAbilities().flying=true;
        p.addEffect(new MobEffectInstance(Pipa.MILK,1));tick(p);p.removeEffect(Pipa.MILK);tick(p);
        h.assertTrue(p.getAbilities().mayfly && p.getAbilities().flying && !PlayerFlight.landing(p),"保留进入前已有的外部飞行");
        for(var mode:new GameType[]{GameType.CREATIVE,GameType.SPECTATOR}) {
            p.setGameMode(mode);p.addEffect(new MobEffectInstance(Pipa.MILK,1));tick(p);p.removeEffect(Pipa.MILK);tick(p);
            h.assertTrue(p.getAbilities().mayfly && !PlayerFlight.landing(p),"保留原版模式权限："+mode);
        }
        h.getLevel().getServer().getPlayerList().remove(p);h.succeed();
    }

    /** 切歌、停玩偶、拆除都不应触碰玩家持有的独立食物效果。 */
    @GameTest(template="range",batch="pipa-food-independent")
    public static void foodIndependentOfDoll(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);
        p.moveTo(h.absoluteVec(new Vec3(54,8,54)));
        p.addEffect(new MobEffectInstance(Pipa.MILK,1200));p.addEffect(new MobEffectInstance(Pipa.PEACE,3600));
        tick(p);p.getAbilities().flying=true;tick(p);
        var pos=new BlockPos(54,2,54);h.setBlock(pos,FriendsWine.DOLL.get());DollBlockEntity d=h.getBlockEntity(pos);
        d.startPlaying();SongState.of(d).pipa$toggle();SongState.of(d).pipa$toggle();d.stopPlaying();h.setBlock(pos,Blocks.AIR);
        tick(p);
        h.assertTrue(p.hasEffect(Pipa.MILK)&&p.hasEffect(Pipa.PEACE)&&p.hasEffect(Pipa.AIRBORNE)&&p.getAbilities().flying,"玩偶操作不能清除独立食物来源");
        p.setHealth(0);tick(p);
        h.assertFalse(p.getAbilities().mayfly||p.getAbilities().flying||PlayerFlight.landing(p)||p.hasEffect(Pipa.AIRBORNE),"死亡立即释放本模组权限和姿势");
        h.getLevel().getServer().getPlayerList().remove(p);h.succeed();
    }
    private static void tick(net.minecraft.server.level.ServerPlayer p) {PlayerFlight.tick(p);}
    /** 最近的原曲玩偶仍拥有其人口；离开该来源后，琵琶曲只能在旧控制落地后重新补位。 */
    @GameTest(template="range",batch="pipa-doll-handoff",timeoutTicks=500)
    public static void nearestDollHandoff(GameTestHelper h) {
        for(var p:BlockPos.betweenClosed(40,0,40,70,0,70))h.setBlock(p,Blocks.STONE);
        var leftPos=new BlockPos(54,2,54);var rightPos=new BlockPos(59,2,54);
        h.setBlock(leftPos,FriendsWine.DOLL.get());h.setBlock(rightPos,FriendsWine.DOLL.get());
        DollBlockEntity left=h.getBlockEntity(leftPos),right=h.getBlockEntity(rightPos);
        var pig=h.spawn(EntityType.PIG,new Vec3(58.5,3,54.5));pig.setNoAi(true);
        SongState.of(left).pipa$toggle();left.startPlaying();
        h.onEachTick(()->{for(var guest:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60)))guest.discard();});
        for(var guest:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60)))guest.discard();
        h.startSequence().thenIdle(2).thenExecute(()->{
            h.assertTrue(MobFlight.controlled(pig),"唯一候选先归琵琶曲来源");right.startPlaying();
            for(var guest:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60)))guest.discard();
            h.assertTrue(DollBlockEntity.nearestActive(h.getLevel(),pig.position(),50)==right,"夹具确保新来源更近");
        }).thenWaitUntil(()->h.assertFalse(MobFlight.controlled(pig),"交接至原曲来源后先完成安全下降"))
                .thenExecute(()->{h.assertFalse(pig.isNoGravity()||pig.hasEffect(Pipa.AIRBORNE),"原曲归属不残留飞行姿势");right.stopPlaying();})
                .thenWaitUntil(()->h.assertTrue(MobFlight.controlled(pig)&&pig.hasEffect(Pipa.AIRBORNE),"近来源停止后，剩余琵琶曲按补位规则重新接管"))
                .thenExecute(()->left.stopPlaying()).thenSucceed();
    }
    /** 经过真实玩家文件保存和读取，核对离线暂停、下降恢复以及跨维度保留时长。 */
    @GameTest(template="range",batch="pipa-player-rejoin",timeoutTicks=200)
    public static void playerSaveRejoinAndDimension(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
        p.moveTo(h.absoluteVec(new Vec3(54,8,54)));p.setOnGround(false);
        p.addEffect(new MobEffectInstance(Pipa.MILK,731));p.addEffect(new MobEffectInstance(Pipa.PEACE,1731));
        tick(p);p.getAbilities().flying=true;tick(p);
        var profile=p.getGameProfile();
        h.getLevel().getServer().getPlayerList().remove(p);
        h.runAfterDelay(25,()->{
            var restored=player(h,profile);
            h.assertTrue(restored.hasEffect(Pipa.MILK)&&restored.getEffect(Pipa.MILK).getDuration()==731,"离线暂停狗奶并从玩家文件恢复");
            h.assertTrue(restored.getEffect(Pipa.PEACE).getDuration()==1731,"离线暂停忘情");
            tick(restored);
            var destination=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            restored.teleportTo(destination,0.5,100,0.5,0,0);tick(restored);
            h.assertTrue(restored.level()==destination && restored.getEffect(Pipa.MILK).getDuration()==731 && restored.getEffect(Pipa.PEACE).getDuration()==1731,"真实维度迁移不重置食物时长");
            restored.teleportTo(h.getLevel(),p.getX(),p.getY(),p.getZ(),0,0);restored.setOnGround(false);
            restored.getAbilities().flying=true;restored.removeEffect(Pipa.MILK);tick(restored);
            h.assertTrue(PlayerFlight.landing(restored),"重进后结束仍进入下降");
            h.getLevel().getServer().getPlayerList().remove(restored);
            var landing=player(h,profile);tick(landing);
            h.assertTrue(PlayerFlight.landing(landing)&&!landing.getAbilities().mayfly,"下降标记跨玩家文件重载，不遗留飞行权限");
            h.getLevel().getServer().getPlayerList().remove(landing);h.succeed();
        });
    }
    /** 原版 mock 固定 isCreative=true；权限测试必须用原生玩家及嵌入连接，避免测试永远走创造分支。 */
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"pipa-lifecycle");
        return player(h,profile);
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,com.mojang.authlib.GameProfile profile) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        return p;
    }
}
