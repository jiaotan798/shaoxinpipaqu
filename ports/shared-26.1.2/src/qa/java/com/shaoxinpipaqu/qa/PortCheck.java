package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import com.shaoxinpipaqu.ExtraTrades;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.SongState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 独立专服按阶段核对真实交互和状态，由两个客户端分别确认观察结果。 */
public final class PortCheck {
    static final Path ROOT=Path.of(System.getProperty("pipa.qa.root"));
    static final BlockPos DOLL=new BlockPos(0,81,0);
    private DollBlockEntity doll;
    private Pig pig;
    private long startTick;
    private int stage,age,orbitAt;

    static String read(String name) throws IOException {
        var file=ROOT.resolve(name);
        return Files.exists(file)?Files.readString(file).trim():"";
    }

    static void write(String name,String value) {
        try { Files.writeString(ROOT.resolve(name),value); }
        catch(IOException failure) { throw new UncheckedIOException(failure); }
    }

    static void require(boolean pass,String message) {
        if(!pass)throw new AssertionError(message);
    }

    private void next(int value) {
        stage=value;
        age=0;
        write("stage.txt",Integer.toString(value));
    }

    void tick(MinecraftServer server) {
        if(stage==99||stage==-1)return;
        try {
            var actor=server.getPlayerList().getPlayerByName("PipaActor");
            var viewer=server.getPlayerList().getPlayerByName("PipaViewer");
            if(actor==null||viewer==null)return;
            var level=server.overworld();
            require(++age<=1800,"联机阶段超时 "+stage);
            switch(stage) {
                case 0 -> {
                    setup(server,level,actor,viewer);
                    next(1);
                }
                case 1 -> {
                    if(SongState.of(doll).pipa$isSelected()) {
                        require(ready(1),"切歌前双方应听到原曲");
                        startTick=doll.getStartTick();
                        next(2);
                    }
                }
                case 2 -> {
                    require(SongState.of(doll).pipa$isSelected()&&doll.getMode()==DollBlockEntity.FULL
                            &&doll.getStartTick()==startTick,"长按切歌应保持选曲、档位和起始时刻");
                    if(!ready(2))return;
                    require(MobFlight.controlled(pig)&&MobFlight.posed(pig)&&pig.getY()>82,"唯一候选应实际起飞");
                    require(doll.getUpdateTag(level.registryAccess()).getBooleanOr("PipaSelected",false),"选曲必须保存到同步标签");
                    next(3);
                }
                case 3 -> {
                    require(SongState.of(doll).pipa$isSelected()&&doll.getStartTick()==startTick,"遥控改档不能切曲或重置起始时刻");
                    if(doll.getMode()!=DollBlockEntity.ORBIT)return;
                    if(orbitAt==0)orbitAt=age;
                    if(age-orbitAt<35||!ready(3))return;
                    doll.stopPlaying();
                    actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Pipa.DOG_MILK.get(),2));
                    next(4);
                }
                case 4 -> {
                    if(MobFlight.controlled(pig)||!pig.onGround()||!actor.hasEffect(Pipa.MILK)
                            ||!actor.getAbilities().flying||!ready(4))return;
                    require(!pig.isNoGravity()&&!MobFlight.posed(pig),"停播降落后恢复重力和姿势");
                    require(actor.getMainHandItem().getCount()==1,"一次狗奶应只消耗一份");
                    actor.removeEffect(Pipa.MILK);
                    next(5);
                }
                case 5 -> {
                    if(!actor.onGround()||actor.getAbilities().mayfly||!ready(5))return;
                    actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Pipa.NOODLES.get()));
                    next(6);
                }
                case 6 -> {
                    if(!actor.hasEffect(Pipa.PEACE)||!ready(6))return;
                    require(actor.getMainHandItem().is(Items.BOWL),"完整吃面后应返碗");
                    var zombie=EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
                    require(zombie!=null,"无法创建攻击测试生物");
                    require(!pig.hurtServer(level,pig.damageSources().mobAttack(zombie),2),"忘情范围内应拦截伤害");
                    trade(level,actor);
                    write("server-result.txt","PASS 真实连接切歌与改档、起落、两种食物、攻击保护和交易扣碗");
                    next(99);
                }
            }
        } catch(Throwable failure) {
            failure.printStackTrace();
            write("server-result.txt","FAIL "+failure);
            next(-1);
        }
    }

    private void setup(MinecraftServer server,ServerLevel level,ServerPlayer actor,ServerPlayer viewer) {
        level.getGameRules().set(GameRules.SPAWN_MOBS,false,server);
        level.getGameRules().set(GameRules.ADVANCE_TIME,false,server);
        level.clockManager().setTotalTicks(level.dimensionType().defaultClock().orElseThrow(),6000);
        for(var block:BlockPos.betweenClosed(-20,80,-20,20,80,20))level.setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(DOLL,FriendsWine.DOLL.get().defaultBlockState());
        doll=(DollBlockEntity)level.getBlockEntity(DOLL);
        require(doll!=null,"玩偶未加载");
        actor.connection.teleport(0.5,81,7.5,180,-14);
        viewer.connection.teleport(-3.5,81,8.5,155,-14);
        pig=EntityType.PIG.create(level,EntitySpawnReason.COMMAND);
        require(pig!=null,"无法创建飞行测试生物");
        pig.snapTo(3.5,81,0.5);
        pig.setNoAi(true);
        pig.setPersistenceRequired();
        level.addFreshEntity(pig);
        actor.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FriendsWine.REMOTE.get()));
        FriendsWine.REMOTE.get().useOn(new UseOnContext(actor,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(DOLL),Direction.UP,DOLL,false)));
        actor.stopUsingItem();
        doll.startPlaying();
    }

    /** 用真实专服玩家与原版结果槽验收三种材料扣取；交易菜单点击由服务端驱动。 */
    private static void trade(ServerLevel level,ServerPlayer actor) {
        var villager=EntityType.VILLAGER.create(level,EntitySpawnReason.COMMAND);
        require(villager!=null,"无法创建交易测试村民");
        villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.NITWIT));
        ExtraTrades.ensure(villager);
        villager.setTradingPlayer(actor);
        var bowl=actor.getMainHandItem().copy();
        actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var menu=new MerchantMenu(1,actor.getInventory(),villager);
        int index=0;
        for(int i=0;i<villager.getOffers().size();i++)if(ExtraTrades.noodles(villager.getOffers().get(i)))index=i;
        menu.setSelectionHint(index);
        menu.getSlot(0).set(new ItemStack(Items.COOKED_BEEF,4));
        menu.getSlot(1).set(new ItemStack(Items.WHEAT,16));
        menu.slotsChanged(actor.getInventory());
        menu.clicked(2,0,ContainerInput.PICKUP,actor);
        require(menu.getCarried().isEmpty()&&menu.quickMoveStack(actor,2).isEmpty(),"缺碗应拦截普通和快速交易");
        actor.setItemInHand(InteractionHand.MAIN_HAND,bowl);
        menu.clicked(2,0,ContainerInput.PICKUP,actor);
        require(menu.getCarried().is(Pipa.NOODLES.get())&&actor.getMainHandItem().isEmpty(),"成功交易应额外扣碗");
        require(menu.getSlot(0).getItem().getCount()==2&&menu.getSlot(1).getItem().getCount()==8
                &&villager.getOffers().get(index).getUses()==1,"两格材料和库存使用量只改变一次");
    }

    private static boolean ready(int stage) throws IOException {
        return read("actor-"+stage+".ok").startsWith("PASS")&&read("viewer-"+stage+".ok").startsWith("PASS");
    }
}
