package com.shaoxinpipaqu;

import com.friendswine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/** 使用真实实体、交易菜单和方块碰撞验证关键边界，测试类不进入成品。 */
@GameTestHolder(Pipa.ID)
@PrefixGameTestTemplate(false)
public final class PipaGameTests {
    private PipaGameTests() {}
    private static void guests(GameTestHelper h) {
        for (var e:h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds().inflate(60))) e.discard();
    }
    private static DollBlockEntity doll(GameTestHelper h) {
        BlockPos pos=new BlockPos(54,2,54);
        h.setBlock(pos,FriendsWine.DOLL.get());
        return h.getBlockEntity(pos);
    }
    @GameTest(template="range",batch="pipa-song")
    public static void songPersistence(GameTestHelper h) {
        var d=doll(h);
        h.assertFalse(SongState.of(d).pipa$isSelected(),"旧存档/新玩偶默认原曲");
        SongState.of(d).pipa$toggle();
        h.assertFalse(d.isPlaying(),"停止时选曲不能启动");
        d.startPlaying(); guests(h);
        int mode=d.getMode();
        SongState.of(d).pipa$toggle();
        h.assertTrue(d.getMode()==mode,"播放中切歌保留档位");
        h.assertTrue(h.getLevel().getEntitiesOfClass(PartyGuestEntity.class,h.getBounds()).isEmpty(),"切歌不能额外生成立板");
        SongState.of(d).pipa$toggle();
        CompoundTag saved=d.saveWithoutMetadata(h.getLevel().registryAccess());
        d.stopPlaying();
        d.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(SongState.of(d).pipa$isSelected() && d.getMode()==mode,"保存恢复歌曲和档位");
        d.stopPlaying(); h.succeed();
    }
    @GameTest(template="range",batch="pipa-food")
    public static void foodConsumption(GameTestHelper h) {
        Player p=h.makeMockPlayer(GameType.SURVIVAL);
        h.getLevel().addFreshEntity(p);
        ItemStack meal=new ItemStack(Pipa.NOODLES.get(),2);
        ItemStack result=Pipa.NOODLES.get().finishUsingItem(meal,h.getLevel(),p);
        h.assertTrue(result.getCount()==1 && p.getInventory().countItem(Items.BOWL)==1,"堆叠吃面只消耗一个并返碗");
        h.assertTrue(p.getEffect(Pipa.PEACE).getDuration()==3600,"忘情持续180秒");
        result=Pipa.NOODLES.get().finishUsingItem(result,h.getLevel(),p);
        h.assertTrue(result.is(Items.BOWL),"最后一份返回碗");
        ItemStack milk=new ItemStack(Pipa.DOG_MILK.get(),2);
        Pipa.DOG_MILK.get().finishUsingItem(milk,h.getLevel(),p);
        h.assertTrue(milk.getCount()==1 && p.getEffect(Pipa.MILK).getDuration()==1200,"狗奶60秒且不返容器");
        p.removeEffect(Pipa.MILK); p.discard(); h.succeed();
    }
    @GameTest(template="range",batch="pipa-peace")
    public static void movingPeace(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        Vec3 center=h.absoluteVec(new Vec3(54,3,54)); p.moveTo(center);
        p.addEffect(new MobEffectInstance(Pipa.PEACE,3600));
        var zombie=h.spawn(EntityType.ZOMBIE,new Vec3(56,3,54));
        var pig=h.spawn(EntityType.PIG,new Vec3(57,3,54));
        zombie.setNoAi(true); pig.setNoAi(true);
        h.assertTrue(PeaceAura.blocks(zombie,pig),"移动来源保护生物互攻");
        zombie.setTarget(pig); h.assertTrue(zombie.getTarget()==null,"阻止重新索敌");
        float health=pig.getHealth();
        h.assertFalse(pig.hurt(pig.damageSources().mobAttack(zombie),2),"真实攻击应被取消");
        h.assertTrue(pig.getHealth()==health,"取消攻击不扣血");
        p.moveTo(center.add(100,0,0));
        h.assertFalse(PeaceAura.blocks(zombie,pig),"离开移动范围应释放");
        h.assertTrue(pig.hurt(pig.damageSources().mobAttack(zombie),2),"离开后恢复伤害");
        h.getLevel().getServer().getPlayerList().remove(p); h.succeed();
    }
    @GameTest(template="range",batch="pipa-trades")
    public static void threeMaterialTrade(GameTestHelper h) {
        Player p=h.makeMockPlayer(GameType.SURVIVAL); h.getLevel().addFreshEntity(p);
        Villager v=h.spawn(EntityType.VILLAGER,new Vec3(54,2,54)); v.setNoAi(true);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NITWIT));
        ExtraTrades.ensure(v); v.setTradingPlayer(p);
        MerchantMenu menu=new MerchantMenu(1,p.getInventory(),v);
        int index=0;
        for (int i=0;i<v.getOffers().size();i++) if (ExtraTrades.noodles(v.getOffers().get(i))) index=i;
        menu.setSelectionHint(index);
        menu.getSlot(0).set(new ItemStack(Items.COOKED_BEEF,4));
        menu.getSlot(1).set(new ItemStack(Items.WHEAT,16));
        menu.slotsChanged(p.getInventory());
        h.assertTrue(menu.getSlot(2).getItem().is(Pipa.NOODLES.get()),"两槽材料形成面条预览");
        menu.clicked(2,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty(),"缺碗时普通取出必须失败");
        menu.quickMoveStack(p,2);
        h.assertTrue(p.getInventory().countItem(Pipa.NOODLES.get())==0,"缺碗时Shift交易也必须失败");
        p.getInventory().add(new ItemStack(Items.BOWL,1));
        menu.clicked(2,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().is(Pipa.NOODLES.get()) && p.getInventory().countItem(Items.BOWL)==0,"成功交易额外扣一个碗");
        h.assertTrue(menu.getSlot(0).getItem().getCount()==2 && menu.getSlot(1).getItem().getCount()==8,"同步扣两槽材料");
        h.assertTrue(v.getOffers().get(index).getUses()==1,"共享库存只增加一次使用");
        p.discard(); h.succeed();
    }
    @GameTest(template="range",batch="pipa-collision")
    public static void sweptCollision(GameTestHelper h) {
        var pig=h.spawn(EntityType.PIG,new Vec3(54.5,3,54.5)); pig.setNoAi(true);
        h.setBlock(new BlockPos(56,3,54),Blocks.STONE);
        h.assertFalse(MobFlight.safe(h.getLevel(),pig,new Vec3(4,0,0),false),"路径中间有墙，即使终点为空也不能通过");
        h.assertTrue(MobFlight.safe(h.getLevel(),pig,new Vec3(-1,0,0),false),"通行路径不应被误拦");
        h.setBlock(new BlockPos(54,2,54),Blocks.STONE);
        h.assertFalse(MobFlight.safe(h.getLevel(),pig,Vec3.ZERO,true),"巡航脚下不足两格时仍需避障");
        h.assertTrue(MobFlight.safe(h.getLevel(),pig,new Vec3(0,0.1,0),true),"允许纯上升补足净空，避免玩偶顶面锁死巡航");
        h.setBlock(new BlockPos(54,4,54),Blocks.STONE);
        h.assertFalse(MobFlight.safe(h.getLevel(),pig,new Vec3(0,0.3,0),true),"净空恢复不能忽略身体上方的实物碰撞");
        h.succeed();
    }
    @GameTest(template="range",batch="pipa-population",timeoutTicks=120)
    public static void populationAndRecovery(GameTestHelper h) {
        guests(h); h.onEachTick(() -> guests(h));
        var d=doll(h); SongState.of(d).pipa$toggle();
        java.util.List<net.minecraft.world.entity.animal.Pig> pigs=new java.util.ArrayList<>();
        for (BlockPos p:BlockPos.betweenClosed(35,0,40,65,0,55)) h.setBlock(p,Blocks.STONE);
        for (int i=0;i<9;i++) { var p=h.spawn(EntityType.PIG,new Vec3(40+i*2,3,48)); p.setNoAi(true); pigs.add(p); }
        var bat=h.spawn(EntityType.BAT,new Vec3(58,3,52)); bat.setNoAi(true); bat.setNoGravity(true);
        d.startPlaying(); guests(h);
        h.startSequence().thenIdle(10).thenExecute(() -> {
            long population=h.getLevel().getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(d.getBlockPos()).inflate(50),m -> m.isAlive() && m.position().distanceToSqr(Vec3.atCenterOf(d.getBlockPos()))<=2500).size();
            long selected=pigs.stream().filter(MobFlight::controlled).count();
            h.assertTrue(population==10 && selected==2,"十只生物必须选两只，实际总数="+population+"、选中猪="+selected);
            h.assertFalse(MobFlight.controlled(bat),"原生飞行者计入总数，但不能接管新增轨迹");
            // 模拟卸载后仅剩持久化恢复字段，重载时必须进入安全下降而非永久悬浮。
            MobFlight.unload(h.getLevel());
            d.stopPlaying();
        // 先让实体 tick 依据 NBT 重建下降控制，不能在 map 刚清空的同一 tick 误判为已经落地。
        }).thenIdle(2).thenWaitUntil(() -> h.assertTrue(pigs.stream().noneMatch(MobFlight::controlled),"重载后必须完成安全下降而非永久悬浮"))
                .thenExecute(() -> h.assertTrue(pigs.stream().noneMatch(Entity::isNoGravity),"重载恢复原重力")).thenSucceed();
    }
    @GameTest(template="range",batch="pipa-restock",timeoutTicks=100)
    public static void extraRestock(GameTestHelper h) {
        Villager v=h.spawn(EntityType.VILLAGER,new Vec3(54,2,54)); v.setNoAi(true);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NITWIT));
        ExtraTrades.ensure(v);
        var offer=v.getOffers().stream().filter(ExtraTrades::noodles).findFirst().orElseThrow();
        for (int i=0;i<16;i++) offer.increaseUses();
        long original=h.getLevel().getDayTime();
        h.getLevel().setDayTime(original+24000);
        h.startSequence().thenIdle(22).thenExecute(() -> {
            h.assertTrue(offer.getUses()==0,"跨天补满附属库存");
            offer.increaseUses(); h.getLevel().setDayTime(original);
        }).thenIdle(22).thenExecute(() -> {
            h.assertTrue(offer.getUses()==1,"日期倒退不补货");
            h.getLevel().setDayTime(original+24000);
        }).thenIdle(22).thenExecute(() -> h.assertTrue(offer.getUses()==0,"回退后下一次跨天正常补货")).thenSucceed();
    }
    @GameTest(template="range",batch="pipa-flight",timeoutTicks=420)
    public static void takeoffAndLand(GameTestHelper h) {
        guests(h); h.onEachTick(() -> guests(h));
        var d=doll(h); SongState.of(d).pipa$toggle();
        var pig=h.spawn(EntityType.PIG,new Vec3(58.5,1,54.5)); pig.setNoAi(true);
        for (BlockPos p:BlockPos.betweenClosed(45,0,45,65,0,65)) h.setBlock(p,Blocks.STONE);
        d.startPlaying(); guests(h);
        h.startSequence().thenIdle(35).thenExecute(() -> {
            h.assertTrue(MobFlight.controlled(pig) && pig.hasEffect(Pipa.AIRBORNE),"单只保底起飞且同步姿势");
            h.assertTrue(pig.getY()>h.absolutePos(new BlockPos(58,3,54)).getY(),"起飞应该上升，实际="+pig.getY());
            var player=h.makeMockPlayer(GameType.SURVIVAL);
            player.moveTo(pig.position().add(0.3,0,0)); player.setDeltaMovement(Vec3.ZERO);
            pig.push(player); player.push(pig);
            h.assertTrue(player.getDeltaMovement().lengthSqr()==0,"飞行生物不能通过双向实体互推挤走玩家");
            d.cycleRemote(); h.assertTrue(d.getMode()==DollBlockEntity.ORBIT,"切档沿用名单");
            d.stopPlaying();
        }).thenWaitUntil(() -> h.assertFalse(MobFlight.controlled(pig),"停止后必须完成安全降落"))
                .thenExecute(() -> { h.assertFalse(pig.isNoGravity(),"落地恢复原重力"); h.assertFalse(pig.hasEffect(Pipa.AIRBORNE),"落地清除临时姿势"); }).thenSucceed();
    }
}
