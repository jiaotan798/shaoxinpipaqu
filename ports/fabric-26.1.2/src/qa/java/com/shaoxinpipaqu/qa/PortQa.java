package com.shaoxinpipaqu.qa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class PortQa implements ModInitializer {
    @Override public void onInitialize() {
        var check=new PortCheck();
        ServerTickEvents.END_SERVER_TICK.register(check::tick);
    }
}
