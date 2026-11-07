package com.shaoxinpipaqu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/** 无坐标切歌包只在服务端主线程处理，校验和防重复仍由共用逻辑执行。 */
public final class Network {
    public static final ResourceLocation SWITCH_SONG = new ResourceLocation(Pipa.ID, "switch_song");

    private Network() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SWITCH_SONG, (server, player, handler, buffer, sender) -> {
            if (!buffer.isReadable()) server.execute(() -> RemoteControl.receive(player));
        });
    }
}
