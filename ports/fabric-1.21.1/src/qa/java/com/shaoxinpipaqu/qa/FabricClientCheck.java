package com.shaoxinpipaqu.qa;

import com.friendswine.DollBlockEntity;
import com.shaoxinpipaqu.MobFlight;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.RemoteControl;
import com.shaoxinpipaqu.client.MemeEffects;
import com.shaoxinpipaqu.client.MemeTiming;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** 两个客户端都须看到实际效果；只设置游戏按键映射，不注入桌面输入。 */
public final class FabricClientCheck implements ClientModInitializer {
    private final String role = System.getProperty("pipa.qa.role");
    private int stage, age, usedAt;
    private boolean done, captured;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.END.register(context -> {
            var mc = Minecraft.getInstance();
            if (done || stage != 2 || captured || mc.level == null) return;
            try {
                float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
                if (!(mc.level.getBlockEntity(FabricCheck.DOLL) instanceof DollBlockEntity doll)
                        || MemeEffects.active(doll, partial) != MemeTiming.Cue.MJ
                        || mc.level.getEntitiesOfClass(Pig.class, mc.player.getBoundingBox().inflate(20), MobFlight::posed).isEmpty()) return;
                // 首几帧字形正在弹出，等到完整显示再比较真实帧缓冲的字芯颜色。
                if (MemeTiming.phaseMs(doll.getElapsedTicks(mc.level.getGameTime()) + partial) < MemeTiming.Cue.MJ.startMs + 300) return;
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(FabricCheck.ROOT.resolve(role + "-mj.png"));
                    int colored = 0;
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                        int rgba = image.getPixelRGBA(x, y);
                        int rgb = (rgba & 255) << 16 | rgba & 0xFF00 | rgba >>> 16 & 255;
                        for (int i = 0; i < 6; i++) if (rgb == MemeTiming.mjColor(i)) { colored++; break; }
                    }
                    if (colored < 30) throw new AssertionError("MJ 时钟已触发但帧缓冲未见足够字芯像素：" + colored);
                }
                FabricCheck.write(role + "-2.ok", "PASS 选曲和生物姿势同步，截取 MJ 绘制帧");
                captured = true;
            } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
        });
    }

    private void tick(Minecraft mc) {
        if (done) return;
        try {
            long window = mc.getWindow().getWindow();
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE
                    || GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != GLFW.GLFW_FALSE
                    || mc.mouseHandler.isMouseGrabbed()) throw new AssertionError("后台窗口约束失效");
            if (mc.level == null || mc.player == null) return;
            if (mc.getSingleplayerServer() != null) throw new AssertionError("验收必须连接独立专服");
            var state = FabricCheck.read("stage.txt");
            if (state.isEmpty()) return;
            int next = Integer.parseInt(state);
            if (stage != next) { stage = next; age = 0; }
            if (stage == -1) throw new AssertionError("专服验收失败");
            if (stage == 99) { finish(mc, "PASS 独立专服、隐藏窗口与游戏状态同步"); return; }
            age++;
            boolean actor = role.equals("actor");
            if (stage <= 2) {
                Vec3 delta = new Vec3(0.5, 83.7, 0.5).subtract(mc.player.getEyePosition());
                mc.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
                mc.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
            }
            if (actor) mc.options.keyAttack.setDown((stage == 1 || stage == 2 && age <= 20)
                    && mc.screen == null && mc.getOverlay() == null && RemoteControl.held(mc.player));
            if (stage == 1 && actor && age % 20 == 0)
                FabricCheck.write("actor-remote.txt", "screen=" + mc.screen + " remote=" + RemoteControl.held(mc.player)
                        + " key=" + mc.options.keyAttack.isDown() + " position=" + mc.player.position()
                        + " item=" + mc.player.getMainHandItem().getComponents());
            if (stage == 3 && actor) {
                if (usedAt == 0 && mc.player.getMainHandItem().is(Pipa.DOG_MILK.get())) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    usedAt = age;
                }
                int elapsed = usedAt == 0 ? 0 : age - usedAt;
                mc.options.keyUse.setDown(usedAt != 0 && elapsed < 40);
                mc.options.keyJump.setDown(elapsed == 45 || elapsed >= 50 && elapsed < 60);
            }
            var subject = actor ? mc.player : mc.level.players().stream()
                    .filter(player -> player.getGameProfile().getName().equals("PipaActor")).findFirst().orElse(null);
            if (stage == 3 && subject != null && subject.hasEffect(Pipa.AIRBORNE))
                FabricCheck.write(role + "-3.ok", "PASS 狗奶飞行姿势同步");
            if (stage == 4 && actor) mc.options.keyJump.setDown(false);
            if (stage == 4 && subject != null && !subject.hasEffect(Pipa.AIRBORNE)) {
                FabricCheck.write(role + "-4.ok", "PASS 清理飞行姿势");
            }
        } catch (Throwable failure) { finish(mc, "FAIL " + failure); }
    }

    private void finish(Minecraft mc, String result) {
        done = true;
        FabricCheck.write(role + "-result.txt", result);
        mc.stop();
    }
}
