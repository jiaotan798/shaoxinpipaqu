package com.shaoxinpipaqu;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 切歌请求没有坐标；服务端以手持遥控器的绑定数据为准。 */
public final class Network {
    private Network() {}

    public record SwitchSong() implements CustomPacketPayload {
        public static final Type<SwitchSong> TYPE = new Type<>(Pipa.id("switch_song"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SwitchSong> CODEC = StreamCodec.unit(new SwitchSong());
        @Override public Type<SwitchSong> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SwitchSong.TYPE, SwitchSong.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SwitchSong.TYPE, (payload, context) -> RemoteControl.receive(context.player()));
    }
}
