package com.shaoxinpipaqu.client;

import com.shaoxinpipaqu.Network;
import com.shaoxinpipaqu.RemoteControl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** 客户端按下沿发出切歌请求；长按只触发一次。 */
public final class PipaClient implements ClientModInitializer {
    private boolean held;

    @Override public void onInitializeClient() {
        ClientConfig.load();
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            boolean down = mc.options.keyAttack.isDown();
            if (down && !held && mc.screen == null && mc.player != null && RemoteControl.held(mc.player))
                ClientPlayNetworking.send(new Network.SwitchSong());
            held = down;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> { FlightVisuals.tick(); MemeEffects.tick(); });
    }
}
