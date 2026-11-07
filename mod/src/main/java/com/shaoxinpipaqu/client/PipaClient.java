package com.shaoxinpipaqu.client;

import com.shaoxinpipaqu.*;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 按键沿发送切歌，长按消费攻击事件但不重复切歌。 */
@Mod(value=Pipa.ID,dist=Dist.CLIENT)
public final class PipaClient {
    private boolean held;
    public PipaClient(IEventBus bus,ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT,ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class,ConfigurationScreen::new);
        bus.addListener((RegisterShadersEvent e) -> {
            try {
                e.registerShader(new ShaderInstance(e.getResourceProvider(),ResourceLocation.fromNamespaceAndPath(Pipa.ID,"meme_picture"),DefaultVertexFormat.POSITION_TEX_COLOR),MemeEffects::loadShader);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException("无法加载表情包着色器",failure);
            }
            MjTexture.reload();
        });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)
                MemeEffects.pictures(e.getPartialTick().getGameTimeDeltaPartialTick(false),e.getCamera(),e.getModelViewMatrix(),e.getProjectionMatrix());
        });
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {FlightVisuals.tick();MemeEffects.tick();});
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered e) -> {
            var p=Minecraft.getInstance().player;
            if (e.isAttack() && p!=null && RemoteControl.held(p)) { e.setCanceled(true); e.setSwingHand(false); }
        });
    }
    private void tick(ClientTickEvent.Pre e) {
        var mc=Minecraft.getInstance();
        boolean down=mc.options.keyAttack.isDown();
        if (down && !held && mc.screen==null && mc.player!=null && RemoteControl.held(mc.player)) PacketDistributor.sendToServer(new Network.SwitchSong());
        held=down;
    }
}
