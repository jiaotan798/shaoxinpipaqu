package com.shaoxinpipaqu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 请求不含目标坐标，所有状态变更回到服务端线程并核验当前持物。 */
public final class Network {
    private Network() {}
    public record SwitchSong() implements CustomPacketPayload {
        public static final Type<SwitchSong> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Pipa.ID,"switch_song"));
        public static final StreamCodec<RegistryFriendlyByteBuf,SwitchSong> CODEC = StreamCodec.unit(new SwitchSong());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent e) {
        e.registrar("1").playToServer(SwitchSong.TYPE,SwitchSong.CODEC,(p,c) -> c.enqueueWork(() -> RemoteControl.receive(c.player())));
    }
}
