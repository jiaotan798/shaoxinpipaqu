package com.shaoxinpipaqu;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;

/** 无坐标切歌包只在服务端主线程处理，校验和防重复仍由共用逻辑执行。 */
public final class Network {
    private static final String PROTOCOL = "1";
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Pipa.ID, "switch_song"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Network() {}
    private record SwitchSong() {}

    public static void register() {
        CHANNEL.registerMessage(0, SwitchSong.class, (message, buffer) -> {}, buffer -> new SwitchSong(),
                (message, supplied) -> {
                    var context = supplied.get();
                    var player = context.getSender();
                    if (player != null) context.enqueueWork(() -> RemoteControl.receive(player));
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void switchSong() { CHANNEL.sendToServer(new SwitchSong()); }
}
