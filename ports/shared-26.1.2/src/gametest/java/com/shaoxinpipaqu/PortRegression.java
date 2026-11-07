package com.shaoxinpipaqu;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 使用两个平台相同的玩法断言，验证正式前置的切歌、飞行和附属食物。 */
public final class PortRegression {
    private static final BlockPos DOLL=new BlockPos(54,2,54);
    private PortRegression() {}

    public static void remoteAndFlight(GameTestHelper helper) {
        for (BlockPos pos:BlockPos.betweenClosed(46,0,46,65,0,65)) helper.setBlock(pos,Blocks.STONE);
        helper.setBlock(DOLL,FriendsWine.DOLL.get());
        DollBlockEntity doll=helper.getBlockEntity(DOLL,DollBlockEntity.class);
        Player player=helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FriendsWine.REMOTE.get()));
        BlockPos position=helper.absolutePos(DOLL);
        FriendsWine.REMOTE.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(position),Direction.UP,position,false)));
        player.stopUsingItem();
        RemoteControl.receive(player);
        RemoteControl.receive(player);
        helper.assertTrue(SongState.of(doll).pipa$isSelected() && !doll.isPlaying(),"停止时切歌不启动，同刻重复请求被去抖");
        helper.assertTrue(doll.getUpdateTag(helper.getLevel().registryAccess()).getBooleanOr("PipaSelected",false),"选曲进入方块实体同步数据");
        var pig=helper.spawn(EntityType.PIG,new Vec3(58.5,1,54.5));
        pig.removeFreeWill();
        doll.startPlaying();
        helper.startSequence().thenIdle(35).thenExecute(() -> {
            helper.assertTrue(MobFlight.controlled(pig) && pig.hasEffect(Pipa.AIRBORNE) && pig.getY()>helper.absolutePos(DOLL).getY(),"琵琶曲单只保底起飞并同步姿势");
            doll.stopPlaying();
        }).thenWaitUntil(() -> helper.assertTrue(!MobFlight.controlled(pig),"停播后安全降落")).thenExecute(() ->
                helper.assertTrue(!pig.isNoGravity() && !pig.hasEffect(Pipa.AIRBORNE),"落地恢复重力并清理飞行效果")).thenSucceed();
    }

    /** 复现低空着陆重叠；盔甲架的窄碰撞箱确保横移第一步不会减少重叠体积。 */
    public static void landingOverlap(GameTestHelper helper) {
        for (BlockPos pos:BlockPos.betweenClosed(55,0,51,62,0,58)) helper.setBlock(pos,Blocks.STONE);
        var pig=helper.spawn(EntityType.PIG,new Vec3(58.2924,1.14955,54.33394));
        var blocker=helper.spawn(EntityType.ARMOR_STAND,new Vec3(58.2924,1,54.33394));
        pig.removeFreeWill();
        blocker.setNoGravity(true);
        double initialX=pig.getX(), initialY=pig.getY();
        helper.assertTrue(pig.getBoundingBox().deflate(0.1,0,0.1).intersects(blocker.getBoundingBox())
                && pig.getBbWidth()-blocker.getBbWidth()>0.2,"盔甲架水平完全包含在猪的碰撞箱内");
        helper.assertTrue(!MobFlight.safe(helper.getLevel(),pig,new Vec3(-0.1,0,0),false),"普通起飞和巡航继续拒绝扫掠到生物");

        // 从保存恢复时直接进入降落状态，无需先让随机巡航路线碰到同一个生物。
        Pipa.data(pig).putBoolean("PipaFlightRecovery",true);
        pig.setNoGravity(true);
        MobFlight.entityTick(pig);
        helper.assertTrue(MobFlight.controlled(pig) && pig.getX()!=initialX && pig.getY()==initialY,
                "降落第一次移动即水平远离已重叠生物");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!MobFlight.controlled(pig),"脱离后成功落地"))
                .thenExecute(() -> helper.assertTrue(pig.onGround() && !pig.isNoGravity() && !pig.hasEffect(Pipa.AIRBORNE),
                        "落地恢复重力并清理飞行效果")).thenSucceed();
    }

    public static void meals(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var noodles=new ItemStack(Pipa.NOODLES.get());
        var result=Pipa.NOODLES.get().finishUsingItem(noodles,helper.getLevel(),player);
        helper.assertTrue(result.is(Items.BOWL) && player.getEffect(Pipa.PEACE).getDuration()==3600,"完整食用牛肉面返碗并提供忘情效果");
        var milk=new ItemStack(Pipa.DOG_MILK.get(),2);
        Pipa.DOG_MILK.get().finishUsingItem(milk,helper.getLevel(),player);
        helper.assertTrue(milk.getCount()==1 && player.getEffect(Pipa.MILK).getDuration()==1200,"狗奶只消耗一份并提供飞行效果");
        helper.succeed();
    }

    public static void bowlTrade(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var villager=helper.spawn(EntityType.VILLAGER,DOLL);
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(),VillagerProfession.NITWIT));
        ExtraTrades.ensure(villager);
        villager.setTradingPlayer(player);
        var menu=new MerchantMenu(1,player.getInventory(),villager);
        int index=0;
        for(int i=0;i<villager.getOffers().size();i++)if(ExtraTrades.noodles(villager.getOffers().get(i)))index=i;
        menu.setSelectionHint(index);
        menu.getSlot(0).set(new ItemStack(Items.COOKED_BEEF,4));
        menu.getSlot(1).set(new ItemStack(Items.WHEAT,16));
        menu.slotsChanged(player.getInventory());
        helper.assertTrue(menu.getSlot(2).getItem().is(Pipa.NOODLES.get()),"正常显示牛肉面预览");
        menu.clicked(2,0,ContainerInput.PICKUP,player);
        helper.assertTrue(menu.getCarried().isEmpty(),"没有碗时普通取出被拦截");
        menu.quickMoveStack(player,2);
        helper.assertTrue(player.getInventory().countItem(Pipa.NOODLES.get())==0,"没有碗时快速交易被拦截");
        player.getInventory().add(new ItemStack(Items.BOWL));
        menu.clicked(2,0,ContainerInput.PICKUP,player);
        helper.assertTrue(menu.getCarried().is(Pipa.NOODLES.get()) && player.getInventory().countItem(Items.BOWL)==0,"成功交易额外扣碗");
        helper.assertTrue(menu.getSlot(0).getItem().getCount()==2 && menu.getSlot(1).getItem().getCount()==8,"成功交易同步扣两槽材料");
        helper.assertTrue(villager.getOffers().get(index).getUses()==1,"一次交易只增加一次库存用量");
        helper.succeed();
    }

    public static void peace(GameTestHelper helper) {
        var level=helper.getLevel();
        var player=helper.makeMockServerPlayerInLevel();
        var center=helper.absoluteVec(new Vec3(54,3,54));
        player.snapTo(center);
        player.addEffect(new MobEffectInstance(Pipa.PEACE,3600));
        var zombie=helper.spawn(EntityType.ZOMBIE,new Vec3(56,3,54));
        var pig=helper.spawn(EntityType.PIG,new Vec3(57,3,54));
        zombie.setTarget(pig);
        helper.assertTrue(zombie.getTarget()==null,"忘情范围内取消生物索敌");
        helper.assertTrue(!pig.hurtServer(level,pig.damageSources().mobAttack(zombie),2),"忘情范围内取消攻击");
        player.snapTo(center.add(100,0,0));
        helper.assertTrue(pig.hurtServer(level,pig.damageSources().mobAttack(zombie),2),"离开范围后恢复伤害");
        level.getServer().getPlayerList().remove(player);
        helper.succeed();
    }
}
