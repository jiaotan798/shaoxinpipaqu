package com.shaoxinpipaqu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.shaoxinpipaqu.Network;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.RemoteControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** 绑定 Forge 客户端输入、卡点渲染与本机配置界面。 */
public final class PipaClient {
    private boolean held;

    private PipaClient() {}

    public static void register(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ClientConfig::load));
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClientConfig.screen(parent)));
        bus.addListener((RegisterShadersEvent event) -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Pipa.ID, "meme_picture"),
                        DefaultVertexFormat.POSITION_TEX_COLOR), MemeEffects::loadShader);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException("无法加载表情包着色器", failure);
            }
            MjTexture.reload();
        });
        var events = MinecraftForge.EVENT_BUS;
        events.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                // Forge 1.20.1 的此事件把投影 PoseStack 作为参数；从原版保存的逆视图旋转恢复世界朝向。
                var modelView = new Matrix4f(new Matrix3f(RenderSystem.getInverseViewRotationMatrix()).invert());
                MemeEffects.pictures(event.getPartialTick(), event.getCamera(), modelView, event.getProjectionMatrix());
            }
        });
        events.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            var player = Minecraft.getInstance().player;
            if (event.isAttack() && player != null && RemoteControl.held(player)) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        });
        events.addListener(new PipaClient()::tick);
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FlightVisuals.tick();
            MemeEffects.tick();
            return;
        }
        var mc = Minecraft.getInstance();
        boolean down = mc.options.keyAttack.isDown();
        if (down && !held && mc.screen == null && mc.player != null && RemoteControl.held(mc.player)) Network.switchSong();
        held = down;
    }
}
