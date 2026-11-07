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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.resources.ResourceLocation;

/** Fabric 客户端输入和世界渲染在同一刻度读取同步的歌曲状态。 */
public final class PipaClient implements ClientModInitializer {
    private boolean held;

    @Override public void onInitializeClient() {
        ClientConfig.load();
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(new ResourceLocation(Pipa.ID, "meme_picture"), DefaultVertexFormat.POSITION_TEX_COLOR, MemeEffects::loadShader);
            MjTexture.reload();
        });
        WorldRenderEvents.LAST.register(context -> MemeEffects.pictures(context.tickDelta(), context.camera(), context.matrixStack().last().pose(), context.projectionMatrix()));
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean down = mc.options.keyAttack.isDown();
            if (down && !held && mc.screen == null && mc.player != null && RemoteControl.held(mc.player)
                    && ClientPlayNetworking.canSend(Network.SWITCH_SONG)) ClientPlayNetworking.send(Network.SWITCH_SONG, PacketByteBufs.empty());
            held = down;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            FlightVisuals.tick();
            MemeEffects.tick();
        });
    }
}
