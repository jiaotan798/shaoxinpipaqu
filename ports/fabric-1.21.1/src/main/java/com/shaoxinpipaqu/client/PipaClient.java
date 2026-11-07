package com.shaoxinpipaqu.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.shaoxinpipaqu.Network;
import com.shaoxinpipaqu.Pipa;
import com.shaoxinpipaqu.RemoteControl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** 在客户端按键边沿发出切歌请求，卡点在世界渲染末尾使用同一同步时钟。 */
public final class PipaClient implements ClientModInitializer {
    private boolean held;

    @Override public void onInitializeClient() {
        ClientConfig.load();
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(Pipa.id("meme_picture"), DefaultVertexFormat.POSITION_TEX_COLOR, MemeEffects::loadShader);
            MjTexture.reload();
        });
        WorldRenderEvents.LAST.register(context -> MemeEffects.pictures(context.tickCounter().getGameTimeDeltaPartialTick(false),
                context.camera(), context.positionMatrix(), context.projectionMatrix()));
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean down = mc.options.keyAttack.isDown();
            if (down && !held && mc.screen == null && mc.player != null && RemoteControl.held(mc.player))
                ClientPlayNetworking.send(new Network.SwitchSong());
            held = down;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> { FlightVisuals.tick(); MemeEffects.tick(); });
    }
}
