package com.shaoxinpipaqu.qa;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class PortQaClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(PortClientCheck.INSTANCE::tick);
    }
}
