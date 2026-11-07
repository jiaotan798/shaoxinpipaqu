package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import com.shaoxinpipaqu.AudioInfo;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.SongState;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 独立专服只判定服务器收到的真实游戏操作，两端文件用于观察到同一状态的阶段屏障。 */
public final class PortCheck {
    static final Path ROOT = Path.of(System.getProperty("pipa.qa.root"));
    static final BlockPos DOLL = new BlockPos(0, 81, 0);
    static final String TEST_PIG = "PipaQaPig";
    static final String TEST_TRADER = "PipaQaTrader";
    private DollBlockEntity doll;
    private Pig pig;
    private Mob landingSubject;
    private ArmorStand landingBlocker;
    private Villager trader;
    private int stage, age;
    private long songStart;
    private boolean prepared;

    static String read(String name) throws IOException {
        var file = ROOT.resolve(name);
        return Files.exists(file) ? Files.readString(file).trim() : "";
    }

    static void write(String name, String value) {
        try { Files.writeString(ROOT.resolve(name), value + System.lineSeparator()); }
        catch (IOException error) { throw new UncheckedIOException(error); }
    }

    static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    private void next(int value) {
        stage = value;
        age = 0;
        write("stage.txt", Integer.toString(value));
    }

    public void tick(MinecraftServer server) {
        if (stage == 99 || stage == -1) return;
        try {
            var actor = server.getPlayerList().getPlayerByName("PipaActor");
            var viewer = server.getPlayerList().getPlayerByName("PipaViewer");
            var level = server.overworld();
            if (!prepared) {
                // 首个服务端 tick 布置石台，让两端进服时从初始区块包接收已更新的光照。
                require(actor == null && viewer == null, "联机夹具须先于客户端加入布置");
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                level.setDayTime(6000);
                for (var block : BlockPos.betweenClosed(-20, 80, -20, 20, 80, 20))
                    level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(DOLL, FriendsWine.DOLL.get().defaultBlockState());
                doll = (DollBlockEntity) level.getBlockEntity(DOLL);
                prepared = true;
                write("scene-prepared.txt", "tick=" + level.getGameTime() + "，布置时未有客户端加入");
            }
            if (actor == null || viewer == null) return;
            require(++age <= (stage == 2 ? 2600 : 1200), "联机阶段超时 " + stage);
            switch (stage) {
                case 0 -> {
                    actor.setGameMode(GameType.SURVIVAL);
                    viewer.setGameMode(GameType.SURVIVAL);
                    actor.connection.teleport(0.5, 81, 7.5, 180, -14);
                    viewer.connection.teleport(-3.5, 81, 7.5, 150, -14);
                    actor.setRespawnPosition(level.dimension(), new BlockPos(0, 81, 7), 180, true, false);
                    pig = EntityType.PIG.create(level);
                    pig.moveTo(3.5, 81, 0.5);
                    pig.setCustomName(Component.literal(TEST_PIG));
                    pig.setNoAi(true);
                    pig.setPersistenceRequired();
                    level.addFreshEntity(pig);
                    // 前置右键绑定写真实物品数据，随后只有客户端左键包能把歌曲切换到琵琶曲。
                    actor.getInventory().clearContent();
                    actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FriendsWine.REMOTE.get()));
                    FriendsWine.REMOTE.get().useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(DOLL), Direction.UP, DOLL, false)));
                    actor.releaseUsingItem();
                    doll.startPlaying();
                    next(1);
                }
                case 1 -> {
                    if (SongState.of(doll).pipa$isSelected()) {
                        songStart = doll.getStartTick();
                        next(2);
                    }
                }
                case 2 -> {
                    require(SongState.of(doll).pipa$isSelected() && doll.getMode() == DollBlockEntity.FULL
                            && doll.getStartTick() == songStart, "长按切歌不应重复触发、重置时钟或改变档位");
                    if (!ready(2)) return;
                    require(MobFlight.controlled(pig) && MobFlight.posed(pig) && pig.getY() > 82, "切歌后唯一候选应实际起飞");
                    next(3);
                }
                case 3 -> {
                    if (age < 90 || !ready(3)) return;
                    require(doll.getMode() == DollBlockEntity.ORBIT && doll.getStartTick() == songStart, "右键切入三档需保留歌曲时钟");
                    // 最后一个卡点紧邻循环结束。等本轮名单稳定后，选择仍实际飞行的个体复现停档降落。
                    double loopAge = doll.getElapsedTicks(level.getGameTime()) % AudioInfo.LOOP_TICKS;
                    if (loopAge < 120 || loopAge > AudioInfo.LOOP_TICKS - 30) return;
                    landingSubject = flying(pig) ? pig : level.getEntitiesOfClass(Mob.class, new AABB(DOLL).inflate(20),
                            PortCheck::flying).stream().findFirst().orElse(null);
                    if (landingSubject == null) return;
                    prepareLandingOverlap(level);
                    next(4);
                }
                case 4 -> {
                    if (age == 1 || age % 20 == 0) write("landing-state.txt", landingState(level));
                    if (doll.getMode() != DollBlockEntity.STOPPED)
                        require(landingSubject.getBoundingBox().intersects(landingBlocker.getBoundingBox()),
                                "脱离重叠应发生在客户端停档之后");
                    if (!ready(4) || MobFlight.controlled(landingSubject) || !landingSubject.onGround()) return;
                    require(doll.getMode() == DollBlockEntity.STOPPED && !landingSubject.isNoGravity(), "停止后生物安全落地并恢复原重力");
                    require(!landingSubject.getBoundingBox().intersects(landingBlocker.getBoundingBox())
                            && !Pipa.data(landingSubject).getBoolean("PipaFlightRecovery"), "停止后需脱离预先重叠实体并清理恢复标记");
                    write("landing-state.txt", "PASS " + landingState(level));
                    landingBlocker.discard();
                    actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Pipa.DOG_MILK.get(), 2));
                    next(5);
                }
                case 5 -> {
                    if (!actor.hasEffect(Pipa.milk()) || !actor.getAbilities().flying || !ready(5)) return;
                    require(actor.getMainHandItem().getCount() == 1 && actor.getY() > 82, "狗奶应消耗一份并允许实际起飞");
                    actor.removeEffect(Pipa.milk());
                    next(6);
                }
                case 6 -> {
                    if (!actor.onGround() || !ready(6)) return;
                    require(!actor.getAbilities().mayfly && !actor.getAbilities().flying, "狗奶结束后回收飞行权限");
                    actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Pipa.NOODLES.get(), 2));
                    next(7);
                }
                case 7 -> {
                    if (!actor.hasEffect(Pipa.peace()) || !ready(7)) return;
                    require(actor.getMainHandItem().getCount() == 1 && actor.getInventory().countItem(Items.BOWL) == 1, "吃面应消耗一份并返碗");
                    var zombie = EntityType.ZOMBIE.create(level);
                    float health = pig.getHealth();
                    require(!pig.hurt(pig.damageSources().mobAttack(zombie), 2) && pig.getHealth() == health, "忘情牛肉面应拦截生物伤害");
                    prepareTrade(level, actor);
                    next(8);
                }
                case 8 -> {
                    if (!ready(8)) return;
                    require(actor.containerMenu instanceof MerchantMenu, "客户端应通过网络打开实际村民交易");
                    var menu = (MerchantMenu) actor.containerMenu;
                    var offer = menu.getOffers().stream().filter(o -> o.getResult().is(Pipa.NOODLES.get())).findFirst().orElseThrow();
                    require(actor.getInventory().countItem(Pipa.NOODLES.get()) == 0 && menu.getCarried().isEmpty()
                            && offer.getUses() == 0, "无碗时普通点击和 Shift 点击均不得完成交易");
                    actor.getInventory().add(new ItemStack(Items.BOWL));
                    next(9);
                }
                case 9 -> {
                    if (actor.getInventory().countItem(Pipa.NOODLES.get()) != 1 || !ready(9)) return;
                    require(actor.getInventory().countItem(Items.BOWL) == 0, "成功交易应额外扣一只碗");
                    actor.closeContainer();
                    actor.kill();
                    next(10);
                }
                case 10 -> {
                    if (!actor.isAlive() || !ready(10)) return;
                    require(!actor.hasEffect(Pipa.peace()) && !actor.getAbilities().mayfly, "正常重生后食物效果与飞行权限应清理");
                    write("server-result.txt", "PASS 网络切歌与档位、生物起落、两种食物、忘情防护、三材料村民交易、死亡重生；两客户端确认模型与四个卡点");
                    next(99);
                }
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            write("server-result.txt", "FAIL " + failure);
            next(-1);
        }
    }

    private static boolean flying(Mob mob) {
        return mob.getY() > 82 && MobFlight.controlled(mob) && MobFlight.posed(mob);
    }

    /** 在客户端发送停档前重现真实的近地重叠；盔甲架不受普通生物推挤，避免原版偶然解开夹住的实体。 */
    private void prepareLandingOverlap(ServerLevel level) {
        landingSubject.moveTo(8.5, 81.15, 0.5);
        landingSubject.setDeltaMovement(Vec3.ZERO);
        landingSubject.setOnGround(false);
        landingBlocker = EntityType.ARMOR_STAND.create(level);
        // 两个箱水平同心，首步横移仍有相同的相交体积，必须按分离距离判断。
        var standData = new CompoundTag();
        standData.putBoolean("Small", true);
        landingBlocker.readAdditionalSaveData(standData);
        landingBlocker.moveTo(8.5, 81, 0.5);
        landingBlocker.setNoGravity(true);
        require(level.addFreshEntity(landingBlocker) && landingSubject.getBoundingBox().intersects(landingBlocker.getBoundingBox()),
                "测试初态必须与固定活体重叠");
        write("landing-overlap.txt", landingState(level));
    }

    private String landingState(ServerLevel level) {
        boolean nearFloor = level.getBlockCollisions(landingSubject, landingSubject.getBoundingBox().move(0, -0.12, 0))
                .iterator().hasNext();
        return "tick=" + level.getGameTime() + ", entity=" + landingSubject.getType()
                + ", position=" + landingSubject.position() + ", velocity=" + landingSubject.getDeltaMovement()
                + ", overlap=" + landingSubject.getBoundingBox().intersects(landingBlocker.getBoundingBox())
                + ", alive=" + landingSubject.isAlive() + ", nearFloor=" + nearFloor + ", onGround=" + landingSubject.onGround()
                + ", controlled=" + MobFlight.controlled(landingSubject) + ", noGravity=" + landingSubject.isNoGravity()
                + ", recovery=" + Pipa.data(landingSubject).getBoolean("PipaFlightRecovery");
    }

    private void prepareTrade(ServerLevel level, ServerPlayer actor) {
        actor.getInventory().clearContent();
        actor.getInventory().setItem(9, new ItemStack(Items.COOKED_BEEF, 2));
        actor.getInventory().setItem(10, new ItemStack(Items.WHEAT, 8));
        trader = EntityType.VILLAGER.create(level);
        trader.setVillagerData(trader.getVillagerData().setProfession(VillagerProfession.NITWIT));
        trader.moveTo(0.5, 81, 5.5);
        trader.setCustomName(Component.literal(TEST_TRADER));
        trader.setNoAi(true);
        trader.setPersistenceRequired();
        level.addFreshEntity(trader);
    }

    private static boolean ready(int value) throws IOException {
        return read("actor-" + value + ".ok").startsWith("PASS") && read("viewer-" + value + ".ok").startsWith("PASS");
    }
}
