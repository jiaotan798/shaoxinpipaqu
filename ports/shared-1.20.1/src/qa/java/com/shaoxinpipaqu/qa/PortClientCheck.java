package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.RemoteControl;
import com.shaoxinpipaqu.client.MemeEffects;
import com.shaoxinpipaqu.client.MemeTiming;
import com.shaoxinpipaqu.mixin.QuadrupedAccessor;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** 后台通过游戏按键状态和原生交互包测试，不发送任何桌面键鼠事件。 */
public final class PortClientCheck {
    private static final PortClientCheck CHECK = new PortClientCheck();
    private final String role = System.getProperty("pipa.qa.role");
    private final EnumSet<MemeTiming.Cue> captured = EnumSet.noneOf(MemeTiming.Cue.class);
    private int stage, age, usedAt, jumpAt, tradeAt, poseSamples;
    private boolean done, windowChecked, respawned;
    private int skyLight = -1;
    private String startup = "";
    private volatile SoundInstance wineTrack, pipaTrack;

    private PortClientCheck() {}
    public static void clientTick(Minecraft mc) { CHECK.tick(mc); }
    public static void afterLevel(float partial) { CHECK.render(Minecraft.getInstance(), partial); }

    public static void captureAudio(SoundInstance sound, AudioStream stream) {
        if (stream.getFormat().getFrameSize() <= 0) return;
        switch (sound.getLocation().toString()) {
            case "friendswine:doll_music" -> CHECK.wineTrack = sound;
            case "shaoxinpipaqu:pipa" -> CHECK.pipaTrack = sound;
        }
    }

    /** 在实际主模型绘制前读取腿部，排除只有网络姿势标记、模型仍在站立的假通过。 */
    public static void captureModel(LivingEntity entity, EntityModel<?> model) {
        if (CHECK.done || CHECK.stage != 2 || !(entity instanceof Pig) || !MobFlight.posed(entity)
                || !entity.getName().getString().equals(PortCheck.TEST_PIG) || !(model instanceof QuadrupedAccessor legs)) return;
        if (Math.abs(legs.pipa$frontRight().xRot + Math.PI / 2) < 0.1
                && Math.abs(legs.pipa$frontLeft().xRot + Math.PI / 2) < 0.1
                && Math.abs(Math.abs(legs.pipa$frontRight().yRot) - Math.PI / 6) < 0.1
                && Math.abs(Math.abs(legs.pipa$frontLeft().yRot) - Math.PI / 6) < 0.1
                && legs.pipa$frontRight().yRot * legs.pipa$frontLeft().yRot < 0
                && Math.abs(legs.pipa$hindRight().xRot - Math.PI / 2) < 0.1) CHECK.poseSamples++;
    }

    private void render(Minecraft mc, float partial) {
        if (done || stage != 2 || mc.level == null || mc.player == null) return;
        try {
            if (!(mc.level.getBlockEntity(PortCheck.DOLL) instanceof DollBlockEntity doll)) return;
            var cue = MemeEffects.active(doll, partial);
            if (cue == null || captured.contains(cue)) return;
            double phase = MemeTiming.phaseMs(doll.getElapsedTicks(mc.level.getGameTime()) + partial);
            if (cue == MemeTiming.Cue.MJ ? phase < cue.startMs + 300 : MemeTiming.alpha(cue, phase) < 0.999F) return;
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                Set<Integer> colors = palette(mc, cue);
                int matched = 0;
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                    if (colors.contains(rgb(image.getPixelRGBA(x, y)))) matched++;
                image.writeToFile(PortCheck.ROOT.resolve(role + "-" + cue.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
                PortCheck.require(matched >= 30, cue + " 时钟已触发但真实帧缓冲没有足够纹理像素：" + matched);
            }
            captured.add(cue);
        } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
    }

    /** 选取原图不透明的常见彩色像素。背景灰白、天空和 UI 不计入表情包验证。 */
    private static Set<Integer> palette(Minecraft mc, MemeTiming.Cue cue) throws IOException {
        var colors = new HashSet<Integer>();
        if (cue == MemeTiming.Cue.MJ) {
            for (int i = 0; i < 6; i++) colors.add(MemeTiming.mjColor(i));
            return colors;
        }
        String name = switch (cue) {
            case DOG -> "big_dog";
            case CABBAGE -> "cabbage";
            case COURAGE -> "courage";
            default -> throw new AssertionError(cue);
        };
        var frequencies = new HashMap<Integer, Integer>();
        var id = new ResourceLocation(Pipa.ID, "textures/meme/" + name + ".png");
        try (var input = mc.getResourceManager().getResourceOrThrow(id).open(); var image = NativeImage.read(input)) {
            for (int y = image.getHeight() / 8; y < image.getHeight() * 7 / 8; y++)
                for (int x = image.getWidth() / 8; x < image.getWidth() * 7 / 8; x++) {
                    int pixel = image.getPixelRGBA(x, y), red = pixel & 255, green = pixel >>> 8 & 255, blue = pixel >>> 16 & 255;
                    if (pixel >>> 24 == 255 && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) >= 15)
                        frequencies.merge(rgb(pixel), 1, Integer::sum);
                }
        }
        frequencies.entrySet().stream().sorted(Map.Entry.<Integer,Integer>comparingByValue().reversed()).limit(40)
                .forEach(entry -> colors.add(entry.getKey()));
        return colors;
    }

    private static int rgb(int rgba) { return (rgba & 255) << 16 | rgba & 0xFF00 | rgba >>> 16 & 255; }

    private void tick(Minecraft mc) {
        if (done) return;
        try {
            long window = mc.getWindow().getWindow();
            PortCheck.require(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE
                    && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE
                    && !mc.mouseHandler.isMouseGrabbed(), "后台窗口必须隐藏、无焦点且不捕获鼠标");
            if (!windowChecked) {
                windowChecked = true;
                PortCheck.write(role + "-window.txt", "PASS hidden, unfocused, mouse not grabbed; checked each client tick");
            }
            if (mc.level == null || mc.player == null) {
                String screen = mc.screen == null ? "none" : mc.screen.getClass().getName();
                String overlay = mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName();
                String state = "screen=" + screen + " overlay=" + overlay;
                // 隐藏客户端无法人工读断连界面，将原版完整原因落盘，避免只留下登录超时。
                if (mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)
                    state += " reason=" + mc.screen.getNarrationMessage().getString();
                if (!startup.equals(state)) {
                    startup = state;
                    PortCheck.write(role + "-startup.txt", state);
                }
                return;
            }
            PortCheck.require(mc.getSingleplayerServer() == null, "验收必须连接独立专服");
            String state = PortCheck.read("stage.txt");
            if (state.isEmpty()) return;
            int next = Integer.parseInt(state);
            if (stage != next) {
                stage = next;
                age = 0;
                usedAt = 0;
                jumpAt = 0;
                tradeAt = 0;
            }
            if (stage == -1) throw new AssertionError("专服验收失败");
            if (stage == 99) { finish(mc, "PASS 隐藏窗口、TCP 状态同步、实际飞行模型与四个卡点"); return; }
            age++;
            boolean actor = role.equals("actor");
            if (stage <= 4) lookAt(mc.player, new Vec3(0.5, 84.2, 0.5));
            mc.options.hideGui = stage < 8;
            if (actor) {
                mc.options.keyAttack.setDown((stage == 1 && PortCheck.read("actor-1.ok").startsWith("PASS")
                        && PortCheck.read("viewer-1.ok").startsWith("PASS") || stage == 2 && age <= 20)
                        && mc.screen == null && mc.getOverlay() == null && RemoteControl.held(mc.player));
                mc.options.keyUse.setDown(false);
                mc.options.keyJump.setDown(false);
            }
            var doll = mc.level.getBlockEntity(PortCheck.DOLL) instanceof DollBlockEntity d ? d : null;
            var subject = actor ? mc.player : mc.level.players().stream()
                    .filter(player -> player.getGameProfile().getName().equals("PipaActor")).findFirst().orElse(null);
            switch (stage) {
                case 1 -> {
                    if (doll == null) return;
                    int sky = mc.level.getBrightness(LightLayer.SKY, PortCheck.DOLL.above(2));
                    if (skyLight != sky) {
                        skyLight = sky;
                        PortCheck.write(role + "-lighting.txt", "玩偶上方天空光照=" + sky);
                    }
                    if (sky == 15 && wineTrack != null && mc.getSoundManager().isActive(wineTrack))
                        ready("原曲 PCM 已解码且声道有效，玩偶上方天空光照正常");
                }
                case 2 -> {
                    if (captured.size() == MemeTiming.Cue.values().length && poseSamples >= 3
                            && pipaTrack != null && mc.getSoundManager().isActive(pipaTrack))
                        ready("琵琶曲 PCM 声道有效；实绘猪飞行关节、MJ 和三张表情包；模型样本 " + poseSamples);
                }
                case 3, 4 -> {
                    if (actor && usedAt == 0 && RemoteControl.held(mc.player) && mc.screen == null) use(mc);
                    if (actor && usedAt != 0 && age - usedAt == 4) mc.gameMode.releaseUsingItem(mc.player);
                    if (doll != null && doll.getMode() == (stage == 3 ? DollBlockEntity.ORBIT : DollBlockEntity.STOPPED))
                        ready(stage == 3 ? "三档同步" : "停止同步");
                }
                case 5, 7 -> {
                    if (actor) consume(mc);
                    if (stage == 5 && subject != null && MobFlight.posed(subject) && subject.getY() > 82) ready("狗奶飞行姿势同步");
                    if (stage == 7 && (!actor || mc.player.hasEffect(Pipa.peace()))) ready("牛肉面流程已观察");
                }
                case 6 -> {
                    if (subject != null && !MobFlight.posed(subject)) ready("飞行姿势清除");
                }
                case 8 -> {
                    if (!actor) ready("进入交易阶段");
                    else tryTradeWithoutBowl(mc);
                }
                case 9 -> {
                    if (!actor) ready("进入扣碗阶段");
                    else if (mc.player.containerMenu instanceof MerchantMenu menu) {
                        if (usedAt == 0 && mc.player.getInventory().countItem(Items.BOWL) == 1) {
                            clickTrade(mc, menu, ClickType.QUICK_MOVE);
                            usedAt = age;
                        }
                        if (usedAt != 0 && mc.player.getInventory().countItem(Pipa.NOODLES.get()) == 1) ready("有碗时 Shift 点击完成交易");
                    }
                }
                case 10 -> {
                    if (actor && !respawned && mc.screen instanceof DeathScreen) {
                        respawned = true;
                        mc.player.respawn();
                    }
                    if (!actor || respawned && mc.player.isAlive() && mc.screen == null) ready("已观察到重生阶段");
                }
            }
        } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
    }

    private void consume(Minecraft mc) {
        var item = stage == 5 ? Pipa.DOG_MILK.get() : Pipa.NOODLES.get();
        if (usedAt == 0 && mc.player.getMainHandItem().is(item)) use(mc);
        if (usedAt != 0 && age - usedAt < 38) mc.options.keyUse.setDown(true);
        if (stage != 5 || !mc.player.hasEffect(Pipa.milk()) || !mc.player.getAbilities().mayfly) return;
        if (jumpAt == 0) jumpAt = age;
        int elapsed = age - jumpAt;
        mc.options.keyJump.setDown(elapsed == 1 || elapsed >= 6 && elapsed < 22);
    }

    private void tryTradeWithoutBowl(Minecraft mc) {
        if (!(mc.player.containerMenu instanceof MerchantMenu menu)) {
            if (usedAt != 0) return;
            var villager = mc.level.getEntitiesOfClass(Villager.class, mc.player.getBoundingBox().inflate(4),
                    v -> v.getName().getString().equals(PortCheck.TEST_TRADER)).stream().findFirst().orElse(null);
            if (villager != null) {
                mc.gameMode.interact(mc.player, villager, InteractionHand.MAIN_HAND);
                usedAt = age;
            }
            return;
        }
        if (tradeAt == 0 && !menu.getOffers().isEmpty()) {
            for (int i = 0; i < menu.getOffers().size(); i++) if (menu.getOffers().get(i).getResult().is(Pipa.NOODLES.get())) {
                menu.setSelectionHint(i);
                menu.tryMoveItems(i);
                mc.player.connection.send(new ServerboundSelectTradePacket(i));
                tradeAt = age;
                break;
            }
        }
        if (tradeAt == 0) return;
        if (age - tradeAt == 12) clickTrade(mc, menu, ClickType.PICKUP);
        if (age - tradeAt == 25) clickTrade(mc, menu, ClickType.QUICK_MOVE);
        if (age - tradeAt >= 40) ready("已向服务器发送无碗普通点击和 Shift 点击");
    }

    private void clickTrade(Minecraft mc, MerchantMenu menu, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(menu.containerId, 2, 0, type, mc.player);
    }

    private void use(Minecraft mc) {
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        usedAt = age;
    }

    private static void lookAt(LocalPlayer player, Vec3 target) {
        Vec3 delta = target.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
    }

    private void ready(String message) {
        if (!java.nio.file.Files.exists(PortCheck.ROOT.resolve(role + "-" + stage + ".ok")))
            PortCheck.write(role + "-" + stage + ".ok", "PASS " + message);
    }

    private void finish(Minecraft mc, String result) {
        done = true;
        PortCheck.write(role + "-result.txt", result);
        mc.stop();
    }
}
