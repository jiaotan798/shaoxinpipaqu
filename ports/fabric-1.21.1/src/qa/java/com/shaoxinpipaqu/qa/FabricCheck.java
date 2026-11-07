package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.friendswine.FriendsWine;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.SongState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 只由隔离专服运行，检查真实遥控器切歌、飞行交接和饮用。 */
public final class FabricCheck implements ModInitializer {
    static final Path ROOT = Path.of(System.getProperty("pipa.qa.root"));
    static final BlockPos DOLL = new BlockPos(0, 81, 0);
    private DollBlockEntity doll;
    private Pig pig;
    private int stage, age;

    static String read(String name) throws IOException {
        var file = ROOT.resolve(name);
        return Files.exists(file) ? Files.readString(file).trim() : "";
    }

    static void write(String name, String value) {
        try { Files.writeString(ROOT.resolve(name), value); }
        catch (IOException error) { throw new UncheckedIOException(error); }
    }

    private void next(int value) {
        stage = value;
        age = 0;
        write("stage.txt", Integer.toString(value));
    }

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (stage == 99 || stage == -1) return;
            try {
                var actor = server.getPlayerList().getPlayerByName("PipaActor");
                var viewer = server.getPlayerList().getPlayerByName("PipaViewer");
                if (actor == null || viewer == null) return;
                var level = server.overworld();
                if (++age > 1200) throw new AssertionError("联机阶段超时 " + stage);
                switch (stage) {
                    case 0 -> {
                        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                        level.setDayTime(6000);
                        for (var block : BlockPos.betweenClosed(-20, 80, -20, 20, 80, 20))
                            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
                        level.setBlockAndUpdate(DOLL, FriendsWine.DOLL.get().defaultBlockState());
                        doll = (DollBlockEntity) level.getBlockEntity(DOLL);
                        actor.connection.teleport(0.5, 81, 6.5, 180, -14);
                        viewer.connection.teleport(-3.5, 81, 7.5, 150, -14);
                        pig = EntityType.PIG.create(level);
                        pig.moveTo(3.5, 81, 0.5);
                        pig.setNoAi(true);
                        pig.setPersistenceRequired();
                        level.addFreshEntity(pig);
                        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FriendsWine.REMOTE.get()));
                        FriendsWine.REMOTE.get().useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(DOLL), Direction.UP, DOLL, false)));
                        actor.releaseUsingItem();
                        doll.startPlaying();
                        next(1);
                    }
                    case 1 -> {
                        if (SongState.of(doll).pipa$isSelected()) next(2);
                    }
                    case 2 -> {
                        if (!SongState.of(doll).pipa$isSelected() || doll.getMode() != DollBlockEntity.FULL)
                            throw new AssertionError("长按切歌不应重复触发或改变档位");
                        if (age < 80 || !ready(2)) return;
                        if (!MobFlight.controlled(pig) || !MobFlight.posed(pig) || pig.getY() <= 82)
                            throw new AssertionError("切歌后唯一候选应实际起飞");
                        doll.stopPlaying();
                        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Pipa.DOG_MILK.get(), 2));
                        next(3);
                    }
                    case 3 -> {
                        if (MobFlight.controlled(pig) || !pig.onGround() || !actor.hasEffect(Pipa.MILK)
                                || !actor.getAbilities().flying || !ready(3)) return;
                        if (actor.getMainHandItem().getCount() != 1) throw new AssertionError("狗奶应消耗一份");
                        actor.removeEffect(Pipa.MILK);
                        next(4);
                    }
                    case 4 -> {
                        if (!actor.onGround() || actor.getAbilities().mayfly || !ready(4)) return;
                        write("server-result.txt", "PASS 遥控切歌、飞行和降落、饮用狗奶与回收飞行权限");
                        next(99);
                    }
                }
            } catch (Throwable failure) {
                failure.printStackTrace();
                write("server-result.txt", "FAIL " + failure);
                next(-1);
            }
        });
    }

    private static boolean ready(int value) throws IOException {
        return read("actor-" + value + ".ok").startsWith("PASS") && read("viewer-" + value + ".ok").startsWith("PASS");
    }
}
